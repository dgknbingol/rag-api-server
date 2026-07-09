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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "education_topics")
public class EducationTopicEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 80)
  private String slug;

  @Column(nullable = false, length = 160)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String summary;

  @Column
  private Integer readingMinutes;

  @Column(nullable = false)
  private int sortOrder;

  @Column(nullable = false)
  private boolean contentReady = false;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "module_id", nullable = false)
  private EducationModuleEntity module;

  @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("sortOrder ASC")
  private List<EducationBlockEntity> blocks = new ArrayList<>();

  protected EducationTopicEntity() {}

  public EducationTopicEntity(
      String slug, String title, String summary, Integer readingMinutes, int sortOrder) {
    this.slug = slug;
    this.title = title;
    this.summary = summary;
    this.readingMinutes = readingMinutes;
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

  public Integer getReadingMinutes() {
    return readingMinutes;
  }

  public void setReadingMinutes(Integer readingMinutes) {
    this.readingMinutes = readingMinutes;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public EducationModuleEntity getModule() {
    return module;
  }

  public void setModule(EducationModuleEntity module) {
    this.module = module;
  }

  public List<EducationBlockEntity> getBlocks() {
    return blocks;
  }

  public void addBlock(EducationBlockEntity block) {
    blocks.add(block);
    block.setTopic(this);
  }

  public boolean hasContent() {
    return contentReady;
  }

  public void markContentReady() {
    this.contentReady = true;
  }
}
