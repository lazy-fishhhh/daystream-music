package com.future.player.service;

import com.future.player.entity.dto.DisableAccountDTO;
import com.future.player.entity.dto.MusicianApplyDTO;
import com.future.player.entity.dto.MusicianListDTO;
import com.future.player.entity.dto.UpdateMusicianDTO;
import com.future.player.entity.vo.MusicianDetailVO;
import com.future.player.entity.vo.MusicianInfoVO;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface MusicianApplyService {
    Result<String> applyMusician(MusicianApplyDTO musicianApplyDTO);
    Result<Boolean> isMusician();

    Result<String> cancelApply();

    Result<String> deleteMusician(DisableAccountDTO disableAccountDTO);

    Result<MusicianInfoVO> getMuscianInfo();

    Result<PageResp<MusicianInfoVO>> getAllMusician(MusicianListDTO musicianListDTO);

    Result<String> updateMusicianInfo(UpdateMusicianDTO updateMusicianDTO);

    Result<MusicianDetailVO> getMusicianDetail(String musicianId);
}
