package com.english.entity;
 
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
 
 @Entity
 @Table(name = "listenings")
 public class Listening {
     @Id
     @GeneratedValue(strategy = GenerationType.IDENTITY)
     private Long id;
     @Column(nullable = false)
     private String title;
     private String audioUrl;
     @Column(columnDefinition = "TEXT")
     private String transcript;
     @Column(columnDefinition = "TEXT")
     private String questions;
     @ManyToOne(fetch = FetchType.LAZY)
     @JoinColumn(name = "module_id")
     @JsonIgnore
     private ExamModule module;
 
     public Listening() {}
 
     public Long getId() { return id; }
     public void setId(Long id) { this.id = id; }
     public String getTitle() { return title; }
     public void setTitle(String title) { this.title = title; }
     public String getAudioUrl() { return audioUrl; }
     public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
     public String getTranscript() { return transcript; }
     public void setTranscript(String transcript) { this.transcript = transcript; }
     public String getQuestions() { return questions; }
     public void setQuestions(String questions) { this.questions = questions; }
     public ExamModule getModule() { return module; }
     public void setModule(ExamModule module) { this.module = module; }
 }
