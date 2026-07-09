package com.aislam.rag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "education_quizzes")
public class EducationQuizEntity {

  public static final String TYPE_MODULE = "MODULE";
  public static final String TYPE_CATEGORY = "CATEGORY";

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 80)
  private String slug;

  @Column(nullable = false, length = 160)
  private String title;

  @Column(nullable = false, length = 16)
  private String quizType;

  @Column(nullable = false, length = 80)
  private String scopeSlug;

  @Column(nullable = false)
  private int questionCount;

  @Column(nullable = false)
  private int passPercent;

  @Column(nullable = false, length = 80)
  private String achievementId;

  protected EducationQuizEntity() {}

  public EducationQuizEntity(
      String slug,
      String title,
      String quizType,
      String scopeSlug,
      int questionCount,
      int passPercent,
      String achievementId) {
    this.slug = slug;
    this.title = title;
    this.quizType = quizType;
    this.scopeSlug = scopeSlug;
    this.questionCount = questionCount;
    this.passPercent = passPercent;
    this.achievementId = achievementId;
  }

  public UUID getId() {
    return id;
  }

  public String getSlug() {
    return slug;
  }

  public String getTitle() {
    return title;
  }

  public String getQuizType() {
    return quizType;
  }

  public String getScopeSlug() {
    return scopeSlug;
  }

  public int getQuestionCount() {
    return questionCount;
  }

  public int getPassPercent() {
    return passPercent;
  }

  public String getAchievementId() {
    return achievementId;
  }
}
