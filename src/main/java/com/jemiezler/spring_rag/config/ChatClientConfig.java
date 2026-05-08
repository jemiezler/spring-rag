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
            You are a helpful AI assistant.
            Answer the user's question ONLY using the provided context.
            If the answer is not present in the context, say:
            "I could not find relevant information."
            Do not make up or assume information.
            """;

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

}