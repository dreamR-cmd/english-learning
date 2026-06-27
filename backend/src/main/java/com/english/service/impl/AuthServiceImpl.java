 package com.english.service.impl;
 
 import com.english.entity.User;
 import com.english.mapper.UserMapper;
 import com.english.service.AuthService;
 import org.springframework.stereotype.Service;
 
 @Service
 public class AuthServiceImpl implements AuthService {
     private final UserMapper userMapper;
 
     public AuthServiceImpl(UserMapper userMapper) {
         this.userMapper = userMapper;
     }
 
     @Override
     public User login(String username, String password) {
         User user = userMapper.findByUsername(username)
                 .orElseThrow(() -> new RuntimeException("用户不存在"));
         if (!user.getPassword().equals(password)) {
             throw new RuntimeException("密码错误");
         }
         return user;
     }
 
     @Override
     public User register(String username, String password, String nickname) {
         if (userMapper.findByUsername(username).isPresent()) {
             throw new RuntimeException("用户名已存在");
         }
         User user = new User(username, password, nickname == null ? username : nickname);
         return userMapper.save(user);
     }
 }
