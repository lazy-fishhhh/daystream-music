package admin.service;

import admin.domain.dto.AdminMusicianApplyDTO;
import admin.domain.dto.MusicianDTO;
import admin.domain.dto.MusicianListDTO;
import admin.domain.dto.MusiciansDTO;
import admin.domain.vo.MusicianListVO;
import admin.domain.vo.MusicianVO;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface AdminMusicianService {
    Result<PageResp<MusicianVO>> getMusicianList(MusiciansDTO musicianDTO);

    Result applyMusician(MusicianDTO musicianDTO);

    Result batchPass(AdminMusicianApplyDTO musiciansDTO);

    Result batchRefuse(AdminMusicianApplyDTO musiciansDTO);

    Result<String> deleteMusicianApply(AdminMusicianApplyDTO adminMusicianApplyDTO);
    Result<PageResp<MusicianListVO>> getMusician(MusicianListDTO musicianListDTO);
}
