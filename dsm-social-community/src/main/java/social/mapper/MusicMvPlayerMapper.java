package social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import entity.pojo.music.MusicMv;
import entity.vo.MusicMvVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MV相关的Mapper接口
 */
@Mapper
public interface MusicMvPlayerMapper extends BaseMapper<MusicMv> {

    /**
     * 根据MV ID查询MV详情
     * @param mvId MV ID
     * @return MV详情VO
     */
    MusicMvVO selectMvDetailById(@Param("mvId") Long mvId);

    /**
     * 批量查询MV详情
     * @param mvIds MV ID列表
     * @return MV详情VO列表
     */
    List<MusicMvVO> selectMvDetailByIds(@Param("mvIds") List<Long> mvIds);



    /**
     * 根据音乐ID查询对应的MV
     * @param musicId 音乐ID
     * @return MV详情VO
     */
    MusicMvVO selectMvByMusicId(@Param("musicId") Long musicId);

    /**
     * 更新MV播放量
     * @param mvId MV ID
     * @param increment 增加的播放量
     * @return 更新的记录数
     */
    int updatePlayCount(@Param("mvId") Long mvId, @Param("increment") Long increment);

    /**
     * 分页查询热门MV
     * @param page 分页参数
     * @return 分页的热门MV列表
     */
    IPage<MusicMvVO> selectHotMvsPage(IPage<MusicMvVO> page);

    /**
     * 分页查询最新MV
     * @param page 分页参数
     * @return 分页的最新MV列表
     */
    IPage<MusicMvVO> selectNewMvsPage(IPage<MusicMvVO> page);

    /**
     * 分页查询VIP专属MV
     * @param page 分页参数
     * @return 分页的VIP专属MV列表
     */
    IPage<MusicMvVO> selectVipMvsPage(IPage<MusicMvVO> page);

    /**
     * 增加MV点赞数
     * @param mvId MV ID
     * @return 更新的记录数
     */
    int likeCountUp(@Param("mvId") Long mvId);

    /**
     * 减少MV点赞数
     * @param mvId MV ID
     * @return 更新的记录数
     */
    int likeCountLow(@Param("mvId") Long mvId);

    /**
     * 增加MV收藏数
     * @param mvId MV ID
     * @return 更新的记录数
     */
    int collectCountUp(@Param("mvId") Long mvId);

    /**
     * 减少MV收藏数
     * @param mvId MV ID
     * @return 更新的记录数
     */
    int collectCountLow(@Param("mvId") Long mvId);

    /**
     * 查询MV点赞数
     * @param mvId MV ID
     * @return 点赞数
     */
    Long selectLikeCount(@Param("mvId") Long mvId);

    /**
     * 查询MV收藏数
     * @param mvId MV ID
     * @return 收藏数
     */
    Long selectCollectCount(@Param("mvId") Long mvId);
}
