package com.guessme.guessme;

import com.guessme.guessme.model.AnswerVerdict;
import com.guessme.guessme.service.VerdictParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VerdictParserTest {

    private VerdictParser parser;

    @BeforeEach
    void setUp() {
        parser = new VerdictParser();
    }

    // ── YES ───────────────────────────────────────────────────────────────────

    @Test
    void parse_simExact_returnsYes() {
        assertEquals(AnswerVerdict.YES, parser.parse("Sim"));
    }

    @Test
    void parse_simLowercase_returnsYes() {
        assertEquals(AnswerVerdict.YES, parser.parse("sim"));
    }

    @Test
    void parse_simUppercase_returnsYes() {
        assertEquals(AnswerVerdict.YES, parser.parse("SIM"));
    }

    @Test
    void parse_simWithTrailingText_returnsYes() {
        assertEquals(AnswerVerdict.YES, parser.parse("Sim, o personagem tem poderes."));
    }

    @Test
    void parse_simWithLeadingWhitespace_returnsYes() {
        assertEquals(AnswerVerdict.YES, parser.parse("  Sim"));
    }

    @Test
    void parse_winResponse_returnsYes() {
        assertEquals(AnswerVerdict.YES, parser.parse("Sim! O personagem é Naruto Uzumaki.\nObra: Naruto"));
    }

    // ── NO ────────────────────────────────────────────────────────────────────

    @Test
    void parse_naoWithAccent_returnsNo() {
        assertEquals(AnswerVerdict.NO, parser.parse("Não"));
    }

    @Test
    void parse_naoLowercaseWithAccent_returnsNo() {
        assertEquals(AnswerVerdict.NO, parser.parse("não"));
    }

    @Test
    void parse_naoUppercaseWithAccent_returnsNo() {
        assertEquals(AnswerVerdict.NO, parser.parse("NÃO"));
    }

    @Test
    void parse_naoWithoutAccent_returnsNo() {
        assertEquals(AnswerVerdict.NO, parser.parse("Nao"));
    }

    @Test
    void parse_naoWithLeadingWhitespace_returnsNo() {
        assertEquals(AnswerVerdict.NO, parser.parse("   Não"));
    }

    @Test
    void parse_naoWithTrailingText_returnsNo() {
        assertEquals(AnswerVerdict.NO, parser.parse("Não, o personagem não é humano."));
    }

    // ── MAYBE ─────────────────────────────────────────────────────────────────

    @Test
    void parse_talvezExact_returnsMaybe() {
        assertEquals(AnswerVerdict.MAYBE, parser.parse("Talvez"));
    }

    @Test
    void parse_talvezLowercase_returnsMaybe() {
        assertEquals(AnswerVerdict.MAYBE, parser.parse("talvez"));
    }

    @Test
    void parse_talvezUppercase_returnsMaybe() {
        assertEquals(AnswerVerdict.MAYBE, parser.parse("TALVEZ"));
    }

    @Test
    void parse_talvezWithLeadingWhitespace_returnsMaybe() {
        assertEquals(AnswerVerdict.MAYBE, parser.parse("  Talvez"));
    }

    @Test
    void parse_talvezWithTrailingText_returnsMaybe() {
        assertEquals(AnswerVerdict.MAYBE, parser.parse("Talvez, depende da interpretação."));
    }

    // ── UNKNOWN ───────────────────────────────────────────────────────────────

    @Test
    void parse_midSentenceVerdict_returnsUnknown() {
        assertEquals(AnswerVerdict.UNKNOWN, parser.parse("A resposta é: Sim, mas depende."));
    }

    @Test
    void parse_provavelmenteSim_returnsUnknown() {
        assertEquals(AnswerVerdict.UNKNOWN, parser.parse("Provavelmente sim"));
    }

    @Test
    void parse_emptyString_returnsUnknown() {
        assertEquals(AnswerVerdict.UNKNOWN, parser.parse(""));
    }

    @Test
    void parse_blankString_returnsUnknown() {
        assertEquals(AnswerVerdict.UNKNOWN, parser.parse("   "));
    }

    @Test
    void parse_nullInput_returnsUnknown() {
        assertEquals(AnswerVerdict.UNKNOWN, parser.parse(null));
    }

    @Test
    void parse_genericErrorMessage_returnsUnknown() {
        assertEquals(AnswerVerdict.UNKNOWN, parser.parse("Sessão não encontrada. Inicie um novo jogo."));
    }
}
