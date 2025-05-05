package com.youngqi.tingshu.comment.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.youngqi.tingshu.comment.service.CommentService;
import com.youngqi.tingshu.common.util.MongoUtil;
import com.youngqi.tingshu.model.comment.Comment;
import com.youngqi.tingshu.user.client.UserFeignClient;
import com.youngqi.tingshu.vo.comment.CommentVo;
import com.youngqi.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

/**
 * @author YSQ
 * @PackageName:com.atguigu.tingshu.comment.service.impl
 * @className: CommentServiceImpl
 * @Description:
 * @date 2025/1/15 11:01
 */
@Slf4j
@Service
@SuppressWarnings({"all"})
public class CommentServiceImpl implements CommentService {
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private UserFeignClient userFeignClient;
    @Override
    public void save(Long userId, CommentVo commentVo) {
        Comment comment = BeanUtil.copyProperties(commentVo, Comment.class);
        UserInfoVo userInfoVo = userFeignClient.getUserInfoVoById(userId).getData();
        comment.setUserId(userId);
        comment.setNickname(userInfoVo.getNickname());
        comment.setAvatarUrl(userInfoVo.getAvatarUrl());
        String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.COMMENT, userId);
        mongoTemplate.save(comment,collectionName);
        Query query = new Query();

    }
}
