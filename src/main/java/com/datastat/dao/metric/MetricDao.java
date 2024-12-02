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

package com.datastat.dao.metric;

import java.util.*;
import java.lang.reflect.*;

import org.apache.commons.lang3.StringUtils;
import org.asynchttpclient.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;

import com.datastat.model.CustomPropertiesConfig;
import com.datastat.model.DatastatRequestBody;
import com.datastat.util.EsAsyncHttpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;


import static java.nio.charset.StandardCharsets.UTF_8;

@Repository(value = "metricDao")
public abstract class MetricDao {

    @Autowired
    Environment env;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    EsAsyncHttpUtil esAsyncHttpUtil;
    
    @Value("${company.query:companyQuery}")
    String companyQueryStr;

    @Value("${user.query:userQuery}")
    String userQueryStr;

    protected static String esUrl;
    protected static JsonNode companyQueryMap;
    protected static JsonNode userQueryMap;
    private static final Logger logger = LoggerFactory.getLogger(MetricDao.class);

    @PostConstruct
    public void init() {
        esUrl = String.format("%s://%s:%s/", env.getProperty("es.scheme"), env.getProperty("es.host"), env.getProperty("es.port"));
        try {
            companyQueryStr = new String(companyQueryStr.getBytes("ISO8859-1"), "UTF-8");
            companyQueryMap = objectMapper.readTree(companyQueryStr);
            userQueryMap = objectMapper.readTree(userQueryStr);
        } catch (Exception e) {
            logger.error("exception", e);
        }
    }

    public String queryMetricsData(CustomPropertiesConfig queryConf, DatastatRequestBody body) {     
        long start = body.getStart();
        long end = body.getEnd();
        Method method = getFunction(body);       
        try {
            Object result = method.invoke(this, queryConf, start, end, body);
            return result.toString();
        } catch (Exception e) {
            logger.error("exception", e);
        }      
        return resultJsonStr(400, null, "operation error");
    }

    @SneakyThrows
    public Method getFunction(DatastatRequestBody body) {
        String operation = body.getOperation();
        operation = operation.equals("total") ? "increase" : operation;
        String methodName = "queryMetric" + StringUtils.capitalize(operation);
        Class metricDaoClass = this.getClass();
        Method method = metricDaoClass.getMethod(methodName, CustomPropertiesConfig.class, long.class, long.class, DatastatRequestBody.class);
        return method;

    }

    public String resultJsonStr(int code, Object data, String msg) {
        return "{\"code\":" + code + ",\"data\":" + data + ",\"msg\":\"" + msg + "\"}";
    }

    public static <T> ArrayList<T> castList(Object obj, Class<T> clazz) {
        ArrayList<T> result = new ArrayList<T>();
        if (obj instanceof ArrayList<?>) {
            for (Object o : (ArrayList<?>) obj) {
                result.add(clazz.cast(o));
            }
        }
        return result;
    }
    
    @SneakyThrows
    public ArrayList<HashMap<String, Object>> getResponseResult(String index, String queryStr) {
        ArrayList<HashMap<String, Object>> tmpList = new ArrayList<>();
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, index, queryStr);
        String responseBody = future.get().getResponseBody(UTF_8);
        JsonNode dataNode = objectMapper.readTree(responseBody);
        Iterator<JsonNode> buckets = dataNode.get("aggregations").get("group_field").get("buckets").elements();
        tmpList = getResponseBuckets(buckets, "increase");      
        return tmpList;
    }
  
    public ArrayList<HashMap<String, Object>> getResponseBuckets(Iterator<JsonNode> buckets, String field) {
        ArrayList<HashMap<String, Object>> tmpList = new ArrayList<>();
        long total = 0l;
        while (buckets.hasNext()) {
            JsonNode bucket = buckets.next();
            long period = bucket.get("key").asLong();
            long value = bucket.get("res").get("value").asLong();
            total += value;
            HashMap<String, Object> tmpMap = new HashMap<>();
            tmpMap.put("date", period);
            tmpMap.put(field, value);
            if (field.equalsIgnoreCase("increase")) {
                tmpMap.put("total", total);
            }
            tmpList.add(tmpMap);
        }
        return tmpList;
    }

    @SneakyThrows
    public int parseTotalCount(CustomPropertiesConfig queryConf, String queryStr, String index) {
        int ans = 0;
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, index, queryStr);
        String responseBody = future.get().getResponseBody(UTF_8);
        JsonNode dataNode = objectMapper.readTree(responseBody);
        Iterator<JsonNode> buckets = dataNode.get("aggregations").get("group_field").get("buckets").elements();
        while (buckets.hasNext()) {
            JsonNode bucket = buckets.next();
            ans = bucket.get("res").get("value").asInt();
        }    
        return ans;
    }

    public ArrayList<HashMap<String, Object>> parseResMap(HashMap<Long, Double> resMap) {
        ArrayList<Long> periods = new ArrayList<>(resMap.keySet());
        Collections.sort(periods);
        Double total = 0d;
        ArrayList<HashMap<String, Object>> resList = new ArrayList<>();
        for (Long period : periods) {
            HashMap<String, Object> tmpMap = new HashMap<>();
            tmpMap.put("date", period);
            tmpMap.put("increase", Math.round(resMap.get(period)));
            total += resMap.get(period);
            tmpMap.put("total", Math.round(total));
            resList.add(tmpMap);
        }
        return resList;
    }

}
