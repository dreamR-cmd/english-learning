 package com.english.controller;
 
 import com.english.dto.ApiResult;
 import com.english.dto.LoginRequest;
 import com.english.entity.User;
 import com.english.service.AuthService;
 import org.springframework.web.bind.annotation.*;
 
 @RestController
 @RequestMapping("/api/auth")
 public class AuthController {
     private final AuthService authService;
 
     public AuthController(AuthService authService) {
         this.authService = authService;
     }
 
     @PostMapping("/login")
     public ApiResult<User> login(@RequestBody LoginRequest request) {
         try {
             User user = authService.login(request.getUsername(), request.getPassword());
             // 不返回密码
             user.setPassword(null);
             return ApiResult.success("登录成功", user);
         } catch (RuntimeException e) {
             return ApiResult.error(401, e.getMessage());
         }
     }
 
     @PostMapping("/register")
     public ApiResult<User> register(@RequestBody LoginRequest request) {
         try {
             User user = authService.register(request.getUsername(), request.getPassword(), request.getUsername());
             user.setPassword(null);
             return ApiResult.success("注册成功", user);
         } catch (RuntimeException e) {
             return ApiResult.error(400, e.getMessage());
         }
     }
 }
