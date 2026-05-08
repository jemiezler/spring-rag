package com.jemiezler.spring_rag.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jemiezler.spring_rag.exception.RagAppException;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    /**
     * Target tokens per chunk.
     * 400–800 is generally ideal for RAG systems.
     */
    private static final int CHUNK_SIZE = 500;

    /**
     * Very small chunks are usually noisy and low quality.
     */
    private static final int MIN_CHUNK_SIZE_CHARS = 350;

    /**
     * Prevent extremely large uploads during demo/tutorial usage.
     */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    private final VectorStore vectorStore;

    public int ingestFile(MultipartFile file) {
        validateFile(file);
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("unknown");
        String contentType = Optional.ofNullable(file.getContentType()).orElse("application/octet-stream");
        long fileSize = file.getSize();
        String documentId = UUID.randomUUID().toString();
        log.info("Starting document ingestion: filename={} contentType={} size={} bytes documentId={}"
                , filename, contentType, fileSize, documentId);
        try {
            Resource resource = toResource(file, filename);
            List<Document> documents = isPdf(filename, contentType)
                    ? readPdf(resource)
                    : readWithTika(resource);
            enrichMetadata(documents, documentId, filename, contentType, fileSize
            );
            return splitAndStore(documents, filename);
        } catch (IOException ex) {
            log.error("Failed to ingest file '{}'", filename, ex);
            throw RagAppException.internalError(
                    "Failed to ingest file: " + filename,
                    ex
            );
        }
    }

    private List<Document> readPdf(Resource resource) {

        var config = PdfDocumentReaderConfig.builder()
                .withPageExtractedTextFormatter(
                        ExtractedTextFormatter.builder()
                                .withNumberOfBottomTextLinesToDelete(3)
                                .withNumberOfTopPagesToSkipBeforeDelete(1)
                                .build())
                .withPagesPerDocument(1)
                .build();

        return new PagePdfDocumentReader(resource, config).get();
    }

    private List<Document> readWithTika(Resource resource) {
        return new TikaDocumentReader(resource).get();
    }

    private void enrichMetadata(List<Document> documents, String documentId, String filename, String contentType,
            long fileSize) {

        String ingestedAt = Instant.now().toString();
        documents.forEach(document -> {
            Map<String, Object> metadata = document.getMetadata();
            metadata.put("documentId", documentId);
            metadata.put("source", filename);
            metadata.put("contentType", contentType);
            metadata.put("fileSize", fileSize);
            metadata.put("ingestedAt", ingestedAt);
        });
    }


    private int splitAndStore(List<Document> documents, String source) {

        var splitter = TokenTextSplitter.builder()
                .withChunkSize(CHUNK_SIZE)
                .withMinChunkSizeChars(MIN_CHUNK_SIZE_CHARS)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(10_000)
                .withKeepSeparator(true)
                .build();

        List<Document> chunks = splitter.apply(documents);

        /*
         * Add chunk-level metadata.
         * Extremely useful for debugging and citations.
         */
        for (int i = 0; i < chunks.size(); i++) {

            Document chunk = chunks.get(i);

            chunk.getMetadata().put("chunkIndex", i);
        }

        vectorStore.add(chunks);

        log.info("Stored {} chunks from '{}'", chunks.size(), source);

        return chunks.size();
    }

    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw RagAppException.badRequest("Uploaded file is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw RagAppException.badRequest(
                    "File exceeds maximum allowed size of 10 MB"
            );
        }
    }

    private Resource toResource(MultipartFile file, String filename)
            throws IOException {

        return new ByteArrayResource(file.getBytes()) {

            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    private boolean isPdf(String filename, String contentType) {

        return filename.toLowerCase().endsWith(".pdf")
                || "application/pdf".equalsIgnoreCase(contentType);
    }
}