package admin.mapper;


import admin.domain.dto.UpdateUserDTO;
import admin.domain.vo.AdminUserVO;
import admin.domain.vo.DetailUserVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import entity.pojo.info.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminUserMapper extends BaseMapper<User> {
    IPage<AdminUserVO> getUserList(@Param("page") Page<AdminUserVO> page,
                                   @Param("status")Integer status,
                                   @Param("key")String key);
    DetailUserVO getDetailUser(@Param("id")Integer id);

    void updateUser(UpdateUserDTO updateUserDTO);
}
