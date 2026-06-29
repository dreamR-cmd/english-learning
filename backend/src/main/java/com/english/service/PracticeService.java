 package com.english.service;

 import com.english.dto.WordPracticeItem;
 import com.english.entity.Listening;
 import com.english.entity.Reading;

 import java.util.List;

 public interface PracticeService {
     List<WordPracticeItem> getWordsByModule(String moduleCode, Long userId);
     List<WordPracticeItem> getDailyWords(Long userId);
     List<Reading> getReadingsByModule(String moduleCode);
     List<Reading> getFeaturedReadingsByModule(Long moduleId);
     List<Listening> getListeningsByModule(String moduleCode);
 }
