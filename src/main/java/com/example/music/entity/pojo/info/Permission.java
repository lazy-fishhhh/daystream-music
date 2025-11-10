package com.example.music.entity.pojo.info;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Permission {
    private Long id;                 // 权限ID
    private String permName;         // 权限名称
    private String permCode;         // 权限编码
    private Integer permType;        // 类型（1-菜单级别，2-按钮级别，3-接口级别）
}