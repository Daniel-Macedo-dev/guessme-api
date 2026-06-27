package com.guessme.guessme.service;

import com.guessme.guessme.model.AnswerVerdict;
import org.springframework.stereotype.Component;

import java.text.Normalizer;

@Component
public class VerdictParser {

    public AnswerVerdict parse(String text) {
        if (text == null) return AnswerVerdict.UNKNOWN;
        String normalized = normalize(text.trim());
        if (normalized.startsWith("sim")) return AnswerVerdict.YES;
        if (normalized.startsWith("nao")) return AnswerVerdict.NO;
        if (normalized.startsWith("talvez")) return AnswerVerdict.MAYBE;
        return AnswerVerdict.UNKNOWN;
    }

    private String normalize(String text) {
        String decomposed = Normalizer.normalize(text.toLowerCase(), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "");
    }
}
