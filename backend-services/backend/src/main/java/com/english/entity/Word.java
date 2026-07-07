package com.english.entity;
 
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
 
 @Entity
 @Table(name = "words")
 public class Word {
     @Id
     @GeneratedValue(strategy = GenerationType.IDENTITY)
     private Long id;
     @Column(nullable = false)
     private String word;
     private String phonetic;
     @Column(columnDefinition = "TEXT")
     private String meaning;
     @Column(columnDefinition = "TEXT")
     private String example;
     @ManyToOne(fetch = FetchType.LAZY)
     @JoinColumn(name = "module_id")
     @JsonIgnore
     private ExamModule module;
 
     public Word() {}
 
     public Long getId() { return id; }
     public void setId(Long id) { this.id = id; }
     public String getWord() { return word; }
     public void setWord(String word) { this.word = word; }
     public String getPhonetic() { return phonetic; }
     public void setPhonetic(String phonetic) { this.phonetic = phonetic; }
     public String getMeaning() { return meaning; }
     public void setMeaning(String meaning) { this.meaning = meaning; }
     public String getExample() { return example; }
     public void setExample(String example) { this.example = example; }
     public ExamModule getModule() { return module; }
     public void setModule(ExamModule module) { this.module = module; }
 }
