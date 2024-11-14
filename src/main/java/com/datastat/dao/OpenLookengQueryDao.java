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

package com.datastat.dao;

import com.datastat.model.CustomPropertiesConfig;
import com.datastat.result.ReturnCode;
import com.datastat.util.EsAsyncHttpUtil;
import com.datastat.util.ResultUtil;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository("openlookengDao")
public class OpenLookengQueryDao extends QueryDao {
    @SneakyThrows
    @Override
    public String queryContributors(CustomPropertiesConfig queryConf, String item) {
        String[] indexes = queryConf.getContributorsIndex().split(";");
        String queryJson = queryConf.getContributorsQueryStr();

        long contributorsCount = 0L;
        int statusCode = 500;
        String statusText = ReturnCode.RC400.getMessage();
        for (String index : indexes) {
            ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, index, queryJson);
            String users = getBucketCount(future, item);
            JsonNode dataNode = objectMapper.readTree(users);
            statusCode = dataNode.get("code").intValue();
            contributorsCount += dataNode.get("data").get(item).longValue();
            statusText = dataNode.get("msg").textValue();
        }
        return ResultUtil.resultJsonStr(statusCode, item, Math.round(contributorsCount), statusText);
    }

    @SneakyThrows
    @Override
    public String queryUsers(CustomPropertiesConfig queryConf, String item) {
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getUsersIndex(), queryConf.getUsersQueryStr());
        return getCountResult(future, item);
    }

    @SneakyThrows
    @Override
    public String querySigs(CustomPropertiesConfig queryConf, String item) {
        return ResultUtil.resultJsonStr(404, item, 0, "Not Found");
    }

    @Override
    public String queryDownload(CustomPropertiesConfig queryConf, String item) {
        return ResultUtil.resultJsonStr(404, item, 0, "Not Found");
    }
}
