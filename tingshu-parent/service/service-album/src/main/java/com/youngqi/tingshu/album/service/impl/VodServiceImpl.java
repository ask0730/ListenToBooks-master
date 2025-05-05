package com.youngqi.tingshu.album.service.impl;

import com.youngqi.tingshu.album.config.VodConstantProperties;
import com.youngqi.tingshu.album.service.VodService;
import com.youngqi.tingshu.common.util.UploadFileUtil;
import com.youngqi.tingshu.vo.album.TrackMediaInfoVo;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.vod.v20180717.VodClient;
import com.tencentcloudapi.vod.v20180717.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;


@Slf4j
@Service
public class VodServiceImpl implements VodService {

    @Autowired
    private VodConstantProperties vodConstantProperties;

    @Autowired
    private VodUploadClient vodUploadClient;

    // 注入认证对象
    @Autowired
    private Credential credential;
    //注入Client对象
    @Autowired
    private VodClient vodClient;

    @Override
    public Map<String, String> uploadTrack(MultipartFile file) {
        HashMap<String, String> resMap = new HashMap<>();
        try {

            //传文件到临时目录
            String tempPath = UploadFileUtil.uploadTempPath(vodConstantProperties.getTempPath(), file);
            //构造请求对象
            VodUploadRequest request = new VodUploadRequest();
            request.setMediaFilePath(tempPath);
            //上传声音文件
            VodUploadResponse vodUploadResponse = vodUploadClient.upload(vodConstantProperties.getRegion(), request);
            if (null !=vodUploadResponse){
                resMap.put("mediaFileId", vodUploadResponse.getFileId());
                resMap.put("mediaUrl", vodUploadResponse.getMediaUrl());
            }
        }catch (Exception e){
            log.error("[专辑服务]上传音频文件到点播平台异常：文件：{}，错误信息：{}", file, e);
            throw new RuntimeException(e);
        }
        return resMap;
    }

    @Override
    public TrackMediaInfoVo getTrackMediaInfo(String mediaFileId) {
        try {


        //创建请求对象
        DescribeMediaInfosRequest req = new DescribeMediaInfosRequest();
        //封装请求唯一标识
        req.setFileIds(new String[]{mediaFileId});
        //获取响应
        DescribeMediaInfosResponse describeMediaInfosResponse = vodClient.DescribeMediaInfos(req);
            MediaInfo[] mediaInfos = describeMediaInfosResponse.getMediaInfoSet();
            if(mediaInfos!=null&&mediaInfos.length>0){
                TrackMediaInfoVo trackMediaInfoVo = new TrackMediaInfoVo();
                MediaInfo mediaInfo = mediaInfos[0];
//                获取媒体文件基本信息对象
                MediaBasicInfo basicInfo = mediaInfo.getBasicInfo();
                trackMediaInfoVo.setType(basicInfo.getType());
                MediaMetaData metaData = mediaInfo.getMetaData();
                trackMediaInfoVo.setDuration(metaData.getDuration());
                trackMediaInfoVo.setSize(metaData.getSize());
                return trackMediaInfoVo;
            }
            //解析结果，拿到音频时常，大小，类型
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void deleteTrackMedia(String mediaFileId) {
        try {

        DeleteMediaRequest request = new DeleteMediaRequest();
        request.setFileId(mediaFileId);
        VodClient vodClient = new VodClient(credential,vodConstantProperties.getRegion());
            vodClient.DeleteMedia(request);
        } catch (TencentCloudSDKException e) {
            log.info("[专辑服务]删除云点播文件：{}，失败{}", mediaFileId, e);
        }
    }
}
