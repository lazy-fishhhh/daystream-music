package entity.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传DTO
 * 用于封装大文件分片上传的参数
 *
 * @author future
 */
@Data
public class FileUploadDTO {
    /**
     * 整体文件的MD5值
     */
    private String md5Value;

    /**
     * 当前上传分片在所有分片文件中的索引位置
     */
    private String chunk;

    /**
     * 整体文件总共被分了多少片
     */
    private String chunks;

    /**
     * 当前分片在整个数据文件中的开始位置
     */
    private String start;

    /**
     * 当前分片在整个数据文件中的结束位置
     */
    private String end;

    /**
     * 整体文件大小
     */
    private String size;

    /**
     * 分片文件的MD5值
     */
    private String chunkMd5;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 分片文件
     */
    private MultipartFile file;

    /**
     * 操作类型：upload（上传分片）、merge（合并分片）
     */
    private String operation;

}

