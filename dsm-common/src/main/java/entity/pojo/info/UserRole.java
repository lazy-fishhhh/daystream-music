package entity.pojo.info;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
//@TableName("user_role")
//mybatis-plus 会自动将驼峰命名映射为下划线命名
public class UserRole {
    @TableId(type= IdType.AUTO)
    private Long id;                 // 主键ID
    private Long userId;             // 用户ID
    private Long roleId;             // 角色ID
    private LocalDateTime createdTime; // 创建时间
}