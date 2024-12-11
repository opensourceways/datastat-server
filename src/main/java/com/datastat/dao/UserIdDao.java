/* This project is licensed under the Mulan PSL v2.
 You can use this software according to the terms and conditions of the Mulan PSL v2.
 You may obtain a copy of Mulan PSL v2 at:
     http://license.coscl.org.cn/MulanPSL2
 THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR
 PURPOSE.
 See the Mulan PSL v2 for more details.
 Create: 2022
*/
package com.datastat.dao;

import java.security.interfaces.RSAPrivateKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.datastat.util.RSAUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class UserIdDao {

    @Autowired
    Environment env;
    
    private static final Logger logger = LoggerFactory.getLogger(UserIdDao.class);
    public String getUserId(String token){
        String userId = null;
        try {
            RSAPrivateKey privateKey = RSAUtil.getPrivateKey(env.getProperty("rsa.authing.privateKey"));
            DecodedJWT decode = JWT.decode(RSAUtil.privateDecrypt(token, privateKey));
            userId = decode.getAudience().get(0);
        } catch (Exception e) {
            logger.error("parse token exception - {}", e.getMessage());
        }
        return userId;
    }

    public String getUserIdByCommunity(String token, String community) {
        String userId = null;
        try {
            RSAPrivateKey privateKey = RSAUtil.getPrivateKey(env.getProperty("rsa.authing." + community + ".privateKey"));
            DecodedJWT decode = JWT.decode(RSAUtil.privateDecrypt(token, privateKey));
            userId = decode.getAudience().get(0);
        } catch (Exception e) {
            logger.error("parse token exception - {}", e.getMessage());
        }
        return userId;
    }
}
