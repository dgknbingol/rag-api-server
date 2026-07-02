package com.aislam.rag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "quiz_options")
public class QuizOptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestionEntity question;

    @Column(nullable = false, length = 1)
    private String optionLabel;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String optionText;

    @Column(nullable = false)
    private boolean correct;

    @Column(nullable = false)
    private int sortOrder;

    protected QuizOptionEntity() {
    }

    public QuizOptionEntity(String optionLabel, String optionText, boolean correct, int sortOrder) {
        this.optionLabel = optionLabel;
        this.optionText = optionText;
        this.correct = correct;
        this.sortOrder = sortOrder;
    }

    public UUID getId() {
        return id;
    }

    public QuizQuestionEntity getQuestion() {
        return question;
    }

    public void setQuestion(QuizQuestionEntity question) {
        this.question = question;
    }

    public String getOptionLabel() {
        return optionLabel;
    }

    public String getOptionText() {
        return optionText;
    }

    public boolean isCorrect() {
        return correct;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
