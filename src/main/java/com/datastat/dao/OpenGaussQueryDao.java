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
import com.datastat.util.ResultUtil;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

@Repository("opengaussDao")
public class OpenGaussQueryDao extends QueryDao {
    private static final Logger logger = LoggerFactory.getLogger(OpenGaussQueryDao.class);

    @SneakyThrows
    @Override
    public String queryDownload(CustomPropertiesConfig queryConf, String item) {
        String[] indexs = queryConf.getDownloadIndex().split(";");
        String[] queryJsons = queryConf.getDownloadQueryStr().split(";");

        int count = 0;
        for (int i = 0; i < queryJsons.length; i++) {
            count += getDownloadRes(indexs[i], queryJsons[i]);
        }
        return ResultUtil.resultJsonStr(200, item, count, "ok");
    }

    @SneakyThrows
    public int getDownloadRes(String index, String queryJson) {
        int count = 0;
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, index, queryJson);
        Response response = future.get();
        String responseBody = response.getResponseBody(UTF_8);
        JsonNode dataNode = objectMapper.readTree(responseBody);
        Iterator<JsonNode> buckets = dataNode.get("aggregations").get("group_by_field").get("buckets")
                .elements();
        if (buckets.hasNext()) {
            JsonNode bucket = buckets.next();
            if (bucket.has("dockerhub")) {
                count = bucket.get("dockerhub").get("value").asInt();
            } else {
                count = bucket.get("doc_count").asInt();
            }
        }
        return count;
    }

    @Override
    public HashMap<String, String> querySigLabel(CustomPropertiesConfig queryConf) {
        String index = queryConf.getSigIndex();
        String queryJson = queryConf.getSigLabelQueryStr();

        HashMap<String, String> sigLabels = new HashMap<>();
        sigLabels.put("No-SIG", "No-Sig");

        try {
            ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, index, queryJson);
            String responseBody = future.get().getResponseBody(UTF_8);
            JsonNode dataNode = objectMapper.readTree(responseBody);
            Iterator<JsonNode> buckets = dataNode.get("aggregations").get("group_field").get("buckets").elements();
            while (buckets.hasNext()) {
                JsonNode bucket = buckets.next();
                String sig = bucket.get("key").asText();
                Iterator<JsonNode> sigBucket = bucket.get("2").get("buckets").elements();
                if (sigBucket.hasNext()) {
                    String label = sigBucket.next().get("key").asText().split("/")[1];
                    sigLabels.put(sig, label);
                }
            }
        } catch (Exception e) {
            logger.error("exception", e);
        }
        return sigLabels;
    }

    @SneakyThrows
    @Override
    public String queryUsers(CustomPropertiesConfig queryConf, String item) {
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getUsersIndex(), queryConf.getUsersQueryStr());
        return getSumBucketValue(future, item);
    }

    @SneakyThrows
    @Override
    public String querySigScoreAll(CustomPropertiesConfig queryConf) {
        HashMap<String, HashMap<String, String>> sigFeatures = getCommunityFeature(queryConf);
        ArrayList<HashMap<String, String>> sigList = new ArrayList<>();
        Set<String> keys = sigFeatures.keySet();
        for (String key : keys) {
            HashMap<String, String> sigFeature = sigFeatures.get(key);
            sigFeature.put("sig_names", key);
            sigList.add(sigFeature);
        }
        return ResultUtil.resultJsonStr(200, objectMapper.valueToTree(sigList), "ok");
    }
}
