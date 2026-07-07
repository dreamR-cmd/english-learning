 package com.english.service;
 
 import com.english.dto.LoginUserInfo;
 
 public interface AuthService {
     LoginUserInfo login(String username, String password);
     LoginUserInfo register(String username, String password, String nickname);
 }
