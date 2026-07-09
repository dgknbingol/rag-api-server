package com.aislam.rag.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "education_categories")
public class EducationCategoryEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 80)
  private String slug;

  @Column(nullable = false, length = 120)
  private String title;

  @Column(nullable = false, length = 200)
  private String subtitle;

  @Column(nullable = false, length = 80)
  private String icon;

  @Column(nullable = false)
  private int sortOrder;

  @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("sortOrder ASC")
  private List<EducationModuleEntity> modules = new ArrayList<>();

  protected EducationCategoryEntity() {}

  public EducationCategoryEntity(
      String slug, String title, String subtitle, String icon, int sortOrder) {
    this.slug = slug;
    this.title = title;
    this.subtitle = subtitle;
    this.icon = icon;
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

  public String getSubtitle() {
    return subtitle;
  }

  public String getIcon() {
    return icon;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public List<EducationModuleEntity> getModules() {
    return modules;
  }

  public void addModule(EducationModuleEntity module) {
    modules.add(module);
    module.setCategory(this);
  }
}
