package com.song.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchGroupingTest {

    @Test
    void groupsRespectItemLimit() {
        List<List<Integer>> groups = SkillFileService.planGroups(
                List.of(0, 1, 2, 3, 4), new int[]{10, 10, 10, 10, 10}, 2, 0);
        assertEquals(3, groups.size());
        assertEquals(List.of(0, 1), groups.get(0));
        assertEquals(List.of(2, 3), groups.get(1));
        assertEquals(List.of(4), groups.get(2));
    }

    @Test
    void groupsRespectCharLimit() {
        List<List<Integer>> groups = SkillFileService.planGroups(
                List.of(0, 1, 2), new int[]{100, 100, 100}, 0, 250);
        assertEquals(2, groups.size());
        assertEquals(List.of(0, 1), groups.get(0));
        assertEquals(List.of(2), groups.get(1));
    }

    @Test
    void oversizedItemKeepsOwnGroup() {
        List<List<Integer>> groups = SkillFileService.planGroups(
                List.of(0, 1), new int[]{9000, 10}, 5, 100);
        assertEquals(2, groups.size());
        assertEquals(List.of(0), groups.get(0));
        assertEquals(List.of(1), groups.get(1));
    }

    @Test
    void emptyPendingProducesNoGroup() {
        assertTrue(SkillFileService.planGroups(List.of(), new int[0], 5, 100).isEmpty());
    }
}
