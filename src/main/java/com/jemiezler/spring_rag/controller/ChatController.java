package com.jemiezler.spring_rag.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jemiezler.spring_rag.service.RagService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/rag")
public class ChatController {

    private final RagService ragService;

    @GetMapping("/ask")
    public String ask(@RequestParam String query) {
        return ragService.ask(query);
    }

}