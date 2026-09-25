package com.song.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillParserTest {

    @Test
    void ignoresDescriptionOutsideFrontMatter() {
        String content = """
                ---
                name: demo
                description: real text
                ---

                # Body
                description: do not touch this
                """;

        assertEquals("real text", SkillParser.extractDescription(content));

        String replaced = SkillParser.replaceDescription(content, "已翻译");
        assertEquals("已翻译", SkillParser.extractDescription(replaced));
        assertTrue(replaced.contains("description: do not touch this"));
        assertTrue(replaced.contains("description: 已翻译"));
    }

    @Test
    void ignoresNestedDescriptionInsideFrontMatter() {
        String content = """
                ---
                name: demo
                metadata:
                  description: nested
                description: top
                ---
                """;

        assertEquals("top", SkillParser.extractDescription(content));
        String replaced = SkillParser.replaceDescription(content, "new");
        assertTrue(replaced.contains("description: nested"));
        assertEquals("new", SkillParser.extractDescription(replaced));
    }

    @Test
    void quotesColonHashAndQuotesOnWriteBack() {
        String content = """
                ---
                description: plain
                ---
                keep: this
                """;

        String value = "a: b # \"q\"";
        String replaced = SkillParser.replaceDescription(content, value);

        assertTrue(replaced.contains("description: \"a: b # \\\"q\\\"\""), replaced);
        assertTrue(replaced.contains("\n---"), replaced);
        assertTrue(replaced.contains("keep: this"));
        assertEquals(value, SkillParser.extractDescription(replaced));
    }

    @Test
    void roundTripsQuotedAndSingleQuotedValues() {
        String doubled = """
                ---
                description: "say \\"hi\\" and C:\\\\temp"
                ---
                """;
        assertEquals("say \"hi\" and C:\\temp", SkillParser.extractDescription(doubled));

        String hashInsideEscape = """
                ---
                description: "say \\"hi\\" # keep"
                ---
                """;
        assertEquals("say \"hi\" # keep", SkillParser.extractDescription(hashInsideEscape));

        String single = """
                ---
                description: 'it''s fine'
                ---
                """;
        assertEquals("it's fine", SkillParser.extractDescription(single));
    }

    @Test
    void stripsPlainCommentButKeepsHashInsideQuotes() {
        String plain = """
                ---
                description: hello # note
                ---
                """;
        assertEquals("hello", SkillParser.extractDescription(plain));

        String quoted = """
                ---
                description: "hello # note"
                ---
                """;
        assertEquals("hello # note", SkillParser.extractDescription(quoted));
    }

    @Test
    void crlfFrontMatterAndNoFrontMatter() {
        String content = "---\r\ndescription: hello\r\n---\r\n\r\ndescription: body\r\n";
        assertEquals("hello", SkillParser.extractDescription(content));
        String replaced = SkillParser.replaceDescription(content, "a:b");
        assertEquals("a:b", SkillParser.extractDescription(replaced));
        assertTrue(replaced.contains("description: body"));

        assertNull(SkillParser.extractDescription("description: orphan\n"));
        assertFalse(SkillParser.hasDescription("description: orphan\n"));
    }

    @Test
    void foldedAndLiteralBlocksStayInsideFrontMatter() {
        String folded = """
                ---
                description: >
                  hello world
                ---
                description: later
                """;
        assertEquals("hello world", SkillParser.extractDescription(folded));

        String literal = """
                ---
                description: |
                  Hello world.
                  Next sentence.
                ---
                """;
        assertEquals("Hello world.\nNext sentence.", SkillParser.extractDescription(literal));
    }
}
