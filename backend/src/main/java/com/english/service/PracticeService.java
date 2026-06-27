 package com.english.service;
 
 import com.english.entity.Listening;
 import com.english.entity.Reading;
 import com.english.entity.Word;
 import java.util.List;
 
 public interface PracticeService {
     List<Word> getWordsByModule(String moduleCode);
     List<Reading> getReadingsByModule(String moduleCode);
     List<Reading> getFeaturedReadingsByModule(Long moduleId);
     List<Listening> getListeningsByModule(String moduleCode);
 }
