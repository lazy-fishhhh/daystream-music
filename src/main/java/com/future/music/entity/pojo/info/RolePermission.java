package com.future.music.entity.pojo.info;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolePermission {
    private Long id;                 // 主键ID
    private Long roleId;             // 角色ID
    private Long permId;             // 权限ID
}