package com.aislam.rag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "education_catalog_meta")
public class EducationCatalogMetaEntity {

  public static final short SINGLETON_ID = 1;

  @Id
  private Short id = SINGLETON_ID;

  @Column(nullable = false)
  private int version = 1;

  protected EducationCatalogMetaEntity() {}

  public EducationCatalogMetaEntity(int version) {
    this.version = version;
  }

  public Short getId() {
    return id;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }
}
