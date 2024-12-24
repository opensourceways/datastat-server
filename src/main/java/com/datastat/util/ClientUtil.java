/* This project is licensed under the Mulan PSL v2.
 You can use this software according to the terms and conditions of the Mulan PSL v2.
 You may obtain a copy of Mulan PSL v2 at:
     http://license.coscl.org.cn/MulanPSL2
 THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR
 PURPOSE.
 See the Mulan PSL v2 for more details.
 Create: 2024/02
*/
package com.datastat.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class ClientUtil {


    // Private constructor to prevent instantiation of the utility class
    private ClientUtil() {
        // private constructor to hide the implicit public one
        throw new AssertionError("ClientUtil class cannot be instantiated.");
    }

    /**
     * Logger instance for ClientUtil.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientUtil.class);

    /**
     * Retrieve the client's IP address from the HttpServletRequest.
     *
     * @param request The HttpServletRequest object
     * @return The client's IP address as a string
     */
    public static String getClientIpAddress(final HttpServletRequest request) {
        String ip = request.getHeader("x-real-ip");
        if (checkIp(ip)) {
            ip = getForwardedIP(request);
        }
        if (checkIp(ip)) {
            ip = request.getRemoteAddr();
            if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
                InetAddress inet = null;
                try {
                    inet = InetAddress.getLocalHost();
                } catch (UnknownHostException e) {
                    LOGGER.error("get local host error: " + e.getMessage());
                }
                ip = inet.getHostAddress();
            }
        }
        return ip;
    }

    /**
     * Check if the provided string is a valid IP address.
     *
     * @param ip The IP address to check.
     * @return true if the IP address is valid, false otherwise.
     */
    private static boolean checkIp(final String ip) {
        return null == ip || ip.length() == 0 || "unknown".equalsIgnoreCase(ip);
    }

    /**
     * Retrieve the client's x-forwarded-for IP address from the HttpServletRequest.
     *
     * @param request The HttpServletRequest object
     * @return The client's IP address as a string
     */
    private static String getForwardedIP(final HttpServletRequest request) {
        String headerName = "x-forwarded-for";
        String ip = request.getHeader(headerName);
        if (!checkIp(ip)) {
            // There will be multiple IP values after multiple reverse proxies, pick the first IP.
            if (ip.contains(",")) {
                ip = ip.split(",")[0];
            }
        }
        return ip;
    }
}

