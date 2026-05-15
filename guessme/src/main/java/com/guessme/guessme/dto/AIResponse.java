package com.guessme.guessme.dto;

public record AIResponse(
        String answer,
        boolean success,
        CharacterData character,
        // nullable: present in every response so the frontend can route subsequent requests
        String sessionId
) {}
