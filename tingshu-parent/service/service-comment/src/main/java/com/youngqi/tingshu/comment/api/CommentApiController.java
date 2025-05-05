package com.youngqi.tingshu.comment.api;

import com.youngqi.tingshu.comment.service.CommentService;
import com.youngqi.tingshu.common.login.YoungQiLogin;
import com.youngqi.tingshu.common.result.Result;
import com.youngqi.tingshu.common.util.AuthContextHolder;
import com.youngqi.tingshu.vo.comment.CommentVo;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author YSQ
 * @PackageName:com.atguigu.tingshu.album.api
 * @className: CommentApiController
 * @Description:
 * @date 2025/1/15 10:26
 */
@Tag(name = "评论接口")
@RestController
@RequestMapping("api/comment")
@SuppressWarnings({"all"})
public class CommentApiController {

    @Autowired
    private CommentService commentService;

    @YoungQiLogin(requiredLogin = true)
    @PostMapping("/save")
    public Result save(@RequestBody CommentVo commentVo){
        Long userId = AuthContextHolder.getUserId();
        commentService.save(userId,commentVo);
        return Result.ok();
    }


}
