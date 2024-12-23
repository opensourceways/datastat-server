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

package com.datastat.interceptor.oneid;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPrivateKey;
import java.util.*;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.datastat.config.context.QueryConfContext;
import com.datastat.dao.RedisDao;
import com.datastat.model.CustomPropertiesConfig;
import com.datastat.util.HttpClientUtils;
import com.datastat.util.ObjectMapperUtil;
import com.datastat.util.RSAUtil;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.util.DigestUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class OneidInterceptor implements HandlerInterceptor {
    @Autowired
    private Environment env;

    @Autowired
    QueryConfContext queryConfContext;

    @Autowired
    RedisDao redisDao;

    @Value("${cookie.token.name:default}")
    private String cookieTokenName;

    @Value("${cookie.token.domains:default}")
    private String allowDomains;

    @Value("${cookie.token.secures:default}")
    private String cookieSecures;

    private static HashMap<String, Boolean> domain2secure;
    private static final Logger logger = LoggerFactory.getLogger(OneidInterceptor.class);

    @PostConstruct
    public void init() {
        domain2secure = HttpClientUtils.getConfigCookieInfo(allowDomains, cookieSecures);
    }

    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object object) throws Exception {
        // 如果不是映射到方法直接通过
        if (!(object instanceof HandlerMethod)) {
            return true;
        }

        // 检查有没有需要用户权限的注解，仅拦截AuthingToken和AuthingUserToken
        HandlerMethod handlerMethod = (HandlerMethod) object;
        Method method = handlerMethod.getMethod();
        if (!method.isAnnotationPresent(OneidToken.class) && !method.isAnnotationPresent(OneidUserToken.class)
                && !method.isAnnotationPresent(CompanyToken.class) && !method.isAnnotationPresent(SigToken.class)) {
            return true;
        }
        OneidToken userLoginToken = method.getAnnotation(OneidToken.class);
        OneidUserToken authingUserToken = method.getAnnotation(OneidUserToken.class);
        SigToken sigToken = method.getAnnotation(SigToken.class);
        CompanyToken companyToken = method.getAnnotation(CompanyToken.class);
        if ((userLoginToken == null || !userLoginToken.required())
                && (authingUserToken == null || !authingUserToken.required())
                && (sigToken == null || !sigToken.required())
                && (companyToken == null || !companyToken.required())) {
            return true;
        }
        CustomPropertiesConfig queryConf = getQueryConf(httpServletRequest);

        // 从请求头中取出 token
        String headerToken = httpServletRequest.getHeader("token");
        String verifyHeaderMsg = verifyHeaderToken(headerToken, queryConf);
        if (!verifyHeaderMsg.equals("success")) {
            tokenError(httpServletRequest, httpServletResponse, "unauthorized");
            return false;
        }

        // 校验domain
        String verifyDomainMsg = verifyDomain(httpServletRequest);
        if (!verifyDomainMsg.equals("success")) {
            tokenError(httpServletRequest, httpServletResponse, verifyDomainMsg);
            return false;
        }

        // 校验cookie
        Cookie tokenCookie = verifyCookie(httpServletRequest);
        if (tokenCookie == null) {
            tokenError(httpServletRequest, httpServletResponse, "unauthorized");
            return false;
        }

        // 解密cookie中加密的token
        String token = tokenCookie.getValue();
        try {
            String authPrivateKey = queryConf.getRsaAuthPrivateKey();
            RSAPrivateKey privateKey = RSAUtil.getPrivateKey(authPrivateKey);
            token = RSAUtil.privateDecrypt(token, privateKey);
        } catch (Exception e) {
            logger.error("decode token in cookie exception - {}", e.getMessage());
            tokenError(httpServletRequest, httpServletResponse, "unauthorized");
            return false;
        }

        // 解析token
        String userId;
        Date issuedAt;
        Date expiresAt;
        String permission;
        String verifyToken;
        try {
            DecodedJWT decode = JWT.decode(token);
            userId = decode.getAudience().get(0);
            issuedAt = decode.getIssuedAt();
            expiresAt = decode.getExpiresAt();
            String permissionTemp = decode.getClaim("permission").asString();
            permission = new String(Base64.getDecoder().decode(permissionTemp.getBytes()));
            verifyToken = decode.getClaim("verifyToken").asString();
        } catch (JWTDecodeException j) {
            logger.error("parse token exception - {}", j.getMessage());
            tokenError(httpServletRequest, httpServletResponse, "unauthorized");
            return false;
        }

        // 校验token
        String verifyTokenMsg = verifyCookieToken(headerToken, token, verifyToken, userId, issuedAt, expiresAt,
                permission, queryConf);
        if (!verifyTokenMsg.equals("success")) {
            tokenError(httpServletRequest, httpServletResponse, verifyTokenMsg);
            return false;
        }

        // 校验sig权限
        if (sigToken != null && sigToken.required()) {
            String verifyUserMsg = verifyUser(sigToken, httpServletRequest, tokenCookie);
            if (!verifyUserMsg.equals("success")) {
                tokenError(httpServletRequest, httpServletResponse, verifyUserMsg);
                return false;
            }
        }

        // 校验company权限
        if (companyToken != null && companyToken.required()) {
            String verifyCompanyPerMsg = verifyCompanyPer(companyToken, httpServletRequest, tokenCookie);
            if (!verifyCompanyPerMsg.equals("success")) {
                tokenError(httpServletRequest, httpServletResponse, verifyCompanyPerMsg);
                return false;
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

    private String verifyCookieToken(String headerToken, String token, String verifyToken,
            String userId, Date issuedAt, Date expiresAt, String permission, CustomPropertiesConfig queryConf) {
        try {
            // 服务端校验headerToken是否有效
            String shaToken = RSAUtil.encryptSha256(headerToken, queryConf.getAuthSalt());
            String md5Token = DigestUtils.md5DigestAsHex(headerToken.getBytes(StandardCharsets.UTF_8));

            // header中的token和cookie中的token不一样
            if (!shaToken.equals(verifyToken) && !md5Token.equals(verifyToken)) {
                return "unauthorized";
            }

            // token 是否过期
            if (expiresAt.before(new Date())) {
                return "token expires";
            }

            // token 签名密码验证
            String password = permission + queryConf.getAuthTokenSessionPassword();
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(password)).build();
            jwtVerifier.verify(token);

        } catch (Exception e) {
            logger.error("verify token exception - {}", e.getMessage());
            return "unauthorized";
        }
        return "success";
    }

    /**
     * 校验header中的token
     *
     * @param headerToken header中的token
     * @return 返回校验信息
     */
    private String verifyHeaderToken(String headerToken, CustomPropertiesConfig queryConf) {
        try {
            if (StringUtils.isBlank(headerToken)) {
                return "unauthorized";
            }

            // token 签名密码验证
            String password = queryConf.getAuthTokenBasePassword();
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(password)).build();
            jwtVerifier.verify(headerToken);
            return "success";
        } catch (Exception e) {
            logger.error("verify Header Token exception - {}", e.getMessage());
            return "unauthorized";
        }
    }

    /**
     * 校验用户sig操作权限
     */
    private String verifyUser(SigToken sigToken, HttpServletRequest httpServletRequest, Cookie tokenCookie) {
        CustomPropertiesConfig queryConf = getQueryConf(httpServletRequest);
        try {
            if (sigToken != null && sigToken.required()) {
                List<String> pers = getUserPermission(httpServletRequest, tokenCookie, httpServletRequest.getHeader("token"));
                for (String per : pers) {
                    if (per.equalsIgnoreCase(queryConf.getSigAction())) {
                        return "success";
                    }
                }
            }
        } catch (Exception e) {
            logger.error("verify user permission exception - {}", e.getMessage());
            return "has no permission";
        }
        return "has no permission";
    }

    private String verifyCompanyPer(CompanyToken companyToken, HttpServletRequest httpServletRequest, Cookie tokenCookie) {
        CustomPropertiesConfig queryConf = getQueryConf(httpServletRequest);
        try {
            if (companyToken != null && companyToken.required()) {
                List<String> pers = getUserPermission(httpServletRequest, tokenCookie, httpServletRequest.getHeader("token"));
                for (String per : pers) {
                    if (per.equalsIgnoreCase(queryConf.getCompanyAction())) {
                        return "success";
                    }
                }
            }
        } catch (Exception e) {
            logger.error("verify user company permission exception - {}", e.getMessage());
            return "has no permission";
        }
        return "has no permission";
    }


    public CustomPropertiesConfig getQueryConf(HttpServletRequest request) {
        String community = request.getParameter("community");
        String serviceType = community == null ? "queryConf" : community.toLowerCase() + "Conf";
        return queryConfContext.getQueryConfig(serviceType);
    }

    // private List<String> getUserPermission(HttpServletRequest httpServletRequest, Cookie tokenCookie, String permissions) {
    //     String community = httpServletRequest.getParameter("community");
    //     String oneIdHost = env.getProperty("oneid.host");
    //     String s = String.format("%s/oneid/user/permissions?community=%s", oneIdHost, community);
    //     try {
    //         HttpResponse<JsonNode> response = Unirest.get(s)
    //                 .header("token", httpServletRequest.getHeader("token"))
    //                 .header("Cookie", "_Y_G_=" + tokenCookie.getValue())
    //                 .asJson();
    //         JSONArray jsonArray = response.getBody().getObject().getJSONObject("data").getJSONArray(permissions);

    //         List<String> list = new ArrayList<>();
    //         for (int i = 0; i < jsonArray.length(); i++) {
    //             list.add(jsonArray.getString(i));
    //         }
    //         return list;
    //     } catch (Exception e) {
    //         throw new RuntimeException(e);
    //     }

    // }

    private List<String> getUserPermission(HttpServletRequest httpServletRequest, Cookie tokenCookie, String userToken) {
        List<String> list = new ArrayList<>();
        String token = getManageToken();
        String tokenCookieValue = tokenCookie.getValue();
        String response = HttpClientUtils.getHttpClient(env.getProperty("permission.api"), token, userToken, tokenCookieValue);
        JsonNode resJson = ObjectMapperUtil.toJsonNode(response);
        if (!resJson.has("data")) {
            return list;
        }
        JsonNode permissions = resJson.get("data").get("permissions");
        for (JsonNode per : permissions) {
            list.add(per.asText());
        }
        return list;
    }

    private String getManageToken() {
        String response = HttpClientUtils.postHttpClient(env.getProperty("manager.token.api"), env.getProperty("manager.token.body"));
        try {
            JsonNode resJson = ObjectMapperUtil.toJsonNode(response);
            return resJson.get("token").asText();
        } catch (Exception e) {
            logger.error("get manage token exception - {}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取包含存token的cookie
     *
     * @param httpServletRequest request
     * @return cookie
     */
    private Cookie verifyCookie(HttpServletRequest httpServletRequest) {
        Cookie[] cookies = httpServletRequest.getCookies();
        Cookie cookie = null;
        if (cookies != null) {
            // 获取cookie中的token
            Optional<Cookie> first = Arrays.stream(cookies).filter(c -> cookieTokenName.equals(c.getName())).findFirst();
            if (first.isPresent()) cookie = first.get();
        }
        return cookie;
    }

    /**
     * 校验domain
     *
     * @param httpServletRequest request
     * @return 是否可访问
     */
    private String verifyDomain(HttpServletRequest httpServletRequest) {
        String referer = httpServletRequest.getHeader("referer");
        String origin = httpServletRequest.getHeader("origin");
        String[] domains = allowDomains.split(";");

        boolean checkReferer = checkDomain(domains, referer);
        boolean checkOrigin = checkDomain(domains, origin);

        if (!checkReferer && !checkOrigin) {
            return "unauthorized";
        }
        return "success";
    }

    private boolean checkDomain(String[] domains, String input) {
        if (StringUtils.isBlank(input)) return true;
        int fromIndex;
        int endIndex;
        if (input.startsWith("http://")) {
            fromIndex = 7;
            endIndex = input.indexOf(":", fromIndex);
        } else {
            fromIndex = 8;
            endIndex = input.indexOf("/", fromIndex);
            endIndex = endIndex == -1 ? input.length() : endIndex;
        }
        String substring = input.substring(0, endIndex);
        for (String domain : domains) {
            if (substring.endsWith(domain)) return true;
        }
        return false;
    }

    private void tokenError(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, String message) throws IOException {
        httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
    }
}