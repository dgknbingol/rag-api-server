package com.aislam.rag.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "education_modules")
public class EducationModuleEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 80)
  private String slug;

  @Column(nullable = false, length = 160)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String summary;

  @Column(nullable = false)
  private int sortOrder;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  private EducationCategoryEntity category;

  @BatchSize(size = 32)
  @OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("sortOrder ASC")
  private List<EducationTopicEntity> topics = new ArrayList<>();

  protected EducationModuleEntity() {}

  public EducationModuleEntity(String slug, String title, String summary, int sortOrder) {
    this.slug = slug;
    this.title = title;
    this.summary = summary;
    this.sortOrder = sortOrder;
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

  public String getSummary() {
    return summary;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public EducationCategoryEntity getCategory() {
    return category;
  }

  public List<EducationTopicEntity> getTopics() {
    return topics;
  }

  public void setCategory(EducationCategoryEntity category) {
    this.category = category;
  }

  public void addTopic(EducationTopicEntity topic) {
    topics.add(topic);
    topic.setModule(this);
  }
}
