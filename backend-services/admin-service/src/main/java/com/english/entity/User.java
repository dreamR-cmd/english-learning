package com.english.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    public static final int DEFAULT_DAILY_WORD_TARGET = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String username;
    @Column(nullable = false)
    private String password;
    private String nickname;
    @Column(name = "daily_word_target")
    private Integer dailyWordTarget;
    @Column(name = "role_id")
    private Long roleId;

    public User() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public Integer getDailyWordTarget() {
        return dailyWordTarget == null ? DEFAULT_DAILY_WORD_TARGET : dailyWordTarget;
    }
    public void setDailyWordTarget(Integer dailyWordTarget) { this.dailyWordTarget = dailyWordTarget; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
}
