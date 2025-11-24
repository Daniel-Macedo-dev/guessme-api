package com.guessme.guessme.dto;

public record AIResponse(
        String text,
        boolean vitoria,
        CharacterData character
) {}
