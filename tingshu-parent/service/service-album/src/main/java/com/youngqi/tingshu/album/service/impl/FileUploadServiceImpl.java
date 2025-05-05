package com.youngqi.tingshu.album.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.youngqi.tingshu.album.config.MinioConstantProperties;
import com.youngqi.tingshu.album.service.FileUploadService;
import com.youngqi.tingshu.common.execption.GuiguException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * @author YSQ
 * @PackageName:com.atguigu.tingshu.album.service.impl
 * @className: FileUploadServiceImpl
 * @Description:
 * @date 2024/10/31 16:26
 */
@Slf4j
@Service
public class FileUploadServiceImpl implements FileUploadService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConstantProperties minioproperties;
    @Override
    public String imageUpload(MultipartFile file) {

        try {
            //校验是否为图片
            BufferedImage read = ImageIO.read(file.getInputStream());
            if (read==null){
                throw new GuiguException(400,"图片格式非法！");
            }
            String folderName="/"+ DateUtil.today()+"/";
            String fileName= IdUtil.randomUUID();
            String extName= FileUtil.extName(file.getOriginalFilename());
            String objectName = folderName + fileName + "." + extName;
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioproperties.getBucketName())
                    .object(objectName)
                    .stream(file.getInputStream(),file.getSize(),-1)
                    .contentType(file.getContentType())
                    .build());


            return minioproperties.getEndpointUrl()+"/"+minioproperties.getBucketName()+objectName;
        } catch (Exception e) {
            log.error("[专辑服务]上传图片文件异常：{}", e);
           throw new RuntimeException(e);
        }
    }
}
