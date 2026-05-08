package com.jemiezler.spring_rag.controller;


import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.jemiezler.spring_rag.dto.DocIngestionResponse;
import com.jemiezler.spring_rag.service.DocumentIngestionService;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentIngestionService ingestionService;

    @PostMapping(value = "/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocIngestionResponse> ingestDocument(@RequestParam("file") @NotNull MultipartFile file) {

        int chunks = ingestionService.ingestFile(file);

        return ResponseEntity.ok(DocIngestionResponse.builder()
                .source(file.getOriginalFilename())
                .chunksStored(chunks)
                .message("Successfully ingested %d chunks from '%s'"
                        .formatted(chunks, file.getOriginalFilename()))
                .build());
    }
}