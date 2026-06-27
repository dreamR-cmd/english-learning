 package com.english.service;
 
 import com.english.entity.User;
 
 public interface AuthService {
     User login(String username, String password);
     User register(String username, String password, String nickname);
 }
