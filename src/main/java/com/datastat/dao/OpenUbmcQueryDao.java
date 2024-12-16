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
package com.datastat.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.springframework.stereotype.Repository;

import com.datastat.model.CustomPropertiesConfig;
import com.datastat.util.ResultUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.SneakyThrows;

import static java.nio.charset.StandardCharsets.UTF_8;

@Repository("openubmcDao")
public class OpenUbmcQueryDao extends QueryDao {

    /**
     * Search ownertype based on username.
     *
     * @param queryConf query config.
     * @param userName user name.
     * @return Response string.
     */
    @Override
    @SneakyThrows
    public String queryUserOwnerType(CustomPropertiesConfig queryConf, String userName) {
        String index = queryConf.getSigIndex();
        String queryStr = queryConf.getAllUserOwnerTypeQueryStr();
        ListenableFuture<Response> future = this.esAsyncHttpUtil.executeElasticSearch(queryConf.getEsBaseUrl(),
                queryConf.getEsAuth(), index, queryStr);

        String responseBody = future.get().getResponseBody(UTF_8);
        HashMap<String, ArrayList<Object>> userData = parseOwnerInfo(responseBody, userName);

        ArrayList<Object> ownerInfo = userData.get(userName.toLowerCase());
        return ResultUtil.resultJsonStr(200, objectMapper.valueToTree(ownerInfo), "success");
    }

    /**
     * Search sig info based on sig name.
     *
     * @param queryConf query config.
     * @param sig sig name.
     * @return Response string.
     */
    @Override
    @SneakyThrows
    public String querySigInfo(CustomPropertiesConfig queryConf, String sig) {
        sig = sig == null ? "*" : sig;
        String queryJson = String.format(queryConf.getSigInfoQueryStr(), sig);
        ListenableFuture<Response> future = esAsyncHttpUtil.executeElasticSearch(queryConf.getEsBaseUrl(),
                queryConf.getEsAuth(), queryConf.getSigIndex(), queryJson);
        String responseBody = future.get().getResponseBody(UTF_8);
        JsonNode dataNode = objectMapper.readTree(responseBody);

        Iterator<JsonNode> buckets = dataNode.get("hits").get("hits").elements();
        ArrayList<HashMap<String, Object>> sigList = new ArrayList<>();
        while (buckets.hasNext()) {
            JsonNode bucket = buckets.next().get("_source");
            HashMap<String, Object> data = objectMapper.convertValue(bucket, new TypeReference<>() {
            });
            sigList.add(data);
        }
        return ResultUtil.resultJsonStr(200, objectMapper.valueToTree(sigList), "ok");
    }
}
