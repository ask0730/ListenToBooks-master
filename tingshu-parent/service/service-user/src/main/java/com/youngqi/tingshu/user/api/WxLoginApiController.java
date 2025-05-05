package com.youngqi.tingshu.user.api;

import com.youngqi.tingshu.common.login.YoungQiLogin;
import com.youngqi.tingshu.common.result.Result;
import com.youngqi.tingshu.common.util.AuthContextHolder;
import com.youngqi.tingshu.user.service.UserInfoService;
import com.youngqi.tingshu.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "微信授权登录接口")
@RestController
@RequestMapping("/api/user/wxLogin")
@Slf4j
public class WxLoginApiController {

    @Autowired
    private UserInfoService userInfoService;


    @GetMapping("/wxLogin/{code}")
    public Result<Map<String,String>> wxLogin(@PathVariable String code){
           Map<String,String>map= userInfoService.wxLogin(code);
          return Result.ok(map);
    }

    /*
    * 获取登陆后的用户信息（必须登录才能访问）
    * */
    @YoungQiLogin
    @GetMapping("/getUserInfo")
    public Result<UserInfoVo> getUserInfo(){
        Long userId = AuthContextHolder.getUserId();
      UserInfoVo userInfoVo = userInfoService.getUserInfo(userId);
        return Result.ok(userInfoVo);
    }

    /*
    *
    * 修改用户信息
    *
    * */
    @YoungQiLogin
    @PostMapping("/updateUser")
    public Result updateUser(@RequestBody UserInfoVo userInfoVo){
        Long userId = AuthContextHolder.getUserId();
        userInfoVo.setId(userId);
        userInfoService.updateUser(userInfoVo);
        return Result.ok();
    }

}
