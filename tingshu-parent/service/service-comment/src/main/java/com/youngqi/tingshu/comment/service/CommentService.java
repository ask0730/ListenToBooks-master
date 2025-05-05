package com.youngqi.tingshu.comment.service;

import com.youngqi.tingshu.vo.comment.CommentVo;

public interface CommentService {
    void save(Long userId,CommentVo commentVo);

}
