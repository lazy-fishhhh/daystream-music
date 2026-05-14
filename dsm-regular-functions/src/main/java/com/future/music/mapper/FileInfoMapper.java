package com.future.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.future.music.chunk.FileInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileInfoMapper extends BaseMapper<FileInfo> {
}
