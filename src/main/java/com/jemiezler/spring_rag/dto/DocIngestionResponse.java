package com.jemiezler.spring_rag.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class DocIngestionResponse {

    private String source;

    private int chunksStored;

    private String message;
}