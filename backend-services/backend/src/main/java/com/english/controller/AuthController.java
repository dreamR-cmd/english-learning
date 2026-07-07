 package com.english.controller;
 
 import com.english.dto.ApiResult;
 import com.english.dto.LoginUserInfo;
 import com.english.dto.LoginRequest;
 import com.english.service.AuthService;
 import io.swagger.v3.oas.annotations.Operation;
 import io.swagger.v3.oas.annotations.tags.Tag;
 import org.springframework.web.bind.annotation.*;
 
 @Tag(name = "认证接口", description = "用户登录、注册相关接口")
 @RestController
 @RequestMapping("/api/auth")
 public class AuthController {
     private final AuthService authService;
 
     public AuthController(AuthService authService) {
         this.authService = authService;
     }
 
     @Operation(summary = "用户登录", description = "使用用户名和密码登录，成功后返回用户基础信息，响应中不会返回密码。")
     @PostMapping("/login")
     public ApiResult<LoginUserInfo> login(@RequestBody LoginRequest request) {
         try {
             LoginUserInfo user = authService.login(request.getUsername(), request.getPassword());
             return ApiResult.success("登录成功", user);
         } catch (RuntimeException e) {
             return ApiResult.error(401, e.getMessage());
         }
     }
 
     @Operation(summary = "用户注册", description = "使用用户名和密码创建新用户，默认昵称使用用户名。")
     @PostMapping("/register")
     public ApiResult<LoginUserInfo> register(@RequestBody LoginRequest request) {
         try {
             LoginUserInfo user = authService.register(request.getUsername(), request.getPassword(), request.getUsername());
             return ApiResult.success("注册成功", user);
         } catch (RuntimeException e) {
             return ApiResult.error(400, e.getMessage());
         }
     }
 }
