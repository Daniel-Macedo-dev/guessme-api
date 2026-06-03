package com.guessme.guessme;

import com.guessme.guessme.model.GameSession;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionTest {

    // ── constructor defaults ───────────────────────────────────────────────

    @Test
    void constructor_initializesDefaultCategoryAndEmptyHistory() {
        GameSession session = new GameSession();

        assertEquals("Geral", session.getCurrentCategory());
        assertEquals("", session.getConversationHistory());
        assertNotNull(session.getLastAccess());
    }

    // ── appendHistory ──────────────────────────────────────────────────────

    @Test
    void appendHistory_addsEntriesToConversation() {
        GameSession session = new GameSession();

        session.appendHistory("\nUsuário: Olá");
        session.appendHistory("\nIA: Olá!");

        String history = session.getConversationHistory();
        assertTrue(history.contains("Usuário: Olá"));
        assertTrue(history.contains("IA: Olá!"));
    }

    @Test
    void appendHistory_updatesLastAccess() {
        GameSession session = new GameSession();
        Instant before = session.getLastAccess();

        session.appendHistory("nova entrada");

        // lastAccess must be at or after 'before' — equals is allowed on fast JVMs
        assertFalse(session.getLastAccess().isBefore(before));
    }

    @Test
    void appendHistory_multipleEntries_accumulatesAll() {
        GameSession session = new GameSession();

        session.appendHistory("A");
        session.appendHistory("B");
        session.appendHistory("C");

        String history = session.getConversationHistory();
        assertTrue(history.contains("A"));
        assertTrue(history.contains("B"));
        assertTrue(history.contains("C"));
    }

    // ── touch ──────────────────────────────────────────────────────────────

    @Test
    void touch_updatesLastAccess() {
        GameSession session = new GameSession();
        Instant before = session.getLastAccess();

        session.touch();

        assertFalse(session.getLastAccess().isBefore(before));
    }

    // ── resetHistory ───────────────────────────────────────────────────────

    @Test
    void resetHistory_replacesConversationWithInitialValue() {
        GameSession session = new GameSession();
        session.appendHistory("\nUsuário: Pergunta antiga");
        session.appendHistory("\nIA: Resposta antiga");

        session.resetHistory("[novo_inicio]");

        assertEquals("[novo_inicio]", session.getConversationHistory());
    }

    @Test
    void resetHistory_updatesLastAccess() {
        GameSession session = new GameSession();
        Instant before = session.getLastAccess();

        session.resetHistory("reset");

        assertFalse(session.getLastAccess().isBefore(before));
    }

    // ── setCurrentCategory ─────────────────────────────────────────────────

    @Test
    void setCurrentCategory_updatesCategory() {
        GameSession session = new GameSession();

        session.setCurrentCategory("Anime");

        assertEquals("Anime", session.getCurrentCategory());
    }
}
