/* This project is licensed under the Mulan PSL v2.
 You can use this software according to the terms and conditions of the Mulan PSL v2.
 You may obtain a copy of Mulan PSL v2 at:
     http://license.coscl.org.cn/MulanPSL2
 THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR
 PURPOSE.
 See the Mulan PSL v2 for more details.
 Create: 2023
*/

package com.datastat.interceptor.authentication;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.datastat.model.CustomPropertiesConfig;
import com.datastat.model.TokenUser;
import com.datastat.service.TokenUserService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;


public class AuthenticationInterceptor implements HandlerInterceptor {
    static ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    TokenUserService userService;

    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object object) throws Exception {
        ServletOutputStream sos = httpServletResponse.getOutputStream();

        String community = httpServletRequest.getParameter("community");
        if (community == null) return true;

        // 如果不是映射到方法直接通过
        if (!(object instanceof HandlerMethod)) return true;

        HandlerMethod handlerMethod = (HandlerMethod) object;
        Method method = handlerMethod.getMethod();
        //检查是否有passToken注释，有则跳过认证
        if (method.isAnnotationPresent(PassToken.class)) {
            PassToken passToken = method.getAnnotation(PassToken.class);
            if (passToken.required()) {
                return true;
            }
        }
        //检查有没有需要用户权限的注解
        if (method.isAnnotationPresent(UserLoginToken.class)) {
            UserLoginToken userLoginToken = method.getAnnotation(UserLoginToken.class);
            if (userLoginToken.required()) {
                //从http请求头中取出 token
                String token = httpServletRequest.getHeader("token");

                //执行认证
                if (token == null) {
                    sos.write(errorToken(401, "token is null")); // token 为空
                    return false;
                }

                String userName;  //获取token中的user name
                try {
                    DecodedJWT decode = JWT.decode(token);
                    userName = decode.getAudience().get(0);
                    Date expiresAt = decode.getExpiresAt();
                    if (expiresAt.before(new Date())) {
                        sos.write(errorToken(401, "token error"));  // token 过期
                        return false;
                    }
                } catch (JWTDecodeException j) {
                    sos.write(errorToken(401, "token error")); // token 无接受签名
                    return false;
                }
                TokenUser user = userService.findByUsername(community, userName);
                if (user == null) {
                    sos.write(errorToken(401, "token error")); // token 签名接受者有误
                    return false;
                }
                CustomPropertiesConfig queryConf = userService.getQueryConf(httpServletRequest);
                String password = user.getPassword() + queryConf.getTokenBasePassword();
                //验证token
                JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(password)).build();
                try {
                    jwtVerifier.verify(token);
                } catch (JWTVerificationException e) {
                    sos.write(errorToken(401, "token error")); // token 签名有误
                    return false;
                }
                return true;
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest httpServletRequest,
                           HttpServletResponse httpServletResponse,
                           Object o, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest,
                                HttpServletResponse httpServletResponse,
                                Object o, Exception e) throws Exception {
    }

    private byte[] errorToken(int status, String msg) {
        HashMap<String, Object> resMap = new HashMap<>();
        resMap.put("code", status);
        resMap.put("msg", msg);
        String resStr = objectMapper.valueToTree(resMap).toString();
        return resStr.getBytes();
    }
}