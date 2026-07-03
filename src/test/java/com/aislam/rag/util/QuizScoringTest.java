package com.aislam.rag.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuizScoringTest {

    @Test
    void wrongAnswerAlwaysZeroPoints() {
        assertEquals(0, QuizScoring.calculatePoints(false, 500));
        assertEquals(0, QuizScoring.calculatePoints(false, 0));
    }

    @Test
    void instantCorrectAnswerMaxPoints() {
        assertEquals(100, QuizScoring.calculatePoints(true, 0));
    }

    @Test
    void oneSecondCorrectAnswer() {
        assertEquals(95, QuizScoring.calculatePoints(true, 1_000));
    }

    @Test
    void fullTimeCorrectAnswerZeroPoints() {
        assertEquals(0, QuizScoring.calculatePoints(true, 20_000));
    }

    @Test
    void clampsNegativeAndOverflowResponseTime() {
        assertEquals(100, QuizScoring.calculatePoints(true, -500));
        assertEquals(0, QuizScoring.calculatePoints(true, 25_000));
    }
}
