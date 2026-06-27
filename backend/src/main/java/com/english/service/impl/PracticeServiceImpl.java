 package com.english.service.impl;
 
 import com.english.entity.Listening;
 import com.english.entity.Reading;
 import com.english.entity.Word;
 import com.english.mapper.ListeningMapper;
 import com.english.mapper.ReadingMapper;
 import com.english.mapper.WordMapper;
 import com.english.service.PracticeService;
 import org.springframework.stereotype.Service;
 import java.util.List;
 
 @Service
 public class PracticeServiceImpl implements PracticeService {
     private final WordMapper wordMapper;
     private final ReadingMapper readingMapper;
     private final ListeningMapper listeningMapper;
 
     public PracticeServiceImpl(WordMapper wordMapper, ReadingMapper readingMapper, ListeningMapper listeningMapper) {
         this.wordMapper = wordMapper;
         this.readingMapper = readingMapper;
         this.listeningMapper = listeningMapper;
     }
 
     @Override
     public List<Word> getWordsByModule(String moduleCode) {
         return wordMapper.findByModuleCode(moduleCode);
     }
 
     @Override
     public List<Reading> getReadingsByModule(String moduleCode) {
         return readingMapper.findByModuleCode(moduleCode);
     }
 
     @Override
     public List<Reading> getFeaturedReadingsByModule(Long moduleId) {
         return readingMapper.findByModuleIdAndFeaturedTrue(moduleId);
     }
 
     @Override
     public List<Listening> getListeningsByModule(String moduleCode) {
         return listeningMapper.findByModuleCode(moduleCode);
     }
 }
