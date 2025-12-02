package social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.info.UserRole;
import org.mapstruct.Mapper;

@Mapper
public interface User2RoleMapper extends BaseMapper<UserRole> {
}
