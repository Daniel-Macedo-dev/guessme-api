package com.guessme.guessme.dto;

import com.guessme.guessme.model.AnswerVerdict;

public record AIResponse(
        String answer,
        boolean success,
        CharacterData character,
        // nullable: present in every response so the frontend can route subsequent requests
        String sessionId,
        AnswerVerdict verdict
) {}
