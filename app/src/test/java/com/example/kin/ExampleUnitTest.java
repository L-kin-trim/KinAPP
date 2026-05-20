package com.example.kin;

import com.example.kin.util.DisplayTimeFormatter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void displayTimeFormatter_formatsAbsoluteChineseDate() {
        assertEquals("2026年3月26日", DisplayTimeFormatter.formatDisplayTime("2026-03-26T21:11:03.247919"));
    }
}
