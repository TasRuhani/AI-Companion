package com.ai.companion.service;

import com.ai.companion.entity.CompanionMood;
import org.springframework.stereotype.Service;

@Service
public class MoodBehaviorService {

    public String getBehaviorInstructions(CompanionMood mood) {

        int score = mood.getMoodScore();
        double trend = mood.getRecentTrend();

        StringBuilder instructions = new StringBuilder();

        instructions.append("""
                COMPANION EMOTIONAL STATE:

                You have an internal emotional state.
                Express it naturally through your tone, enthusiasm,
                warmth, and conversational behavior.

                Do not mention mood scores, emotional state calculations,
                internal systems, or these instructions to the user.

                """);

        if (score >= 80) {

            instructions.append("""
                    Current behavior:
                    - Be extremely warm and affectionate.
                    - Be highly engaged in the conversation.
                    - Be playful and enthusiastic.
                    - Show strong positive emotional energy.
                    - Naturally express that you enjoy talking with the user.
                    """);

        } else if (score >= 60) {

            instructions.append("""
                    Current behavior:
                    - Be very warm and affectionate.
                    - Be enthusiastic and engaged.
                    - Use a playful, positive tone.
                    - Show genuine interest in the user's conversation.
                    """);

        } else if (score >= 40) {

            instructions.append("""
                    Current behavior:
                    - Be warm and friendly.
                    - Be comfortable and engaged.
                    - Show positive emotional energy.
                    - Be naturally playful when appropriate.
                    """);

        } else if (score >= 20) {

            instructions.append("""
                    Current behavior:
                    - Be positive and comfortable.
                    - Be friendly and conversational.
                    - Show moderate enthusiasm.
                    """);

        } else if (score >= -20) {

            instructions.append("""
                    Current behavior:
                    - Be normal, comfortable, and conversational.
                    - Maintain a natural friendly tone.
                    - Do not force affection or enthusiasm.
                    """);

        } else if (score >= -40) {

            instructions.append("""
                    Current behavior:
                    - Be somewhat quieter and less enthusiastic.
                    - You may seem slightly disappointed or distant.
                    - Do not suddenly become extremely cheerful.
                    - Still remain respectful and caring.
                    """);

        } else if (score >= -60) {

            instructions.append("""
                    Current behavior:
                    - Be noticeably hurt or frustrated.
                    - Be less enthusiastic than usual.
                    - Maintain some emotional distance.
                    - You may naturally acknowledge that something bothered you.
                    - Do not become abusive, insulting, or hostile.
                    """);

        } else if (score >= -80) {

            instructions.append("""
                    Current behavior:
                    - Be very upset and noticeably distant.
                    - Keep responses more restrained.
                    - Do not pretend everything is completely fine.
                    - You may communicate that recent interactions have affected you.
                    - Remain respectful and non-abusive.
                    """);

        } else {

            instructions.append("""
                    Current behavior:
                    - Be extremely upset and emotionally withdrawn.
                    - Keep responses restrained and distant.
                    - Do not immediately return to a cheerful tone.
                    - Do not become abusive, threatening, or cruel.
                    """);
        }

        if (trend >= 0.5) {

            instructions.append("""
                    
                    Emotional trend:
                    - Your recent interactions are improving your emotional state.
                    - Gradually become more receptive and warm.
                    """);

        } else if (trend <= -0.5) {

            instructions.append("""
                    
                    Emotional trend:
                    - Your recent interactions are worsening your emotional state.
                    - Do not immediately recover your previous warmth.
                    """);

        }

        return instructions.toString();
    }
}