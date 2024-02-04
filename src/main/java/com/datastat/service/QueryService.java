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

package com.datastat.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.datastat.config.context.QueryConfContext;
import com.datastat.dao.QueryDao;
import com.datastat.dao.RedisDao;
import com.datastat.dao.context.MetricDaoContext;
import com.datastat.dao.context.QueryDaoContext;
import com.datastat.dao.metric.MetricDao;
import com.datastat.model.CustomPropertiesConfig;
import com.datastat.model.vo.*;
import com.datastat.result.ReturnCode;
import com.datastat.util.ArrayListUtil;
import com.datastat.util.PageUtils;
import com.datastat.util.RSAUtil;
import com.datastat.util.StringValidationUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.datastat.model.DatastatRequestBody;
import com.datastat.model.NpsBody;
import com.datastat.model.QaBotRequestBody;
import com.datastat.model.meetup.MeetupApplyForm;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Primary
@Service(value = "queryService")
public class QueryService {
    @Autowired
    Environment env;

    @Autowired
    QueryConfContext queryConfContext;

    @Autowired
    QueryDaoContext queryDaoContext;

    @Autowired
    MetricDaoContext metricDaoContext;

    @Autowired
    RedisDao redisDao;

    @Autowired
    ObjectMapper objectMapper;

    private static long redisDefaultExpire;
    private static List<String> communityList;
    private static final Logger logger = LoggerFactory.getLogger(QueryService.class);

    @PostConstruct
    public void init() {
        redisDefaultExpire = Long.parseLong(env.getProperty("redis.keyExpire", "60"));
        communityList = Arrays.asList(env.getProperty("communitys").split(","));
    }

    public Boolean checkCommunity(String community) {
        if (communityList.contains(community.toLowerCase())) {
            return true;
        }
        return false;
    }

    public String queryContributors(HttpServletRequest request, String community) {
        String item = "contributors";
        String key = community + item;
        String result = null; //(String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.queryContributors(queryConf, item);
//            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryDurationAggFromProjectHostArchPackage(HttpServletRequest request, String community) {
        String item = "avgDuration";
        String key = community + item;
        String result = null; //(String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.queryDurationAggFromProjectHostArchPackage(queryConf, item);
//            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String querySigs(HttpServletRequest request, String community) {
        String item = "sigs";
        String key = community + item;
        String result = null; //(String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.querySigs(queryConf, item);
//            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryUsers(HttpServletRequest request, String community) {
        String item = "users";
        String key = community + item;
        String result = null; //(String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.queryUsers(queryConf, item);
//            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryNoticeUsers(HttpServletRequest request, String community) {
        String item = "noticeusers";
        String key = community + item;
        String result = null; //(String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.queryNoticeUsers(queryConf, item);
//            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryModuleNums(HttpServletRequest request, String community) {
        String item = "modulenums";
        String key = community + item;
        String result = null; //(String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.queryModuleNums(queryConf, item);
//            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryBusinessOsv(HttpServletRequest request, String community) {
        String item = "businessOsv";
        String key = community + item;
        String result = null; //(String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.queryBusinessOsv(queryConf, item);
//            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryCommunityMembers(HttpServletRequest request, String community) {
        String item = "communitymembers";
        String key = community + item;
        String result = null; //(String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.queryCommunityMembers(queryConf, item);
//            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryDownload(HttpServletRequest request, String community) {
        String item = "download";
        String key = community + item;
        String result = null; //(String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.queryDownload(queryConf, item);
//            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryCount(HttpServletRequest request, String community, String item) {
        String key = community + item;
        String result = null; //(String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.queryCount(queryConf, item);
//            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryAll(HttpServletRequest request, String community) throws Exception {
        if (!checkCommunity(community)) return getQueryDao(request).resultJsonStr(404, "error", "not found");
        String item = "all";
        String key = community.toLowerCase() + item;
        String result = (String) redisDao.get(key);

        JsonNode newData;
        JsonNode oldData = null;
        boolean isFlush = false;

        if (result != null) {
            JsonNode all = objectMapper.readTree(result);
            String updateAt = all.get("update_at").asText();
            oldData = all.get("data");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
            try {
                Date updateDate = sdf.parse(updateAt);
                Date now = new Date();
                long diffs = (now.getTime() - updateDate.getTime());
                if (diffs > Long.parseLong(env.getProperty("redis.flush.interval", "7200000"))) {
                    isFlush = true;
                }
            } catch (ParseException e) {
                logger.error("exception", e);
            }
        }

        if (isFlush || result == null) {
            boolean flag = false;
            try {
                QueryDao queryDao = getQueryDao(request);
                CustomPropertiesConfig queryConf = getQueryConf(request);
                String resultNew = queryDao.queryAll(queryConf, community);

                JsonNode allNew = objectMapper.readTree(resultNew);
                newData = allNew.get("data");
                if (oldData != null) {
                    flag = false; //TODO errorAlertService.errorAlert(community, oldData, newData);
                }
                if (!flag) {
                    redisDao.set(key, resultNew, -1L);
                    result = resultNew;
                }
            } catch (Exception e) {
                logger.error("exception", e);
            }
        }

        return result;
    }

    public String queryBlueZoneContributes(HttpServletRequest request, BlueZoneContributeVo body) {
        String item = "contributes";
        QueryDao queryDao = getQueryDao(request);
        CustomPropertiesConfig queryConf = getQueryConf(request);

        String token = queryConf.getBlueZoneApiToken();
        if (StringUtils.isBlank(body.getToken()) || !body.getToken().equals(token)) {
            return queryDao.resultJsonStr(401, item, "token error", "token error");
        }
        return queryDao.queryBlueZoneContributes(queryConf, body, item);
    }

    public String putBlueZoneUser(HttpServletRequest request, BlueZoneUserVo userVo) {
        String item = "user";
        QueryDao queryDao = getQueryDao(request);
        CustomPropertiesConfig queryConf = getQueryConf(request);

        String token = queryConf.getBlueZoneApiToken();
        if (StringUtils.isBlank(userVo.getToken()) || !userVo.getToken().equals(token)) {
            return queryDao.resultJsonStr(401, item, "token error", "token error");
        }
        return queryDao.putBlueZoneUser(queryConf, userVo, item);
    }

    public String queryOrgStarAndFork(HttpServletRequest request, String community) {
        String item = "starFork";
        String key = community + item;
        String result = null; //(String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            // CustomPropertiesConfig queryConf = getQueryConf(request);
            CustomPropertiesConfig queryConf = queryConfContext.getQueryConfig("queryConf");
            result = queryDao.queryOrgStarAndFork(queryConf, community, item);
//            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryCveDetails(HttpServletRequest request, String community, String lastCursor, String pageSize) {
        String item = "cveDetails";
        String key = community + item;
        String result = null;
        if (pageSize == null) result = (String) redisDao.get(key);

        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.queryCveDetails(queryConf, lastCursor, pageSize, item);
//            if (pageSize == null) redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryNewYearPer(HttpServletRequest request, String oauth2_proxy) {
        QueryDao queryDao = getQueryDao(request);
        String referer = request.getHeader("Referer");
        String community = null;
        try {
            community = referer.split("\\.")[1];
        } catch (Exception e) {
            logger.error("exception", e);
            return resultJsonStr(404, "error", "Referer error");
        }      
        CustomPropertiesConfig queryConf = getQueryConf(community);
        
        return queryDao.queryNewYearPer(queryConf, oauth2_proxy, community);
    }

    public String queryNewYearMonthCount(HttpServletRequest request, String oauth2_proxy) {
        QueryDao queryDao = getQueryDao(request);
        String referer = request.getHeader("Referer");
        String community = null;
        try {
            community = referer.split("\\.")[1];
        } catch (Exception e) {
            logger.error("exception", e);
            return resultJsonStr(404, "error", "Referer error");
        }      
        CustomPropertiesConfig queryConf = getQueryConf(community);
        return queryDao.queryNewYearMonthCount(queryConf, oauth2_proxy);
    }

    public String queryBugQuestionnaire(HttpServletRequest request, String community, String lastCursor, String pageSize) {
        String item = "bugQuestionnaire";
        String key = community + item;
        String result = null;
        if (pageSize == null) result = (String) redisDao.get(key);

        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.queryBugQuestionnaire(queryConf, lastCursor, pageSize, item);
//            if (pageSize == null) redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String putBugQuestionnaire(HttpServletRequest request, String community, String lang, BugQuestionnaireVo bugQuestionnaireVo) {
        String item = "bugQuestionnaire";
        String result = "";
        lang = lang == null ? "zh" : lang.toLowerCase();
        QueryDao queryDao = getQueryDao(request);
        CustomPropertiesConfig queryConf = getQueryConf(request);
        result = queryDao.putBugQuestionnaire(queryConf, community, item, lang, bugQuestionnaireVo);
        return result;
    }

    public String queryObsDetails(HttpServletRequest request, String branch, String limit) {
        QueryDao queryDao = getQueryDao(request);
        CustomPropertiesConfig queryConf = getQueryConf(request);
        return queryDao.queryObsDetails(queryConf, branch);
    }

    public String queryIsoBuildTimes(HttpServletRequest request, IsoBuildTimesVo body) {
        QueryDao queryDao = getQueryDao(request);
        CustomPropertiesConfig queryConf = getQueryConf(request);
        return queryDao.queryIsoBuildTimes(queryConf, body);
    }

    public String querySigDetails(HttpServletRequest request, SigDetailsVo body) {
        QueryDao queryDao = getQueryDao(request);
        CustomPropertiesConfig queryConf = getQueryConf(request);
        return queryDao.querySigDetails(queryConf, body);
    }

    public String queryCompanyContributors(HttpServletRequest request, String community, String contributeType, String timeRange, String version, String repo) {
        if (!checkCommunity(community)) return getQueryDao(request).resultJsonStr(404, "error", "not found");
        String item = "companyContribute";
        String key = community.toLowerCase() + item + contributeType.toLowerCase() + timeRange + version + repo;
        String result = (String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.queryCompanyContributors(queryConf, community, contributeType, timeRange, version, repo, null);
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryUserContributors(HttpServletRequest request, String community, String contributeType, String timeRange, String repo) {
        if (!checkCommunity(community)) return getQueryDao(request).resultJsonStr(404, "error", "not found");
        String item = "userContribute";
        String key = community.toLowerCase() + item + contributeType.toLowerCase() + timeRange.toLowerCase() + repo;
        String result = (String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.queryUserContributors(queryConf, community, contributeType, timeRange, repo, null);
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryIssueScore(HttpServletRequest request, String startDate, String endDate) {
        QueryDao queryDao = getQueryDao(request);
        CustomPropertiesConfig queryConf = getQueryConf(request);
        return queryDao.queryIssueScore(queryConf, startDate, endDate);
    }

    public String queryBuildCheckInfo(HttpServletRequest request, BuildCheckInfoQueryVo queryBody, String lastCursor, String pageSize) {
        String item = "buildCheckInfo";
        String validateResult = validateBuildCheckInfo(queryBody, item);
        if (!StringUtils.isBlank(validateResult)) {
            return validateResult;
        }
        QueryDao queryDao = getQueryDao(request);
        CustomPropertiesConfig queryConf = getQueryConf(queryBody.getCommunityName());
        return queryDao.queryBuildCheckInfo(queryConf, queryBody, item, lastCursor, pageSize);
    }

    public String putUserActionsInfo(HttpServletRequest request, String community, String data) {
        QueryDao queryDao = getQueryDao(request);
        if (!checkCommunity(community)) return queryDao.resultJsonStr(404, "error", "not found");
        return queryDao.putUserActionsInfo(community, data);
    }

    public String querySigName(HttpServletRequest request, String community, String lang) {
        if (!checkCommunity(community)) return getQueryDao(request).resultJsonStr(404, "error", "not found");
        String item = "sigsname";
        String key = community + item + lang;
        String result = (String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.querySigName(queryConf, community, lang);
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String querySigInfo(HttpServletRequest request, String community, String sig, String repo, String user, String search, String page, String pageSize) throws Exception {
        if (!checkCommunity(community)) return getQueryDao(request).resultJsonStr(404, "error", "not found");
        if (search != null && search.equals("fuzzy")) {
            return queryFuzzySigInfo(request, community, sig, repo, user, search, page, pageSize);
        }
        return querySigInfo(request, community, sig);
    }

    public String querySigRepo(HttpServletRequest request, String community, String sig, String page, String pageSize) throws Exception {
        String item = "repo";
        String key = community.toLowerCase() + sig + item;
        String result = (String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.querySigRepo(queryConf, sig);
            redisDao.set(key, result, redisDefaultExpire);
        }
        if (pageSize == null || page == null) return result;

        JsonNode all = objectMapper.readTree(result);
        if (all.get("data") != null) {
            JsonNode res = all.get("data");
            ArrayList<String> resList = objectMapper.convertValue(res, new TypeReference<>() {
            });

            int currentPage = Integer.parseInt(page);
            int pagesize = Integer.parseInt(pageSize);
            Map data = PageUtils.getDataByPage(currentPage, pagesize, resList);
            ArrayList<HashMap<String, Object>> dataList = new ArrayList<>();
            dataList.add((HashMap<String, Object>) data);
            HashMap<String, Object> resMap = new HashMap<>();
            resMap.put("code", 200);
            resMap.put("data", dataList);
            resMap.put("msg", "success");
            result = objectMapper.valueToTree(dataList).toString();
        }
        return result;
    }

    public String querySigCompanyContributors(HttpServletRequest request, String community, String contributeType, String timeRange, String sig) {
        if (!checkCommunity(community)) return getQueryDao(request).resultJsonStr(404, "error", "not found");
        String item = "companyContribute";
        String key = community.toLowerCase() + item + contributeType.toLowerCase() + timeRange.toLowerCase() + sig;
        String result = (String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.queryCompanyContributors(queryConf, community, contributeType, timeRange, null, null, sig);
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryCompanyName(HttpServletRequest request, String community) {
        String key = community.toLowerCase() + "companyname";
        String result = (String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.queryCompanyName(queryConf, community);
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryCompanyUserContribute(HttpServletRequest request, String community, String company, String contributeType,
                                             String timeRange, String token) {
        QueryDao queryDao = getQueryDao(request);
        CustomPropertiesConfig queryConf = getQueryConf(request);
        // 是否有企业访问权限
        boolean per = checkPermission(request, queryDao, queryConf, token, company);
        if (!per) return resultJsonStr(400, "", "No Permission");

        String key = community.toLowerCase() + company + "usertypecontribute_" + contributeType.toLowerCase() + timeRange.toLowerCase();
        String result = (String) redisDao.get(key);
        if (result == null) {
            result = queryDao.queryGroupUserContributors(queryDao, queryConf, "company", company, contributeType, timeRange);
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryCompanySigContribute(HttpServletRequest request, String community, String company, String contributeType,
                                            String timeRange, String token) {
        QueryDao queryDao = getQueryDao(request);
        CustomPropertiesConfig queryConf = getQueryConf(request);
        // 是否有企业访问权限
        boolean per = checkPermission(request, queryDao, queryConf, token, company);
        if (!per) return resultJsonStr(400, "", "No Permission");

        String key = community.toLowerCase() + company + "sigtypecontribute_" + contributeType.toLowerCase() + timeRange.toLowerCase();
        String result = (String) redisDao.get(key);
        if (result == null) {
            result = queryDao.queryGroupSigContribute(queryDao, queryConf, "company", company, contributeType, timeRange);
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryCompanySigDetails(HttpServletRequest request, String community, String company, String timeRange, String token) {
        QueryDao queryDao = getQueryDao(request);
        CustomPropertiesConfig queryConf = getQueryConf(request);
        // 是否有企业访问权限
        boolean per = checkPermission(request, queryDao, queryConf, token, company);
        if (!per) return resultJsonStr(400, "", "No Permission");

        String key = community.toLowerCase() + company + "sig" + timeRange.toLowerCase();
        String result = (String) redisDao.get(key);
        if (result == null) {
            result = queryDao.queryCompanySigDetails(queryConf, company, timeRange);
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String querySigUserTypeCount(HttpServletRequest request, String community, String sig, String contributeType, String timeRange) {
        if (!checkCommunity(community)) return getQueryDao(request).resultJsonStr(404, "error", "not found");
        String key = community.toLowerCase() + sig + "usertypecontribute_" + contributeType.toLowerCase() + timeRange.toLowerCase();
        String result = (String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.queryGroupUserContributors(queryDao, queryConf, "sig", sig, contributeType, timeRange);
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryCompanyUsers(HttpServletRequest request, String community, String company, String timeRange, String token) {
        QueryDao queryDao = getQueryDao(request);
        CustomPropertiesConfig queryConf = getQueryConf(request);
        // 是否有企业访问权限
        boolean per = checkPermission(request, queryDao, queryConf, token, company);
        if (!per) return resultJsonStr(400, "", "No Permission");

        String key = community.toLowerCase() + company + "companyusers" + timeRange.toLowerCase();
        String result = (String) redisDao.get(key);
        if (result == null) {
            result = queryDao.queryCompanyUsers(queryConf, company, timeRange);
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryCommunityRepos(HttpServletRequest request, String community) {
        if (!checkCommunity(community)) return getQueryDao(request).resultJsonStr(404, "error", "not found");
        String key = community.toLowerCase() + "repos";
        String result = null; //(String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.queryCommunityRepos(queryConf);
//            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String querySigScore(HttpServletRequest request, String community, String sig, String timeRange) {
        String key = community.toLowerCase() + sig + "sigscore" + timeRange.toLowerCase();
        String result = (String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.querySigScore(queryConf, sig, timeRange, "");
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String querySigScoreAll(HttpServletRequest request, String community) {
        if (!checkCommunity(community)) return getQueryDao(request).resultJsonStr(404, "error", "not found");
        String keyStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String key = community.toLowerCase() + "sigscoreall" + keyStr;
        String result = (String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.querySigScoreAll(queryConf);
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String querySigRadarScore(HttpServletRequest request, String community, String sig, String timeRange) {
        String key = community.toLowerCase() + sig + "sigradarscore" + timeRange.toLowerCase();
        String result = (String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.querySigScore(queryConf, sig, timeRange, "radar");
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryCompanySigs(HttpServletRequest request, String community, String timeRange) {
        if (!checkCommunity(community)) return getQueryDao(request).resultJsonStr(404, "error", "not found");
        String key = community.toLowerCase() + "companysigs" + timeRange.toLowerCase();
        String result = (String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.queryCompanySigs(queryConf, timeRange);
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String querySigsOfTCOwners(HttpServletRequest request, String community) {
        if (!checkCommunity(community)) return getQueryDao(request).resultJsonStr(404, "error", "not found");
        String key = community.toLowerCase() + "sigs_of_tc_owners";
        String result = (String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.querySigsOfTCOwners(queryConf);
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryUserSigContribute(HttpServletRequest request, String community, String user, String contributeType, String timeRange) {
        if (!checkCommunity(community)) return getQueryDao(request).resultJsonStr(404, "error", "not found");
        String key = community.toLowerCase() + user + "sigtypecontribute_" + contributeType.toLowerCase() + timeRange.toLowerCase();
        String result = (String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.queryGroupSigContribute(queryDao, queryConf, "user", user, contributeType, timeRange);
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryUserOwnerType(HttpServletRequest request, String community, String user) {
        if (!checkCommunity(community)) return getQueryDao(request).resultJsonStr(404, "error", "not found");
        String key = community.toLowerCase() + user + "ownertype";
        String result = (String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.queryUserOwnerType(queryConf, user);
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryUserContributeDetails(HttpServletRequest request, String community, String user, String sig, String contributeType,
                                             String timeRange, String page, String pageSize, String comment_type, String filter) throws Exception {
        if (!checkCommunity(community)) return getQueryDao(request).resultJsonStr(404, "error", "not found");
        String key = community.toLowerCase() + user + sig + contributeType.toLowerCase() + timeRange.toLowerCase() + comment_type;
        String result = (String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.queryUserContributeDetails(queryDao, queryConf, community, user, sig, contributeType, timeRange, comment_type, filter);
            redisDao.set(key, result, redisDefaultExpire);
        }
        if (page != null && pageSize != null) {
            JsonNode all = objectMapper.readTree(result);
            if (all.get("data").get(user) == null) return result;

            Iterator<JsonNode> buckets = all.get("data").get(user).iterator();
            ArrayList<JsonNode> userCount = new ArrayList<>();
            ArrayList<JsonNode> filterRes = new ArrayList<>();
            while (buckets.hasNext()) {
                JsonNode bucket = buckets.next();
                if (filter == null) userCount.add(bucket);
                if (filter != null && bucket.get("info").toString().contains(filter)) filterRes.add(bucket);
            }
            ArrayList<JsonNode> resList = filter == null ? userCount : filterRes;
            Map map = PageUtils.getDataByPage(Integer.parseInt(page), Integer.parseInt(pageSize), resList);
            HashMap<String, Object> resMap = new HashMap<>();
            resMap.put("code", 200);
            resMap.put("data", map);
            resMap.put("msg", "success");
            return objectMapper.valueToTree(resMap).toString();
        }

        return result;
    }

    public String putGiteeHookUser(HttpServletRequest request, String requestBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode all = objectMapper.readTree(requestBody);
            String hookName = all.get("hook_name").asText();
            JsonNode action = switch (hookName) {
                case "merge_request_hooks" -> all.get("pull_request");
                case "issue_hooks" -> all.get("issue");
                case "note_hooks" -> all.get("comment");
                default -> null;
            };
            if (action == null) return resultJsonStr(400, "user_count", 0, "parse body fail");
            ArrayList<Map<String, Object>> users = new ArrayList<>();

            JsonNode user = action.get("user");
            String createdAt = action.get("created_at").asText();
            Map<String, Object> userRes = giteeWebhookUser(user, createdAt);
            if (userRes != null) users.add(userRes);

            JsonNode assignee = action.get("assignee");
            Map<String, Object> assigneeRes = giteeWebhookUser(assignee, createdAt);
            if (assigneeRes != null) users.add(assigneeRes);

            TreeSet<Map<String, Object>> userSet = new TreeSet<>(Comparator.comparing(o -> o.get("email").toString()));
            userSet.addAll(users);

            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            queryDao.putGiteeHookUser(queryConf, userSet);
            return resultJsonStr(200, "user_count", userSet.size(), "success");
        } catch (Exception e) {
            logger.error("exception", e);
            return resultJsonStr(400, "user_count", 0, "parse body fail");
        }
    }

    public String queryUserLists(HttpServletRequest request, String community, String group, String name) {
        if (!checkCommunity(community)) return getQueryDao(request).resultJsonStr(404, "error", "not found");
        String key = community.toLowerCase() + group + name + "userlist";
        String result = (String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.queryUserLists(queryConf, community, group, name);
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String querySigRepoCommitters(HttpServletRequest request, String community, String sig) {
        if (!checkCommunity(community)) return getQueryDao(request).resultJsonStr(404, "error", "not found");
        String key = community.toLowerCase() + sig + "committers";
        String result = (String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.querySigRepoCommitters(queryConf, sig);
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }



    public QueryDao getQueryDao(HttpServletRequest request) {
        String community = request.getParameter("community");
        String serviceType = community == null ? "queryDao" : community.toLowerCase() + "Dao";
        return queryDaoContext.getQueryDao(serviceType);
    }

    public CustomPropertiesConfig getQueryConf(HttpServletRequest request) {
        String community = request.getParameter("community");
        return getQueryConf(community);
    }

    public CustomPropertiesConfig getQueryConf(String community) {
        String serviceType = community == null ? "queryConf" : community.toLowerCase() + "Conf";
        return queryConfContext.getQueryConfig(serviceType);
    }

    private String resultJsonStr(int code, String item, Object data, String msg) {
        return "{\"code\":" + code + ",\"data\":{\"" + item + "\":" + data + "},\"msg\":\"" + msg + "\"}";
    }

    private String resultJsonStr(int code, Object data, String msg) {
        return "{\"code\":" + code + ",\"data\":" + data + ",\"msg\":\"" + msg + "\"}";
    }

    private String validateBuildCheckInfo(BuildCheckInfoQueryVo buildCheckInfoQueryVo, String item) {
        List<String> checkTotalValidField = Arrays.asList("success", "failed");
        double MIN_BUILD_DURATION = 0;
        double min_duration_time;
        String errorMsg = "";

        String check_total = buildCheckInfoQueryVo.getCheckTotal();
        if (!StringUtils.isBlank(check_total) && !checkTotalValidField.contains(check_total.toLowerCase())) {
            errorMsg = "check_total is invalid, Only allows: SUCCESS and FAILED";
            return resultJsonStr(400, item, ReturnCode.RC400.getMessage(), errorMsg);
        }

        Map<String, String> build_duration = buildCheckInfoQueryVo.getBuildDuration();
        if (StringUtils.isNotBlank(build_duration.get("min_duration_time"))) {
            min_duration_time = Double.parseDouble(build_duration.get("min_duration_time"));
            if (min_duration_time < MIN_BUILD_DURATION) {
                errorMsg = "build_time is invalid, Only allows: bigger than " + MIN_BUILD_DURATION + "";
                return resultJsonStr(400, item, ReturnCode.RC400.getMessage(), errorMsg);
            }
        }

        Map<String, String> pr_create_time = buildCheckInfoQueryVo.getPrCreateTime();
        Map<String, String> result_build_time = buildCheckInfoQueryVo.getResultBuildTime();
        Map<String, String> result_update_time = buildCheckInfoQueryVo.getResultUpdateTime();
        Map<String, String> mistake_update_time = buildCheckInfoQueryVo.getMistakeUpdateTime();

        String pr_create_start_time = pr_create_time.get("start_time");
        String pr_create_end_time = pr_create_time.get("end_time");
        String result_build_start_time = result_build_time.get("start_time");
        String result_build_end_time = result_build_time.get("end_time");
        String result_update_start_time = result_update_time.get("start_time");
        String result_update_end_time = result_update_time.get("end_time");
        String mistake_update_start_time = mistake_update_time.get("start_time");
        String mistake_update_end_time = mistake_update_time.get("end_time");

        if (!StringValidationUtil.isDateTimeStrValid(pr_create_start_time)) {
            errorMsg = "pr_create_start_time format is invalid";
            return resultJsonStr(400, item, ReturnCode.RC400.getMessage(), errorMsg);
        }
        if (!StringValidationUtil.isDateTimeStrValid(pr_create_end_time)) {
            errorMsg = "pr_create_end_time format is invalid";
            return resultJsonStr(400, item, ReturnCode.RC400.getMessage(), errorMsg);
        }
        if (!StringValidationUtil.isDateTimeStrValid(result_build_start_time)) {
            errorMsg = "result_build_start_time format is invalid";
            return resultJsonStr(400, item, ReturnCode.RC400.getMessage(), errorMsg);
        }
        if (!StringValidationUtil.isDateTimeStrValid(result_build_end_time)) {
            errorMsg = "result_build_end_time format is invalid";
            return resultJsonStr(400, item, ReturnCode.RC400.getMessage(), errorMsg);
        }
        if (!StringValidationUtil.isDateTimeStrValid(result_update_start_time)) {
            errorMsg = "result_update_start_time format is invalid";
            return resultJsonStr(400, item, ReturnCode.RC400.getMessage(), errorMsg);
        }
        if (!StringValidationUtil.isDateTimeStrValid(result_update_end_time)) {
            errorMsg = "result_update_end_time format is invalid";
            return resultJsonStr(400, item, ReturnCode.RC400.getMessage(), errorMsg);
        }
        if (!StringValidationUtil.isDateTimeStrValid(mistake_update_start_time)) {
            errorMsg = "mistake_update_start_time format is invalid";
            return resultJsonStr(400, item, ReturnCode.RC400.getMessage(), errorMsg);
        }
        if (!StringValidationUtil.isDateTimeStrValid(mistake_update_end_time)) {
            errorMsg = "mistake_update_end_time format is invalid";
            return resultJsonStr(400, item, ReturnCode.RC400.getMessage(), errorMsg);
        }

        return null;
    }

    private String querySigInfo(HttpServletRequest request, String community, String sig) {
        String item = "siginfo";
        String key = community + sig + item;
        String result = (String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.querySigInfo(queryConf, sig);
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    private String queryFuzzySigInfo(HttpServletRequest request, String community, String sig, String repo, String user, String search, String page, String pageSize) throws Exception {
        String key = community + "allsiginfo";
        String result = null;
        result = (String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.querySigInfo(queryConf, sig);
            redisDao.set(key, result, redisDefaultExpire);
        }
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode all = objectMapper.readTree(result);
        if (all.get("code").asInt() != 200) {
            return resultJsonStr(400, ReturnCode.RC400.getMessage(), ReturnCode.RC400.getMessage());
        }
        JsonNode res = all.get("data");
        ArrayList<HashMap<String, Object>> resList = objectMapper.convertValue(res, new TypeReference<>() {
        });
        ArrayList<HashMap<String, Object>> tempList = new ArrayList<>();
        for (HashMap<String, Object> list : resList) {
            String sig_name = list.get("sig_name").toString();
            ArrayList<String> repos = (ArrayList<String>) list.get("repos");
            Boolean bool = sig != null && !sig_name.toLowerCase().contains(sig.toLowerCase()) ? false : true;
            ArrayList<String> maintainers = (ArrayList<String>) list.get("maintainers");
            if (bool && matchList(repos, repo) && matchList(maintainers, user)) {
                tempList.add(list);
            }
        }

        Collections.sort(tempList, Comparator.comparing(t -> t.get("sig_name").toString().toLowerCase()));

        if (pageSize != null && page != null) {
            int currentPage = Integer.parseInt(page);
            int pagesize = Integer.parseInt(pageSize);
            Map data = PageUtils.getDataByPage(currentPage, pagesize, tempList);
            ArrayList<HashMap<String, Object>> dataList = new ArrayList<>();
            dataList.add((HashMap<String, Object>) data);
            tempList = dataList;
        }
        HashMap<String, Object> resMap = new HashMap<>();
        resMap.put("code", 200);
        resMap.put("data", tempList);
        resMap.put("msg", "success");
        result = objectMapper.valueToTree(resMap).toString();
        return result;
    }

    private Boolean matchList(ArrayList<String> arrayList, String str) {
        if (str == null) {
            return true;
        }
        if (arrayList == null) {
            return false;
        }
        for (String list : arrayList) {
            if (list.toLowerCase().contains(str.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkPermission(HttpServletRequest request, QueryDao queryDao, CustomPropertiesConfig queryConf, String token, String company) {
        try {
            RSAPrivateKey privateKey = RSAUtil.getPrivateKey(env.getProperty("rsa.authing.privateKey"));
            DecodedJWT decode = JWT.decode(RSAUtil.privateDecrypt(token, privateKey));
            String permissionList = decode.getClaim("permissionList").asString();
            String[] pers = new String(Base64.getDecoder().decode(permissionList.getBytes())).split(",");
            for (String per : pers) {
                String[] perList = per.split(":");
                if (perList.length > 1 && perList[1].equalsIgnoreCase(queryConf.getCompanyAction())) return true;
            }

            String login = queryDao.getOneIdUserGiteeLoginName(request);
            if (StringUtils.isNotBlank(login)) {
                String org = queryDao.queryUserCompany(queryConf, login);
                ArrayList<String> companyNameList = queryDao.getCompanyNameList(company);
                for (String name : companyNameList) {
                    if (org.equals(name)) return false;//true;
                }
            }
        } catch (Exception e) {
            logger.error("exception", e);
        }
        return false;
    }

    private Map<String, Object> giteeWebhookUser(JsonNode user, String createdAt) {
        if (user == null || user.isNull() || user.isEmpty()) return null;

        String email = user.get("email").asText();
        if (StringUtils.isBlank(email)) return null;

        long id = user.get("id").asLong();
        String login = user.get("login").asText();

        return Map.of("id", id, "gitee_id", login, "email", email, "created_at", createdAt);
    }

    public String querySigPrStateCount(HttpServletRequest request, String community, String sig, Long ts) {
        QueryDao queryDao = getQueryDao(request);
        if (!checkCommunity(community)) return queryDao.resultJsonStr(404, "error", "not found");
        CustomPropertiesConfig queryConf = getQueryConf(request);
        String result = queryDao.querySigPrStateCount(queryConf, sig, ts);     
        return result;
    }
    
    public String queryMetricsData(HttpServletRequest request, String community, DatastatRequestBody body) {
        if (!checkCommunity(community)) return getQueryDao(request).resultJsonStr(404, "error", "not found");
        String filter = body.getFilter();
        String serviceType = community.toLowerCase() + filter.toLowerCase() + "MetricDao";
        MetricDao metricDao = metricDaoContext.getQueryMetricsDao(serviceType);
        CustomPropertiesConfig queryConf = getQueryConf(request);
        String result = metricDao.queryMetricsData(queryConf, body);
        return result;
    }

    public String queryClaName(HttpServletRequest request, String community, Long ts) {
        QueryDao queryDao = getQueryDao(request);
        if (!checkCommunity(community)) return queryDao.resultJsonStr(404, "error", "not found");
        CustomPropertiesConfig queryConf = getQueryConf(request);
        String result = queryDao.queryClaName(queryConf, ts);     
        return result;
    }

    public String getIPLocation(HttpServletRequest request, String ip) {
        QueryDao queryDao = getQueryDao(request); 
        String result = queryDao.getIPLocation(ip);     
        return result;
    }

    public String getEcosystemRepoInfo(HttpServletRequest request, String community, String ecosystemType, String lang, String sortType,
            String sortOrder, String page, String pageSize) {
        QueryDao queryDao = getQueryDao(request);
        if (!checkCommunity(community)) return queryDao.resultJsonStr(404, "error", "not found");
        CustomPropertiesConfig queryConf = getQueryConf(request);
        sortOrder = sortOrder == null ? "desc" : sortOrder;
        lang = lang == null ? "zh" : lang.toLowerCase();
        // String key = community.toLowerCase() + ecosystemType.toLowerCase() + "ecosysteminfo" + sortOrder + lang;

        String result = null;
        try {
            if (result == null) {
                result = queryDao.getEcosystemRepoInfo(queryConf, ecosystemType, lang, sortOrder);
                // redisDao.set(key, result, redisDefaultExpire);
            }
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode resultJson = objectMapper.readTree(result);
            if (resultJson.get("code").asInt() != 200) {
                return resultJsonStr(404, null, "error");
            }
            ArrayList<HashMap<String, Object>> resList = objectMapper.convertValue(resultJson.get("data"),
                    new TypeReference<ArrayList<HashMap<String, Object>>>() {
                    });
            if (sortType != null && (sortType.equals("date") || sortType.equals("repo"))) {
                resList = ArrayListUtil.sortByType(resList, sortType, sortOrder);
            }
    
            if (pageSize != null && page != null && resList.size() > 0) {
                int currentPage = Integer.parseInt(page);
                int pagesize = Integer.parseInt(pageSize);
                Map<String, Object> data = PageUtils.getDataByPage(currentPage, pagesize, resList);
                data.put("type", resList.get(0).get("type"));
                data.put("name", resList.get(0).get("name"));
                data.put("description", resList.get(0).get("description"));
                ArrayList<HashMap<String, Object>> dataList = new ArrayList<>();
                dataList.add((HashMap<String, Object>) data);
                resList = dataList;
            }
            return resultJsonStr(200, objectMapper.valueToTree(resList), "ok");
        } catch (Exception e) {
            logger.error("exception", e);
        }
        return resultJsonStr(400, null, "error");        
    }

    public String getSigReadme(HttpServletRequest request, String community, String sig, String lang) {
        String result = null;
        // String key = community.toLowerCase() + sig + "readme" + lang;
        // result = (String) redisDao.get(key);
        QueryDao queryDao = getQueryDao(request);
        CustomPropertiesConfig queryConf = getQueryConf(request);
        if (result == null) {
            result = queryDao.getSigReadme(queryConf, sig, lang);
            // boolean set = redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryCommunityIsv(HttpServletRequest request, String community, String name, String softwareType, String company) {
        if (!checkCommunity(community)) return getQueryDao(request).resultJsonStr(404, "error", "not found");
        String key = community.toLowerCase() + "isvinfo";
        String result = (String) redisDao.get(key);
        QueryDao queryDao = getQueryDao(request);
        CustomPropertiesConfig queryConf = getQueryConf(request);
        if (result == null) {
            result = queryDao.getCommunityIsv(queryConf, "/home/");
            redisDao.set(key, result, redisDefaultExpire);
        }
        try {
            JsonNode isvs = objectMapper.readTree(result);
            ArrayList<JsonNode> resList = new ArrayList<>();
            for (JsonNode isv : isvs) {
                if (matchList(isv, "name", name) && matchList(isv, "type", softwareType) && matchList(isv, "company", company)) {
                    resList.add(isv);
                }
            }
            return resultJsonStr(200, objectMapper.valueToTree(resList), "ok");
        } catch (Exception e) {
            logger.error("exception", e);
        }
        return resultJsonStr(400, null, "query error");
    }

    private Boolean matchList(JsonNode isv, String field, String value) {
        if (value == null) {
            return true;
        }
        if (isv.get(field).asText().contains(value)) {
            return true;
        }
        return false;
    }

    public String putMeetupApplyForm(HttpServletRequest request, String community, MeetupApplyForm meetupApplyForm, String token) {
        String item = "meetupApplyForm";
        String res = "";
        QueryDao queryDao = getQueryDao(request);
        CustomPropertiesConfig queryConf = getQueryConf(request);
        try {
            res = queryDao.putMeetupApplyForm(queryConf, item, meetupApplyForm, token);
        } catch (Exception e) {
            logger.error("exception", e);
        }
        return res;
    }

    public String queryCommunityVersions(HttpServletRequest request, String community) {
        String key = community.toLowerCase() + "versions";
        String result = (String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.queryCommunityVersions(queryConf);
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String getRepoReadme(HttpServletRequest request, String community, String name) {
        String key = community.toLowerCase() + name + ".md";
        String result = (String) redisDao.get(key);
        QueryDao queryDao = getQueryDao(request);
        CustomPropertiesConfig queryConf = getQueryConf(request);
        if (result == null) {
            result = queryDao.getRepoReadme(queryConf, name);
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String putUserPermissionApply(HttpServletRequest request, String community, String data) {
        QueryDao queryDao = getQueryDao(request);
        return queryDao.putUserPermissionApply(community, data);
    }

    public String QaBotChat(HttpServletRequest request, QaBotRequestBody data) {
        QueryDao queryDao = getQueryDao(request);
        return queryDao.QaBotChat(data);
    }

    public String QaBotSuggestions(HttpServletRequest request, QaBotRequestBody body) {
        QueryDao queryDao = getQueryDao(request);
        return queryDao.QaBotSuggestions(body);
    }

    public String QaBotSatisfaction(HttpServletRequest request, QaBotRequestBody body) {
        QueryDao queryDao = getQueryDao(request);
        return queryDao.QaBotSatisfaction(body);
    }

    public String QaBotUserFeedback(HttpServletRequest request, QaBotRequestBody body) {
        QueryDao queryDao = getQueryDao(request);
        return queryDao.QaBotUserFeedback(body);
    }

    public ResponseEntity queryReviewerRecommend(PrReviewerVo input) {
        String community = input.getCommunity();
        String serviceType = community == null ? "queryDao" : community.toLowerCase() + "Dao";
        QueryDao queryDao = queryDaoContext.getQueryDao(serviceType);
        CustomPropertiesConfig queryConf = getQueryConf(community);
        return queryDao.queryReviewerRecommend(queryConf, input);
    }

    public String getNps(HttpServletRequest request, String community, NpsBody body) {
        if (!checkCommunity(community) && !community.equals("xihe")) return getQueryDao(request).resultJsonStr(404, "error", "not found");
        QueryDao queryDao = getQueryDao(request);
        CustomPropertiesConfig queryConf = getQueryConf(request);
        String token = (String) redisDao.get("nps_moderation_token");
        if (token == null) {
            token = queryDao.getHuaweiCloudToken(env.getProperty("moderation.user.name"), env.getProperty("moderation.user.password"),
                        env.getProperty("moderation.domain.name"), env.getProperty("moderation.token.endpoint"));
            redisDao.set("nps_moderation_token", token, 36000l);
        }
        return queryDao.getNps(queryConf, community, body, token);
    }

    public String queryInnovationItems(HttpServletRequest request, String community) {
        if (!"openeuler".equals(community.toLowerCase())) {
            return getQueryDao(request).resultJsonStr(404, "error", "not found");
        }
        String key = community.toLowerCase() + "innovationItems";
        QueryDao queryDao = getQueryDao(request);
        CustomPropertiesConfig queryConf = getQueryConf(request);
        String result = (String) redisDao.get(key);
        if (result == null) {
            result = queryDao.queryInnovationItems(queryConf);
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryAllProjects(HttpServletRequest request, String community, String timeRange, String groupField, String type) {
        if (!checkCommunity(community)) return getQueryDao(request).resultJsonStr(404, "error", "not found");
        String key = "allProjects" + community.toLowerCase() + timeRange.toLowerCase() + groupField.toLowerCase() + type.toLowerCase();
        String result = (String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.queryAllProjects(queryConf, community, timeRange, groupField, type);
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryByProjectName(HttpServletRequest request, String community, String timeRange, String groupField, String projectName, String type) {
        if (!checkCommunity(community)) return getQueryDao(request).resultJsonStr(404, "error", "not found");
        String key = "projectName" + community.toLowerCase() + timeRange.toLowerCase() + groupField.toLowerCase() + projectName.toLowerCase() + type.toLowerCase();
        String result = (String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            if (projectName.equals("allInnoItems")) { // 选择所有创新项目
                result = queryDao.queryAllInnoItems(queryConf, community, timeRange, groupField, type);
            } else { // 选择单个创新项目
                result = queryDao.queryByProjectName(queryConf, community, timeRange, groupField, projectName, type);
            }
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String querySigDefect(HttpServletRequest request, String community, String timeRange, String sigName) {
        if (!checkCommunity(community)) return getQueryDao(request).resultJsonStr(404, "error", "not found");
        String key = "sigDefect" + community.toLowerCase() + timeRange.toLowerCase() + sigName.toLowerCase();
        String result = (String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.querySigDefect(queryConf, community, timeRange, sigName);
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String querySigContribute(HttpServletRequest request, String community, String timeRange, String projectName, String type, String version) {
        if (!checkCommunity(community)) return getQueryDao(request).resultJsonStr(404, "error", "not found");
        String key = "sigcontribute" + StringUtils.lowerCase(community) + StringUtils.lowerCase(timeRange) +
            StringUtils.lowerCase(projectName) +StringUtils.lowerCase(type) +StringUtils.lowerCase(version);
        String result = (String) redisDao.get(key);
        if (result == null) {
            QueryDao queryDao = getQueryDao(request);
            CustomPropertiesConfig queryConf = getQueryConf(request);
            result = queryDao.querySigContribute(queryConf, community, timeRange, projectName, type, version);
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryModelFoundry(HttpServletRequest request, String repo) {
        QueryDao queryDao = getQueryDao(request);
        CustomPropertiesConfig queryConf = getQueryConf(request);
        String key = "modelfoundrycownload_repo_" + repo;
        String result = (String) redisDao.get(key);
        if (result == null) {
            result = queryDao.queryModelFoundry(queryConf, repo);
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }

    public String queryModelFoundryTrends(HttpServletRequest request, String repo) {
        QueryDao queryDao = getQueryDao(request);
        CustomPropertiesConfig queryConf = getQueryConf(request);
        String key = "modelfoundrycownload_repo_trends_" + repo;
        String result = (String) redisDao.get(key);
        if (result == null) {
            result = queryDao.queryModelFoundryTrends(queryConf, repo);
            redisDao.set(key, result, redisDefaultExpire);
        }
        return result;
    }
}