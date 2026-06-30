package com.aislam.rag.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationalQueryDetectorTest {

    private final ConversationalQueryDetector detector = new ConversationalQueryDetector();

    @Test
    void detectsExactGreetings() {
        assertTrue(detector.isConversational("Selam"));
        assertTrue(detector.isConversational("merhaba"));
    }

    @Test
    void doesNotTreatIslamicQuestionsAsGreetings() {
        assertFalse(detector.isConversational("İslam nedir"));
        assertFalse(detector.isConversational("İslam'ın şartları nelerdir?"));
        assertFalse(detector.isConversational("Namazın farzları"));
        assertFalse(detector.isConversational("İyi bir Müslüman nasıl olur?"));
    }
}
