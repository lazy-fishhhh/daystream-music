package social.service;

import entity.pojo.util.Result;
import org.springframework.stereotype.Service;
import social.domain.dto.MusicianApplyDTO;

@Service
public interface UserMusicianService {
    Result getUserInfo();

    Result applyMusician(MusicianApplyDTO musicianApplyDTO);
    Result isMusician();
}
