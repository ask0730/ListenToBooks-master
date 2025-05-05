package com.youngqi.tingshu.common.login;

import com.youngqi.tingshu.common.constant.RedisConstant;
import com.youngqi.tingshu.common.execption.GuiguException;
import com.youngqi.tingshu.common.result.ResultCodeEnum;
import com.youngqi.tingshu.common.util.AuthContextHolder;
;
import com.youngqi.tingshu.vo.user.UserInfoVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @author YSQ
 * @PackageName:com.atguigu.tingshu.common.login
 * @className: YoungQiAspect
 * @Description: 自定义注解的切面类
 * @date 2024/11/12 21:37
 */
@Slf4j
@Aspect
@Component
public class YoungQiAspect {
    /*
    *       环绕通知：对所有业务模块api包下使用定义注解方法进行增强，
    *
    *
    * */
    @Autowired
    private RedisTemplate redisTemplate;
    @Around("execution(* com.youngqi.tingshu.*.api.*.*(..)) && @annotation(youngQiLogin)")
    public Object aspectLogin(ProceedingJoinPoint pjp,YoungQiLogin youngQiLogin)throws Throwable{
          //获取前端提交的token
        //获取请求对象
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        //接口转为实现类
        ServletRequestAttributes servletRequestAttributes=(ServletRequestAttributes) requestAttributes;
        HttpServletRequest request = servletRequestAttributes.getRequest();

        String token = request.getHeader("token");
        //查询存到redis中的信息
        String loginKey= RedisConstant.USER_LOGIN_KEY_PREFIX+token;
        UserInfoVo userInfoVo =(UserInfoVo) redisTemplate.opsForValue().get(loginKey);
        //判断是否必须登录，若必须，当用户信息为空，抛异常
        if (youngQiLogin.requiredLogin() && userInfoVo==null){
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }
        //如果用户信息有值，存入ThreadLoacal
        if (userInfoVo!=null){
            AuthContextHolder.setUserId(userInfoVo.getId());
        }
        //调目标方法
        Object proceed = null;
        try {
            proceed = pjp.proceed();
        } catch (Throwable e) {
            e.printStackTrace();
        }finally {
            //防止目标方法有异常走不到移除userId
            log.info("移除--------userID" );
            AuthContextHolder.removeUserId();
        }


        //清理TLocal中的id

        return proceed;

    }
}
