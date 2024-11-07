/* This project is licensed under the Mulan PSL v2.
 You can use this software according to the terms and conditions of the Mulan PSL v2.
 You may obtain a copy of Mulan PSL v2 at:
     http://license.coscl.org.cn/MulanPSL2
 THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR
 PURPOSE.
 See the Mulan PSL v2 for more details.
 Create: 2024
*/

package com.datastat.controller;

import com.datastat.aop.LimitRequest;
import com.datastat.interceptor.authentication.ApiToken;
import com.datastat.result.ResultData;
import com.datastat.service.DataPushService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据上传.
 */
@RestController
@RequestMapping(value = "/push")
public class DataPushController {
    /**
     * 数据上传服务.
     */
    @Autowired
    private DataPushService dataPushService;

    /**
     * 推送性能数据.
     *
     * @param request 请求
     * @param body 请求体
     * @return 返回值
     */
    @LimitRequest(callTime = 1, callCount = 100)
    @ApiToken
    @RequestMapping(value = "/realtime/performance_data", method = RequestMethod.POST)
    public ResultData putRealtimePerf(HttpServletRequest request, @RequestBody String body) {
        return dataPushService.pushRealTimePerf(request, body);
    }
}
