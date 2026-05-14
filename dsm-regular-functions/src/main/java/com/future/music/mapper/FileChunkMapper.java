package com.future.music.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.future.music.chunk.FileChunk;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileChunkMapper extends BaseMapper<FileChunk> {
}