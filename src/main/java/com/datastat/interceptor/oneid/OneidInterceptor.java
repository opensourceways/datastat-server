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
import com.datastat.util.RSAUtil;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
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

    @Value("${cookie.token.name}")
    private String cookieTokenName;

    @Value("${cookie.token.domains}")
    private String allowDomains;

    @Value("${cookie.token.secures}")
    private String cookieSecures;

    private static HashMap<String, Boolean> domain2secure;

    @PostConstruct
    public void init() {
        domain2secure = HttpClientUtils.getConfigCookieInfo(allowDomains, cookieSecures);
    }

    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object object) throws Exception {
        // ???????????????????????????????????????
        if (!(object instanceof HandlerMethod)) {
            return true;
        }

        // ??????????????????????????????????????????????????????AuthingToken???AuthingUserToken
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

        // ????????????????????? token
        String headerToken = httpServletRequest.getHeader("token");
        if (StringUtils.isBlank(headerToken)) {
            tokenError(httpServletRequest, httpServletResponse, "unauthorized");
            return false;
        }

        // ??????domain
        String verifyDomainMsg = verifyDomain(httpServletRequest);
        if (!verifyDomainMsg.equals("success")) {
            tokenError(httpServletRequest, httpServletResponse, verifyDomainMsg);
            return false;
        }

        // ??????cookie
        Cookie tokenCookie = verifyCookie(httpServletRequest);
        if (tokenCookie == null) {
            tokenError(httpServletRequest, httpServletResponse, "unauthorized");
            return false;
        }

        // ??????cookie????????????token
        String token = tokenCookie.getValue();
        try {
            RSAPrivateKey privateKey = RSAUtil.getPrivateKey(env.getProperty("rsa.authing.privateKey"));
            token = RSAUtil.privateDecrypt(token, privateKey);
        } catch (Exception e) {
            tokenError(httpServletRequest, httpServletResponse, "unauthorized");
            return false;
        }

        // ??????token
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
            tokenError(httpServletRequest, httpServletResponse, "unauthorized");
            return false;
        }

        // ??????token
        String verifyTokenMsg = verifyToken(headerToken, token, verifyToken, userId, issuedAt, expiresAt, permission);
        if (!verifyTokenMsg.equals("success")) {
            tokenError(httpServletRequest, httpServletResponse, verifyTokenMsg);
            return false;
        }

        // ??????sig??????
        if (sigToken != null && sigToken.required()) {
            String verifyUserMsg = verifyUser(sigToken, httpServletRequest, tokenCookie);
            if (!verifyUserMsg.equals("success")) {
                tokenError(httpServletRequest, httpServletResponse, verifyUserMsg);
                return false;
            }
        }

        // ??????company??????
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

    private String verifyToken(String headerToken, String token, String verifyToken,
                               String userId, Date issuedAt, Date expiresAt, String permission) {
        try {
            // header??????token???cookie??????token?????????
            if (!headerToken.equals(verifyToken)) {
                return "unauthorized";
            }

            // token ????????????
            if (expiresAt.before(new Date())) {
                return "token expires";
            }

            // token ??????????????????
            String password = permission + env.getProperty("oneid.token.base.password");
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(password)).build();
            jwtVerifier.verify(token);

            // ???????????????token??????
            String redisKey = userId + issuedAt.toString();
            String beforeToken = (String) redisDao.get(redisKey);
            if (token.equalsIgnoreCase(beforeToken)) {
                return "unauthorized";
            }
        } catch (Exception e) {
            return "unauthorized";
        }
        return "success";
    }

    /**
     * ????????????sig????????????
     */
    private String verifyUser(SigToken sigToken, HttpServletRequest httpServletRequest, Cookie tokenCookie) {
        CustomPropertiesConfig queryConf = getQueryConf(httpServletRequest);
        try {
            if (sigToken != null && sigToken.required()) {
                List<String> pers = getUserPermission(httpServletRequest, tokenCookie, "permissions");
                for (String per : pers) {
                    if (per.equalsIgnoreCase(queryConf.getSigAction())) {
                        return "success";
                    }
                }
            }
        } catch (Exception e) {
            return "has no permission";
        }
        return "has no permission";
    }

    private String verifyCompanyPer(CompanyToken companyToken, HttpServletRequest httpServletRequest, Cookie tokenCookie) {
        CustomPropertiesConfig queryConf = getQueryConf(httpServletRequest);
        try {
            if (companyToken != null && companyToken.required()) {
                List<String> pers = getUserPermission(httpServletRequest, tokenCookie, "companyList");
                for (String per : pers) {
                    String[] perList = per.split(":");
                    if (perList.length > 1 && perList[1].equalsIgnoreCase(queryConf.getCompanyAction())) {
                        return "success";
                    }
                }
            }
        } catch (Exception e) {
            return "has no permission";
        }
        return "has no permission";
    }


    public CustomPropertiesConfig getQueryConf(HttpServletRequest request) {
        String community = request.getParameter("community");
        String serviceType = community == null ? "queryConf" : community.toLowerCase() + "Conf";
        return queryConfContext.getQueryConfig(serviceType);
    }

    private List<String> getUserPermission(HttpServletRequest httpServletRequest, Cookie tokenCookie, String permissions) {
        String community = httpServletRequest.getParameter("community");
        String company = httpServletRequest.getParameter("company");
        String contributeType = httpServletRequest.getParameter("contributeType");
        String timeRange = httpServletRequest.getParameter("timeRange");
        String oneIdHost = env.getProperty("oneid.host");
//        String s = String.format("http://119.8.46.32:9999/oneid/user/permissions?community=%s&company=%s&contributeType=%s&timeRange=%s", community, company, contributeType, timeRange);
        String s = String.format("%s/oneid/user/permissions?community=%s&company=%s&contributeType=%s&timeRange=%s", oneIdHost, community, company, contributeType, timeRange);

        try {
            HttpResponse<JsonNode> response = Unirest.get(s)
                    .header("token", httpServletRequest.getHeader("token"))
                    .header("Cookie", "_Y_G_=" + tokenCookie.getValue())
                    .asJson();
            JSONArray jsonArray = response.getBody().getObject().getJSONObject("data").getJSONArray(permissions);

            List<String> list = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                list.add(jsonArray.getString(i));
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * ???????????????token???cookie
     *
     * @param httpServletRequest request
     * @return cookie
     */
    private Cookie verifyCookie(HttpServletRequest httpServletRequest) {
        Cookie[] cookies = httpServletRequest.getCookies();
        Cookie cookie = null;
        if (cookies != null) {
            // ??????cookie??????token
            Optional<Cookie> first = Arrays.stream(cookies).filter(c -> cookieTokenName.equals(c.getName())).findFirst();
            if (first.isPresent()) cookie = first.get();
        }
        return cookie;
    }

    /**
     * ??????domain
     *
     * @param httpServletRequest request
     * @return ???????????????
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
//        HttpClientUtils.setCookie(httpServletRequest, httpServletResponse, cookieTokenName, null, true, 0, "/", domain2secure);
        httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
    }
}