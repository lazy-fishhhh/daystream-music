package admin.service;

import admin.domain.dto.MusicianDTO;
import admin.domain.dto.MusiciansDTO;
import admin.domain.vo.MusicianVO;
import entity.pojo.util.PageResp;
import entity.pojo.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface AdminMusicianService {
    Result<PageResp<MusicianVO>> getMusicianList(MusiciansDTO musicianDTO);

    Result applyMusician(MusicianDTO musicianDTO);
}
