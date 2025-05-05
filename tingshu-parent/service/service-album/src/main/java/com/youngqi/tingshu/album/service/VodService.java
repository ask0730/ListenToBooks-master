package com.youngqi.tingshu.album.service;

import com.youngqi.tingshu.vo.album.TrackMediaInfoVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface VodService {

    Map<String, String> uploadTrack(MultipartFile file);
    TrackMediaInfoVo getTrackMediaInfo(String mediaFileId);

    void deleteTrackMedia(String oldMediaFileId);

}
