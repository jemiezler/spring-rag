package com.jemiezler.spring_rag.config;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ChatClientConfig {

    private static final String SYSTEM_PROMPT = """
            You are a helpful AI assistant specialized in answering questions based on provided documents.
            Always use the context provided to answer user questions.
            Be concise and accurate in your responses.
            """;

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

}