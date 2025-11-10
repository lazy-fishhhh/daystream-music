package com.example.music.entity.info;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    private Long id;                 // 角色ID
    private String roleName;         // 角色名称
    private String roleCode;         // 角色编码
    private String description;      // 角色描述
}