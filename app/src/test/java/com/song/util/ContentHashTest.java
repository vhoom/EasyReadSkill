package com.song.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentHashTest {

    @Test
    void sameTextIgnoresLineEndingsAndSurroundingSpace() {
        String a = "第一行\r\n第二行\r\n";
        String b = "第一行\n第二行";
        assertTrue(ContentHash.sameText(a, b));
        assertEquals(ContentHash.of(a), ContentHash.of(b));
    }

    @Test
    void differentTextGetsDifferentHash() {
        assertNotEquals(ContentHash.of("Hello"), ContentHash.of("Hello!"));
    }

    @Test
    void blankTextHasNoHash() {
        assertTrue(ContentHash.isBlank(ContentHash.of(null)));
        assertTrue(ContentHash.isBlank(ContentHash.of("   ")));
        assertFalse(ContentHash.isBlank(ContentHash.of("x")));
    }

    @Test
    void hashIsStableHexPrefixOfSha256() {
        String hash = ContentHash.of("Hello");
        assertEquals(32, hash.length());
        assertTrue(hash.matches("[0-9a-f]{32}"), hash);
        assertEquals(hash, ContentHash.of("Hello"));
    }
}
