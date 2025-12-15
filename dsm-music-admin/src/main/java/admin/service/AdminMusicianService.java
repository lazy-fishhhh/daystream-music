package admin.service;

import admin.domain.dto.*;
import admin.domain.vo.*;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import org.springframework.stereotype.Service;

public interface AdminMusicianService {
    Result<PageResp<MusicianVO>> getMusicianList(MusiciansDTO musicianDTO);

    Result applyMusician(MusicianDTO musicianDTO);

    Result batchPass(AdminMusicianApplyDTO musiciansDTO);

    Result batchRefuse(AdminMusicianApplyDTO musiciansDTO);

    Result<String> deleteMusicianApply(AdminMusicianApplyDTO adminMusicianApplyDTO);
    Result<PageResp<MusicianListVO>> getMusician(MusicianListDTO musicianListDTO);

    Result<BanUserStatusVO> banMusician(String musicianId);

    Result<String> deleteMusician(String musicianId);

    Result<String> deleteBatch(DeletedMusicianDTO deletedMusicianDTO);

    Result<DetailMusicianVO> getDteailMusica(String id);

    Result<String> updateMusicianInfo(UpdateMusicianDTO updateMusicianDTO);
}
