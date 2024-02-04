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

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.datastat.model.BlueZoneUser;
import com.datastat.model.CustomPropertiesConfig;
import com.datastat.model.NpsBody;
import com.datastat.model.QaBotRequestBody;
import com.datastat.model.SigDetails;
import com.datastat.model.SigDetailsMaintainer;
import com.datastat.model.UserTagInfo;
import com.datastat.model.meetup.MeetupApplyForm;
import com.datastat.model.vo.*;
import com.datastat.model.yaml.*;
import com.datastat.result.ReturnCode;
import com.datastat.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;

import io.netty.util.internal.StringUtil;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.asynchttpclient.*;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;

@Primary
@Repository(value = "queryDao")
public class QueryDao {
    @Autowired
    Environment env;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    EsAsyncHttpUtil esAsyncHttpUtil;

    @Autowired
    KafkaDao kafkaDao;

    @Autowired
    ObsDao obsDao;

    protected static String esUrl;
    protected EsQueryUtils esQueryUtils;
    protected List<String> robotUsers;
    protected List<String> domain_ids;
    private static final Logger logger = LoggerFactory.getLogger(QueryDao.class);
    private static List<Map<String, Object>> giteeWebhookList = new ArrayList<>();

    @PostConstruct
    public void init() {
        esUrl = String.format("%s://%s:%s/", env.getProperty("es.scheme"), env.getProperty("es.host"), env.getProperty("es.port"));
        esQueryUtils = new EsQueryUtils();
        robotUsers = Arrays.asList(Objects.requireNonNull(env.getProperty("skip.robot.user")).split(","));
        domain_ids = Arrays.asList(Objects.requireNonNull(env.getProperty("qa.domain.ids")).split(","));
    }

    @SneakyThrows
    public String queryContributors(CustomPropertiesConfig queryConf, String item) {
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getGiteeAllIndex(), queryConf.getContributorsQueryStr());
        return getBucketCount(future, item);
    }

    @SneakyThrows
    public String queryDurationAggFromProjectHostArchPackage(CustomPropertiesConfig queryConf, String item) {
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getDurationAggIndex(), queryConf.getDurationAggQueryStr());
        return parseDurationAggFromProjectHostArchPackageResult(future, item);
    }

    @SneakyThrows
    public String querySigs(CustomPropertiesConfig queryConf, String item) {
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getSigIndex(), queryConf.getSigQueryStr());
        Response response = future.get();
        int statusCode = response.getStatusCode();
        String statusText = response.getStatusText();
        String responseBody = response.getResponseBody(UTF_8);
        JsonNode dataNode = objectMapper.readTree(responseBody);
        Iterator<JsonNode> buckets = dataNode.get("aggregations").get("2").get("buckets").elements();
        long count = 0;
        while (buckets.hasNext()) {
            JsonNode bucket = buckets.next();
            count += bucket.get("1").get("value").asLong();
        }
        return resultJsonStr(statusCode, item, count, statusText);
    }

    @SneakyThrows
    public String queryUsers(CustomPropertiesConfig queryConf, String item) {
        return resultJsonStr(404, item, 0, "Not Found");
    }

    @SneakyThrows
    public String queryNoticeUsers(CustomPropertiesConfig queryConf, String item) {
        return resultJsonStr(404, item, 0, "Not Found");
    }

    @SneakyThrows
    public String queryModuleNums(CustomPropertiesConfig queryConf, String item) {
        String[] communities = queryConf.getMultiCommunity().split(",");
        int temp = 0;
        String result = resultJsonStr(404, item, 0, "Not Found");
        for (int i = 0; i < communities.length; i++) {
            if (i == communities.length - 1) {
                temp = temp + objectMapper.readTree(getGiteeResNum(queryConf.getAccessToken(), communities[i])).get("data").get("modulenums").intValue();
                result = resultJsonStr(200, item, temp, "OK");
            } else {
                temp = temp + objectMapper.readTree(getGiteeResNum(queryConf.getAccessToken(), communities[i])).get("data").get("modulenums").intValue();
            }
        }
        return result;
    }

    @SneakyThrows
    public String queryBusinessOsv(CustomPropertiesConfig queryConf, String item) {
        return resultJsonStr(200, item, queryConf.getBusinessOsv(), "OK");
    }

    @SneakyThrows
    public String queryCommunityMembers(CustomPropertiesConfig queryConf, String item) {
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getGiteeAllIndex(), queryConf.getCommunityMembersQueryStr());
        return getSumBucketValue(future, item);
    }

    @SneakyThrows
    public String queryDownload(CustomPropertiesConfig queryConf, String item) {
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getDownloadIndex(), queryConf.getDownloadQueryStr());
        Response response = future.get();
        int count = 0;
        int statusCode = response.getStatusCode();
        String statusText = response.getStatusText();
        String responseBody = response.getResponseBody(UTF_8);
        JsonNode dataNode = objectMapper.readTree(responseBody);
        Iterator<JsonNode> buckets = dataNode.get("aggregations").get("group_by_field").get("buckets").elements();
        if (buckets.hasNext()) {
            JsonNode bucket = buckets.next();
            count = bucket.get("count").get("value").asInt();
        }
        return resultJsonStr(statusCode, item, count, statusText);
    }

    @SneakyThrows
    public String queryCount(CustomPropertiesConfig queryConf, String item) {
        ListenableFuture<Response> future = esAsyncHttpUtil.executeCount(esUrl, queryConf.getGiteeAllIndex(), queryConf.getCountQueryStr(item));
        Response response = future.get();
        long count;
        int statusCode = response.getStatusCode();
        String statusText = response.getStatusText();
        String responseBody = response.getResponseBody(UTF_8);
        JsonNode dataNode = objectMapper.readTree(responseBody);
        count = dataNode.get("count").asLong();
        return resultJsonStr(statusCode, item, count, statusText);
    }

    @SneakyThrows
    public String queryAll(CustomPropertiesConfig queryConf, String community) {
        Map<String, Object> contributes = queryContributes(queryConf, community);
        JsonNode contributorsNode = objectMapper.readTree(this.queryContributors(queryConf, "contributors")).get("data").get("contributors");
        JsonNode usersNode = objectMapper.readTree(this.queryUsers(queryConf, "users")).get("data").get("users");
        Object users = usersNode == null ? null : usersNode.intValue();
        JsonNode noticeusersNode = objectMapper.readTree(this.queryNoticeUsers(queryConf, "noticeusers")).get("data").get("noticeusers");
        Object noticeusers = noticeusersNode == null ? null : noticeusersNode.intValue();
        JsonNode sigsNode = objectMapper.readTree(this.querySigs(queryConf, "sigs")).get("data").get("sigs");
        Object sigs = sigsNode == null ? null : sigsNode.intValue();
        JsonNode modulenumsNode = objectMapper.readTree(this.queryModuleNums(queryConf, "modulenums")).get("data").get("modulenums");
        Object modulenums = modulenumsNode == null ? null : modulenumsNode.intValue();
        JsonNode businessOsvNode = objectMapper.readTree(this.queryBusinessOsv(queryConf, "businessOsv")).get("data").get("businessOsv");
        Object businessOsv = businessOsvNode == null ? null : businessOsvNode.intValue();
        JsonNode communityMembersNode = objectMapper.readTree(this.queryCommunityMembers(queryConf, "communitymembers")).get("data").get("communitymembers");
        Object communityMembers = businessOsvNode == null ? null : communityMembersNode.intValue();
        JsonNode downloadNode = objectMapper.readTree(this.queryDownload(queryConf, "download")).get("data").get("download");
        Object downloads = downloadNode == null ? null : downloadNode.intValue();
        Object downloadUser = 0;
        if (community.equalsIgnoreCase("mindspore") || community.equalsIgnoreCase("opengauss")) {
            downloadUser = users;
            users = downloads;
        }
        contributes.put("downloads", downloads);
        contributes.put("contributors", contributorsNode.intValue());
        contributes.put("users", users);
        contributes.put("noticeusers", noticeusers);
        contributes.put("sigs", sigs);
        contributes.put("modulenums", modulenums);
        contributes.put("businessosv", businessOsv);
        contributes.put("communitymembers", communityMembers);
        contributes.put("downloaduser", downloadUser);

        HashMap<String, Object> resMap = new HashMap<>();
        resMap.put("code", 200);
        resMap.put("data", contributes);
        resMap.put("msg", "success");
        resMap.put("update_at", (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")).format(new Date()));
        return objectMapper.valueToTree(resMap).toString();
    }

    @SneakyThrows
    public String queryBlueZoneContributes(CustomPropertiesConfig queryConf, BlueZoneContributeVo body, String item) {
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getBlueZoneUserContributesIndex(), getBlueZoneContributesQuery(body));
        return getBlueZoneContributesRes(future, item);
    }

    @SneakyThrows
    public String putBlueZoneUser(CustomPropertiesConfig queryConf, BlueZoneUserVo userVo, String item) {
        BulkRequest request = new BulkRequest();
        RestHighLevelClient restHighLevelClient = getRestHighLevelClient();

        LocalDateTime now = LocalDateTime.now();
        String nowStr = now.toString().split("\\.")[0] + "+08:00";

        String index = queryConf.getBlueZoneUserIndex();
        List<BlueZoneUser> users = userVo.getUsers();
        String productLineCode = userVo.getProductLineCode();
        productLineCode = productLineCode == null ? "" : productLineCode;

        List<String> products = Arrays.asList(queryConf.getProductLineCodeAllUpdate().split(";"));
        if (StringUtils.isNotBlank(productLineCode) && products.contains(productLineCode)) {
            // 某些产品线需要全量更新
            DeleteByQueryRequest requestDelete = new DeleteByQueryRequest(index);
            requestDelete.setConflicts("proceed");
            requestDelete.setQuery(new TermQueryBuilder("product_line_code", productLineCode));
            esQueryUtils.deleteByQuery(restHighLevelClient, index, requestDelete);
        }

        HashMap<String, HashSet<String>> id2emails = esQueryUtils.queryBlueUserEmails(restHighLevelClient, index);
        for (BlueZoneUser user : users) {
            String id;
            if (StringUtils.isNotBlank(user.getGitee_id())) id = user.getGitee_id();
            else if (StringUtils.isNotBlank(user.getGitee_id())) id = user.getGitee_id();
            else continue;

            Map resMap = objectMapper.convertValue(user, Map.class);
            resMap.put("created_at", nowStr);
            String email = user.getEmail();
            List<String> inputEmails = Arrays.asList(email.split(";"));

            if (StringUtils.isNotBlank(productLineCode) && products.contains(productLineCode)) {
                resMap.put("emails", inputEmails);
            } else {
                HashSet<String> emails = id2emails.getOrDefault(id, new HashSet<>());
                emails.addAll(inputEmails);
                resMap.put("emails", new ArrayList<>(emails));
            }

            resMap.remove("email");
            resMap.put("product_line_code", productLineCode);
            request.add(new IndexRequest(index, "_doc", id).source(resMap));
        }

        if (request.requests().size() != 0)
            restHighLevelClient.bulk(request, RequestOptions.DEFAULT);
        restHighLevelClient.close();

        String res = resultJsonStr(200, item, 0, "there`s no user");
        if (users.size() > 0) res = resultJsonStr(200, item + "_count", users.size(), "update success");

        return res;
    }

    @SneakyThrows
    public String queryOrgStarAndFork(CustomPropertiesConfig queryConf, String community, String item) {
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getStarForkIndex(), queryConf.getStarForkQueryStr());
        return getOrgStarAndForkRes(future, item, community);
    }

    @SneakyThrows
    public String queryCveDetails(CustomPropertiesConfig queryConf, String lastCursor, String pageSize, String item) {
        RestHighLevelClient restHighLevelClient = getRestHighLevelClient();
        String indexName = queryConf.getCveDetailsIndex();
        if (pageSize == null) return esQueryUtils.esScroll(restHighLevelClient, item, indexName);
        return esQueryUtils.esFromId(restHighLevelClient, item, lastCursor, Integer.parseInt(pageSize), indexName);
    }

    @SneakyThrows
    public String queryNewYearPer(CustomPropertiesConfig queryConf, String oauth2_proxy, String community) {
        String user = getUserFromCookie(queryConf, oauth2_proxy);
        String localFile = "om-data/obs/" + community.toLowerCase() + "_" + env.getProperty("year") + ".csv";
        List<HashMap<String, Object>> report = CsvFileUtil.readFile(localFile);
        HashMap<String, Object> resMap = new HashMap<>();
        resMap.put("code", 200);
        resMap.put("msg", "OK");
        if (report == null)
            resMap.put("data", new ArrayList<>());
        else if (user == null)
            resMap.put("data", report);
        else {
            List<HashMap<String, Object>> user_login = report.stream()
                    .filter(m -> m.getOrDefault("user_login", "").equals(user)).collect(Collectors.toList());
            resMap.put("data", user_login);
        }
      
        BulkRequest request = new BulkRequest();
        RestHighLevelClient restHighLevelClient = getRestHighLevelClient();
        Date now = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        String nowStr = simpleDateFormat.format(now);
        String uuid = UUID.randomUUID().toString();
        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("user_login", user);
        dataMap.put("community", community);
        dataMap.put("created_at", nowStr);
        request.add(new IndexRequest("new_year_report", "_doc", uuid + nowStr).source(dataMap));
        if (request.requests().size() != 0)
            restHighLevelClient.bulk(request, RequestOptions.DEFAULT);
        restHighLevelClient.close();

        resMap.put("update_at", (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")).format(new Date()));
        return objectMapper.valueToTree(resMap).toString();
    }

    @SneakyThrows
    public String queryNewYearMonthCount(CustomPropertiesConfig queryConf, String oauth2_proxy) {
        String user = getUserFromCookie(queryConf, oauth2_proxy);
        String queryJson = String.format(queryConf.getMonthCountQueryStr(), user);
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getGiteeAllIndex(), queryJson);
        String responseBody = future.get().getResponseBody(UTF_8);
        JsonNode dataNode = objectMapper.readTree(responseBody);
        Iterator<JsonNode> buckets = dataNode.get("aggregations").get("group_field").get("buckets").elements();

        HashMap<String, Object> dataMap = new HashMap<>();
        long monthTime;
        int count;
        if (buckets.hasNext()) {
            JsonNode bucket = buckets.next();
            monthTime = bucket.get("key").asLong();
            count = bucket.get("doc_count").asInt();
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(monthTime);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
            String monthString = sdf.format(c.getTime()).split("-")[1];
            int month = Integer.parseInt(monthString);
            dataMap.put("month", month);
            dataMap.put("count", count);
        }
        return resultJsonStr(200, objectMapper.valueToTree(dataMap), "ok");
    }

    @SneakyThrows
    private String getUserFromCookie(CustomPropertiesConfig queryConf, String oauth2_proxy) {
        String cookie_oauth2_proxy = "_oauth2_proxy=" + oauth2_proxy;
        HttpResponse<String> response = Unirest.get(queryConf.getGiteeUserInfoUrl())
            .header("cookie", cookie_oauth2_proxy)
            .asString();

        if (response.getStatus() != 200) {
            throw new Exception("unauthorized");
        }
        JsonNode res = objectMapper.readTree(response.getBody());
        String user = res.get("user").asText();
        return user;
    }

    @SneakyThrows
    public String queryBugQuestionnaire(CustomPropertiesConfig queryConf, String lastCursor, String pageSize, String item) {
        RestHighLevelClient restHighLevelClient = getRestHighLevelClient();
        String indexName = queryConf.getBugQuestionnaireIndex();
        String result;
        if (pageSize == null)
            result = esQueryUtils.esScroll(restHighLevelClient, item, indexName);
        else
            result = esQueryUtils.esFromId(restHighLevelClient, item, lastCursor, Integer.parseInt(pageSize), indexName);

        result = dataDesensitizationProcessing(result, item);
        return result;
    }

    public String putBugQuestionnaire(CustomPropertiesConfig queryConf, String community, String item, String lang, BugQuestionnaireVo bugQuestionnaireVo) {
        ArrayList<String> validationMesseages = bugQuestionnaireVo.checkoutFieldValidate(bugQuestionnaireVo, community, lang);
        if (validationMesseages.size() != 0) {
            return "{\"code\":400,\"data\":{\"" + item + "\":\"write error\"},\"msg:" + validationMesseages + "\"}";
        }        
        try{
            Date now = new Date();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
            String nowStr = simpleDateFormat.format(now);
            String uuid = UUID.randomUUID().toString();
            HashMap<String, Object> bugQuestionnaireMap = objectMapper.convertValue(bugQuestionnaireVo, new TypeReference<>() {
            });
            bugQuestionnaireMap.put("created_at", nowStr);
            if (lang.equals("en")) {
                bugQuestionnaireMap.put("is_en", 1);
            }

            BulkRequest request = new BulkRequest();
            RestHighLevelClient restHighLevelClient = getRestHighLevelClient();
            String index = queryConf.getBugQuestionnaireIndex();
            request.add(new IndexRequest(index, "_doc", uuid + nowStr).source(bugQuestionnaireMap));
            if (request.requests().size() != 0)
                restHighLevelClient.bulk(request, RequestOptions.DEFAULT);
            restHighLevelClient.close();
            return resultJsonStr(200, objectMapper.valueToTree("success"), "success");
        } catch (Exception e) {
            logger.error("exception", e);
            return resultJsonStr(400, null, "error");
        }
    }

    @SneakyThrows
    public String queryObsDetails(CustomPropertiesConfig queryConf, String branch) {
        String format = String.format(queryConf.getObsDetailsQueryStr(), branch);
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getObsDetailsIndex(), format);
        ArrayList<JsonNode> obsDetails = getObsDetails(future);
        return resultJsonStr(200, objectMapper.valueToTree(obsDetails), "ok");
    }

    @SneakyThrows
    public String queryIsoBuildTimes(CustomPropertiesConfig queryConf, IsoBuildTimesVo body) {
        ArrayList<JsonNode> dataList = getIsoBuildTimes(queryConf, body);
        return resultJsonStr(200, objectMapper.valueToTree(dataList), "ok");
    }

    @SneakyThrows
    public String querySigDetails(CustomPropertiesConfig queryConf, SigDetailsVo body) {
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getSigDetailsIndex(), queryConf.getSigDetailsQueryStr());
        ArrayList<JsonNode> dataList = getSigDetails(future, queryConf, body);
        return resultJsonStr(200, objectMapper.valueToTree(dataList), "ok");
    }

    @SneakyThrows
    public String queryCompanyContributors(CustomPropertiesConfig queryConf, String community, String contributeType,
            String timeRange, String version, String repo, String sig) {
        // 展示特征
        if (contributeType.equals("feature")) {
            return getVersionFeature(queryConf, community, version, "company");
        }
        List<String> claCompanies = queryClaCompany(queryConf.getClaCorporationIndex());
        List<Map<String, String>> companies = getCompanyNameCnEn(env.getProperty("company.name.yaml"), env.getProperty("company.name.local.yaml"));
        Map<String, String> companyNameCnEn = companies.get(0);
        Map<String, String> companyNameAlCn = companies.get(1);

        String contributesQueryStr = queryConf.getCompanyContributorsQuery(queryConf, community, contributeType, timeRange, version, repo, sig);
        String index = version == null ? queryConf.getGiteeAllIndex() : queryConf.getGiteeVersionIndex();
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, index, contributesQueryStr);
        JsonNode dataNode = objectMapper.readTree(future.get().getResponseBody(UTF_8));
        Iterator<JsonNode> buckets = dataNode.get("aggregations").get("group_field").get("buckets").elements();

        ArrayList<JsonNode> dataList = new ArrayList<>();
        HashMap<String, Object> dataMap = new HashMap<>();
        long independent = 0;
        while (buckets.hasNext()) {
            JsonNode bucket = buckets.next();
            String company = bucket.get("key").asText();
            long contribute = bucket.get("sum_field").get("value").asLong();
            if (!claCompanies.contains(company) ||
                company.equals("深圳易宝软件") ||
                company.contains("华为合作方") ||
                company.equalsIgnoreCase("openeuler")) {
                independent += contribute;
                continue;
            }
            if (company.contains("华为技术有限公司")) {
                continue;
            }
            String companyCn = companyNameAlCn.getOrDefault(company.trim(), company.trim());
            String companyEn = companyNameCnEn.getOrDefault(company.trim(), companyCn);
            dataMap.put("company_cn", companyCn);
            dataMap.put("company_en", companyEn);
            dataMap.put("contribute", contribute);
            JsonNode resNode = objectMapper.valueToTree(dataMap);
            dataList.add(resNode);
        }
        dataMap.put("company_cn", "个人贡献者");
        dataMap.put("company_en", "independent");
        dataMap.put("contribute", independent);
        JsonNode resNode = objectMapper.valueToTree(dataMap);
        dataList.add(resNode);

        return resultJsonStr(200, objectMapper.valueToTree(dataList), "ok");
    }

    @SneakyThrows
    public String queryUserContributors(CustomPropertiesConfig queryConf, String community, String contributeType, String timeRange, String repo, String sig) {
        if (contributeType.equalsIgnoreCase("comment")) return queryUserCommentContributors(queryConf, community, contributeType, timeRange, repo, sig);
        String contributesQueryStr = queryConf.getAggCountQueryStr(queryConf, "gitee_id", contributeType, timeRange, community, repo, sig);
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getGiteeAllIndex(), contributesQueryStr);
        JsonNode dataNode = objectMapper.readTree(future.get().getResponseBody(UTF_8));
        Iterator<JsonNode> buckets = dataNode.get("aggregations").get("group_field").get("buckets").elements();

        ArrayList<JsonNode> dataList = new ArrayList<>();
        HashMap<String, Object> dataMap = new HashMap<>();
        while (buckets.hasNext()) {
            JsonNode bucket = buckets.next();
            String giteeId = bucket.get("key").asText();
            long contribute = bucket.get("sum_field").get("value").asLong();
            if (contribute == 0 || robotUsers.contains(giteeId)) {
                continue;
            }
            dataMap.put("gitee_id", giteeId);
            dataMap.put("contribute", contribute);
            JsonNode resNode = objectMapper.valueToTree(dataMap);
            dataList.add(resNode);
        }
        return resultJsonStr(200, objectMapper.valueToTree(dataList), "ok");
    }

    @SneakyThrows
    public String queryUserCommentContributors(CustomPropertiesConfig queryConf, String community, String contributeType, String timeRange, String repo, String sig) {
        String contributesQueryStr = queryConf.getAggCommentQueryStr(queryConf, "gitee_id", timeRange, repo);
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getGiteeAllIndex(), contributesQueryStr);
        JsonNode dataNode = objectMapper.readTree(future.get().getResponseBody(UTF_8));
        Iterator<JsonNode> buckets = dataNode.get("aggregations").get("group_field").get("buckets").elements();

        ArrayList<JsonNode> dataList = new ArrayList<>();
        HashMap<String, Object> dataMap = new HashMap<>();
        while (buckets.hasNext()) {
            JsonNode bucket = buckets.next();
            String giteeId = bucket.get("key").asText();
            long contribute = bucket.get("comment").get("value").asLong();
            long invalidComment = bucket.get("invalid_comment").get("value").asLong();
            long validComment = Math.subtractExact(contribute, invalidComment);
            if (contribute == 0 || robotUsers.contains(giteeId)) {
                continue;
            }
            dataMap.put("gitee_id", giteeId);
            dataMap.put("contribute", contribute);
            dataMap.put("invalid_comment", invalidComment);
            dataMap.put("valid_comment", validComment);
            JsonNode resNode = objectMapper.valueToTree(dataMap);
            dataList.add(resNode);
        }
        return resultJsonStr(200, objectMapper.valueToTree(dataList), "ok");
    }

    @SneakyThrows
    public String queryIssueScore(CustomPropertiesConfig queryConf, String startDate, String endDate) {
        startDate = StringUtils.isBlank(startDate) ? "2020-01-01" : startDate;
        endDate = StringUtils.isBlank(endDate) ? "now" : endDate;
        String queryJson = String.format(queryConf.getIssueScoreQueryStr(), startDate, endDate);
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getIssueScoreIndex(), queryJson);
        return parseIssueScoreFutureRes(future);
    }

    @SneakyThrows
    public String queryBuildCheckInfo(CustomPropertiesConfig queryConf, BuildCheckInfoQueryVo queryBody, String item, String lastCursor, String pageSize) {
        RestHighLevelClient restHighLevelClient = getRestHighLevelClient();
        SearchSourceBuilder queryResultSourceBuilder = assembleResultSourceBuilder("update_at", queryBody);
        pageSize = pageSize == null ? "1000" : pageSize;
        pageSize = Integer.parseInt(pageSize) > 1000 ? "1000" : pageSize;
        String resultInfo = esQueryUtils.esScrollFromId(restHighLevelClient, item, Integer.parseInt(pageSize),
                queryConf.getBuildCheckResultIndex(), lastCursor, queryResultSourceBuilder);
        SearchSourceBuilder mistakeSourceBuilder = assembleMistakeSourceBuilder("update_at", queryBody);
        String mistakeInfoStr = esQueryUtils.esScroll(restHighLevelClient, item, queryConf.getBuildCheckMistakeIndex(), 5000, mistakeSourceBuilder);

        ArrayList<ObjectNode> finalResultJSONArray = new ArrayList<>();
        int totalCount = 0;
        String cursor = "";
        JsonNode resNode = objectMapper.readTree(resultInfo);
        Iterator<JsonNode> resbuckets = resNode.get("data").elements();
        cursor = resNode.get("cursor").asText();
        totalCount = resNode.get("totalCount").asInt();
        JsonNode dataNode = objectMapper.readTree(mistakeInfoStr);
        while (resbuckets.hasNext()) {
            ObjectNode resbucket = (ObjectNode) resbuckets.next();
            String pr_url = resbucket.get("pr_url").asText();
            int build_no = resbucket.get("build_no").asInt();
            String result_update_at = resbucket.get("update_at").asText();
            resbucket.put("result_update_at", result_update_at);
            resbucket.remove("update_at");
            ArrayList<ObjectNode> mistakeList = new ArrayList<>();
            Iterator<JsonNode> buckets = dataNode.get("data").elements();
            while (buckets.hasNext()) {
                ObjectNode bucket = (ObjectNode) buckets.next();
                String mistake_pr_url = bucket.get("pr_url").asText();
                int mistake_build_no = bucket.get("build_no").asInt();
                if (mistake_pr_url.equals(pr_url) && mistake_build_no == build_no) {
                    mistakeList.add(bucket);
                }
            }
            String currentMistakeUpdateAt = mistakeList.size() > 0 ? mistakeList.get(0).get("update_at").asText()
                    : result_update_at;
            resbucket.putPOJO("ci_mistake", mistakeList);
            resbucket.put("ci_mistake_update_at", currentMistakeUpdateAt);

            if (result_update_at.compareTo(currentMistakeUpdateAt) < 0) {
                result_update_at = currentMistakeUpdateAt;
            }
            boolean isAdd = isLocatedInTimeWindow(queryBody, result_update_at);
            if (!isAdd) {
                continue;
            }
            finalResultJSONArray.add(resbucket);
        }

        HashMap<String, Object> resMap = new HashMap<>();
        resMap.put("code", 200);
        resMap.put("data", finalResultJSONArray);
        resMap.put("totalCount", totalCount);
        resMap.put("cursor", cursor);
        resMap.put("msg", "susscess");
        resMap.put("update_at", (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")).format(new Date()));
        return objectMapper.valueToTree(resMap).toString();
    }

    @SneakyThrows
    public String putUserActionsInfo(String community, String data) {
        String sdata = new String(Base64.getDecoder().decode(data));
        JsonNode userVo = objectMapper.readTree(sdata);
        Date now = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        String nowStr = simpleDateFormat.format(now);
        String id = userVo.get("_track_id").asText();
        HashMap<String, Object> resMap = objectMapper.convertValue(userVo, HashMap.class);
        resMap.put("created_at", nowStr);
        resMap.put("community", community);
        kafkaDao.sendMess(env.getProperty("producer.topic.tracker"), id, objectMapper.valueToTree(resMap).toString());
        return resultJsonStr(200, "track_id", id, "collect over");
    }

    @SneakyThrows
    public String putUserPermissionApply(String community, String username) {
        Date now = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        String nowStr = simpleDateFormat.format(now);
        String id = username;

        HashMap<String, Object> resMap = new HashMap<>();
        resMap.put("username", username);
        resMap.put("created_at", nowStr);
        resMap.put("community", community);

        kafkaDao.sendMess(env.getProperty("producer.topic.userApply"), id, objectMapper.valueToTree(resMap).toString());
        return resultJsonStr(200, null, "ok");
    }

    @SneakyThrows
    public String querySigName(CustomPropertiesConfig queryConf, String community, String lang) {
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getSigIndex(), queryConf.getSigNameQueryStr());
        String responseBody = future.get().getResponseBody(UTF_8);
        JsonNode dataNode = objectMapper.readTree(responseBody);
        Iterator<JsonNode> buckets = dataNode.get("aggregations").get("sig_names").get("buckets").elements();
        HashMap<String, Object> dataMap = new HashMap<>();
        ArrayList<String> sigList = new ArrayList<>();
        while (buckets.hasNext()) {
            JsonNode bucket = buckets.next();
            String sig = bucket.get("key").asText();
            sigList.add(sig);
        }
        dataMap.put(community, sigList);

        return resultJsonStr(200, objectMapper.valueToTree(dataMap), "ok");
    }

    @SneakyThrows
    public String querySigInfo(CustomPropertiesConfig queryConf, String sig) {
        sig = sig == null ? "*" : sig;
        String queryJson = String.format(queryConf.getSigInfoQueryStr(), sig);
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getSigIndex(), queryJson);
        String responseBody = future.get().getResponseBody(UTF_8);
        JsonNode dataNode = objectMapper.readTree(responseBody);

        Iterator<JsonNode> buckets = dataNode.get("hits").get("hits").elements();
        ArrayList<HashMap<String, Object>> sigList = new ArrayList<>();
        while (buckets.hasNext()) {
            JsonNode bucket = buckets.next().get("_source");
            HashMap<String, Object> data = objectMapper.convertValue(bucket, HashMap.class);
            sigList.add(data);
        }
        return resultJsonStr(200, objectMapper.valueToTree(sigList), "ok");
    }

    @SneakyThrows
    public String querySigRepo(CustomPropertiesConfig queryConf, String sig) {
        sig = sig == null ? "*" : sig;
        String queryJson = String.format(queryConf.getSigRepoQueryStr(), sig);
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getSigIndex(), queryJson);
        String responseBody = future.get().getResponseBody(UTF_8);
        JsonNode dataNode = objectMapper.readTree(responseBody);

        Iterator<JsonNode> buckets = dataNode.get("aggregations").get("group_field").get("buckets").elements();
        ArrayList<String> repoList = new ArrayList<>();
        while (buckets.hasNext()) {
            JsonNode bucket = buckets.next();
            String repo = bucket.get("key").asText();
            repoList.add(repo);
        }

        return resultJsonStr(200, objectMapper.valueToTree(repoList), "ok");
    }

    @SneakyThrows
    public String queryCompanyName(CustomPropertiesConfig queryConf, String community) {
        List<String> companyList = queryClaCompany(queryConf.getClaCorporationIndex());
        List<Map<String, String>> companies = getCompanyNameCnEn(env.getProperty("company.name.yaml"), env.getProperty("company.name.local.yaml"));
        Map<String, String> companyNameCnEn = companies.get(0);
        Map<String, String> companyNameAlCn = companies.get(1);

        List<HashMap<String, Object>> companyNameList = new ArrayList<>();
        for (String company : companyList) {
            HashMap<String, Object> nameMap = new HashMap<>();
            String companyCn = companyNameAlCn.getOrDefault(company.trim(), company.trim());
            String companyEn = companyNameCnEn.getOrDefault(company.trim(), companyCn);
            nameMap.put("company_cn", companyCn);
            nameMap.put("company_en", companyEn);
            companyNameList.add(nameMap);
        }
        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put(community, companyNameList);

        return resultJsonStr(200, objectMapper.valueToTree(dataMap), "ok");
    }

    @SneakyThrows
    public String queryGroupUserContributors(QueryDao queryDao, CustomPropertiesConfig queryConf, String groupField,
                                             String group, String contributeType, String timeRange) {
        JsonNode TcOwners = querySigOwnerTypeCount(queryConf, "TC");

        JsonNode ownerType;
        switch (groupField) {
            case "sig":
                ownerType = querySigOwnerTypeCount(queryConf, group);
                break;
            case "company":
                // group = CompanyCN2Cla(community, group);
                group = getCompanyNames(group);
                ownerType = queryOwnerTypeCount(queryConf, group);
                break;
            default:
                return "";
        }

        Iterator<String> users = ownerType.fieldNames();
        HashMap<String, String> data = new HashMap<>();
        while (users.hasNext()) {
            String user = users.next();
            data.put(user.toLowerCase(), ownerType.get(user).asText());
        }
        JsonNode ownerTypeLower = objectMapper.valueToTree(data);

        String index = queryConf.getGiteeAllIndex();
        String label = queryDao.querySigLabel(queryConf).getOrDefault(group, null);
        String queryStr = queryConf.getAggGroupCountQueryStr(groupField, group, contributeType, timeRange, label);
        if (queryStr == null)
            return resultJsonStr(400, contributeType, ReturnCode.RC400.getMessage(), ReturnCode.RC400.getMessage());

        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, index, queryStr);
        String responseBody = future.get().getResponseBody(UTF_8);
        JsonNode dataNode = objectMapper.readTree(responseBody);
        Iterator<JsonNode> buckets = dataNode.get("aggregations").get("group_field").get("buckets").elements();

        ArrayList<JsonNode> dataList = new ArrayList<>();
        ArrayList<String> userList = new ArrayList<>();
        while (buckets.hasNext()) {
            JsonNode bucket = buckets.next();
            String giteeId = bucket.get("key").asText();
            long contribute = bucket.get("sum_field").get("value").asLong();
            if (contribute == 0 || robotUsers.contains(giteeId)) continue;

            String userType = ownerTypeLower.has(giteeId.toLowerCase()) ? userType = ownerTypeLower.get(giteeId.toLowerCase()).asText() : "contributor";
            HashMap<String, Object> dataMap = new HashMap<>();
            dataMap.put("gitee_id", giteeId);
            dataMap.put("contribute", contribute);
            dataMap.put("usertype", userType);
            if (TcOwners.has(giteeId)) dataMap.put("is_TC_owner", 1);

            JsonNode resNode = objectMapper.valueToTree(dataMap);
            dataList.add(resNode);
            userList.add(giteeId.toLowerCase());
        }

        Iterator<String> owners = ownerType.fieldNames();
        while (owners.hasNext()) {
            String owner = owners.next();
            if (userList.contains(owner.toLowerCase())) continue;

            HashMap<String, Object> dataMap = new HashMap<>();
            dataMap.put("gitee_id", owner);
            dataMap.put("contribute", 0);
            dataMap.put("usertype", ownerType.get(owner));
            if (TcOwners.has(owner)) dataMap.put("is_TC_owner", 1);

            JsonNode resNode = objectMapper.valueToTree(dataMap);
            dataList.add(resNode);
        }

        return resultJsonStr(200, objectMapper.valueToTree(dataList), "ok");
    }

    @SneakyThrows
    public String queryGroupSigContribute(QueryDao queryDao, CustomPropertiesConfig queryConf, String groupField,
                                          String group, String contributeType, String timeRange) {
        String field;
        switch (groupField) {
            case "user":
                field = "user_login.keyword";
                break;
            case "company":
                group = getCompanyNames(group);
                field = "tag_user_company.keyword";
                break;
            default:
                return "";
        }

        String index = queryConf.getGiteeAllIndex();
        String[] queryJson = queryConf.getGroupAggSigQueryStr().split(";");
        ArrayList<HashMap<String, Object>> tempList = new ArrayList<>();
        for (String query : queryJson) {
            String queryStr = queryConf.getAggGroupSigCountQueryStr(query, contributeType, timeRange, group, field);
            tempList.addAll(getData(queryDao, queryConf, index, queryStr));
        }

        HashMap<String, Long> dataMap = MapCombine(tempList);
        double sum = dataMap.get("sum");
        ArrayList<HashMap<String, Object>> resList = new ArrayList<>();
        for (String key : dataMap.keySet()) {
            if (key.equals("sum"))
                continue;
            HashMap<String, Object> map = new HashMap<>();
            double percent = dataMap.get(key) / sum;
            map.put("contribute", dataMap.get(key));
            if (key.equals("No-SIG")) key = "Others";
            map.put("sig_name", key);
            map.put("percent", percent);
            resList.add(map);
        }
        resList.sort((t1, t2) -> (int) (Long.parseLong(t2.get("contribute").toString()) - Long.parseLong(t1.get("contribute").toString())));

        ArrayList<JsonNode> dataList = new ArrayList<>();
        int rank = 1;
        for (HashMap<String, Object> item : resList) {
            item.put("rank", rank);
            JsonNode resNode = objectMapper.valueToTree(item);
            dataList.add(resNode);
            rank += 1;
        }
        return resultJsonStr(200, objectMapper.valueToTree(dataList), "ok");
    }

    @SneakyThrows
    public String queryCompanySigDetails(CustomPropertiesConfig queryConf, String company, String timeRange) {
        String companyStr = getCompanyNames(company);

        String[] queryStrs = queryConf.getAggCompanyGiteeQueryStr(queryConf.getCompanyUsers(), timeRange, companyStr);
        ListenableFuture<Response> f = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getGiteeAllIndex(), queryStrs[0]);
        JsonNode dataNode = objectMapper.readTree(f.get().getResponseBody(UTF_8));
        double userTotal = dataNode.get("aggregations").get("group_field").get("value").asDouble();

        HashMap<String, Integer> sigUserMetrics = getCompanySigUsers(queryConf, companyStr, timeRange);
        HashMap<String, Integer> sigContribute = getCompanySigContribute(queryConf, companyStr, timeRange, "pr");
        Iterator<String> sigList = sigUserMetrics.keySet().iterator();

        ArrayList<HashMap<String, Object>> resList = new ArrayList<>();
        HashMap<String, HashMap<String, String>> sigFeatures = getCommunityFeature(queryConf);
        while (sigList.hasNext()) {
            String sig = sigList.next();
            HashMap<String, Object> item = getSigFeature(sigFeatures, sig);
            Integer userCount = sigUserMetrics.get(sig);
            Integer contribute = sigContribute.get(sig);
            double percent = userTotal == 0 ? 0 : userCount / userTotal;
            item.put("user", userCount);
            item.put("userPercent", percent);
            item.put("contribute", contribute);
            resList.add(item);
        }
        return resultJsonStr(200, objectMapper.valueToTree(resList), "ok");
    }

    public HashMap<String, Object> getSigFeature(HashMap<String, HashMap<String, String>> sigFeatures, String sig) {
        HashMap<String, String> sigInfo = sigFeatures.get(sig);
        HashMap<String, Object> res = new HashMap<>();
        res.put("sig", sig);
        if (sigInfo != null) {
            String feature = sigInfo.getOrDefault("feature", "");
            String group = sigInfo.getOrDefault("group", "");
            String en_feature = sigInfo.getOrDefault("en_feature", "");
            String en_group = sigInfo.getOrDefault("en_group", "");
            res.put("feature", feature);
            res.put("group", group);
            res.put("en_feature", en_feature);
            res.put("en_group", en_group);
        }
        return res;
    }

    @SneakyThrows
    public HashMap<String, Integer> getCompanySigUsers(CustomPropertiesConfig queryConf, String companyStr, String timeRange) {
        String queryStr = queryConf.getQueryStrWithTimeRange(queryConf.getCompanySigUserQueryStr(), timeRange, companyStr);
        HashMap<String, Integer> sigMap = commonCompanySigContribute(queryConf.getGiteeAllIndex(), queryStr);
        return sigMap;
    }
    
    @SneakyThrows
    public HashMap<String, Integer> getCompanySigContribute(CustomPropertiesConfig queryConf, String companyStr,
            String timeRange, String contributeType) {
        String queryStr = queryConf.getAggCompanySigCountQueryStr(queryConf.getCompanyContributeQueryStr(), companyStr, timeRange,
                contributeType);
        HashMap<String, Integer> sigMap = commonCompanySigContribute(queryConf.getGiteeAllIndex(), queryStr);
        return sigMap;
    }

    @SneakyThrows
    protected HashMap<String, Integer> commonCompanySigContribute(String giteeIndex, String queryStr) {
        HashMap<String, Integer> sigMap = new HashMap<>();
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, giteeIndex, queryStr);
        String responseBody = future.get().getResponseBody(UTF_8);
        JsonNode dataNode = objectMapper.readTree(responseBody);
        Iterator<JsonNode> buckets = dataNode.get("aggregations").get("group_field").get("buckets").elements();
        int count = 0;
        while (buckets.hasNext()) {
            JsonNode bucket = buckets.next();
            String sig = bucket.get("key").asText();
            count = bucket.get("count").get("value").asInt();
            sigMap.put(sig, count);
        }
        return sigMap;
    }

    @SneakyThrows
    public String queryCompanyUsers(CustomPropertiesConfig queryConf, String company, String timeRange) {
        company = getCompanyNames(company);
        String index = queryConf.getGiteeAllIndex();
        String[] queryStrs = queryConf.getAggCompanyGiteeQueryStr(queryConf.getCompanyUsers(), timeRange, company);

        ArrayList<Integer> companyUsersList = new ArrayList<>();
        for (int i = 0; i < queryStrs.length; i++) {
            ListenableFuture<Response> f = esAsyncHttpUtil.executeSearch(esUrl, index, queryStrs[i]);
            String responseBody = f.get().getResponseBody(UTF_8);
            JsonNode dataNode = objectMapper.readTree(responseBody);
            int value = dataNode.get("aggregations").get("group_field").get("value").asInt();
            companyUsersList.add(value);
        }

        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("value", companyUsersList);
        List<String> metrics = Arrays.asList(new String[]{"D0", "D1", "D2"});
        dataMap.put("metrics", metrics);
        return resultJsonStr(200, objectMapper.valueToTree(dataMap), "ok");
    }

    @SneakyThrows
    public String queryCommunityRepos(CustomPropertiesConfig queryConf) {
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getGiteeAllIndex(), queryConf.getCommunityRepoQueryStr());
        JsonNode dataNode = objectMapper.readTree(future.get().getResponseBody(UTF_8));

        ArrayList<String> dataList = new ArrayList<>();
        Iterator<JsonNode> hits = dataNode.get("hits").get("hits").elements();
        while (hits.hasNext()) {
            JsonNode hit = hits.next();
            String repository = hit.get("_source").get("repository").asText();
            dataList.add(repository);
        }
        return resultJsonStr(200, objectMapper.valueToTree(dataList), "ok");
    }

    @SneakyThrows
    public String querySigScore(CustomPropertiesConfig queryConf, String sig, String timeRange, String type) {
        String index = type.equals("radar") ? queryConf.getSigRadarScoreIndex() : queryConf.getSigScoreIndex();
        String queryStr = queryConf.getQueryStrWithTimeRange(queryConf.getSigScoreQueryStr(), timeRange, sig);

        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, index, queryStr);
        String responseBody = future.get().getResponseBody(UTF_8);
        JsonNode dataNode = objectMapper.readTree(responseBody);
        Iterator<JsonNode> buckets = dataNode.get("hits").get("hits").elements();
        ArrayList<HashMap<String, Object>> sigList = new ArrayList<>();
        HashMap<String, HashMap<String, String>> sigFeatures = getCommunityFeature(queryConf);
        while (buckets.hasNext()) {
            JsonNode bucket = buckets.next().get("_source");
            HashMap<String, Object> data = objectMapper.convertValue(bucket, HashMap.class);
            HashMap<String, String> sigInfo = sigFeatures.get(sig);
            String feature = "";
            String group = "";
            String enFeature = "";
            String enGroup = "";
            if (sigInfo != null) {
                feature = sigInfo.get("feature");
                group = sigInfo.get("group");
                enFeature = sigInfo.get("en_feature");
                enGroup = sigInfo.get("en_group");
            }
            data.put("feature", feature);
            data.put("group", group);
            data.put("en_feature", enFeature);
            data.put("en_group", enGroup);
            sigList.add(data);
        }
        return resultJsonStr(200, objectMapper.valueToTree(sigList), "ok");
    }

    @SneakyThrows
    public String querySigScoreAll(CustomPropertiesConfig queryConf) {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, -1);
        String queryStr = String.format(queryConf.getAllSigScoreQueryStr(), c.getTimeInMillis());
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getSigScoreIndex(), queryStr);
        String responseBody = future.get().getResponseBody(UTF_8);
        JsonNode dataNode = objectMapper.readTree(responseBody);

        Iterator<JsonNode> buckets = dataNode.get("hits").get("hits").elements();
        if (!buckets.hasNext()) {
            c.add(Calendar.DATE, -1);
            queryStr = String.format(queryConf.getAllSigScoreQueryStr(), c.getTimeInMillis());
            future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getSigScoreIndex(), queryStr);
            responseBody = future.get().getResponseBody(UTF_8);
            dataNode = objectMapper.readTree(responseBody);
            buckets = dataNode.get("hits").get("hits").elements();
        }

        ArrayList<HashMap<String, Object>> sigList = new ArrayList<>();
        HashMap<String, HashMap<String, String>> sigFeatures = getCommunityFeature(queryConf);
        while (buckets.hasNext()) {
            JsonNode bucket = buckets.next().get("_source");
            HashMap<String, Object> data = objectMapper.convertValue(bucket, HashMap.class);
            String sig = bucket.get("sig_names").asText();
            HashMap<String, String> sigInfo = sigFeatures.get(sig);
            String feature = "";
            String group = "";
            String en_feature = "";
            String en_group = "";
            if (sigInfo != null) {
                feature = sigInfo.get("feature");
                group = sigInfo.get("group");
                en_feature = sigInfo.get("en_feature");
                en_group = sigInfo.get("en_group");
            }
            data.put("feature", feature);
            data.put("group", group);
            data.put("en_feature", en_feature);
            data.put("en_group", en_group);
            data.remove("value");
            sigList.add(data);
        }
        logger.info(resultJsonStr(200, objectMapper.valueToTree(sigList), "ok"));
        return resultJsonStr(200, objectMapper.valueToTree(sigList), "ok");
    }

    @SneakyThrows
    public String queryCompanySigs(CustomPropertiesConfig queryConf, String timeRange) {
        List<String> claCompanies = queryClaCompany(queryConf.getClaCorporationIndex());
        List<Map<String, String>> companies = getCompanyNameCnEn(env.getProperty("company.name.yaml"), env.getProperty("company.name.local.yaml"));
        Map<String, String> companyNameCnEn = companies.get(0);
        Map<String, String> companyNameAlCn = companies.get(1);

        String queryStr = queryConf.getQueryStrWithTimeRange(queryConf.getAllCompanySigQueryStr(), timeRange);
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getGiteeAllIndex(), queryStr);
        String responseBody = future.get().getResponseBody(UTF_8);
        JsonNode dataNode = objectMapper.readTree(responseBody);
        Iterator<JsonNode> buckets = dataNode.get("aggregations").get("group_field").get("buckets").elements();

        ArrayList<JsonNode> dataList = new ArrayList<>();
        while (buckets.hasNext()) {
            JsonNode bucket = buckets.next();
            String company = bucket.get("key").asText();
            if (!claCompanies.contains(company) ||
                company.equals("深圳易宝软件") ||
                company.contains("华为合作方") ||
                company.equalsIgnoreCase("openeuler")) {
                continue;
            }
            if (company.contains("华为技术有限公司")) {
                continue;
            }
            Iterator<JsonNode> its = bucket.get("sigs").get("buckets").elements();
            ArrayList<String> sigList = new ArrayList<>();
            while (its.hasNext()) {
                JsonNode it = its.next();
                sigList.add(it.get("key").asText());
            }
            String companyCn = companyNameAlCn.getOrDefault(company.trim(), company.trim());
            String companyEn = companyNameCnEn.getOrDefault(company.trim(), companyCn);
            HashMap<String, Object> dataMap = new HashMap<>();
            dataMap.put("company_cn", companyCn);
            dataMap.put("company_en", companyEn);
            dataMap.put("sigList", sigList);
            JsonNode resNode = objectMapper.valueToTree(dataMap);
            dataList.add(resNode);
        }
        return resultJsonStr(200, objectMapper.valueToTree(dataList), "ok");
    }

    @SneakyThrows
    public String querySigsOfTCOwners(CustomPropertiesConfig queryConf) {
        String index = queryConf.getSigIndex();
        String queryJson = queryConf.getUserOwnsSigStr();
        String yamlFile = queryConf.getTcOwnerUrl();

        JsonNode TC_owners = querySigOwnerTypeCount(queryConf, "TC");
        Map<String, String> userName = getUserNameCnEn(yamlFile);

        Iterator<String> users = TC_owners.fieldNames();
        ArrayList<HashMap<String, Object>> res = new ArrayList<>();
        while (users.hasNext()) {
            String user = users.next();
            String queryStr = String.format(queryJson, user);
            ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, index, queryStr);
            String responseBody = future.get().getResponseBody(UTF_8);
            JsonNode dataNode = objectMapper.readTree(responseBody);
            Iterator<JsonNode> buckets = dataNode.get("aggregations").get("2").get("buckets").elements();
            ArrayList<String> sigList = new ArrayList<>();
            while (buckets.hasNext()) {
                JsonNode bucket = buckets.next();
                sigList.add(bucket.get("key").asText());
            }
            HashMap<String, Object> resData = new HashMap<>();
            String user_cn = userName.get(user);
            resData.put("user", user);
            resData.put("name", user_cn);
            resData.put("sigs", sigList);
            res.add(resData);
        }
        HashMap<String, Object> resMap = new HashMap<>();
        resMap.put("code", 200);
        resMap.put("data", res);
        resMap.put("msg", "success");
        resMap.put("update_at", (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")).format(new Date()));
        return objectMapper.valueToTree(resMap).toString();
    }

    @SneakyThrows
    public String queryUserOwnerType(CustomPropertiesConfig queryConf, String userName) {
        String index = queryConf.getSigIndex();
        String queryStr = queryConf.getAllUserOwnerTypeQueryStr();

        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, index, queryStr);
        String responseBody = future.get().getResponseBody(UTF_8);
        JsonNode dataNode = objectMapper.readTree(responseBody);
        Iterator<JsonNode> buckets = dataNode.get("aggregations").get("group_field").get("buckets").elements();

        HashMap<String, ArrayList<Object>> userData = new HashMap<>();
        while (buckets.hasNext()) {
            JsonNode bucket = buckets.next();
            String sig = bucket.get("key").asText();
            Iterator<JsonNode> users = bucket.get("user").get("buckets").elements();
            while (users.hasNext()) {
                JsonNode userBucket = users.next();
                String user = userBucket.get("key").asText();
                if (!user.equalsIgnoreCase(userName)) continue;

                Iterator<JsonNode> types = userBucket.get("type").get("buckets").elements();
                ArrayList<String> typeList = new ArrayList<>();
                while (types.hasNext()) {
                    JsonNode type = types.next();
                    typeList.add(type.get("key").asText());
                }
                HashMap<String, Object> user_type = new HashMap<>();
                user_type.put("sig", sig);
                user_type.put("type", typeList);

                if (userData.containsKey(user.toLowerCase())) {
                    userData.get(user.toLowerCase()).add(user_type);
                } else {
                    ArrayList<Object> tempList = new ArrayList<>();
                    tempList.add(user_type);
                    userData.put(user.toLowerCase(), tempList);
                }
            }
        }

        HashMap<String, Object> resMap = new HashMap<>();
        resMap.put("code", 200);
        resMap.put("data", userData.get(userName.toLowerCase()));
        resMap.put("msg", "success");
        resMap.put("update_at", (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")).format(new Date()));
        return objectMapper.valueToTree(resMap).toString();
    }

    @SneakyThrows
    public String queryUserContributeDetails(QueryDao queryDao, CustomPropertiesConfig queryConf, String community, String user,
                                             String sig, String contributeType, String timeRange, String comment_type, String filter) {
        String index = queryConf.getGiteeAllIndex();
        ArrayList<Object> params = queryConf.getAggUserCountQueryParams(contributeType, timeRange);

        String label = sig != null ? queryDao.querySigLabel(queryConf).getOrDefault(sig, "*") : "*";
        String query = queryConf.getUserContributeDetailsQuery(queryConf, sig, label);

        RestHighLevelClient restHighLevelClient = getRestHighLevelClient();
        // BoolQueryBuilder queryBuilder = esQueryUtils.getQueryBuilder(params, comment_type, filter, user);
        return esQueryUtils.esUserCount(community, restHighLevelClient, index, user, sig, params, comment_type, filter, query);
    }

    @SneakyThrows
    public synchronized void putGiteeHookUser(CustomPropertiesConfig queryConf, Set<Map<String, Object>> users) {
        giteeWebhookList.addAll(users);

        if (giteeWebhookList.size() >= 100) {
            try {
                writeToEsIndex(giteeWebhookList, queryConf);
                logger.info("already write {} documents to es", giteeWebhookList.size());
                giteeWebhookList = new ArrayList<>();
            } catch (Exception e) {
                logger.error("write /gitee/webhook to es failed");
            }
        }
    }

    @SneakyThrows
    private synchronized void writeToEsIndex(List<Map<String, Object>> giteeWebhookList, CustomPropertiesConfig queryConf) {
        BulkRequest request = new BulkRequest();
        for (Map<String, Object> user : giteeWebhookList) {
            String id = user.get("id").toString() + "_" + user.get("email").toString() + "_" + user.get("gitee_id");
            request.add(new IndexRequest(queryConf.getGiteeEmailIndex(), "_doc", id).source(user));
        }
        String scheme = env.getProperty("es.private.scheme");
        String host = env.getProperty("es.private.host");
        int port = Integer.parseInt(env.getProperty("es.private.port", "9200"));
        String esUser = env.getProperty("es.private.user");
        String password = env.getProperty("es.private.password");
        try (RestHighLevelClient restHighLevelClient = RestHighLevelClientUtil.create(Collections.singletonList(host), port, scheme, esUser, password)) {
            restHighLevelClient.bulk(request, RequestOptions.DEFAULT);
        }
    }

    @SneakyThrows
    public String queryUserLists(CustomPropertiesConfig queryConf, String community, String group, String name) {
        String queryStr = queryConf.getAggUserListQueryStr(queryConf.getUserListQueryStr(), group, name);
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getGiteeAllIndex(), queryStr);

        String responseBody = future.get().getResponseBody(UTF_8);
        JsonNode dataNode = objectMapper.readTree(responseBody);
        Iterator<JsonNode> buckets = dataNode.get("aggregations").get("group_field").get("buckets").elements();

        ArrayList<String> dataMap = new ArrayList<>();
        while (buckets.hasNext()) {
            JsonNode bucket = buckets.next();
            String user = bucket.get("key").asText();
            dataMap.add(user);
        }
        HashMap<String, Object> resMap = new HashMap<>();
        resMap.put("code", 200);
        resMap.put("data", dataMap);
        resMap.put("msg", "success");
        resMap.put("update_at", (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")).format(new Date()));
        return objectMapper.valueToTree(resMap).toString();
    }

    @SneakyThrows
    public String querySigRepoCommitters(CustomPropertiesConfig queryConf, String sig) {
        String queryStr = String.format(queryConf.getSigRepoCommittersQueryStr(), sig);
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getSigIndex(), queryStr);

        String responseBody = future.get().getResponseBody(UTF_8);
        JsonNode dataNode = objectMapper.readTree(responseBody);
        Iterator<JsonNode> buckets = dataNode.get("aggregations").get("group_field").get("buckets").elements();

        ArrayList<Object> dataList = new ArrayList<>();
        ArrayList<String> committerList = new ArrayList<>();
        ArrayList<String> committerRepoList = new ArrayList<>();

        while (buckets.hasNext()) {
            JsonNode bucket = buckets.next();
            String repo = bucket.get("key").asText();
            Iterator<JsonNode> user_buckets = bucket.get("user").get("buckets").elements();
            ArrayList<String> userList = new ArrayList<>();
            while (user_buckets.hasNext()) {
                JsonNode userBucket = user_buckets.next();
                String user = userBucket.get("key").asText();
                userList.add(user);
                committerList.add(user);
            }
            HashMap<String, Object> dataMap = new HashMap<>();
            dataMap.put("repo", repo);
            dataMap.put("gitee_id", userList);
            dataList.add(dataMap);
            committerRepoList.add(repo);
        }
        Set<String> set = new HashSet<>(committerList);
        ArrayList<String> committers = new ArrayList<>(set);

        String res = querySigRepo(queryConf, sig);
        JsonNode resNode = objectMapper.readTree(res);
        if (resNode.get("code").asInt() == 200 && resNode.get("data").size() != 0) {
            Iterator<JsonNode> repos = resNode.get("data").elements();
            while (repos.hasNext()) {
                String repo = repos.next().asText();
                if (committerRepoList.contains(repo)) continue;

                HashMap<String, Object> dataMap = new HashMap<>();
                dataMap.put("repo", repo);
                dataMap.put("gitee_id", Collections.emptyList());
                dataList.add(dataMap);
            }
        }

        HashMap<String, Object> resData = new HashMap<>();
        String siginfo = querySigInfo(queryConf, sig);
        JsonNode sigMaintainers = objectMapper.readTree(siginfo).get("data");
        if (sigMaintainers.size() != 0) {
            JsonNode maintainers = sigMaintainers.get(0).get("maintainers");
            resData.put("maintainers", maintainers);
        }
        resData.put("committers", committers);
        resData.put("committerDetails", dataList);

        HashMap<String, Object> resMap = new HashMap<>();
        resMap.put("code", 200);
        resMap.put("data", resData);
        resMap.put("msg", "success");
        resMap.put("update_at", (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")).format(new Date()));
        return objectMapper.valueToTree(resMap).toString();
    }




    public HashMap<String, String> querySigLabel(CustomPropertiesConfig queryConf) {
        return new HashMap<>();
    }

    public ArrayList<String> getCompanyNameList(String name) {
        ArrayList<String> res = new ArrayList<>();
        res.add(name);
        YamlUtil yamlUtil = new YamlUtil();
        // CompanyYaml companies = yamlUtil.readUrlYaml(env.getProperty("company.name.yaml"), CompanyYaml.class);
        CompanyYaml companies = yamlUtil.readLocalYaml(env.getProperty("company.name.yaml"), CompanyYaml.class);
        for (CompanyYamlInfo companyInfo : companies.getCompanies()) {
            String cnCompany = companyInfo.getCompany_cn().trim();
            String enCompany = companyInfo.getCompany_en().trim();
            if (name.equals(cnCompany) || name.equals(enCompany)) {
                res.add(enCompany);
                res.add(cnCompany);
                List<String> aliases = companyInfo.getAliases();
                if (aliases != null) {
                    res.addAll(aliases);
                }
            }
        }
        return res;
    }

    public String queryUserCompany(CustomPropertiesConfig queryConf, String user) {
        String index = queryConf.getAccountOrgIndex();
        String company = "independent";
        if (StringUtils.isBlank(index)) return company;
        try {
            String queryJson = String.format(queryConf.getAccountOrgQueryStr(), user);
            ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getSigIndex(), queryJson);
            String responseBody = future.get().getResponseBody(UTF_8);
            JsonNode dataNode = objectMapper.readTree(responseBody);
            Iterator<JsonNode> buckets = dataNode.get("aggregations").get("group_field").get("buckets").elements();

            while (buckets.hasNext()) {
                JsonNode bucket = buckets.next();
                Iterator<JsonNode> orgBuckets = bucket.get("2").get("buckets").elements();
                if (orgBuckets.hasNext()) {
                    JsonNode orgBucket = orgBuckets.next();
                    company = orgBucket.get("key").asText();
                }
            }
        } catch (Exception e) {
            logger.error("exception", e);
        }
        return company;
    }

    public String getOneIdUserGiteeLoginName(HttpServletRequest request) {
        try {
            Cookie tokenCookie = getCookie(request);
            String s = String.format("%s/oneid/personal/center/user", env.getProperty("oneid.host"));
            HttpResponse<com.mashape.unirest.http.JsonNode> response = Unirest.get(s)
                    .header("token", request.getHeader("token"))
                    .header("Cookie", "_Y_G_=" + tokenCookie.getValue())
                    .asJson();
            JSONArray jsonArray = response.getBody().getObject().getJSONObject("data").getJSONArray("identities");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject identity = jsonArray.getJSONObject(i);
                if (identity.getString("identity").equals("gitee")) {
                    return identity.getString("login_name");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }


    public String resultJsonStr(int code, String item, Object data, String msg) {
        String updateAt = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")).format(new Date());
        return "{\"code\":" + code + ",\"data\":{\"" + item + "\":" + data + "},\"msg\":\"" + msg + "\",\"update_at\":\"" + updateAt + "\"}";
    }

    public String resultJsonStr(int code, Object data, String msg) {
        String updateAt = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")).format(new Date());
        return "{\"code\":" + code + ",\"data\":" + data + ",\"msg\":\"" + msg + "\",\"update_at\":\"" + updateAt + "\"}";
    }

    protected Map<String, Object> queryContributes(CustomPropertiesConfig queryConf, String community) {
        String giteeIndex = queryConf.getGiteeAllIndex();
        String claIndex = queryConf.getClaCorporationIndex();
        String contributesQueryStr = queryConf.getGiteeContributesQueryStr();

        long prs = 0;
        long issues = 0;
        long comments = 0;
        long repos = 0;
        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("prs", prs);
        dataMap.put("issues", issues);
        dataMap.put("comments", comments);
        dataMap.put("repos", repos);

        Map<String, Integer> communityPartners = getCommunityPartners(env.getProperty("community.partners.yaml"));
        Integer otherPartners = communityPartners.getOrDefault(community.toLowerCase(), 0);
        try {
            List<String> companies = queryClaCompany(claIndex);
            dataMap.put("partners", companies.size() + otherPartners);
        } catch (Exception ex) {
            dataMap.put("partners", otherPartners);
            logger.error("exception", ex);
        }

        try {
            ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, giteeIndex, contributesQueryStr);
            String responseBody = future.get().getResponseBody(UTF_8);
            JsonNode dataNode = objectMapper.readTree(responseBody);
            Iterator<JsonNode> buckets = dataNode.get("aggregations").get("orgs").get("buckets").elements();
            while (buckets.hasNext()) {
                JsonNode bucket = buckets.next();
                prs += bucket.get("prs").get("value").asLong();
                issues += bucket.get("issues").get("value").asLong();
                comments += bucket.get("comments").get("value").asLong();
                repos += bucket.get("repos").get("value").asLong();
            }
            dataMap.put("prs", prs);
            dataMap.put("issues", issues);
            dataMap.put("comments", comments);
            dataMap.put("repos", repos);
        } catch (Exception e) {
            logger.error("exception", e);
        }
        return dataMap;
    }

    protected Map<String, Integer> getCommunityPartners(String yamlFile) {
        YamlUtil yamlUtil = new YamlUtil();
        CommunityPartnersYaml communities = yamlUtil.readLocalYaml(yamlFile, CommunityPartnersYaml.class);
        // CommunityPartnersYaml communities = yamlUtil.readUrlYaml(yamlFile, CommunityPartnersYaml.class);

        HashMap<String, Integer> resMap = new HashMap<>();
        for (CommunityPartnersYamlInfo community : communities.getCommunity()) {
            int sum = community.getPartners().stream().mapToInt(Integer::intValue).sum();
            resMap.put(community.getName(), sum);
        }
        return resMap;
    }

    protected List<String> queryClaCompany(String index) throws Exception {
        ArrayList<String> companies = new ArrayList<>();
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, index, "{\"size\": 10000}");
        String responseBody = future.get().getResponseBody(UTF_8);
        JsonNode dataNode = objectMapper.readTree(responseBody);
        Iterator<JsonNode> hits = dataNode.get("hits").get("hits").elements();
        while (hits.hasNext()) {
            JsonNode source = hits.next().get("_source");
            companies.add(source.get("corporation_name").asText());
        }
        return companies;
    }

    protected String getBucketCount(ListenableFuture<Response> future, String dataFlag) {
        Response response;
        String statusText = ReturnCode.RC400.getMessage();
        long count = 0;
        int statusCode = 500;
        try {
            response = future.get();
            statusCode = response.getStatusCode();
            statusText = response.getStatusText();
            String responseBody = response.getResponseBody(UTF_8);
            JsonNode dataNode = objectMapper.readTree(responseBody);
            Iterator<JsonNode> buckets = dataNode.get("aggregations").get("distinct_field").get("buckets").elements();
            count = Lists.newArrayList(buckets).size();
        } catch (Exception e) {
            logger.error("exception", e);
        }
        return resultJsonStr(statusCode, dataFlag, count, statusText);
    }

    protected String parseDurationAggFromProjectHostArchPackageResult(ListenableFuture<Response> future, String dataFlag) {
        Response response;
        String statusText = ReturnCode.RC400.getMessage();
        double count = 0d;
        int statusCode = 500;
        try {
            response = future.get();
            statusCode = response.getStatusCode();
            statusText = response.getStatusText();
            String responseBody = response.getResponseBody(UTF_8);
            JsonNode dataNode = objectMapper.readTree(responseBody);

            JsonNode dataMap = dataNode.get("aggregations").get("datamap");
            if (dataMap == null) return null;

            HashMap<String, Object> projectObj = new HashMap<>();
            for (JsonNode project_bucket : dataMap.get("buckets")) {
                String projectName = project_bucket.get("key").asText();
                JsonNode hostarchNode = project_bucket.get("group_by_hostarch");

                HashMap<String, Object> archObj = new HashMap<>();
                for (JsonNode arch_bucket : hostarchNode.get("buckets")) {
                    String archName = arch_bucket.get("key").asText();
                    JsonNode archNode = arch_bucket.get("group_by_package");

                    HashMap<String, Object> packageObj = new HashMap<>();
                    for (JsonNode package_bucket : archNode.get("buckets")) {
                        String packageName = package_bucket.get("key").asText();
                        JsonNode value = package_bucket.get("avg_of_duration").get("value");
                        Double avgDurationSecs = Double.valueOf((new DecimalFormat("0.000")).format(value.asDouble()));
                        packageObj.put(packageName, avgDurationSecs);
                    }
                    archObj.put(archName, packageObj);
                }
                projectObj.put(projectName, archObj);
            }
            HashMap<String, Object> respro = new HashMap<>();
            respro.put(dataFlag, projectObj);
            HashMap<String, Object> resMap = new HashMap<>();
            resMap.put("code", 200);
            resMap.put("data", respro);
            resMap.put("msg", statusText);
            resMap.put("update_at", (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")).format(new Date()));
            return objectMapper.valueToTree(resMap).toString();
        } catch (Exception e) {
            logger.error("exception", e);
        }
        return resultJsonStr(statusCode, dataFlag, Math.round(count), statusText);
    }

    protected String getSumBucketValue(ListenableFuture<Response> future, String dataFlag) {
        Response response;
        String statusText = ReturnCode.RC400.getMessage();
        double count = 0d;
        int statusCode = 500;
        try {
            response = future.get();
            statusCode = response.getStatusCode();
            statusText = response.getStatusText();
            String responseBody = response.getResponseBody(UTF_8);
            JsonNode dataNode = objectMapper.readTree(responseBody);
            if (dataNode.get("aggregations").get("datamap") == null) {
                count = dataNode.get("aggregations").get("data").get("value").asDouble();
            } else {
                for (JsonNode jsonNode : dataNode.get("aggregations").get("datamap").get("buckets")) {
                    count += jsonNode.get("data").get("value").asDouble();
                }
            }
        } catch (Exception e) {
            logger.error("exception", e);
        }
        return resultJsonStr(statusCode, dataFlag, Math.round(count), statusText);
    }

    protected String getCountResult(ListenableFuture<Response> future, String dataFlag) {
        Response response;
        String statusText = ReturnCode.RC400.getMessage();
        double count = 0d;
        int statusCode = 500;
        try {
            response = future.get();
            statusCode = response.getStatusCode();
            statusText = response.getStatusText();
            String responseBody = response.getResponseBody(UTF_8);
            JsonNode dataNode = objectMapper.readTree(responseBody);
            JsonNode buckets = dataNode.get("aggregations").get("count").get("buckets");
            for (JsonNode bucket : buckets) {
                count += bucket.get("doc_count").asLong();
            }
        } catch (Exception e) {
            logger.error("exception", e);
        }
        return resultJsonStr(statusCode, dataFlag, Math.round(count), statusText);
    }

    protected String getGiteeResNum(String access_token, String community) throws Exception {
        AsyncHttpClient client = EsAsyncHttpUtil.getClient();
        RequestBuilder builder = esAsyncHttpUtil.getBuilder();
        Param access_tokenParam = new Param("access_token", access_token);
        Param visibility = new Param("visibility", "public");
        Param affiliation = new Param("affiliation", "admin");
        Param sort = new Param("sort", "full_name");
        Param direction = new Param("direction", "asc");
        Param q = new Param("q", community);
        Param page = new Param("page", "1");
        Param per_page = new Param("per_page", "1");
        ArrayList<Param> params = new ArrayList<>();
        params.add(access_tokenParam);
        params.add(visibility);
        params.add(affiliation);
        params.add(sort);
        params.add(direction);
        params.add(q);
        params.add(page);
        params.add(per_page);
        Request request = builder.setUrl(env.getProperty("gitee.user.repos")).setQueryParams(params)
                .addHeader("Content-Type", "application/json;charset=UTF-8").setMethod("GET").build();
        ListenableFuture<Response> responseListenableFuture = client.executeRequest(request);
        Response response = responseListenableFuture.get();
        String total_count = response.getHeader("total_count");
        return resultJsonStr(response.getStatusCode(), "modulenums", (total_count == null ? 0 : total_count), response.getStatusText());
    }

    protected String getOrgStarAndForkRes(ListenableFuture<Response> future, String dataflage, String community) {
        Response response;
        String statusText;
        String badReq;
        int statusCode;
        List<String> communities = Arrays.stream(community.split(",")).map(String::toLowerCase).toList();
        try {
            response = future.get();
            statusCode = response.getStatusCode();
            statusText = response.getStatusText();
            String responseBody = response.getResponseBody(UTF_8);
            JsonNode dataNode = objectMapper.readTree(responseBody);
            JsonNode buckets = dataNode.get("aggregations").get("owner").get("buckets");
            Iterator<JsonNode> it = buckets.elements();

            ArrayList<Object> res = new ArrayList<>();
            while (it.hasNext()) {
                JsonNode bucket = it.next();
                String com = bucket.get("key").asText();
                if (!communities.get(0).equals("allproject") && !communities.contains(com.toLowerCase())) {
                    continue;
                }
                HashMap dataMap = new HashMap();
                dataMap.put("community", com);
                dataMap.put("stars", bucket.get("stars").get("value").asInt());
                dataMap.put("forks", bucket.get("forks").get("value").asInt());
                dataMap.put("pulls", bucket.get("pulls").get("value").asInt());
                dataMap.put("issues", bucket.get("issues").get("value").asInt());
                dataMap.put("commits", bucket.get("commits").get("value").asInt());
                res.add(objectMapper.valueToTree(dataMap));
            }

            HashMap resMap = new HashMap();
            resMap.put("code", statusCode);
            resMap.put("data", res);
            resMap.put("msg", statusText);
            resMap.put("update_at", (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")).format(new Date()));
            return objectMapper.valueToTree(resMap).toString();
        } catch (Exception e) {
            statusText = "fail";
            badReq = ReturnCode.RC400.getMessage();
            statusCode = 500;
            logger.error("exception", e);
        }
        return resultJsonStr(statusCode, dataflage, badReq, statusText);
    }

    protected String dataDesensitizationProcessing(String jsonRes, String item) {
        JsonNode dataMap;
        try {
            dataMap = objectMapper.readTree(jsonRes);
            Iterator<JsonNode> buckets = dataMap.get("data").elements();
            ArrayList<JsonNode> dataList = new ArrayList<>();
            while (buckets.hasNext()) {
                ObjectNode bucket = (ObjectNode) buckets.next();
                String email = bucket.get("email").asText();
                String desensitizedEmail = StringDesensitizationUtils.maskEmail(email);
                bucket.put("email", desensitizedEmail);
                dataList.add(bucket);
            }
            ObjectNode resMap = (ObjectNode) dataMap;
            resMap.putPOJO("data", dataList);
            return objectMapper.valueToTree(resMap).toString();
        } catch (Exception e) {
            logger.error("exception", e);
            return resultJsonStr(400, item, ReturnCode.RC400.getMessage(), ReturnCode.RC400.getMessage());
        }
    }

    protected ArrayList<JsonNode> getObsDetails(ListenableFuture<Response> future) throws Exception {
        String responseBody = future.get().getResponseBody(UTF_8);
        JsonNode dataNode = objectMapper.readTree(responseBody);
        JsonNode hits = dataNode.get("hits").get("hits");
        Iterator<JsonNode> it = hits.elements();

        ArrayList<JsonNode> dataList = new ArrayList<>();
        while (it.hasNext()) {
            JsonNode hit = it.next();
            JsonNode source = hit.get("_source");
            HashMap<String, Object> packageMap = new HashMap<>();
            packageMap.put("repo_name", source.get("package").asText());
            packageMap.put("obs_version", source.get("versrel").asText());
            packageMap.put("architecture", source.get("hostarch").asText());
            packageMap.put("obs_branch", source.get("project").asText());
            packageMap.put("build_state", source.get("code").asText());

            ArrayList<Long> buildTimes = new ArrayList<>();
            buildTimes.add(source.get("duration").asLong());
            packageMap.put("history_build_times", buildTimes);

            JsonNode resNode = objectMapper.valueToTree(packageMap);
            dataList.add(resNode);
        }
        return dataList;
    }

    protected ArrayList<JsonNode> getIsoBuildTimes(CustomPropertiesConfig queryConf, IsoBuildTimesVo body) throws Exception {
        List<String> branches = new ArrayList<>();
        Integer limit = body.getLimit();
        int size = (limit == null) ? 10 : limit;

        if (body.getBranchs() == null) {
            String queryStr = "{\"size\": 0,\"aggs\": {\"obs_project\": {\"terms\": {\"field\": \"obs_project.keyword\",\"size\": 10000,\"min_doc_count\": 1}}}}";
            ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getIsoBuildIndex(), queryStr);
            String responseBody = future.get().getResponseBody(UTF_8);
            JsonNode dataNode = objectMapper.readTree(responseBody);
            JsonNode jsonNode = dataNode.get("aggregations").get("obs_project").get("buckets");
            Iterator<JsonNode> it = jsonNode.elements();
            while (it.hasNext()) {
                JsonNode next = it.next();
                branches.add(next.get("key").asText());
            }
        } else {
            branches = body.getBranchs();
        }

        ArrayList<JsonNode> dataList = new ArrayList<>();
        HashMap<String, Object> dataMap = new HashMap<>();
        for (String branch : branches) {
            String queryStr = String.format(queryConf.getIsoBuildQueryStr(), branch, size);
            ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getIsoBuildIndex(), queryStr);
            String responseBody = future.get().getResponseBody(UTF_8);
            JsonNode dataNode = objectMapper.readTree(responseBody);

            JsonNode jsonNode = dataNode.get("hits").get("hits");
            Iterator<JsonNode> it = jsonNode.elements();
            while (it.hasNext()) {
                JsonNode hit = it.next();
                JsonNode source = hit.get("_source");
                dataMap.put("branch", source.get("obs_project").asText());
                dataMap.put("date", source.get("archive_start").asText());
                dataMap.put("build_result", "");
                dataMap.put("build_time", source.get("build_version_time").asLong());
                dataMap.put("iso_time", source.get("make_ios_time").asLong());
                JsonNode resNode = objectMapper.valueToTree(dataMap);
                dataList.add(resNode);
            }
        }
        return dataList;
    }

    protected ArrayList<JsonNode> getSigDetails(ListenableFuture<Response> future, CustomPropertiesConfig queryConf, SigDetailsVo body) throws Exception {
        List<String> sig_names = body.getSigs();
        String responseBody = future.get().getResponseBody(UTF_8);
        JsonNode dataNode = objectMapper.readTree(responseBody);

        ArrayList<JsonNode> dataList = new ArrayList<>();
        JsonNode jsonNode = dataNode.get("hits").get("hits");
        Iterator<JsonNode> it = jsonNode.elements();
        while (it.hasNext()) {
            SigDetails sig = new SigDetails();
            JsonNode hit = it.next();
            JsonNode source = hit.get("_source");

            ArrayList<SigDetailsMaintainer> maintainers = new ArrayList<>();
            JsonNode maintainerInfo = source.get("maintainer_info");
            if (maintainerInfo != null) {
                Iterator<JsonNode> jsonNodes = maintainerInfo.elements();
                while (jsonNodes.hasNext()) {
                    JsonNode maintainer = jsonNodes.next();
                    JsonNode giteeId = maintainer.get("gitee_id");
                    String giteeIdStr = giteeId == null ? "" : giteeId.asText();
                    JsonNode email = maintainer.get("email");
                    String emailStr = email == null ? "" : email.asText();
                    maintainers.add(new SigDetailsMaintainer(giteeIdStr, emailStr));
                }
            } else {
                Iterator<JsonNode> jsonNodes = source.get("maintainers").elements();
                while (jsonNodes.hasNext()) {
                    JsonNode maintainer = jsonNodes.next();
                    maintainers.add(new SigDetailsMaintainer(maintainer.textValue(), ""));
                }
            }

            ArrayList<String> repos = new ArrayList<>();
            Iterator<JsonNode> repoNodes = source.get("repos").elements();
            while (repoNodes.hasNext()) {
                JsonNode repo = repoNodes.next();
                repos.add(repo.textValue());
            }
            String description = source.get("description") == null ? "" : source.get("description").asText();

            sig.setName(source.get("sig_name").asText());
            sig.setDescription(description);
            sig.setMaintainer(maintainers);
            sig.setRepositories(repos);
            JsonNode resNode = objectMapper.convertValue(sig, JsonNode.class);

            if (sig_names == null) {
                dataList.add(resNode);
            } else if (sig_names.contains(sig.getName())) {
                dataList.add(resNode);
            }
        }

        return dataList;
    }

    protected String parseIssueScoreFutureRes(ListenableFuture<Response> future) {
        Response response;
        String statusText = ReturnCode.RC400.getMessage();
        int statusCode = 500;
        ArrayList<HashMap<String, Object>> resJsonArray = new ArrayList<>();
        try {
            response = future.get();
            statusCode = response.getStatusCode();
            statusText = response.getStatusText();

            if (statusCode != 200) return resultJsonStr(statusCode, resJsonArray, statusText);

            String responseBody = response.getResponseBody(UTF_8);
            JsonNode dataNode = objectMapper.readTree(responseBody);
            JsonNode records = dataNode.get("aggregations").get("group_by_user_login").get("buckets");

            for (JsonNode record : records) {
                String issue_author = record.get("key").asText();
                Double issue_score = record.get("sum_of_score").get("value").asDouble();

                HashMap<String, Object> recordJsonObj = new HashMap<>();
                recordJsonObj.put("issue_author", issue_author);
                recordJsonObj.put("issue_score", issue_score);
                resJsonArray.add(recordJsonObj);
            }
            HashMap<String, Object> resMap = new HashMap<>();
            resMap.put("code", 200);
            resMap.put("data", resJsonArray);
            resMap.put("msg", statusText);
            return objectMapper.valueToTree(resMap).toString();
        } catch (Exception e) {
            logger.error("exception", e);
        }
        return resultJsonStr(statusCode, resJsonArray, statusText);
    }

    protected SearchSourceBuilder assembleResultSourceBuilder(String sortKeyword, BuildCheckInfoQueryVo buildCheckInfoQueryVo) {
        SearchSourceBuilder builder = new SearchSourceBuilder();
        builder.sort(sortKeyword, SortOrder.DESC);
        builder.sort("_id", SortOrder.DESC);

        String prUrl = buildCheckInfoQueryVo.getPrUrl();
        String prTitle = buildCheckInfoQueryVo.getPrTitle();
        String prCommitter = buildCheckInfoQueryVo.getPrCommitter();
        String prBranch = buildCheckInfoQueryVo.getPrBranch();
        String buildNo = buildCheckInfoQueryVo.getBuildNo();
        String checkTotal = buildCheckInfoQueryVo.getCheckTotal();
        Map<String, String> buildDuration = buildCheckInfoQueryVo.getBuildDuration();
        Map<String, String> prCreateTime = buildCheckInfoQueryVo.getPrCreateTime();
        Map<String, String> resultUpdateTime = buildCheckInfoQueryVo.getResultUpdateTime();
        Map<String, String> resultBuildTime = buildCheckInfoQueryVo.getResultBuildTime();

        String minDurationTime = buildDuration.get("min_duration_time");
        String maxDurationTime = buildDuration.get("max_duration_time");
        String prCreateStartTime = prCreateTime.get("start_time");
        String prCreateEndTime = prCreateTime.get("end_time");
        String resultUpdateStartTime = resultUpdateTime.get("start_time");
        String resultUpdateEndTime = resultUpdateTime.get("end_time");
        String resultBuildStartTime = resultBuildTime.get("start_time");
        String resultBuildEndTime = resultBuildTime.get("end_time");

        TermQueryBuilder termPrUrlQueryBuilder = null;
        TermQueryBuilder termPrTitleQueryBuilder = null;
        TermQueryBuilder termPrCommitterQueryBuilder = null;
        TermQueryBuilder termPrBranchQueryBuilder = null;
        TermQueryBuilder termBuildNoQueryBuilder = null;
        TermQueryBuilder termCheckTotalQueryBuilder = null;
        RangeQueryBuilder rangeBuildTimeQueryBuilder = null;
        RangeQueryBuilder rangePrCreateTimeQueryBuilder = null;
        RangeQueryBuilder rangeResultUpdateTimeQueryBuilder = null;
        RangeQueryBuilder rangeResultBuildTimeQueryBuilder = null;

        if (!StringUtil.isNullOrEmpty(prUrl))
            termPrUrlQueryBuilder = QueryBuilders.termQuery("pr_url.keyword", prUrl);
        if (!StringUtil.isNullOrEmpty(prTitle))
            termPrTitleQueryBuilder = QueryBuilders.termQuery("pr_title.keyword", prTitle);
        if (!StringUtil.isNullOrEmpty(prCommitter))
            termPrCommitterQueryBuilder = QueryBuilders.termQuery("pr_committer.keyword", prCommitter);
        if (!StringUtil.isNullOrEmpty(prBranch))
            termPrBranchQueryBuilder = QueryBuilders.termQuery("pr_branch.keyword", prBranch);
        if (!StringUtil.isNullOrEmpty(buildNo))
            termBuildNoQueryBuilder = QueryBuilders.termQuery("build_no", Long.parseLong(buildNo));
        if (!StringUtil.isNullOrEmpty(checkTotal))
            termCheckTotalQueryBuilder = QueryBuilders.termQuery("check_total.keyword", checkTotal);
        if (!StringUtil.isNullOrEmpty(minDurationTime)) {
            rangeBuildTimeQueryBuilder = QueryBuilders.rangeQuery("build_time").gte(minDurationTime);
        }
        if (!StringUtil.isNullOrEmpty(maxDurationTime)) {
            rangeBuildTimeQueryBuilder = rangeBuildTimeQueryBuilder.lte(maxDurationTime);
        }

        if (!StringUtil.isNullOrEmpty(prCreateStartTime)) {
            rangePrCreateTimeQueryBuilder = QueryBuilders.rangeQuery("pr_create_at").gte(prCreateStartTime)
                    .format("yyyy-MM-dd HH:mm:ss");
        }
        if (!StringUtil.isNullOrEmpty(prCreateEndTime)) {
            rangePrCreateTimeQueryBuilder = rangePrCreateTimeQueryBuilder.lte(prCreateEndTime)
                    .format("yyyy-MM-dd HH:mm:ss");
        }

        if (!StringUtil.isNullOrEmpty(resultUpdateStartTime)) {
            rangeResultUpdateTimeQueryBuilder = QueryBuilders.rangeQuery("update_at").gte(resultUpdateStartTime)
                    .format("yyyy-MM-dd HH:mm:ss");
        }
        if (!StringUtil.isNullOrEmpty(resultUpdateEndTime)) {
            rangeResultUpdateTimeQueryBuilder = rangeResultUpdateTimeQueryBuilder.lte(resultUpdateEndTime)
                    .format("yyyy-MM-dd HH:mm:ss");
        }

        if (!StringUtil.isNullOrEmpty(resultBuildStartTime)) {
            rangeResultBuildTimeQueryBuilder = QueryBuilders.rangeQuery("build_at").gte(resultBuildStartTime)
                    .format("yyyy-MM-dd HH:mm:ss");
        }
        if (!StringUtil.isNullOrEmpty(resultBuildEndTime)) {
            rangeResultBuildTimeQueryBuilder = rangeResultBuildTimeQueryBuilder.lte(resultBuildEndTime)
                    .format("yyyy-MM-dd HH:mm:ss");
        }

        BoolQueryBuilder mustQuery = QueryBuilders.boolQuery();

        if (termPrUrlQueryBuilder != null) mustQuery = mustQuery.must(termPrUrlQueryBuilder);
        if (termPrTitleQueryBuilder != null) mustQuery = mustQuery.must(termPrTitleQueryBuilder);
        if (termPrCommitterQueryBuilder != null) mustQuery = mustQuery.must(termPrCommitterQueryBuilder);
        if (termPrBranchQueryBuilder != null) mustQuery = mustQuery.must(termPrBranchQueryBuilder);
        if (termBuildNoQueryBuilder != null) mustQuery = mustQuery.must(termBuildNoQueryBuilder);
        if (termCheckTotalQueryBuilder != null) mustQuery = mustQuery.must(termCheckTotalQueryBuilder);

        if (rangeBuildTimeQueryBuilder != null)
            mustQuery = mustQuery.must(rangeBuildTimeQueryBuilder);
        if (rangePrCreateTimeQueryBuilder != null)
            mustQuery = mustQuery.must(rangePrCreateTimeQueryBuilder);
        if (rangeResultUpdateTimeQueryBuilder != null)
            mustQuery = mustQuery.must(rangeResultUpdateTimeQueryBuilder);
        if (rangeResultBuildTimeQueryBuilder != null)
            mustQuery = mustQuery.must(rangeResultBuildTimeQueryBuilder);

        builder.query(mustQuery);
        return builder;
    }

    protected SearchSourceBuilder assembleMistakeSourceBuilder(String sortKeyword, BuildCheckInfoQueryVo buildCheckInfoQueryVo) {
        SearchSourceBuilder builder = new SearchSourceBuilder();
        builder.sort(sortKeyword, SortOrder.DESC);
        return builder;
    }

    protected boolean isLocatedInTimeWindow(BuildCheckInfoQueryVo buildCheckInfoQueryVo, String resultMistakeLatestTimeStr) {
        boolean justifiedResult = false;
        SimpleDateFormat simpleDateFormatWithTimeZone = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String DEFAULT_START_TIME = "2000-01-01 00:00:00";
        String DEFAULT_END_TIME = "2100-01-01 00:00:00";
        String mistakeUpdateStartTimeStr = buildCheckInfoQueryVo.getMistakeUpdateTime().get("start_time");
        String mistakeUpdateEndTimeStr = buildCheckInfoQueryVo.getMistakeUpdateTime().get("end_time");
        if (StringUtil.isNullOrEmpty(mistakeUpdateStartTimeStr)) {
            mistakeUpdateStartTimeStr = DEFAULT_START_TIME;
        }
        if (StringUtil.isNullOrEmpty(mistakeUpdateEndTimeStr)) {
            mistakeUpdateEndTimeStr = DEFAULT_END_TIME;
        }

        Date resultMistakeLatestTime = null;
        Date mistakeUpdateStartTime = null;
        Date mistakeUpdateEndTime = null;
        try {
            resultMistakeLatestTime = simpleDateFormatWithTimeZone.parse(resultMistakeLatestTimeStr);
            mistakeUpdateStartTime = simpleDateFormat.parse(mistakeUpdateStartTimeStr);
            mistakeUpdateEndTime = simpleDateFormat.parse(mistakeUpdateEndTimeStr);
        } catch (ParseException e) {
            logger.error("exception", e);
        }
        assert resultMistakeLatestTime != null;
        if (resultMistakeLatestTime.compareTo(mistakeUpdateStartTime) >= 0 &&
                resultMistakeLatestTime.compareTo(mistakeUpdateEndTime) <= 0) {
            justifiedResult = true;
        }
        return justifiedResult;
    }

    protected Cookie getCookie(HttpServletRequest httpServletRequest) {
        Cookie[] cookies = httpServletRequest.getCookies();
        Cookie cookie = null;
        if (cookies != null) {
            // 获取cookie中的token
            Optional<Cookie> first = Arrays.stream(cookies).filter(c -> Objects.equals(env.getProperty("cookie.token.name"), c.getName())).findFirst();
            if (first.isPresent()) cookie = first.get();
        }
        return cookie;
    }

    protected JsonNode querySigOwnerTypeCount(CustomPropertiesConfig queryConf, String sig) {
        String index = queryConf.getSigIndex();
        String queryJson = queryConf.getSigOwnerType();
        if (queryJson == null) return null;

        String queryStr = String.format(queryJson, sig);
        return commonOwnerType(index, queryStr);
    }

    protected JsonNode queryOwnerTypeCount(CustomPropertiesConfig queryConf, String company) {
        String index = queryConf.getSigIndex();
        String queryJson = queryConf.getAllSigOwnerType();
        if (queryJson == null) return null;

        String queryStr = String.format(queryJson, company);
        return commonOwnerType(index, queryStr);
    }

    protected JsonNode commonOwnerType(String index, String queryStr) {
        try {
            ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, index, queryStr);
            String responseBody = future.get().getResponseBody(UTF_8);
            JsonNode dataNode = objectMapper.readTree(responseBody);
            Iterator<JsonNode> buckets = dataNode.get("aggregations").get("group_field").get("buckets").elements();

            HashMap<String, Object> dataMap = new HashMap<>();
            while (buckets.hasNext()) {
                JsonNode bucket = buckets.next();
                String giteeId = bucket.get("key").asText();
                if (robotUsers.contains(giteeId)) continue;
                if (dataMap.containsKey(giteeId) && dataMap.get(giteeId).equals("maintainers")) continue;

                Iterator<JsonNode> ownerTypeBucket = bucket.get("owner_type").get("buckets").elements();
                String ownerType = "committers";
                while (ownerTypeBucket.hasNext()) {
                    JsonNode ownerTypeNode = ownerTypeBucket.next();
                    ownerType = ownerTypeNode.get("key").asText();
                    if (ownerType.equals("maintainers")) break;
                }
                dataMap.put(giteeId, ownerType);
            }
            return objectMapper.valueToTree(dataMap);
        } catch (Exception e) {
            logger.error("exception", e);
            return null;
        }
    }

    protected String getCompanyNames(String name) {
        ArrayList<String> res = getCompanyNameList(name);
        String names = "(";
        for (String r : res) {
            names = names + "\\\"" + r + "\\\",";
        }
        names = names + ")";
        return names;
    }

    protected ArrayList<HashMap<String, Object>> getData(QueryDao queryDao, CustomPropertiesConfig queryConf, String index, String queryStr) {
        ArrayList<HashMap<String, Object>> dataList = new ArrayList<>();
        HashMap<String, String> sigLabels = queryDao.querySigLabel(queryConf);
        try {

            ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, index, queryStr);
            String responseBody = future.get().getResponseBody(UTF_8);
            JsonNode dataNode = objectMapper.readTree(responseBody);

            Iterator<JsonNode> buckets = dataNode.get("aggregations").get("group_field").get("buckets").elements();
            while (buckets.hasNext()) {
                JsonNode bucket = buckets.next();
                long contribute = bucket.get("sum_field").get("value").asLong();
                HashMap<String, Object> dataMap = new HashMap<>();
                String sig_name = bucket.get("key").asText();
                sig_name = label2sig(sig_name, sigLabels);
                dataMap.put("sig_name", sig_name);
                dataMap.put("contribute", contribute);
                dataList.add(dataMap);
            }
            return dataList;
        } catch (Exception e) {
            logger.error("exception", e);
            return null;
        }
    }

    protected String label2sig(String label, HashMap<String, String> sigLabels) {
        String sig = label;
        if (null != sigLabels) {
            for (String key : sigLabels.keySet()) {
                if (label.equals(sigLabels.get(key))) {
                    sig = key;
                }
            }
        }
        return sig;
    }

    protected HashMap<String, Long> MapCombine(ArrayList<HashMap<String, Object>> list) {
        HashMap<String, Long> res = new HashMap<>();
        Long sum = 0l;
        for (HashMap<String, Object> map : list) {
            String keyName = map.get("sig_name").toString();
            Long keyValue = Long.parseLong(map.get("contribute").toString());
            sum += keyValue;
            if (!res.containsKey(keyName)) {
                res.put(keyName, keyValue);
            } else {
                Long value = res.get(keyName) + keyValue;
                res.put(keyName, value);
            }
        }
        res.put("sum", sum);
        return res;
    }

    protected String getBlueZoneContributesQuery(BlueZoneContributeVo body) {
        List<String> giteeIds = body.getGiteeId();
        List<String> githubIds = body.getGithubId();
        String startTime = body.getStartTime();
        String endTime = body.getEndTime();
        String query;

        //请求参数是否有gitee_id和github_id
        StringBuilder queryString = new StringBuilder();
        if (giteeIds != null && !giteeIds.isEmpty()) {
            for (String giteeId : giteeIds) {
                queryString.append("gitee_id.keyword:\\\"").append(giteeId).append("\\\" OR ");
            }
        }
        if (githubIds != null && !githubIds.isEmpty()) {
            for (String githubId : githubIds) {
                queryString.append("github_id.keyword:\\\"").append(githubId).append("\\\" OR ");
            }
        }
        String qStr = queryString.toString();
        if (StringUtils.isBlank(qStr)) qStr = "*";
        else qStr = qStr.substring(0, qStr.length() - 4);

        //请求参数是否有时间范围
        if (StringUtils.isNotBlank(startTime) && StringUtils.isNotBlank(endTime)) {
            String queryStr = "{\"size\": 10000,\"query\": {\"bool\": {\"filter\": [" +
                    "{\"range\": {\"created_at\": {\"gte\": \"%s\",\"lte\": \"%s\"}}}," +
                    "{\"query_string\": {\"analyze_wildcard\": true,\"query\": \"%s\"}}]}}}";
            query = String.format(queryStr, startTime, endTime, qStr);
        } else {
            String queryStr = "{\"size\": 10000,\"query\": {\"bool\": {\"filter\": [" +
                    "{\"query_string\": {\"analyze_wildcard\": true,\"query\": \"%s\"}}]}}}";
            query = String.format(queryStr, qStr);
        }

        return query;
    }

    protected String getBlueZoneContributesRes(ListenableFuture<Response> future, String dataFlag) {
        String statusText = "请求内部错误";
        String badReq = "参数有误";
        int statusCode = 500;
        try {
            Response response = future.get();
            statusCode = response.getStatusCode();
            statusText = response.getStatusText();
            String responseBody = response.getResponseBody(UTF_8);
            JsonNode dataNode = objectMapper.readTree(responseBody);
            JsonNode hits = dataNode.get("hits").get("hits");
            Iterator<JsonNode> it = hits.elements();
            ArrayList<Object> prList = new ArrayList<>();
            ArrayList<Object> issueList = new ArrayList<>();
            ArrayList<Object> commentList = new ArrayList<>();
            ArrayList<Object> commitList = new ArrayList<>();
            while (it.hasNext()) {
                JsonNode hit = it.next();
                String id = hit.get("_id").asText();
                JsonNode source = hit.get("_source");
                if (source.has("is_pr")) {
                    Map sourceMap = objectMapper.convertValue(source, Map.class);
                    sourceMap.put("id", id);
                    JsonNode pr = objectMapper.valueToTree(sourceMap);
                    prList.add(pr);
                }
                if (source.has("is_issue")) {
                    Map sourceMap = objectMapper.convertValue(source, Map.class);
                    sourceMap.put("id", id);
                    sourceMap.remove("url");
                    JsonNode pr = objectMapper.valueToTree(sourceMap);
                    issueList.add(sourceMap);
                }
                if (source.has("is_comment")) {
                    Map sourceMap = objectMapper.convertValue(source, Map.class);
                    sourceMap.put("id", id);
                    sourceMap.remove("url");
                    JsonNode pr = objectMapper.valueToTree(sourceMap);
                    commentList.add(sourceMap);
                }
                if (source.has("is_commit")) {
                    Map sourceMap = objectMapper.convertValue(source, Map.class);
                    sourceMap.put("id", id);
                    sourceMap.remove("url");
                    JsonNode pr = objectMapper.valueToTree(sourceMap);
                    commitList.add(sourceMap);
                }
            }
            HashMap<String, Object> dataMap = new HashMap<>();
            dataMap.put("prs", prList);
            dataMap.put("issues", issueList);
            dataMap.put("comments", commentList);
            dataMap.put("commits", commitList);
            JsonNode jsonNode1 = objectMapper.valueToTree(dataMap);

            HashMap<String, Object> resMap = new HashMap<>();
            resMap.put("code", statusCode);
            resMap.put("data", jsonNode1);
            resMap.put("msg", statusText);
            resMap.put("update_at", (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")).format(new Date()));
            return objectMapper.valueToTree(resMap).toString();
        } catch (Exception e) {
            logger.error("exception", e);
        }
        return resultJsonStr(statusCode, dataFlag, badReq, statusText);
    }

    protected RestHighLevelClient getRestHighLevelClient() {
        String scheme = env.getProperty("es.scheme");
        String host = env.getProperty("es.host");
        int port = Integer.parseInt(env.getProperty("es.port", "9200"));
        String esUser = env.getProperty("es.user");
        String password = env.getProperty("es.password");
        return HttpClientUtils.restClient(host, port, scheme, esUser, password);
    }

    protected List<Map<String, String>> getCompanyNameCnEn(String yamlFile, String localYamlPath) {
        YamlUtil yamlUtil = new YamlUtil();
        // String localFile = yamlUtil.wget(yamlFile, localYamlPath);
        CompanyYaml companies = yamlUtil.readLocalYaml(yamlFile, CompanyYaml.class);

        HashMap<String, String> company_enMap = new HashMap<>();
        HashMap<String, String> company_cnMap = new HashMap<>();
        ArrayList<Map<String, String>> res = new ArrayList<>();
        for (CompanyYamlInfo company : companies.getCompanies()) {
            List<String> aliases = company.getAliases();
            String companyEn = company.getCompany_en().trim();
            String companyCn = company.getCompany_cn().trim();
            if (aliases != null) {
                for (String alias : aliases) {
                    company_enMap.put(alias, companyEn);
                    company_cnMap.put(alias, companyCn);
                }
            }
            company_enMap.put(company.getCompany_cn().trim(), companyEn);
        }
        res.add(company_enMap);
        res.add(company_cnMap);
        return res;
    }

    protected HashMap<String, HashMap<String, String>> getCommunityFeature(CustomPropertiesConfig queryConf) {
        HashMap<String, HashMap<String, String>> resData = new HashMap<>();
        try {
            String yamlFile = queryConf.getSigFeatureUrl();
            YamlUtil yamlUtil = new YamlUtil();
            SigYaml res = yamlUtil.readLocalYaml(yamlFile, SigYaml.class);
            // SigYaml res = yamlUtil.readUrlYaml(yamlFile, SigYaml.class);

            List<GroupYamlInfo> features = res.getFeature();
            for (GroupYamlInfo feature : features) {
                String group = feature.getGroup();
                String en_group = feature.getEn_group();
                List<SigYamlInfo> groupInfo = feature.getGroup_list();
                for (SigYamlInfo item : groupInfo) {
                    List<String> sigs = item.getSigs();
                    for (String sig : sigs) {
                        HashMap<String, String> it = new HashMap<>();
                        String name = item.getName();
                        String en_name = item.getEn_name();
                        it.put("group", group);
                        it.put("feature", name);
                        it.put("en_group", en_group);
                        it.put("en_feature", en_name);
                        resData.put(sig, it);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("exception", e);
        }
        return resData;
    }

    protected Map<String, String> getUserNameCnEn(String yamlFile) {
        YamlUtil yamlUtil = new YamlUtil();
        UserNameYaml users = yamlUtil.readLocalYaml(yamlFile, UserNameYaml.class);
        // UserNameYaml users = yamlUtil.readUrlYaml(yamlFile, UserNameYaml.class);

        HashMap<String, String> userMap = new HashMap<>();
        for (UserInfoYaml user : users.getUsers()) {
            String user_en = user.getEn().trim();
            String user_cn = user.getCn().trim();
            userMap.put(user_en, user_cn);
        }
        return userMap;
    }

    @SneakyThrows
    public String querySigPrStateCount(CustomPropertiesConfig queryConf, String sig, Long ts) {
        sig = StringUtils.isBlank(sig) ? "*" : sig;
        String queryJson = String.format(queryConf.getSigPrStateCountQuery(), ts, sig);
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getGiteeAllIndex(), queryJson);
        return parseSigPrStateCountRes(future);
    }

    protected String parseSigPrStateCountRes(ListenableFuture<Response> future) {
        Response response;
        String statusText = ReturnCode.RC400.getMessage();
        int statusCode = 500;
        HashMap<String, Object> recordJsonObj = new HashMap<>();
        try {
            response = future.get();
            statusCode = response.getStatusCode();
            statusText = response.getStatusText();
            if (statusCode != 200) return resultJsonStr(statusCode, recordJsonObj, statusText);

            String responseBody = response.getResponseBody(UTF_8);
            JsonNode dataNode = objectMapper.readTree(responseBody);
            JsonNode records = dataNode.get("aggregations").get("group_field").get("buckets");
            for (JsonNode record : records) {
                recordJsonObj.put("merged", record.get("merged").get("value"));
                recordJsonObj.put("closed", record.get("closed").get("value"));
                recordJsonObj.put("open", record.get("open").get("value"));
            }
            return resultJsonStr(statusCode, objectMapper.valueToTree(recordJsonObj), statusText);
        } catch (Exception e) {
            logger.error("exception", e);
        }
        return resultJsonStr(statusCode, recordJsonObj, statusText);
    }

    @SneakyThrows
    public String queryClaName(CustomPropertiesConfig queryConf, Long ts) {
        String claIndex = queryConf.getClaCorporationIndex();
        String queryJson = queryConf.getClaNameQuery();
        ArrayList<String> companies = new ArrayList<>();
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, claIndex, String.format(queryJson, ts));
        String responseBody = future.get().getResponseBody(UTF_8);
        JsonNode dataNode = objectMapper.readTree(responseBody);
        Iterator<JsonNode> hits = dataNode.get("hits").get("hits").elements();
        while (hits.hasNext()) {
            JsonNode source = hits.next().get("_source");
            companies.add(source.get("corporation_name").asText());
        }
        return resultJsonStr(200, objectMapper.valueToTree(companies), "ok");
    }

    public String getIPLocation(String ip) {
        InputStream database = obsDao.getData();
        try {
            DatabaseReader reader = new DatabaseReader.Builder(database).build();
            InetAddress ipAddress = InetAddress.getByName(ip);
            CityResponse response = reader.city(ipAddress);

            String continent_name = response.getContinent().getName();
            String region_iso_code = response.getMostSpecificSubdivision().getName();
            String city_name = response.getCity().getName();
            String country_iso_code = response.getCountry().getIsoCode();
            Double lon = response.getLocation().getLatitude();
            Double lat = response.getLocation().getLongitude();

            HashMap<String, Object> location = new HashMap<>();
            location.put("lon", lon);
            location.put("lat", lat);

            HashMap<String, Object> loc = new HashMap<>();
            loc.put("continent_name", continent_name);
            loc.put("region_iso_code", region_iso_code);
            loc.put("city_name", city_name);
            loc.put("country_iso_code", country_iso_code);
            loc.put("location", location);

            HashMap<String, Object> res = new HashMap<>();
            res.put("ip", ip);
            res.put("geoip", loc);

            String result = objectMapper.valueToTree(res).toString();
            return result;
        } catch (Exception e) {
            logger.error("exception", e);
        }
        return resultJsonStr(404, null, "error");
    }

    public String getEcosystemRepoInfo(CustomPropertiesConfig queryConf, String ecosystemType, String lang, String sortOrder) {
        return resultJsonStr(400, null, "error");
    }

    public String getSigReadme(CustomPropertiesConfig queryConf, String sig, String lang) {
        return resultJsonStr(400, null, "error");
    }

    @SneakyThrows
    public String getCommunityIsv(CustomPropertiesConfig queryConf, String localYamlPath) {
        String yamlFile = queryConf.getIsvYamlUrl();
        YamlUtil yamlUtil = new YamlUtil();
        CommunityIsvYaml communityIsvs = yamlUtil.readLocalYaml(yamlFile, CommunityIsvYaml.class);       
        return objectMapper.valueToTree(communityIsvs.getList()).toString();
    }

    public String putMeetupApplyForm(CustomPropertiesConfig queryConf, String item, MeetupApplyForm meetupApplyForm, String token) {
        ArrayList<String> errorMesseages = meetupApplyForm.validMeetupApplyFormField();
        if (errorMesseages.size() > 0) {
            return "{\"code\":400,\"data\":{\"" + item + "\":\"write error\"},\"msg:" + errorMesseages + "\"}";
        }
        Map meetupApplyFormMap = objectMapper.convertValue(meetupApplyForm, Map.class);
        return putDataSource(queryConf.getMeetupApplyFormIndex(), meetupApplyFormMap, token);
    }

    public String getUserId(String token){
        String userId = null;
        try {
            RSAPrivateKey privateKey = RSAUtil.getPrivateKey(env.getProperty("rsa.authing.privateKey"));
            DecodedJWT decode = JWT.decode(RSAUtil.privateDecrypt(token, privateKey));
            userId = decode.getAudience().get(0);
        } catch (Exception e) {
            logger.error("exception", e);
        }
        return userId;
    }

    public String putDataSource(String indexName, Map dataSource, String token) {
        String userId = getUserId(token);
        if (userId == null)
            return "{\"code\":400,\"data\":\"user failed\",\"msg\":\"user failed\"}";
        LocalDateTime now = LocalDateTime.now();
        String nowStr = now.toString().split("\\.")[0] + "+08:00";
        dataSource.put("created_at", nowStr);
        dataSource.put("user_id", userId);

        BulkRequest request = new BulkRequest();
        request.add(new IndexRequest(indexName, "_doc", userId + nowStr).source(dataSource));

        String res = "{\"code\":400,\"data\":\"failed\",\"msg\":\"failed\"}";
        RestHighLevelClient restHighLevelClient = getRestHighLevelClient();
        if (request.requests().size() != 0) {
            try {
                BulkResponse bulk = restHighLevelClient.bulk(request, RequestOptions.DEFAULT);
                int status_code = bulk.status().getStatus();
                if (status_code == 200) {
                    res = "{\"code\":200,\"data\":\"success\",\"msg\":\"success\"}";
                }
            } catch (IOException e) {
                logger.error("exception", e);
            }
        }
        try {
            restHighLevelClient.close();
        } catch (IOException e) {
            logger.error("exception", e);
        }
        return res;
    }

    @SneakyThrows
    public String queryCommunityVersions(CustomPropertiesConfig queryConf) {
        String index = queryConf.getGiteeVersionIndex();
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, index, queryConf.getCommunityVersions());
        JsonNode dataNode = objectMapper.readTree(future.get().getResponseBody(UTF_8));
        JsonNode buckets = dataNode.get("aggregations").get("group_field").get("buckets");
        ArrayList<String> versions = new ArrayList<>();
        for (JsonNode bucket : buckets) {
            String version = bucket.get("key").asText();
            if (version.contains("Next") || version.contains("LoongArch"))
                continue;
            versions.add(version);
        }
        return resultJsonStr(200, objectMapper.valueToTree(versions), "ok");
    }

    public String getRepoReadme(CustomPropertiesConfig queryConf, String name) {
        return resultJsonStr(400, null, "error");
    }

    public String getHuaweiCloudToken(String username, String password, String domain, String endpoint) {
        try {
            String body = "{\"auth\":{\"identity\":{\"methods\":[\"password\"],\"password\":{\"user\":{\"name\":\"%s\","
                    + " \"password\":\"%s\",\"domain\":{\"name\":\"%s\"}}}},\"scope\":{\"project\":{\"name\":\"cn-north-4\"}}}}";
            body = String.format(body, username, password, domain);
            HttpResponse<com.mashape.unirest.http.JsonNode> response = Unirest
                    .post(endpoint)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .asJson();

            String token = response.getHeaders().get("X-Subject-Token").get(0);
            return token;
        } catch (Exception e) {
            return null;
        }
    }

    public HashMap<String, Object> getExtends(QaBotRequestBody body){
        List<String> domain_Ids = domain_ids;
        HashMap<String, Object> extend = body.getExtend();
        if (extend != null && extend.containsKey("domain_ids")) {
            domain_Ids = ArrayListUtil.castList(extend.get("domain_ids"), String.class);
        }
        HashMap<String, Object> final_extend = new HashMap<>();
        final_extend.put("domain_ids", domain_Ids);
        return final_extend;
    }

    @SneakyThrows
    public String QaBotChat(QaBotRequestBody body) {
        String urlStr = env.getProperty("qa.endpoint") + "/v1/%s/qabots/%s/chat";
        HashMap<String, Object> data = new HashMap<>();
        data.put("question", body.getQuestion());
        data.put("extends", getExtends(body));
        data.put("chat_enable", true);
        List<Integer> query_types = Arrays.asList(0, 2);
        data.put("query_types", query_types);
        String dataStr = objectMapper.writeValueAsString(data);
        return QaBotRequest(dataStr, urlStr);
    }

    @SneakyThrows
    public String QaBotSuggestions(QaBotRequestBody body) {
        String urlStr = env.getProperty("qa.endpoint") + "/v1/%s/qabots/%s/suggestions";
        HashMap<String, Object> data = new HashMap<>();
        data.put("question", body.getQuestion());
        data.put("top", body.getTop());
        data.put("extends", getExtends(body));
        String dataStr = objectMapper.writeValueAsString(data);
        return QaBotRequest(dataStr, urlStr);
    }

    @SneakyThrows
    public String QaBotUserFeedback(QaBotRequestBody body) {
        String urlStr = env.getProperty("qa.endpoint") + "/v1/%s/qabots/%s/user_feedback";
        HashMap<String, Object> data = new HashMap<>();
        data.put("session_id", body.getSession_id());
        data.put("feedback", body.getFeedback());
        data.put("comment", body.getComment());
        String dataStr = objectMapper.writeValueAsString(data);
        return QaBotRequest(dataStr, urlStr);
    }

    @SneakyThrows
    public String QaBotSatisfaction(QaBotRequestBody body) {
        String urlStr = env.getProperty("qa.endpoint") + "/v1/%s/qabots/%s/requests/" + body.getRequest_id()
                + "/satisfaction";
        HashMap<String, Object> data = new HashMap<>();
        data.put("degree", body.getDegree());
        data.put("feedback_tag", body.getFeedback_tag());
        data.put("comment", body.getComment());
        String dataStr = objectMapper.writeValueAsString(data);
        return QaBotRequest(dataStr, urlStr);
    }

    public String QaBotRequest(String dataStr, String urlStr) {
        try {
            URL url = new URL(String.format(urlStr, env.getProperty("qa.project_id"), env.getProperty("qabot_id")));
            String token = getHuaweiCloudToken(env.getProperty("qa.user.name"), env.getProperty("qa.user.password"),
                    env.getProperty("qa.domain.name"), env.getProperty("qa.token.endpoint"));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.addRequestProperty("Content-Type", "application/json");
            connection.addRequestProperty("X-Auth-Token", token);

            OutputStreamWriter osw = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
            osw.append(dataStr);
            osw.flush();
            InputStream is = connection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String ans = "";
            while (br.ready()) {
                ans += br.readLine();
            }
            return ans;
        } catch (Exception e) {
            logger.error("exception", e);
            return resultJsonStr(400, "error", "error");
        }
    }

    @SneakyThrows
    public ResponseEntity queryReviewerRecommend(CustomPropertiesConfig queryConf, PrReviewerVo input) {
        String giteeAllIndex = queryConf.getGiteeAllIndex();
        String userTagIndex = queryConf.getUserTagIndex();

        String host = env.getProperty("es.host");
        int port = Integer.parseInt(env.getProperty("es.port", "9200"));
        String scheme = env.getProperty("es.scheme");
        String esUser = env.getProperty("es.user");
        String password = env.getProperty("es.password");
        RestHighLevelClient restHighLevelClient = HttpClientUtils.restClient(host, port, scheme, esUser, password);
        EsQueryUtils esQueryUtils = new EsQueryUtils();

        HashMap<String, UserTagInfo> inputUser2Info = new HashMap<>();
        for (String reviewer : input.getReviewers()) {
            UserTagInfo userTagInfo = new UserTagInfo();
            userTagInfo.setGiteeId(reviewer);
            inputUser2Info.put(reviewer, userTagInfo);
        }

        // 评论过相关PR的人 + 输入的人
        HashMap<String, UserTagInfo> user2Info = esQueryUtils.QueryPrReviewerByInter(restHighLevelClient, input, giteeAllIndex, robotUsers);
        inputUser2Info.putAll(user2Info);
    
        // 获取评论过该仓库的人
        Map<String, Map<String, Object>> mindsporeUserTag = esQueryUtils.QueryPrReviewerByRepo(restHighLevelClient, input, userTagIndex, inputUser2Info);

        // 返回测试结果
        ArrayList<String> keys = new ArrayList<>(mindsporeUserTag.keySet());
        ArrayList<String> reviewers = new ArrayList<>(new HashSet<>(input.getReviewers()));
        keys.removeAll(reviewers);

        List<String> resReviewers = randomItems(reviewers);
        List<String> res = randomItems(keys);
        res.addAll(resReviewers);
        return result(HttpStatus.OK, "success", res);
    }

    private ResponseEntity result(HttpStatus status, String msg, Object data) {
        HashMap<String, Object> res = new HashMap<>();
        res.put("code", status.value());
        res.put("data", data);
        res.put("msg", msg);
        res.put("update_at", (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")).format(new Date()));
        return new ResponseEntity<>(res, status);
    }

    private List<String> randomItems(List<String> items) {
        SecureRandom secureRandom =  new SecureRandom();
        ArrayList<String> res = new ArrayList<>();
        if (items.size() >= 2) {
            int i = secureRandom.nextInt(items.size());
            res.add(items.get(i));
            items.remove(i);
            i = secureRandom.nextInt(items.size());
            res.add(items.get(i));
        } else {
            res.addAll(items);
        }
        return res;

    }

    @SneakyThrows
    public Boolean moderation(String text, String token) {
        String body = String.format("{\"items\":[{\"text\":\"%s\",\"type\":\"content\"}]}", text);
        HttpResponse<String> response = Unirest.post(env.getProperty("moderation.url"))
                    .header("X-Auth-Token", token)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .asString();
        JsonNode resp = objectMapper.readTree(response.getBody());
        if (response.getStatus() == 200 && resp.get("result").get("suggestion").asText().equals("pass"))
            return true;
        return false;
    }

    public String getNps(CustomPropertiesConfig queryConf, String community, NpsBody body, String token) {
        HashMap<String, Object> resMap = new HashMap<>();
        resMap.put("community", community);
        resMap.put("feedbackPageUrl", body.getFeedbackPageUrl());
        resMap.put("feedbackValue", body.getFeedbackValue());
        resMap.put("feedbackText", body.getFeedbackText());
        if (!moderation(body.getFeedbackText(), token)) return resultJsonStr(400, null, "Content is not compliant");
        try{
            Date now = new Date();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
            String nowStr = simpleDateFormat.format(now);
            String uuid = UUID.randomUUID().toString();
            resMap.put("created_at", nowStr);

            BulkRequest request = new BulkRequest();
            RestHighLevelClient restHighLevelClient = getRestHighLevelClient();
            String index = queryConf.getNpsIndex();
            request.add(new IndexRequest(index, "_doc", uuid).source(resMap));
            if (request.requests().size() != 0)
                restHighLevelClient.bulk(request, RequestOptions.DEFAULT);
            restHighLevelClient.close();
            return resultJsonStr(200, objectMapper.valueToTree("success"), "success");
        } catch (Exception e) {
            logger.error("exception", e);
            return resultJsonStr(400, null, "error");
        }
    }

    @SneakyThrows
    public String queryInnovationItems(CustomPropertiesConfig queryConf) {
        List<String> res = getInnovationItemsNames(queryConf);
        return resultJsonStr(200, objectMapper.valueToTree(res), "ok");
    }

    @SneakyThrows
    public List<String> getInnovationItemsNames(CustomPropertiesConfig queryConf) {
        YamlUtil yamlUtil = new YamlUtil();
        InnovationItemYaml items = yamlUtil.readLocalYaml(queryConf.getInnovationItemAddress(), 
        InnovationItemYaml.class);
        List<String> res = new ArrayList<>();
        for (InnovationItemInfo item : items.getInnovation_projects()) {
            String name = item.getProject_name().trim();
            res.add(name);
        }
        return res;
    }

    @SneakyThrows
    public String queryAllProjects(CustomPropertiesConfig queryConf, String community, String timeRange, String groupField, String type) {
        String allProjectQueryStr = queryConf.getAggIssueQueryStr(queryConf, groupField, timeRange, type);
        if (StringUtils.isBlank(allProjectQueryStr)) {
            return resultJsonStr(400, null, "incorrect query");
        }
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getGiteeAllIndex(), allProjectQueryStr);
        JsonNode dataNode = objectMapper.readTree(future.get().getResponseBody(UTF_8));
        
        Iterator<JsonNode> buckets = dataNode.get("aggregations").get("group_field").get("buckets").elements();
        // 如果按公司排序，那么有中英文切换；如果按照SIG组排序，那么只输出英文名称
        List<Map<String, Object>> res = new ArrayList<>();
        if ("company".equals(groupField)) {
            res = packageByCompany(buckets, queryConf);
        } else if ("sig".equals(groupField)) {
            res = packageBySig(buckets);
        } else {
            return resultJsonStr(400, null, "incorrect input parameter");
        }
        return resultJsonStr(200, objectMapper.valueToTree(res), "ok");
    }

    @SneakyThrows
    public String querySigContributors(CustomPropertiesConfig queryConf, String community, String type, String timeRange) {
        String sigContributeQueryStr = queryConf.getAggSigContributeQueryStr(queryConf, type, timeRange);
        if (StringUtils.isBlank(sigContributeQueryStr)) {
            return resultJsonStr(400, null, "incorrect query");
        }
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getGiteeAllIndex(), sigContributeQueryStr);
        JsonNode dataNode = objectMapper.readTree(future.get().getResponseBody(UTF_8));
        Iterator<JsonNode> buckets = dataNode.get("aggregations").get("group_field").get("buckets").elements();
        List<Map<String, Object>> res = new ArrayList<>();
        // 按照SIG组排序
        res = packageBySig(buckets);
        return resultJsonStr(200, objectMapper.valueToTree(res), "ok");
    }

    @SneakyThrows
    public List<Map<String, Object>> packageByCompany(Iterator<JsonNode> buckets, CustomPropertiesConfig queryConf) {
        // 获取companies变量，保存公司的中英文对应名称
        List<Map<String, String>> companies = getCompanyNameCnEn(env.getProperty("company.name.yaml"),
            env.getProperty("company.name.local.yaml"));
        Map<String, String> companyNameCnEn = companies.get(0);
        Map<String, String> companyNameAlCn = companies.get(1);
        List<Map<String, Object>> dataList = new ArrayList<>();
        HashMap<String, Object> dataMap = new HashMap<>();
        while (buckets.hasNext()) {
            JsonNode bucket = buckets.next();
            String company = bucket.get("key").asText();
            long doneNumber = bucket.get("doc_count").asLong();
            if (doneNumber == 0) {
                continue;
            }
            String companyCn = companyNameAlCn.getOrDefault(company.trim(), company.trim());
            String companyEn = companyNameCnEn.getOrDefault(company.trim(), companyCn);
            dataMap.put("company", company);
            dataMap.put("company_en", companyEn);
            dataMap.put("company_cn", companyCn);
            dataMap.put("contribute", doneNumber);
            dataList.add(new HashMap<>(dataMap));
        }
        // 过滤返回结果
        List<Map<String, Object>> newList = filterData(dataList, queryConf);
        return newList;
    }

    @SneakyThrows
    public List<Map<String, Object>> filterData(List<Map<String, Object>> dataList, CustomPropertiesConfig queryConf) {
        List<String> claCompanies = queryClaCompany(queryConf.getClaCorporationIndex());
        List<Map<String, Object>> newList = new ArrayList<>();
        long independent = 0;
        Map<String, Object> dataMap = new HashMap<>();
        for (Map<String, Object> data : dataList) {
            String company = (String) data.get("company");
            if (!claCompanies.contains(company) ||
                    company.equals("深圳易宝软件") ||
                    company.contains("华为合作方") ||
                    company.equalsIgnoreCase("openeuler")) {
                independent += (long) data.get("contribute");
                continue;
            }
            if (company.contains("华为技术有限公司")) {
                continue;
            }
            dataMap.put("company_en", data.get("company_en"));
            dataMap.put("company_cn", data.get("company_cn"));
            dataMap.put("contribute", data.get("contribute"));
            newList.add(new HashMap<>(data));
        }
        dataMap.put("company_cn", "个人贡献者");
        dataMap.put("company_en", "independent");
        dataMap.put("contribute", independent);
        // 判断是否没有结果
        if (newList.size() == 0 && independent == 0) {
            return newList;
        }
        newList.add(new HashMap<>(dataMap));
        return newList;
    }

    @SneakyThrows
    public List<Map<String, Object>> packageBySig(Iterator<JsonNode> buckets) {
        List<Map<String, Object>> dataList = new ArrayList<>();
        Map<String, Object> dataMap = new HashMap<>();
        while (buckets.hasNext()) {
            JsonNode bucket = buckets.next();
            String sigName = bucket.get("key").asText();
            long doneNumber = bucket.get("doc_count").asLong();
            if (doneNumber == 0) {
                continue;
            }
            dataMap.put("sig_name", sigName);
            dataMap.put("contribute", doneNumber);
            dataList.add(new HashMap<>(dataMap));
        }
        return dataList;
    }

    @SneakyThrows
    public String queryByProjectName(CustomPropertiesConfig queryConf, String community, String timeRange, String groupField, String projectName, String type) {
        // 查询单个项目的结果
        List<Map<String, Object>> res = getSingleProject(queryConf, community, timeRange, groupField, projectName, type);
        return resultJsonStr(200, objectMapper.valueToTree(res), "ok");
    }

    @SneakyThrows
    public List<Map<String, Object>> getSingleProject(CustomPropertiesConfig queryConf, String community, String timeRange, String groupField, String projectName, String type) {
        String projectQueryStr = queryConf.getAggProjectPrQueryStr(queryConf, timeRange, groupField, projectName, type);
        if (StringUtils.isBlank(projectQueryStr)) {
            return new ArrayList<>();
        }
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getGiteeAllIndex(), projectQueryStr);
        JsonNode dataNode = objectMapper.readTree(future.get().getResponseBody(UTF_8));
        Iterator<JsonNode> buckets = dataNode.get("aggregations").get("group_field").get("buckets").elements();
        List<Map<String, Object>> res = new ArrayList<>();
        // 如果按公司排序，那么有中英文切换；如果按照SIG组排序，那么只输出英文名称
        if ("company".equals(groupField)) {
            res = packageByCompany(buckets, queryConf);
        } else if ("sig".equals(groupField)) {
            res = packageBySig(buckets);
        } else {
            res = new ArrayList<>();
        }
        return res;
    }

    @SneakyThrows
    public String queryAllInnoItems(CustomPropertiesConfig queryConf, String community, String timeRange, String groupField, String type) {
        // 获取所有创新项目的名称
        List<String> projectNames = getInnovationItemsNames(queryConf);
        List<Map<String, Object>> res = new ArrayList<>();
        for (String projectName : projectNames) {
            List<Map<String, Object>> projectRes = getSingleProject(queryConf, community, timeRange, groupField, projectName, type);
            res.addAll(projectRes);
        }
        // 合并不同创新项目的相同结果
        List<Map<String, Object>> merged = new ArrayList<>();
        if (!groupField.equals("company")) {
            merged = mergeSig(res);
        } else {
            merged = mergeCompany(res);
        }
        return resultJsonStr(200, objectMapper.valueToTree(merged), "ok");
    }

    @SneakyThrows
    public List<Map<String, Object>> mergeSig(List<Map<String, Object>> list) {
        // 合并sig组集合
        Set<String> set = new HashSet<>();
        for (Map<String, Object> map : list) {
            String sig_name = (String) map.get("sig_name");
            set.add(sig_name);
        }
        List<Map<String, Object>> res = new ArrayList<>();
        for (String sig_name : set) {
            Map<String, Object> resMap = new HashMap<>();
            resMap.put("sig_name", sig_name);
            resMap.put("contribute", (long) 0);
            for (Map<String, Object> map : list) {
                String mapSigName = (String) map.get("sig_name");
                if (mapSigName.equals(sig_name)) {
                    long contribute = (long) map.get("contribute");
                    resMap.put("contribute", (long) ((long) resMap.get("contribute") + contribute));
                }
            }
            res.add(resMap);
        }
        return res;
    }

    @SneakyThrows
    public List<Map<String, Object>> mergeCompany(List<Map<String, Object>> list) {
        // 合并公司集合
        Set<String> set = new HashSet<>();
        for (Map<String, Object> map : list) {
            String company_cn = (String) map.get("company_cn");
            set.add(company_cn);
        }
        List<Map<String, Object>> res = new ArrayList<>();
        for (String company_cn : set) {
            Map<String, Object> resMap = new HashMap<>();
            resMap.put("company_cn", company_cn);
            resMap.put("contribute", (long) 0);
            for (Map<String, Object> map : list) {
                String mapCn = (String) map.get("company_cn");
                String mapEn = (String) map.get("company_en");
                resMap.put("company_en", mapEn);
                if (mapCn.equals(company_cn)) {
                    long contribute = (long) map.get("contribute");
                    resMap.put("contribute", (long) ((long) resMap.get("contribute") + contribute));
                }
            }
            res.add(resMap);
        }
        return res;
    }
    
    @SneakyThrows
    public String querySigDefect(CustomPropertiesConfig queryConf, String community, String timeRange, String sigName) {
        // 查询本项目所有issue
        String[] types = new String[]{"allIssue", "closedIssue", "allCve", "fixedCve"};
        ListenableFuture<Response> future = null;
        String projectQueryStr = null;
        JsonNode dataNode = null;
        long res = 0;
        Map<String, Object> resMap = new HashMap<>();
        for (int i = 0; i < types.length; i++) {
            String type = types[i];
            projectQueryStr = queryConf.getAggSigDefectQueryStr(queryConf, timeRange, sigName, type);
            if (StringUtils.isBlank(projectQueryStr)) {
                resMap.put(type, (long) 0);
                continue;
            }
            future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getGiteeAllIndex(), projectQueryStr);
            dataNode = objectMapper.readTree(future.get().getResponseBody(UTF_8));
            res = dataNode.get("hits").get("total").get("value").asLong();
            resMap.put(type, res);
        }
        return resultJsonStr(200, objectMapper.valueToTree(resMap), "ok");
    }

    @SneakyThrows
    public String getVersionFeature(CustomPropertiesConfig queryConf, String community, String version, String groupField) {
        String companyFeature = queryConf.getAggCompanyFeatureQueryStr(queryConf, version, groupField);
        if (StringUtils.isBlank(companyFeature)) {
            return resultJsonStr(400, null, "incorrect query");
        }
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getGiteeFeatureIndex(), companyFeature);
        JsonNode dataNode = objectMapper.readTree(future.get().getResponseBody(UTF_8));
        Iterator<JsonNode> buckets = dataNode.get("aggregations").get("group_field").get("buckets").elements();
        // 如果按公司排序，那么有中英文切换；如果按照SIG组排序，那么只输出英文名称
        List<Map<String, Object>> res = new ArrayList<>();
        if ("company".equals(groupField)) {
            res = packageByCompany(buckets, queryConf);
        } else if ("sig".equals(groupField)) {
            res = packageBySig(buckets);
        } else {
            return resultJsonStr(400, null, "incorrect input parameter");
        }
        return resultJsonStr(200, objectMapper.valueToTree(res), "ok");
    }

    @SneakyThrows
    public String getVersionSig(CustomPropertiesConfig queryConf, String community, String type, String version) {
        String sigPr = queryConf.getAggSigVersionQuery(queryConf, type, version);
        if (StringUtils.isBlank(sigPr)) {
            return resultJsonStr(400, null, "incorrect query");
        }
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getGiteeVersionIndex(), sigPr);
        JsonNode dataNode = objectMapper.readTree(future.get().getResponseBody(UTF_8));
        Iterator<JsonNode> buckets = dataNode.get("aggregations").get("group_field").get("buckets").elements();
        List<Map<String, Object>> res = packageBySig(buckets);
        return resultJsonStr(200, objectMapper.valueToTree(res), "ok");
    }
    
    @SneakyThrows
    public String querySigContribute(CustomPropertiesConfig queryConf, String community, String timeRange, String projectName, String type, String version) {
        if (timeRange != null && projectName != null && version == null) { // 按照项目查找
            return queryByProjects(queryConf, community, timeRange, projectName, type);
        } else if (timeRange == null && projectName == null && version != null) {  // 按照版本查找
            return queryByVersion(queryConf, community, type, version);
        } else {
            return resultJsonStr(400, null, "incorrect input parameters.");
        }
    }

    @SneakyThrows
    public String queryByProjects(CustomPropertiesConfig queryConf, String community, String timeRange, String projectName, String type) {
        // 获取所有创新项目的名称
        List<String> projectNames = getInnovationItemsNames(queryConf);
        if ("all".equals(projectName)) { // 项目范围：全部项目
            if ("issue_cve".equals(type) || "issue_done".equals(type)) { // 度量指标：issue闭环个数,cve闭环个数
                return queryAllProjects(queryConf, community, timeRange, "sig", type);
            } else if ("pr".equals(type) || "issue".equals(type) || "comment".equals(type)) { // 度量指标：pr, issue, comment
                return querySigContributors(queryConf, community, type, timeRange);
            } else {
                return resultJsonStr(400, null, "incorrect input parameters.");
            }
        } else if ("allInnoItems".equals(projectName)) { // 项目范围：全部创新项目
            return queryAllInnoItems(queryConf, community, timeRange, "sig", type);
        } else if (projectNames.contains(projectName)) { // 项目范围：某个创新项目
            return queryByProjectName(queryConf, community, timeRange, "sig", projectName, type);
        } else {
            return resultJsonStr(400, null, "incorrect input parameters.");
        }
    }

    @SneakyThrows
    public String queryByVersion(CustomPropertiesConfig queryConf, String community, String type, String version) {
        if ("feature".equals(type)) {
            return getVersionFeature(queryConf, community, version, "sig");
        } else if ("pr".equals(type)) {
            return getVersionSig(queryConf, community, type, version);
        } else {
            return resultJsonStr(400, null, "incorrect input parameters.");
        }
    }

    @SneakyThrows
    public String queryModelFoundry(CustomPropertiesConfig queryConf, String repo) {
        String query = String.format(queryConf.getModelFoundryDownloadQueryStr(), 0, System.currentTimeMillis(), repo);
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getModelFoundryIndex(), query);
        Response response = future.get();
        Double count = 0.0;
        int statusCode = response.getStatusCode();
        String statusText = response.getStatusText();
        String responseBody = response.getResponseBody(UTF_8);
        JsonNode dataNode = objectMapper.readTree(responseBody);
        Iterator<JsonNode> buckets = dataNode.get("aggregations").get("group_field").get("buckets").elements();
        while (buckets.hasNext()) {
            JsonNode bucket = buckets.next();
            count += bucket.get("res").get("value").asDouble();
        }
        return resultJsonStr(statusCode, count, statusText);
    }

    @SneakyThrows
    public String queryModelFoundryTrends(CustomPropertiesConfig queryConf, String repo) {
        long currentTimeMillis = System.currentTimeMillis();
        Calendar alendar = Calendar.getInstance();
        alendar.setTime(new Date());
        alendar.add(Calendar.YEAR, -1);
        long pastTimeMillis = alendar.getTimeInMillis();
        String query = String.format(queryConf.getModelFoundryDownloadTrendQueryStr(), pastTimeMillis, currentTimeMillis, repo);
        ListenableFuture<Response> future = esAsyncHttpUtil.executeSearch(esUrl, queryConf.getModelFoundryIndex(), query);
        Response response = future.get();
        int statusCode = response.getStatusCode();
        String statusText = response.getStatusText();
        String responseBody = response.getResponseBody(UTF_8);
        JsonNode dataNode = objectMapper.readTree(responseBody);
        ArrayList<HashMap<String, Object>> resList = parseModelFoundryTrends(dataNode);
        return resultJsonStr(statusCode, objectMapper.valueToTree(resList), statusText);
    }

    @SneakyThrows
    protected ArrayList<HashMap<String, Object>> parseModelFoundryTrends(JsonNode dataNode){
        Iterator<JsonNode> buckets = dataNode.get("aggregations").get("group_field").get("buckets").elements();
        ArrayList<HashMap<String, Object>> resList = new ArrayList<>();
        while (buckets.hasNext()) {
            JsonNode bucket = buckets.next();
            Long period = bucket.get("key").asLong();
            Double cur = 0.0;
            if (bucket.has("res")) {
                cur = bucket.get("res").get("value").asDouble();
            }
            HashMap<String, Object> tmpMap = new HashMap<>();
            tmpMap.put("date", period);
            tmpMap.put("download", Math.round(cur));
            resList.add(tmpMap);
        }
        return resList;
    }
}
