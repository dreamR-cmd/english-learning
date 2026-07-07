 package com.english.service.impl;

 import com.english.dto.WordPracticeItem;
 import com.english.entity.Listening;
 import com.english.entity.Reading;
 import com.english.entity.User;
 import com.english.entity.UserDailyWordAssignment;
 import com.english.entity.UserWordProgress;
 import com.english.entity.Word;
 import com.english.mapper.ListeningMapper;
 import com.english.mapper.ReadingMapper;
 import com.english.mapper.UserDailyWordAssignmentMapper;
 import com.english.mapper.UserMapper;
 import com.english.mapper.UserWordProgressMapper;
 import com.english.mapper.WordMapper;
 import com.english.service.PracticeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

 import java.time.LocalDate;
 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.HashMap;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Objects;
 import java.util.Set;
 import java.util.stream.Collectors;

 @Service
 public class PracticeServiceImpl implements PracticeService {
     private final WordMapper wordMapper;
     private final ReadingMapper readingMapper;
     private final ListeningMapper listeningMapper;
     private final UserMapper userMapper;
     private final UserWordProgressMapper userWordProgressMapper;
     private final UserDailyWordAssignmentMapper userDailyWordAssignmentMapper;

     public PracticeServiceImpl(WordMapper wordMapper,
                                ReadingMapper readingMapper,
                                ListeningMapper listeningMapper,
                                UserMapper userMapper,
                                UserWordProgressMapper userWordProgressMapper,
                                UserDailyWordAssignmentMapper userDailyWordAssignmentMapper) {
         this.wordMapper = wordMapper;
         this.readingMapper = readingMapper;
         this.listeningMapper = listeningMapper;
         this.userMapper = userMapper;
         this.userWordProgressMapper = userWordProgressMapper;
         this.userDailyWordAssignmentMapper = userDailyWordAssignmentMapper;
     }

     @Override
     public List<WordPracticeItem> getWordsByModule(String moduleCode, Long userId) {
         List<Word> words = wordMapper.findByModuleCode(moduleCode);
         Map<Long, UserWordProgress> progressMap = new HashMap<>();

         if (userId != null) {
             for (UserWordProgress progress : userWordProgressMapper.findByUserIdAndModuleCode(userId, moduleCode)) {
                 progressMap.put(progress.getWordId(), progress);
             }
         }

         return words.stream()
                 .map(word -> {
                     UserWordProgress progress = progressMap.get(word.getId());
                     if (progress != null && Boolean.TRUE.equals(progress.getReviewReady())) {
                         return null;
                     }
                     int knownCount = progress != null && progress.getKnownCount() != null
                             ? progress.getKnownCount()
                             : 0;
                     return WordPracticeItem.from(word, knownCount);
                 })
                 .filter(Objects::nonNull)
                 .toList();
     }

     @Override
     @Transactional
     public List<WordPracticeItem> getDailyWords(Long userId) {
         User user = userMapper.findById(userId)
                 .orElseThrow(() -> new RuntimeException("用户不存在"));
         LocalDate today = LocalDate.now();

         List<UserDailyWordAssignment> assignments =
                 userDailyWordAssignmentMapper.findByUserIdAndPracticeDateOrderByIdAsc(userId, today);
         if (assignments.isEmpty()) {
             assignments = createDailyAssignments(user, today);
         } else {
             assignments = alignDailyAssignments(user, today, assignments);
         }

         List<Long> wordIds = assignments.stream()
                 .map(UserDailyWordAssignment::getWordId)
                 .distinct()
                 .toList();
         if (wordIds.isEmpty()) {
             return List.of();
         }

         Map<Long, Word> wordMap = new LinkedHashMap<>();
         for (Word word : wordMapper.findAllById(wordIds)) {
             wordMap.put(word.getId(), word);
         }

         Map<Long, UserWordProgress> progressMap = new HashMap<>();
         for (UserWordProgress progress : userWordProgressMapper.findByUserIdAndWordIdIn(userId, wordIds)) {
             progressMap.put(progress.getWordId(), progress);
         }

         return assignments.stream()
                 .map(assignment -> {
                     Word word = wordMap.get(assignment.getWordId());
                     if (word == null) {
                         return null;
                     }

                     UserWordProgress progress = progressMap.get(word.getId());
                     if (progress != null && Boolean.TRUE.equals(progress.getReviewReady())) {
                         return null;
                     }

                     int knownCount = progress != null && progress.getKnownCount() != null
                             ? progress.getKnownCount()
                             : 0;
                     return WordPracticeItem.from(word, knownCount);
                 })
                 .filter(Objects::nonNull)
                 .toList();
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

     private List<UserDailyWordAssignment> createDailyAssignments(User user, LocalDate practiceDate) {
         Set<Long> reviewReadyWordIds = userWordProgressMapper.findByUserIdAndReviewReadyTrue(user.getId()).stream()
                 .map(UserWordProgress::getWordId)
                 .collect(Collectors.toSet());

         List<Word> eligibleWords = wordMapper.findAll().stream()
                 .filter(word -> !reviewReadyWordIds.contains(word.getId()))
                 .collect(Collectors.toCollection(ArrayList::new));

         if (eligibleWords.isEmpty()) {
             return List.of();
         }

         Collections.shuffle(eligibleWords);
         int limit = Math.min(user.getDailyWordTarget(), eligibleWords.size());
         List<UserDailyWordAssignment> assignments = new ArrayList<>();

         for (int i = 0; i < limit; i++) {
             Word word = eligibleWords.get(i);
             UserDailyWordAssignment assignment = new UserDailyWordAssignment();
             assignment.setUserId(user.getId());
             assignment.setWordId(word.getId());
             assignment.setPracticeDate(practiceDate);
             assignments.add(assignment);
         }

         return userDailyWordAssignmentMapper.saveAll(assignments);
     }

     private List<UserDailyWordAssignment> alignDailyAssignments(User user,
                                                                LocalDate practiceDate,
                                                                List<UserDailyWordAssignment> assignments) {
         int target = user.getDailyWordTarget();
         if (assignments.size() == target) {
             return assignments;
         }

         if (assignments.size() > target) {
             List<UserDailyWordAssignment> kept = new ArrayList<>(assignments.subList(0, target));
             List<Long> removedIds = assignments.subList(target, assignments.size()).stream()
                     .map(UserDailyWordAssignment::getId)
                     .toList();
             if (!removedIds.isEmpty()) {
                 userDailyWordAssignmentMapper.deleteByIdIn(removedIds);
             }
             return kept;
         }

         Set<Long> assignedWordIds = assignments.stream()
                 .map(UserDailyWordAssignment::getWordId)
                 .collect(Collectors.toSet());
         Set<Long> reviewReadyWordIds = userWordProgressMapper.findByUserIdAndReviewReadyTrue(user.getId()).stream()
                 .map(UserWordProgress::getWordId)
                 .collect(Collectors.toSet());

         List<Word> eligibleWords = wordMapper.findAll().stream()
                 .filter(word -> !assignedWordIds.contains(word.getId()))
                 .filter(word -> !reviewReadyWordIds.contains(word.getId()))
                 .collect(Collectors.toCollection(ArrayList::new));
         Collections.shuffle(eligibleWords);

         int missing = Math.min(target - assignments.size(), eligibleWords.size());
         List<UserDailyWordAssignment> additions = new ArrayList<>();
         for (int i = 0; i < missing; i++) {
             UserDailyWordAssignment assignment = new UserDailyWordAssignment();
             assignment.setUserId(user.getId());
             assignment.setWordId(eligibleWords.get(i).getId());
             assignment.setPracticeDate(practiceDate);
             additions.add(assignment);
         }

         List<UserDailyWordAssignment> nextAssignments = new ArrayList<>(assignments);
         nextAssignments.addAll(userDailyWordAssignmentMapper.saveAll(additions));
         return nextAssignments;
     }
 }
