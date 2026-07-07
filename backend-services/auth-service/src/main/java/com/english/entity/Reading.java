package com.english.entity;
 
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
 
 @Entity
 @Table(name = "readings")
 public class Reading {
     @Id
     @GeneratedValue(strategy = GenerationType.IDENTITY)
     private Long id;
     @Column(nullable = false)
     private String title;
     @Column(columnDefinition = "TEXT")
     private String content;
     @Column(columnDefinition = "TEXT")
     private String questions;
     @Column(name = "is_featured")
     private Boolean featured;
     @ManyToOne(fetch = FetchType.LAZY)
     @JoinColumn(name = "module_id")
     @JsonIgnore
     private ExamModule module;
 
     public Reading() {}
 
     public Long getId() { return id; }
     public void setId(Long id) { this.id = id; }
     public String getTitle() { return title; }
     public void setTitle(String title) { this.title = title; }
     public String getContent() { return content; }
     public void setContent(String content) { this.content = content; }
     public String getQuestions() { return questions; }
     public void setQuestions(String questions) { this.questions = questions; }
     public Boolean getFeatured() { return featured; }
     public void setFeatured(Boolean featured) { this.featured = featured; }
     public ExamModule getModule() { return module; }
     public void setModule(ExamModule module) { this.module = module; }
 }
