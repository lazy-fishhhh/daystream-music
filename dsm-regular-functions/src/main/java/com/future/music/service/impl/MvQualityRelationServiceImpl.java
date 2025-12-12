package com.future.music.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.future.music.mapper.MvQualityRelationMapper;
import com.future.music.service.IMvQualityRelationService;
import entity.pojo.music.MvQualityRelation;
import org.springframework.stereotype.Service;

/**
 * MV画质关联服务实现类
 */
@Service
public class MvQualityRelationServiceImpl extends ServiceImpl<MvQualityRelationMapper, MvQualityRelation> implements IMvQualityRelationService {
}
