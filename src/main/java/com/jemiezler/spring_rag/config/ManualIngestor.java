package com.jemiezler.spring_rag.config;

import java.util.List;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.ai.document.Document;
@Component
public class ManualIngestor implements CommandLineRunner {
    private final VectorStore vectorStore;

    public ManualIngestor(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }
    @Override
    public void run(String... args) {
        // Create a list of documents manually
        List<Document> documents = List.of(
            new Document("Spring AI provides a unified interface for interacting with different AI models."),
            new Document("Gemma 4 is a powerful local LLM that can be used for code reviews."),
            new Document("The Vers project uses Go and RAG to automate AI code reviews.")
        );

        // This converts text to 768-dim vectors using nomic-embed-text
        vectorStore.add(documents);
        System.out.println(">>> Database is no longer empty!");
    }
}
