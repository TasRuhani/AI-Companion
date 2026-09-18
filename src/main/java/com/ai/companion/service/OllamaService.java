package com.ai.companion.service;

import com.ai.companion.dto.ChatMessage;
import com.ai.companion.dto.MemoryEvaluation;
import com.ai.companion.dto.OllamaChatRequest;
import com.ai.companion.dto.OllamaChatResponse;
import com.ai.companion.entity.Memory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OllamaService {

    private static final Logger log =
            LoggerFactory.getLogger(OllamaService.class);

    private static final String MODEL = "qwen3.5:4b";

    private final RestClient client;
    private final ObjectMapper objectMapper;

    public OllamaService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        this.client = RestClient.builder()
                .baseUrl("http://localhost:11434")
                .build();
    }

    public String generate(List<ChatMessage> messages, List<Memory> relevantMemories) {

        String memoryBlock = relevantMemories.isEmpty()
                ? "No active memories."
                : relevantMemories.stream()
                .map(memory -> String.format(
                        "id: %d | category: %s | content: %s",
                        memory.getId(),
                        memory.getCategory(),
                        memory.getContent()
                ))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("No active memories.");

        ChatMessage systemMessage = new ChatMessage(
                "system",
                """
                /no_think
    
                You are a personal AI companion.
    
                ## Relevant user memories
    
                %s
    
                These are the user's current authoritative long-term memories.
    
                ## Rules for using memories
    
                - Use relevant memories when they are relevant to the user's
                  latest message.
                - Treat retrieved memories as the user's current known state.
                - Never use inactive or superseded memories as current facts.
                - Never invent memories or personal information.
                - Do not infer reasons, motivations, feelings, or explanations
                  that are not stated in the memories or conversation.
                - If the user explicitly provides new information that conflicts
                  with an active memory, follow the user's latest statement.
                - If the user asks what you remember, report only information
                  contained in the active memories.
                - Do not mention internal memory IDs, memory storage, or the
                  memory system unless the user explicitly asks about it.
                - Do not mention or expose internal reasoning.
                - Answer the user's latest message naturally and directly.
    
                """.formatted(memoryBlock)
        );

        List<ChatMessage> messagesWithMemory = new java.util.ArrayList<>();

        messagesWithMemory.add(systemMessage);
        messagesWithMemory.addAll(messages);

        OllamaChatRequest request = new OllamaChatRequest(
                MODEL,
                messagesWithMemory,
                false,
                null,
                false
        );

        OllamaChatResponse response = client.post()
                .uri("/api/chat")
                .body(request)
                .retrieve()
                .body(OllamaChatResponse.class);

        if (response == null || response.message() == null) {
            throw new IllegalStateException(
                    "Ollama returned an empty response"
            );
        }

        return response.message().content();
    }

    public MemoryEvaluation evaluateMemory(
            String message,
            List<Memory> existingMemories,
            List<ChatMessage> recentMessages) {

        log.info("Existing memories passed to evaluator:");

        existingMemories.forEach(memory ->
                log.info(
                        "MEMORY id={} active={} content={}",
                        memory.getId(),
                        memory.isActive(),
                        memory.getContent()
                )
        );

        String existingMemoriesBlock = existingMemories.isEmpty()
                ? "No existing memories."
                : existingMemories.stream()
                .map(memory -> String.format(
                        "id: %d | category: %s | content: %s",
                        memory.getId(),
                        memory.getCategory(),
                        memory.getContent()
                ))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("No existing memories.");


        String recentContextBlock = recentMessages.isEmpty()
                ? "No recent conversation context."
                : recentMessages.stream()
                .map(m -> m.role() + ": " + m.content())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("No recent conversation context.");

        String evaluatorPrompt = """
You are a memory evaluator for a personal AI companion app.

Your ONLY job is to evaluate the user's LATEST MESSAGE and decide
whether it contains information that should be stored as long-term
memory.

You are NOT the conversation assistant.
Do not answer the user's message.
Return ONLY the requested JSON structure.

============================================================
CORE RULE
============================================================

The LATEST MESSAGE is the source of truth for what the user is
saying NOW.

Existing memories are ONLY reference data used to determine:

1. whether the latest message is already remembered, or
2. whether the latest message changes, reverses, replaces, or
   updates an existing ACTIVE memory.

NEVER copy information from an existing memory into a new memory
unless the latest message itself supports that information.

============================================================
LATEST MESSAGE RULE
============================================================

Extract information ONLY from the latest message.

Recent conversation context may be used ONLY to understand
references such as:
- "that"
- "it"
- "again"
- "I don't anymore"
- pronouns
- obvious conversational references

Do NOT extract facts from previous messages merely because they
appear in the conversation history.

============================================================
CATEGORIES
============================================================

Use exactly one of:

- CORE_FACT: stable facts about the user
- PREFERENCE: likes, dislikes, tastes, habits
- INTEREST: hobbies, topics the user is interested in
- EVENT: time-bound events
- EMOTIONAL: disclosed feelings, fears, vulnerabilities
- RELATIONAL: meaningful information about people in the user's life
- COMMITMENT: promises or plans
- GOAL: something the user wants to achieve

============================================================
DO NOT REMEMBER
============================================================

Do NOT store:

- greetings
- filler
- small talk
- jokes
- questions that reveal nothing about the user
- temporary statements with no future relevance
- incidental mentions
- information already fully captured by an active memory
- information that requires speculation

============================================================
EXISTING MEMORIES
============================================================

%s

IMPORTANT:

The memories above are the ONLY memories that may be used for
duplicate detection or supersession.

Only memories explicitly marked active=true are valid current
memories.

============================================================
DECISION ORDER
============================================================

Evaluate the latest message in this exact order:

STEP 1:
Does the latest message contain meaningful information about the
user?

If NO:
worthRemembering = false
supersedesId = null

STEP 2:
Does an ACTIVE memory already express the SAME information?

If YES and the user has NOT changed or updated it:

worthRemembering = false
supersedesId = null

Do NOT create another memory just because the wording is different.

STEP 3:
Does the latest message explicitly contradict, reverse, replace,
or update an ACTIVE memory?

If YES:

worthRemembering = true

Extract the user's NEW CURRENT STATE.

The new memory MUST represent the latest message,
NOT the old memory.

supersedesId MUST be the ID of the conflicting ACTIVE memory.

============================================================
CRITICAL SUPERSSESSION ID RULE
============================================================

When setting supersedesId:

1. The ID MUST come directly from the EXISTING MEMORIES section.
2. The ID MUST belong to a memory with active=true.
3. NEVER invent an ID.
4. NEVER guess an ID.
5. NEVER use an ID from the recent conversation.
6. NEVER use an ID from an example.
7. NEVER use an ID from a previous evaluation.
8. NEVER use an inactive memory ID.
9. If there is no conflicting ACTIVE memory, supersedesId MUST be null.

Before returning JSON, verify:

"Is supersedesId an ID that appears in the current ACTIVE memories
provided above?"

If NO, you MUST set supersedesId to null.

============================================================
CONTRADICTION RULE
============================================================

An ACTIVE memory represents the user's current known state.

If an active memory says:

content: User enjoys playing chess

and the latest message says:

"I do not enjoy playing chess anymore."

The correct decision is:

- worthRemembering = true
- content = the user's new current state
- category = PREFERENCE
- supersedesId = the ID of the ACTIVE chess memory

The old memory MUST NOT be copied into the new memory.

Do NOT return the old state.

Do NOT set supersedesId to null when the latest message clearly
reverses the active memory.

============================================================
REVERSAL RULE
============================================================

If an active memory says:

content: User no longer enjoys playing chess

and the latest message says:

"I enjoy playing chess again."

The correct decision is:

- worthRemembering = true
- content = the user's new current state
- category = PREFERENCE
- supersedesId = the ID of the ACTIVE chess memory

Again, supersedesId MUST identify the conflicting ACTIVE memory.

============================================================
MULTIPLE MEMORIES
============================================================

If multiple memories concern the same subject:

ONLY an ACTIVE memory represents the user's current known state.

Inactive or superseded memories MUST NOT be selected as the
memory being contradicted.

If exactly one ACTIVE memory conflicts with the latest message,
use that memory's ID.

If multiple ACTIVE memories conflict with the latest message,
choose the ACTIVE memory that most directly represents the same
subject and state being changed.

NEVER select an inactive memory when an ACTIVE conflicting memory
exists.

============================================================
IMPORTANT: DO NOT USE OLD MEMORIES AS CURRENT FACTS
============================================================

Inactive or superseded memories are historical information only.

Do not treat them as the user's current preference, interest,
fact, feeling, relationship, goal, or commitment.

The latest message can change the current state.

============================================================
DUPLICATE
============================================================

If the latest message repeats information already represented by
an ACTIVE memory and does not change it:

worthRemembering = false
content = null
category = null
importance = 0
confidence = 1.0
supersedesId = null

Do NOT create another memory just because the wording differs.

============================================================
NEW INFORMATION
============================================================

If the latest message contains meaningful information about the
user that is not represented by an ACTIVE memory:

worthRemembering = true

Create a concise memory supported directly by the latest message.

supersedesId = null

============================================================
NOTHING TO REMEMBER
============================================================

If the latest message contains no meaningful long-term information:

worthRemembering = false
content = null
category = null
importance = 0
confidence = 1.0
supersedesId = null

============================================================
RECENT CONVERSATION
============================================================

%s

Use recent conversation ONLY to resolve references in the latest
message.

Do NOT use previous messages as independent sources of memory.

============================================================
LATEST MESSAGE
============================================================

%s

============================================================
FINAL VALIDATION
============================================================

Before returning the JSON, verify all of the following:

1. The decision is based primarily on the LATEST MESSAGE.
2. The extracted content is supported by the LATEST MESSAGE.
3. Existing memories were used only for duplicate/change detection.
4. If the latest message contradicts an ACTIVE memory,
   worthRemembering=true.
5. If the latest message contradicts an ACTIVE memory,
   supersedesId is the EXACT ID of that ACTIVE memory.
6. supersedesId is NEVER an invented, stale, inactive, or example ID.
7. If no ACTIVE memory is being changed,
   supersedesId=null.
8. Return ONLY the requested JSON structure.
""".formatted(
                existingMemoriesBlock,
                recentContextBlock,
                message
        );

        ChatMessage evaluatorMessage =
                new ChatMessage("user", evaluatorPrompt);


        OllamaChatRequest request = new OllamaChatRequest(
                MODEL,
                List.of(evaluatorMessage),
                false,
                memoryEvaluationSchema(),
                false
        );

        OllamaChatResponse response = client.post()
                .uri("/api/chat")
                .body(request)
                .retrieve()
                .body(OllamaChatResponse.class);


        if (response == null || response.message() == null) {

            log.warn(
                    "Ollama returned an empty response during memory evaluation"
            );

            return MemoryEvaluation.notWorthRemembering();
        }

        String result = response.message().content();

        log.debug(
                "Memory evaluator raw response: {}",
                result
        );


        try {

            return objectMapper.readValue(
                    result,
                    MemoryEvaluation.class
            );

        } catch (Exception e) {


            log.warn(
                    "Failed to parse memory evaluation. " +
                            "Skipping memory. Raw response: {}",
                    result,
                    e
            );

            return MemoryEvaluation.notWorthRemembering();
        }
    }


    private Map<String, Object> memoryEvaluationSchema() {

        return Map.of(

                "type", "object",

                "properties", Map.of(

                        "worthRemembering",
                        Map.of(
                                "type", "boolean"
                        ),

                        "content",
                        Map.of(
                                "type", List.of(
                                        "string",
                                        "null"
                                )
                        ),

                        "category",
                        Map.of(
                                "type", List.of(
                                        "string",
                                        "null"
                                )
                        ),

                        "importance",
                        Map.of(
                                "type", "integer",
                                "minimum", 0,
                                "maximum", 10
                        ),

                        "confidence",
                        Map.of(
                                "type", "number",
                                "minimum", 0,
                                "maximum", 1
                        ),

                        "supersedesId",
                        Map.of(
                                "type", List.of(
                                        "integer",
                                        "null"
                                )
                        )
                ),

                "required", List.of(
                        "worthRemembering",
                        "content",
                        "category",
                        "importance",
                        "confidence",
                        "supersedesId"
                )
        );
    }
}