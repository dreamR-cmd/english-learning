package com.english.service.impl;

import com.english.entity.User;
import com.english.entity.UserFavorite;
import com.english.entity.UserWordProgress;
import com.english.entity.Word;
import com.english.entity.WrongRecord;
import com.english.mapper.UserFavoriteMapper;
import com.english.mapper.UserMapper;
import com.english.mapper.UserWordProgressMapper;
import com.english.mapper.WordMapper;
import com.english.mapper.WrongRecordMapper;
import com.english.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final WrongRecordMapper wrongRecordMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final UserWordProgressMapper userWordProgressMapper;
    private final WordMapper wordMapper;

    public UserServiceImpl(UserMapper userMapper,
                           WrongRecordMapper wrongRecordMapper,
                           UserFavoriteMapper userFavoriteMapper,
                           UserWordProgressMapper userWordProgressMapper,
                           WordMapper wordMapper) {
        this.userMapper = userMapper;
        this.wrongRecordMapper = wrongRecordMapper;
        this.userFavoriteMapper = userFavoriteMapper;
        this.userWordProgressMapper = userWordProgressMapper;
        this.wordMapper = wordMapper;
    }

    @Override
    public User updateProfile(Long userId, String nickname, Integer dailyWordTarget) {
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (nickname != null && !nickname.trim().isEmpty()) {
            user.setNickname(nickname.trim());
        }
        if (dailyWordTarget != null) {
            int normalized = Math.max(1, Math.min(100, dailyWordTarget));
            user.setDailyWordTarget(normalized);
        } else if (user.getDailyWordTarget() == null) {
            user.setDailyWordTarget(User.DEFAULT_DAILY_WORD_TARGET);
        }
        return userMapper.save(user);
    }

    @Override
    public WrongRecord saveWrongRecord(WrongRecord record) {
        return wrongRecordMapper.save(record);
    }

    @Override
    public List<WrongRecord> getWrongRecords(Long userId) {
        return wrongRecordMapper.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public void removeWrongRecord(Long userId, Long wrongRecordId) {
        wrongRecordMapper.deleteByIdAndUserId(wrongRecordId, userId);
    }

    @Override
    public UserFavorite addFavorite(Long userId, Long readingId) {
        if (userFavoriteMapper.findByUserIdAndReadingId(userId, readingId).isPresent()) {
            return null;
        }
        UserFavorite fav = new UserFavorite();
        fav.setUserId(userId);
        fav.setReadingId(readingId);
        return userFavoriteMapper.save(fav);
    }

    @Override
    public void removeFavorite(Long userId, Long readingId) {
        userFavoriteMapper.deleteByUserIdAndReadingId(userId, readingId);
    }

    @Override
    public List<UserFavorite> getFavorites(Long userId) {
        return userFavoriteMapper.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public boolean isFavorite(Long userId, Long readingId) {
        return userFavoriteMapper.findByUserIdAndReadingId(userId, readingId).isPresent();
    }

    @Override
    @Transactional
    public UserWordProgress markWordKnown(Long userId, Long wordId) {
        Word word = wordMapper.findById(wordId)
                .orElseThrow(() -> new RuntimeException("单词不存在"));

        UserWordProgress progress = userWordProgressMapper.findByUserIdAndWordId(userId, wordId)
                .orElseGet(() -> {
                    UserWordProgress created = new UserWordProgress();
                    created.setUserId(userId);
                    created.setWordId(wordId);
                    created.setKnownCount(0);
                    created.setReviewReady(false);
                    return created;
                });

        if (word.getModule() != null) {
            progress.setModuleCode(word.getModule().getCode());
        }

        int currentCount = progress.getKnownCount() == null ? 0 : progress.getKnownCount();
        int nextCount = Math.min(4, currentCount + 1);
        progress.setKnownCount(nextCount);
        progress.setReviewReady(nextCount >= 4);
        return userWordProgressMapper.save(progress);
    }

    @Override
    @Transactional
    public UserWordProgress resetWordProgress(Long userId, Long wordId) {
        UserWordProgress progress = userWordProgressMapper.findByUserIdAndWordId(userId, wordId)
                .orElse(null);
        if (progress == null) {
            return null;
        }

        progress.setKnownCount(0);
        progress.setReviewReady(false);
        return userWordProgressMapper.save(progress);
    }

    @Override
    public List<UserWordProgress> getReviewWords(Long userId) {
        return userWordProgressMapper.findByUserIdAndReviewReadyTrueOrderByUpdatedAtDesc(userId);
    }
}
