package com.future.player.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.player.entity.dto.UpdateMusicianDTO;
import entity.pojo.music.Musician;
import entity.pojo.music.SingerTagRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.future.player.entity.vo.MusicianInfoVO;

import java.util.List;


@Mapper
public interface PlayerMusicianMapper extends BaseMapper<Musician> {


    MusicianInfoVO selectMusicianInfo(Long userId);
    List<Long> selectMusicianTag(Long musicianId);
    /**
     * 搜索音乐人列表（支持按名称和标签搜索）
     */
    IPage<MusicianInfoVO> searchMusicians(@Param("name") String name,
                                          @Param("tagIds") List<Long> tagIds,
                                          @Param("page") Page<MusicianInfoVO> page);
    void updateMusicianInfo(UpdateMusicianDTO updateMusicianDTO);
    void insertTag(SingerTagRelation tagRelation);
    void deleteMusician(Long id);

}