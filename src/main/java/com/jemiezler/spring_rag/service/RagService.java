package com.jemiezler.spring_rag.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public String ask(String query) {

        log.info("Processing RAG query: {}", query);

        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(5)
                        .build()
        );

        if (documents == null || documents.isEmpty()) {
            return "No relevant information found.";
        }

        String context = documents.stream()
                .map(doc -> """
                        Source: %s
                        Content:
                        %s
                        """.formatted(
                        doc.getMetadata().get("source"),
                        doc.getText()
                ))
                .collect(Collectors.joining("\n\n"));

        String userPrompt = buildUserPrompt(context, query);

        return chatClient.prompt()
                .user(userPrompt)
                .call()
                .content();
    }

    private String buildUserPrompt(String context, String query) {

        PromptTemplate template = new PromptTemplate(
                new ClassPathResource("prompts/rag-user-prompt.st")
        );

        return template.render(Map.of(
                "context", context,
                "query", query
        ));
    }
}