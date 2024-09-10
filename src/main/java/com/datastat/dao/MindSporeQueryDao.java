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
import com.datastat.util.YamlUtil;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.text.SimpleDateFormat;
import static java.nio.charset.StandardCharsets.UTF_8;

@Repository("mindsporeDao")
public class MindSporeQueryDao extends QueryDao {
    public static final int HTTP_STATUS_CODE_SUCCESS = 200;
    public static final int HTTP_STATUS_CODE_NOT_FOUND = 404;

    @SneakyThrows
    @Override
    public String querySigName(final CustomPropertiesConfig queryConf, final String community, final String lang) {
        String defaultLang = lang == null ? "zh" : lang;
        HashMap<String, Object> res = getSigFromYaml(queryConf, defaultLang);
        HashMap<String, Object> resData = new HashMap<>();
        resData.put("name", res.get("name"));
        resData.put("description", res.get("description"));
        resData.put("SIG_list", res.get("SIG list"));
        HashMap<String, Object> resMap = new HashMap<>();
        resMap.put("code", HTTP_STATUS_CODE_SUCCESS);
        resMap.put("data", resData);
        resMap.put("msg", "success");
        resMap.put("update_at", (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")).format(new Date()));
        return objectMapper.valueToTree(resMap).toString();
    }

    @SneakyThrows
    @Override
    public String querySigs(final CustomPropertiesConfig queryConf, final String item) {
        return resultJsonStr(HTTP_STATUS_CODE_NOT_FOUND, item, queryConf.getSigQueryStr(), "Not Found");
    }

    private HashMap<String, Object> getSigFromYaml(CustomPropertiesConfig queryConf, String lang) {
        HashMap<String, Object> res = new HashMap<>();
        String mindSporeSigYaml;
        switch (lang){
            case "zh":
                mindSporeSigYaml = queryConf.getSigYamlZh();
                break;
            case "en":
                mindSporeSigYaml = queryConf.getSigYamlEn();
                break;
            default :
                return res;
        }

        String localYamlPath = env.getProperty("company.name.local.yaml");
        YamlUtil yamlUtil = new YamlUtil();
        String localFile = yamlUtil.wget(mindSporeSigYaml, localYamlPath);
        res = yamlUtil.readYaml(localFile);
        return res;
    }

    @SneakyThrows
    @Override
    public String getEcosystemRepoInfo(final CustomPropertiesConfig queryConf, final String ecosystemType,
          final String lang, final String sortOrder) {
        String index = queryConf.getEcosystemRepoIndex();
        String queryJson = queryConf.getEcosystemRepoQuery();
        String queryStr = String.format(queryJson, ecosystemType, lang, sortOrder);
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, index, queryStr);
        String responseBody = future.get().getResponseBody(UTF_8);
        JsonNode dataNode = objectMapper.readTree(responseBody);
        Iterator<JsonNode> buckets = dataNode.get("hits").get("hits").elements();

        ArrayList<JsonNode> resList = new ArrayList<>();
        while (buckets.hasNext()) {
            JsonNode bucket = buckets.next();
            JsonNode res = bucket.get("_source");
            resList.add(res);
        }
        return resultJsonStr(HTTP_STATUS_CODE_SUCCESS, objectMapper.valueToTree(resList), "ok");
    }

    @SneakyThrows
    @Override
    public String getSigReadme(final CustomPropertiesConfig queryConf, final String sig, final String lang) {
        String defaultLang = lang == null ? "zh" : lang;
        String urlStr = "";
        HashMap<String, Object> sigInfo = getSigFromYaml(queryConf, defaultLang);
        ArrayList<HashMap<String, String>> sigList = (ArrayList<HashMap<String, String>>) sigInfo.get("SIG list");
        for (HashMap<String, String> siginfo : sigList) {
            if (sig.equalsIgnoreCase(siginfo.get("name"))) {
                urlStr = siginfo.get("links").replace("/blob/", "/raw/").replace("/tree/", "/raw/");
            }
        }
        URL url = new URL(urlStr);
        URLConnection urlConnection = url.openConnection();
        HttpURLConnection connection = null;
        if (urlConnection instanceof HttpURLConnection) {
            connection = (HttpURLConnection) urlConnection;
            connection.setConnectTimeout(0);
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
        String res = "";
        String current;
        while ((current = in.readLine()) != null) {
            res += current + '\n';
        }
        return resultJsonStr(HTTP_STATUS_CODE_SUCCESS, objectMapper.valueToTree(res), "ok");
    }

    @SneakyThrows
    @Override
    public String queryContributors(final CustomPropertiesConfig queryConf, final String item) {
        int count = 0;
        String[] indexes = queryConf.getAggContributorsIndex().split(";");
        String[] queries = queryConf.getAggContributorsQueryStr().split(";");
        String[] codeQueries = queryConf.getAggCodeContributorsQueryStr().split(";");
        count = queryCountContributors(indexes, queries) + queryCountContributors(indexes, codeQueries);
        return resultJsonStr(HTTP_STATUS_CODE_SUCCESS, item, count, "ok");
    }

    @SneakyThrows
    public int queryCountContributors(final String[] indexes, final String queries[]) {
        int count = 0;
        for (int i = 0; i < indexes.length; i++) {
            ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, indexes[i], queries[i]);
            String resBody = future.get().getResponseBody(UTF_8);
            JsonNode dataNode = objectMapper.readTree(resBody);
            JsonNode buckets = dataNode.get("aggregations").get("group_field").get("buckets");
            if (buckets.elements().hasNext()) {
                count +=  buckets.get(0).get("users").get("value").asInt();
            }
        }
        return count;
    }
}
