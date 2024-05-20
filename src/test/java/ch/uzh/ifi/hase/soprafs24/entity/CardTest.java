package ch.uzh.ifi.hase.soprafs24.entity;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("dev")
class CardTest {

    @Test
    void testMapCodeToInternalRepresentation_Defuse() {
        Card card = new Card();
        card.setCode("KS");
        card.beforeSave();
        assertEquals("defuse", card.getInternalCode());

        card.setCode("X1");
        card.beforeSave();
        assertEquals("defuse", card.getInternalCode());
    }

    @Test
    void testMapCodeToInternalRepresentation_Explosion() {
        Card card = new Card();
        card.setCode("AS");
        card.beforeSave();
        assertEquals("explosion", card.getInternalCode());

        card.setCode("AD");
        card.beforeSave();
        assertEquals("explosion", card.getInternalCode());
    }

    @Test
    void testMapCodeToInternalRepresentation_Attack() {
        Card card = new Card();
        card.setCode("JS");
        card.beforeSave();
        assertEquals("attack", card.getInternalCode());

        card.setCode("JH");
        card.beforeSave();
        assertEquals("attack", card.getInternalCode());
    }

    @Test
    void testMapCodeToInternalRepresentation_Skip() {
        Card card = new Card();
        card.setCode("0S");
        card.beforeSave();
        assertEquals("skip", card.getInternalCode());

        card.setCode("0H");
        card.beforeSave();
        assertEquals("skip", card.getInternalCode());
    }

    @Test
    void testMapCodeToInternalRepresentation_Future() {
        Card card = new Card();
        card.setCode("9S");
        card.beforeSave();
        assertEquals("future", card.getInternalCode());

        card.setCode("6D");
        card.beforeSave();
        assertEquals("future", card.getInternalCode());
    }

    @Test
    void testMapCodeToInternalRepresentation_Favor() {
        Card card = new Card();
        card.setCode("8S");
        card.beforeSave();
        assertEquals("favor", card.getInternalCode());

        card.setCode("8H");
        card.beforeSave();
        assertEquals("favor", card.getInternalCode());
    }

    @Test
    void testMapCodeToInternalRepresentation_Shuffle() {
        Card card = new Card();
        card.setCode("7S");
        card.beforeSave();
        assertEquals("shuffle", card.getInternalCode());

        card.setCode("7H");
        card.beforeSave();
        assertEquals("shuffle", card.getInternalCode());
    }

    @Test
    void testMapCodeToInternalRepresentation_Tacocat() {
        Card card = new Card();
        card.setCode("5S");
        card.beforeSave();
        assertEquals("tacocat", card.getInternalCode());

        card.setCode("5H");
        card.beforeSave();
        assertEquals("tacocat", card.getInternalCode());
    }

    @Test
    void testMapCodeToInternalRepresentation_Cattermelon() {
        Card card = new Card();
        card.setCode("4S");
        card.beforeSave();
        assertEquals("cattermelon", card.getInternalCode());

        card.setCode("4H");
        card.beforeSave();
        assertEquals("cattermelon", card.getInternalCode());
    }

    @Test
    void testMapCodeToInternalRepresentation_HairyPotatoCat() {
        Card card = new Card();
        card.setCode("3S");
        card.beforeSave();
        assertEquals("hairypotatocat", card.getInternalCode());

        card.setCode("3H");
        card.beforeSave();
        assertEquals("hairypotatocat", card.getInternalCode());
    }

    @Test
    void testMapCodeToInternalRepresentation_BeardCat() {
        Card card = new Card();
        card.setCode("2S");
        card.beforeSave();
        assertEquals("beardcat", card.getInternalCode());

        card.setCode("2H");
        card.beforeSave();
        assertEquals("beardcat", card.getInternalCode());
    }

    @Test
    void testMapCodeToInternalRepresentation_Lucky() {
        Card card = new Card();
        card.setCode("QS");
        card.beforeSave();
        assertEquals("lucky", card.getInternalCode());

        card.setCode("QH");
        card.beforeSave();
        assertEquals("lucky", card.getInternalCode());
    }

    @Test
    void testMapCodeToInternalRepresentation_Unknown() {
        Card card = new Card();
        card.setCode("unknownCode");
        card.beforeSave();
        assertEquals("unknown", card.getInternalCode());
    }
}

