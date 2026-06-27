 package com.english.entity;
 
 import jakarta.persistence.*;
 
 @Entity
 @Table(name = "exam_modules")
 public class ExamModule {
     @Id
     @GeneratedValue(strategy = GenerationType.IDENTITY)
     private Long id;
     @Column(nullable = false)
     private String name;
     @Column(nullable = false)
     private String code;
     private String description;
     private String icon;
     private Integer sortOrder;
 
     public ExamModule() {}
 
     public Long getId() { return id; }
     public void setId(Long id) { this.id = id; }
     public String getName() { return name; }
     public void setName(String name) { this.name = name; }
     public String getCode() { return code; }
     public void setCode(String code) { this.code = code; }
     public String getDescription() { return description; }
     public void setDescription(String description) { this.description = description; }
     public String getIcon() { return icon; }
     public void setIcon(String icon) { this.icon = icon; }
     public Integer getSortOrder() { return sortOrder; }
     public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
 }
