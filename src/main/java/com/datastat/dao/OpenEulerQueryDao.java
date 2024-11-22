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
import com.datastat.util.ResultUtil;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import static java.nio.charset.StandardCharsets.UTF_8;

@Repository("openeulerDao")
public class OpenEulerQueryDao extends QueryDao {
    private static final Logger logger = LoggerFactory.getLogger(OpenEulerQueryDao.class);
    @SneakyThrows
    @Override
    public String queryUsers(CustomPropertiesConfig queryConf, String item) {
        String index = queryConf.getUsersIndex();
        String[] queryJsons = queryConf.getUsersQueryStr().split(";");

        double userCount = 0d;
        int statusCode = 500;
        String statusText = ReturnCode.RC400.getMessage();
        for (String queryJson : queryJsons) {
            //获取执行结果
            ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, index, queryJson);
            String users = getSumBucketValue(future, item);
            JsonNode dataNode = objectMapper.readTree(users);
            statusCode = dataNode.get("code").intValue();
            userCount += dataNode.get("data").get(item).intValue();
            statusText = dataNode.get("msg").textValue();
        }
        return ResultUtil.resultJsonStr(statusCode, item, Math.round(userCount), statusText);
    }

    @Override
    public String queryDownload(CustomPropertiesConfig queryConf, String item) {
        return ResultUtil.resultJsonStr(200, item, 0, "ok");
    }

    @Override
    public String getRepoReadme(CustomPropertiesConfig queryConf, String name) {
        String res = "";
        try {
            String path = env.getProperty("TC.oEEP.url");
            String urlStr = path + URLEncoder.encode(name, "utf-8") + ".md";
            urlStr = urlStr.replaceAll("\\+", "%20");
            URL url = new URL(urlStr);
            URLConnection urlConnection = url.openConnection();
            HttpURLConnection connection = null;
            if (urlConnection instanceof HttpURLConnection) {
                connection = (HttpURLConnection) urlConnection;
                connection.setConnectTimeout(0);
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            String current;
            while ((current = in.readLine()) != null) {
                res += current + '\n';
            }
            return ResultUtil.resultJsonStr(200, objectMapper.valueToTree(res), "ok");
        } catch (Exception e) {
            logger.error("exception", e);
        }
        return ResultUtil.resultJsonStr(400, null, "ok");
    }

    @Override
    @SneakyThrows
    public String queryIsvCount(CustomPropertiesConfig queryConf, String item) {
        String query = queryConf.getIsvCountQuery();
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getIsvCountIndex(), query);
        Response response = future.get();
        long count;
        int statusCode = response.getStatusCode();
        String statusText = response.getStatusText();
        String responseBody = response.getResponseBody(UTF_8);
        JsonNode dataNode = objectMapper.readTree(responseBody);
        count = dataNode.get("aggregations").get("count").get("value").asLong();
        return ResultUtil.resultJsonStr(statusCode, item, count, statusText);
    }
}
