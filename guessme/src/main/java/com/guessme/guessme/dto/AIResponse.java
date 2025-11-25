package com.guessme.guessme.dto;

public record AIResponse(
        String answer,
        boolean success,
        CharacterData character
) {}
