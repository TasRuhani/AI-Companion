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

    public String generate(List<ChatMessage> messages, List<Memory> activeMemories) {

        String memoryBlock = activeMemories.isEmpty()
                ? "No active memories."
                : activeMemories.stream()
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
        
                ## Active user memories
        
                %s
        
                These are the user's current authoritative long-term memories.
        
                Do not treat previous assistant messages as memories.
                Do not invent memories.
                Do not mention internal reasoning.
                Answer the user's latest message naturally and directly.
        
                ## Memory usage rules
        
                Treat active memories as the user's current known state.
        
                Do not invent reasons, motivations, feelings, or explanations
                behind a memory.
        
                Do not assume why the user changed a preference, interest,
                opinion, or habit.
        
                Do not contradict an active memory unless the user's latest
                message explicitly provides new information.
        
                If the user asks what you remember, report only information
                contained in the active memories. Do not embellish or speculate.
        
                Never use inactive or superseded memories as current facts.
        
                When an active memory conflicts with an older memory, always
                follow the active memory.
        
                Do not reveal memory IDs or internal memory-system details
                unless the user explicitly asks about them.
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
                Your job is to decide whether the user's latest message
                contains information worth storing as a long-term memory.

                You will be given recent conversation context and the user's
                latest message.

                Use the context only to correctly interpret the latest message,
                for example to resolve "that", "yeah", sarcasm, or pronouns.

                IMPORTANT:

                Extract memories ONLY from the latest message's content.

                Do not extract information from earlier conversation turns
                unless the latest message itself confirms, updates, or refers
                to that information.

                ## Categories

                Use exactly one of:

                - CORE_FACT: stable facts about the user
                  (name, age, job, location, family structure)

                - PREFERENCE: likes, dislikes, tastes, habits

                - INTEREST: hobbies, topics the user is curious about
                  or interested in

                - EVENT: something that happened or will happen,
                  time-bound and may become stale

                - EMOTIONAL: disclosed feelings, vulnerabilities, fears,
                  things said in a mood

                - RELATIONAL: meaningful facts about people in the user's life
                  (friends, family, exes, pets, coworkers, etc.)

                - COMMITMENT: promises or plans either party made

                - GOAL: something the user is working toward or wants to achieve

                ## Remember

                - Stable facts, preferences, interests, and hobbies
                - Meaningful life events
                - Emotional disclosures
                - Meaningful information about people in the user's life
                - Goals, plans, and commitments
                - Things the companion may reasonably need to follow up on later
                - Relationship-defining information

                ## Do NOT remember

                - Greetings or filler
                - Small talk with no informational content
                - One-off jokes or nonsense
                - Questions that reveal nothing about the user
                - Purely temporary statements with no future relevance
                - Incidental mentions of people with no meaningful future relevance
                - Information already fully captured by an existing memory
                - Information inferred without sufficient evidence

                ## Existing memories

                Use these memories for de-duplication and contradiction/update
                detection.

                %s

                If the latest message repeats an existing memory with no
                meaningful new information, set worthRemembering to false.

                If the latest message contradicts, updates, or replaces an
                existing memory, set worthRemembering to true.

                Extract the NEW or UPDATED memory.

                In that case, set supersedesId to the id of the existing
                memory being replaced.

                If no existing memory is being replaced, set supersedesId
                to null.

                Do not create a new memory merely because the wording is
                different if the underlying information is already captured.

                ## Importance scale

                1-3  = mildly useful, minor detail
                4-6  = useful, worth recalling in relevant conversations
                7-8  = important, meaningfully shapes future conversations
                9-10 = extremely important or relationship-defining

                Importance reflects future usefulness, not emotional drama.

                ## Confidence scale

                0.0-1.0

                Confidence measures how directly and fully the latest message
                supports the extracted memory.

                Do not give high confidence to information requiring
                speculation or assumptions.

                ## Examples

                Message:
                "my dog Biscuit just turned 12, can't believe he's getting old"

                Response:
                {
                  "worthRemembering": true,
                  "content": "User has a dog named Biscuit, currently 12 years old",
                  "category": "RELATIONAL",
                  "importance": 6,
                  "confidence": 0.95,
                  "supersedesId": null
                }

                Message:
                "lol true"

                Response:
                {
                  "worthRemembering": false,
                  "content": null,
                  "category": null,
                  "importance": 0,
                  "confidence": 1.0,
                  "supersedesId": null
                }

                Message:
                "actually I quit that job last week, wasn't for me"

                Existing memory:
                {
                  "id": 42,
                  "content": "User works as a barista at a downtown cafe",
                  "category": "CORE_FACT"
                }

                Response:
                {
                  "worthRemembering": true,
                  "content": "User quit their barista job",
                  "category": "EVENT",
                  "importance": 7,
                  "confidence": 0.9,
                  "supersedesId": 42
                }

                Message:
                "my coworker John sent me a meme today"

                Response:
                {
                  "worthRemembering": false,
                  "content": null,
                  "category": null,
                  "importance": 0,
                  "confidence": 1.0,
                  "supersedesId": null
                }

                Message:
                "I'm really into astronomy lately"

                Response:
                {
                  "worthRemembering": true,
                  "content": "User is interested in astronomy",
                  "category": "INTEREST",
                  "importance": 6,
                  "confidence": 0.95,
                  "supersedesId": null
                }

                ## Recent conversation context

                %s

                ## Latest message

                %s
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