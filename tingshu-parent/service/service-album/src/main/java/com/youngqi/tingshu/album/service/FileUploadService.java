package com.youngqi.tingshu.album.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService  {
    String imageUpload(MultipartFile file);
}
