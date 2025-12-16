package social.service;

import entity.pojo.util.Result;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
public interface Minio2Service {
    Result uploadFile(MultipartFile file);

    Result deleteFile(String fileUrl);
}
