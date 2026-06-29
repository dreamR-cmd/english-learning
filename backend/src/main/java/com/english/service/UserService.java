package com.english.service;

import com.english.entity.User;
import com.english.entity.UserFavorite;
import com.english.entity.WrongRecord;

import java.util.List;

public interface UserService {
    User updateProfile(Long userId, String nickname);
    WrongRecord saveWrongRecord(WrongRecord record);
    List<WrongRecord> getWrongRecords(Long userId);
    void removeWrongRecord(Long userId, Long wrongRecordId);
    UserFavorite addFavorite(Long userId, Long readingId);
    void removeFavorite(Long userId, Long readingId);
    List<UserFavorite> getFavorites(Long userId);
    boolean isFavorite(Long userId, Long readingId);
}
