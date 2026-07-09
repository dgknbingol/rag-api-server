package com.aislam.rag.entity;

import com.aislam.rag.entity.converter.StringListJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "education_blocks")
public class EducationBlockEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 32)
  private String type;

  @Column(columnDefinition = "TEXT")
  private String text;

  @Column(length = 200)
  private String title;

  @Column(length = 500)
  private String url;

  @Convert(converter = StringListJsonConverter.class)
  @Column(columnDefinition = "TEXT")
  private List<String> items = new ArrayList<>();

  @Column(nullable = false)
  private int sortOrder;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "topic_id", nullable = false)
  private EducationTopicEntity topic;

  protected EducationBlockEntity() {}

  public EducationBlockEntity(
      String type, String text, String title, String url, List<String> items, int sortOrder) {
    this.type = type;
    this.text = text;
    this.title = title;
    this.url = url;
    this.items = items == null ? new ArrayList<>() : items;
    this.sortOrder = sortOrder;
  }

  public UUID getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public String getText() {
    return text;
  }

  public String getTitle() {
    return title;
  }

  public String getUrl() {
    return url;
  }

  public List<String> getItems() {
    return items;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public EducationTopicEntity getTopic() {
    return topic;
  }

  public void setTopic(EducationTopicEntity topic) {
    this.topic = topic;
  }
}
