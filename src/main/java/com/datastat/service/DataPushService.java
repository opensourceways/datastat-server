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

package com.datastat.service;

import com.datastat.constant.Constant;
import com.datastat.dao.KafkaDao;
import com.datastat.result.ResultData;
import com.datastat.result.ReturnCode;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 数据上传服务.
 */
@Service
public class DataPushService {
    /**
     * 日志记录.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DataPushService.class);

    /**
     * 性能数据kafka topic.
     */
    @Value("${producer.topic.perfData: }")
    private String perfDataTopic;

    /**
     * kafka操作.
     */
    @Autowired
    private KafkaDao kafkaDao;

    /**
     * 将性能数据推送到kafka.
     *
     * @param request 请求
     * @param body 请求体
     * @return 是否操作成功
     */
    public ResultData pushRealTimePerf(HttpServletRequest request, String body) {
        String community = request.getParameter("community");
        if (StringUtils.isBlank(body) || !Constant.PERF_DATA_COMMUNITY.contains(community)) {
            LOGGER.error("performance data, community is invalid");
            return ResultData.fail(ReturnCode.RC400.getCode(), "invalid performance data");
        }
        if (StringUtils.isNotBlank(perfDataTopic)) {
            kafkaDao.sendMess(perfDataTopic, community, body);
        }
        return ResultData.success("");
    }
}
