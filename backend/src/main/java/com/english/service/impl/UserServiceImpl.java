package com.english.service.impl;

import com.english.entity.User;
import com.english.entity.UserFavorite;
import com.english.entity.WrongRecord;
import com.english.mapper.UserFavoriteMapper;
import com.english.mapper.UserMapper;
import com.english.mapper.WrongRecordMapper;
import com.english.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final WrongRecordMapper wrongRecordMapper;
    private final UserFavoriteMapper userFavoriteMapper;

    public UserServiceImpl(UserMapper userMapper,
                           WrongRecordMapper wrongRecordMapper,
                           UserFavoriteMapper userFavoriteMapper) {
        this.userMapper = userMapper;
        this.wrongRecordMapper = wrongRecordMapper;
        this.userFavoriteMapper = userFavoriteMapper;
    }

    @Override
    public User updateProfile(Long userId, String nickname) {
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setNickname(nickname);
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
}
