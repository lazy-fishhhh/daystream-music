package com.example.music.entity.info;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRole {
    private Long id;                 // 主键ID
    private Long userId;             // 用户ID
    private Long roleId;             // 角色ID
    private LocalDateTime createdTime; // 创建时间
}