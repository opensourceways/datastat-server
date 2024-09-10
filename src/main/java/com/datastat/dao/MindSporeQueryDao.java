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

import java.text.SimpleDateFormat;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

@Repository("mindsporeDao")
public class MindSporeQueryDao extends QueryDao {

    /**
     * 根据指定的查询配置、社区和语言查询SIG名称.
     *
     * @param queryConf 查询配置对象
     * @param community 社区名称
     * @param lang 语言，如果为空则默认为"zh"
     * @return 返回查询结果，包含SIG名称、描述和SIG列表等信息
     */
    @SneakyThrows
    @Override
    public String querySigName(CustomPropertiesConfig queryConf, String community, String lang) {
        lang = lang == null ? "zh" : lang;
        HashMap<String, Object> res = getSigFromYaml(queryConf, lang);
        HashMap<String, Object> resData = new HashMap<>();
        resData.put("name", res.get("name"));
        resData.put("description", res.get("description"));
        resData.put("SIG_list", res.get("SIG list"));
        HashMap<String, Object> resMap = new HashMap<>();
        resMap.put("code", 200);
        resMap.put("data", resData);
        resMap.put("msg", "success");
        resMap.put("update_at", (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")).format(new Date()));
        return objectMapper.valueToTree(resMap).toString();
    }

    /**
     * 根据指定的查询配置和项目查询SIG信息.
     *
     * @param queryConf 查询配置对象
     * @param item 项目名称
     * @return 返回查询结果，包含SIG名称、描述和SIG列表等信息
     */
    @SneakyThrows
    @Override
    public String querySigs(CustomPropertiesConfig queryConf, String item) {
        return resultJsonStr(404, item, queryConf.getSigQueryStr(), "Not Found");
    }

    /**
     * 从YAML文件中获取SIG信息.
     *
     * @param queryConf 查询配置对象
     * @param lang 语言，如果为空则默认为"zh"
     * @return 返回SIG信息，包含名称、描述和SIG列表等信息
     */
    private HashMap<String, Object> getSigFromYaml(CustomPropertiesConfig queryConf, String lang) {
        HashMap<String, Object> res = new HashMap<>();
        String mindSporeSigYaml;
        switch (lang) {
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
    /**
     * 根据指定的生态系统类型、语言和排序顺序获取生态系统仓库信息.
     *
     * @param queryConf 查询配置对象
     * @param ecosystemType 生态系统类型
     * @param lang 语言，如果为空则默认为"zh"
     * @param sortOrder 排序顺序，如果为空则默认为"desc"
     * @return 返回 ecosystems 类型的仓库信息列表
     */
    @SneakyThrows
    @Override
    public String getEcosystemRepoInfo(CustomPropertiesConfig queryConf, String ecosystemType, String lang,
          String sortOrder) {
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
        return resultJsonStr(200, objectMapper.valueToTree(resList), "ok");
    }

    /**
     * 根据指定的SIG名称、语言获取SIG的README文件内容.
     *
     * @param queryConf 查询配置对象
     * @param sig SIG名称
     * @param lang 语言，如果为空则默认为"zh"
     * @return 返回SIG的README文件内容
     */
    @SneakyThrows
    @Override
    public String getSigReadme(CustomPropertiesConfig queryConf, String sig, String lang) {
        lang = lang == null ? "zh" : lang;
        String urlStr = "";
        HashMap<String, Object> sigInfo = getSigFromYaml(queryConf, lang);
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
        return resultJsonStr(200, objectMapper.valueToTree(res), "ok");
    }

    /**
     * 根据指定的项目查询贡献者数量.
     *
     * @param queryConf 查询配置对象
     * @param item 项目名称
     * @return 返回项目贡献者数量
     */
    @SneakyThrows
    @Override
    public String queryContributors(CustomPropertiesConfig queryConf, String item) {
        int count = 0;
        String[] indexes = queryConf.getAggContributorsIndex().split(";");
        String[] queries = queryConf.getAggContributorsQueryStr().split(";");
        String[] codeQueries = queryConf.getAggCodeContributorsQueryStr().split(";");
        count = queryCountContributors(indexes, queries) + queryCountContributors(indexes, codeQueries);
        return resultJsonStr(200, item, count, "ok");
    }
    /**
     * 根据指定的索引和查询语句查询贡献者数量.
     *
     * @param indexes 索引数组
     * @param queries 查询语句数组
     * @return 返回贡献者数量
     */
    @SneakyThrows
    public int queryCountContributors(String[] indexes, String[] queries) {
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
