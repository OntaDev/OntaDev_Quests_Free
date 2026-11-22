// OntaDev_Quests Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.quests.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ArgsTest {

    @Test
    void aSingleNumberIsARangeOfOne() {
        assertArrayEquals(new int[]{7, 7}, Args.range("7"));
    }

    @Test
    void fromToIsInclusive() {
        assertArrayEquals(new int[]{5, 12}, Args.range("5-12"));
    }

    @Test
    void garbageReversedAndNonPositiveRangesAreRejected() {
        assertNull(Args.range("abc"));
        assertNull(Args.range("12-5"));
        assertNull(Args.range("0-3"));
        assertNull(Args.range("3-"));
        assertNull(Args.range("-3"));
    }
}
