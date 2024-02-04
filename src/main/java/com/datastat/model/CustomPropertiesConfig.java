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

package com.datastat.model;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import com.datastat.model.yaml.InnovationItemInfo;
import com.datastat.model.yaml.InnovationItemYaml;
import com.datastat.util.YamlUtil;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

@Data
public class CustomPropertiesConfig {
    // -- other --
    private String tokenUserName;
    private String tokenUserPassword;
    private String tokenBasePassword;
    private String tokenExpireSeconds;
    private String accessToken;
    private String multiCommunity;
    private String sigOwnerType;
    private String allSigOwnerType;
    private String companyUsers;
    private String sigFeatureUrl;
    private String userOwnsSigStr;
    private String tcOwnerUrl;
    private String UserReportCsvData;
    private String businessOsv;
    private String blueZoneApiToken;
    private String productLineCodeAllUpdate;
    private String companyAction;
    private String sigAction;
    private String isvYamlUrl;
    private String giteeUserInfoUrl;

    // -- index --
    private String extOsIndex;
    private String sigIndex;
    private String usersIndex;
    private String contributorsIndex;
    private String noticeUsersIndex;
    //    private String communityMembersIndex;
    private String giteeAllIndex;
    private String claCorporationIndex;
    private String cveDetailsIndex;
    private String durationAggIndex;
    private String bugQuestionnaireIndex;
    private String obsDetailsIndex;
    private String isoBuildIndex;
    private String sigDetailsIndex;
    private String issueScoreIndex;
    private String buildCheckResultIndex;
    private String buildCheckMistakeIndex;
    private String meetingsIndex;
    private String sigScoreIndex;
    private String sigRadarScoreIndex;
    private String trackerIndex;
    private String accountOrgIndex;
    private String downloadIndex;
    private String starForkIndex;
    private String blueZoneUserIndex;
    private String blueZoneUserContributesIndex;
    private String giteeEmailIndex;
    private String userCountIndex;
    private String downloadIpIndex;
    private String ecosystemRepoIndex;
    private String giteeVersionIndex;
    private String meetupApplyFormIndex;
    private String userTagIndex;
    private String npsIndex;
    private String giteeFeatureIndex;
    private String modelFoundryIndex;

    // -- query str --
    private String extOsQueryStr;
    private String sigQueryStr;
    private String usersQueryStr;
    private String contributorsQueryStr;
    private String giteeContributesQueryStr;
    private String noticeUsersQueryStr;
    private String communityMembersQueryStr;
    private String giteeAggCompanyQueryStr;
    private String giteeAggUserQueryStr;
    private String durationAggQueryStr;
    private String obsDetailsQueryStr;
    private String isoBuildQueryStr;
    private String sigDetailsQueryStr;
    private String issueScoreQueryStr;
    private String sigNameQueryStr;
    private String sigInfoQueryStr;
    private String sigRepoQueryStr;
    private String sigGiteeQueryStr;
    private String sigContributeQueryStr;
    private String sigMeetingsQueryStr;
    private String sigMaintainersQueryStr;
    private String companyNameQueryStr;
    private String companyUserQueryStr;
    private String companySigQueryStr;
    private String companyContributeQueryStr;
    private String companySigUserQueryStr;
    private String companyAggUserQueryStr;
    private String sigAggUserQueryStr;
    private String companyAggSigQueryStr;
    private String sigScoreQueryStr;
    private String allSigScoreQueryStr;
    private String allCompanySigQueryStr;
    private String groupAggSigQueryStr;
    private String userOwnerTypeQueryStr;
    private String allUserOwnerTypeQueryStr;
    private String userListQueryStr;
    private String sigRepoCommittersQueryStr;
    private String giteeAllIssueByMilQueryStr;
    private String giteeAllQueryStr;
    private String bugQuestionnaireQueryStr;
    private String accountOrgQueryStr;
    private String downloadQueryStr;
    private String giteeStarCountQueryStr;
    private String giteeIssueCountQueryStr;
    private String giteePrCountQueryStr;
    private String starForkQueryStr;
    private String monthCountQueryStr;
    private String sigYamlEn;
    private String sigYamlZh;
    private String sigLabelQueryStr;
    private String communityRepoQueryStr;
    private String sigCountQuery;
    private String aggContributeDetailQuery;
    private String userContributeDetailQuery;
    private String userCountQuery;
    private String userActiveQuery;
    private String downloadCountQueryStr;
    private String downloadIpIncreaseQuery;
    private String downloadIpCountQuery;
    private String aggTotalUserCountQuery;
    private String aggTotalContributeDetailQuery;
    private String sigOwnerQuery;
    private String sigPrStateCountQuery;
    private String claNameQuery;
    private String ecosystemRepoQuery;
    private String companyVersionPrQuery;
    private String companyVersionClocQuery;
    private String communityVersions;
    private String eurPackagesQuery;
    private String aggGroupCommentQueryStr;
    private String innovationItemAddress;
    private String issueDoneQueryStr;
    private String issueCveQueryStr;
    private String repoUrlPrefix;
    private String projectPrQueryStr;
    private String projectIssueQueryStr;
    private String projectCommentQueryStr;
    private String projectIssueCveQueryStr;
    private String projectIssueDoneQueryStr;
    private String sigIssueQueryStr;
    private String sigCveQueryStr;
    private String companyFeatureQueryStr;
    private String giteeAggSigQueryStr;
    private String sigVersionPrQueryStr;
    private String modelFoundryDownloadQueryStr;
    private String modelFoundryDownloadTrendQueryStr;
    
    protected static final Map<String, String> contributeTypeMap = new HashMap<>();
    protected static final Map<String, String> groupFieldMap = new HashMap<>();

    @PostConstruct
    public void init() {
        contributeTypeMap.put("pr", "is_pull_state_merged");
        contributeTypeMap.put("issue", "is_gitee_issue");
        contributeTypeMap.put("comment", "is_gitee_comment");

        groupFieldMap.put("company", "tag_user_company");
        groupFieldMap.put("sig", "sig_names");
    }

    public String getCountQueryStr(String item) {
        String queryStr = "";
        switch (item) {
            case "stars":
                queryStr = getGiteeStarCountQueryStr();
                break;
            case "issues":
                queryStr = getGiteeIssueCountQueryStr();
                break;
            case "prs":
                queryStr = getGiteePrCountQueryStr();
                break;
            default:
                return "";
        }
        return queryStr;
    }

    public String getAggCountQueryStr(CustomPropertiesConfig queryConf, String groupField, String contributeType, String timeRange, String community, String repo, String sig) {
        String queryJson = groupField.equals("company") ? getGiteeAggCompanyQueryStr() : getGiteeAggUserQueryStr();
        return getQueryStrByType(contributeType, queryJson, timeRange, null);
    }

    public String getQueryStrByType(String contributeType, String queryJson, String timeRange, String item) {
        String orDefault = contributeTypeMap.getOrDefault(contributeType.toLowerCase(), "");
        if (StringUtils.isBlank(orDefault)) return "";
        if (item == null) return getQueryStrWithTimeRange(queryJson, timeRange, orDefault);
        return getQueryStrWithTimeRange(queryJson, timeRange, item, orDefault);
    }

    public String getAggGroupCountQueryStr(String groupField, String group, String contributeType, String timeRange, String label) {
        String queryJson;
        switch (groupField) {
            case "sig":
                queryJson = getSigAggUserQueryStr();
                break;
            case "company":
                queryJson = getCompanyAggUserQueryStr();
                break;
            default:
                return null;
        }
        String orDefault = contributeTypeMap.getOrDefault(contributeType.toLowerCase(), "");
        if (StringUtils.isBlank(orDefault)) return null;
        return getQueryStrWithTimeRange(queryJson, timeRange, group, orDefault);
    }

    public String getAggGroupSigCountQueryStr(String queryJson, String contributeType, String timeRange, String group, String field) {
        String orDefault = contributeTypeMap.getOrDefault(contributeType.toLowerCase(), "");
        if (StringUtils.isBlank(orDefault)) return null;
        return getQueryStrWithTimeRange(queryJson, timeRange, field, group, orDefault);
    }

    public String getAggCompanySigCountQueryStr(String queryJson, String company, String timeRange, String contributeType) {
        String orDefault = contributeTypeMap.getOrDefault(contributeType.toLowerCase(), "");
        if (StringUtils.isBlank(orDefault)) return null;
        return getQueryStrWithTimeRange(queryJson, timeRange, company, orDefault);
    }

    public String[] getAggCompanyGiteeQueryStr(String queryJson, String timeRange, String company) {
        if (queryJson == null) return null;

        String[] queryJsons = queryJson.split(";");
        String[] queryStr = new String[queryJsons.length];
        for (int i = 0; i < queryJsons.length; i++) {
            queryStr[i] = getQueryStrWithTimeRange(queryJsons[i], timeRange, company);
        }
        return queryStr;
    }

    public String getAggCommentQueryStr(CustomPropertiesConfig queryConf, String groupField, String timeRange, String repo) {
        String group = groupField.equals("company") ? "tag_user_company" : "user_login";
        String queryJson = getAggGroupCommentQueryStr();
        if (queryJson == null) return null;
        long currentTimeMillis = System.currentTimeMillis();
        long lastTimeMillis = getPastTime(timeRange);
        return queryStrFormat(queryJson, lastTimeMillis, currentTimeMillis, group);
    }

    public String getQueryStrWithTimeRange(String queryJson, String timeRange, Object... args) {
        if (queryJson == null) return null;

        long currentTimeMillis = System.currentTimeMillis();
        long lastTimeMillis = getPastTime(timeRange);
        Object[] params = new Object[args.length + 2];
        params[0] = lastTimeMillis;
        params[1] = currentTimeMillis;
        for (int i = 0; i < args.length; i++) {
            params[i + 2] = args[i];
        }
        return queryStrFormat(queryJson, params);
    }

    public ArrayList<Object> getAggUserCountQueryParams(String contributeType, String timeRange) {
        long currentTimeMillis = System.currentTimeMillis();
        long lastTimeMillis = getPastTime(timeRange);
        ArrayList<Object> list = new ArrayList<>();
        list.add(contributeType);
        list.add(lastTimeMillis);
        list.add(currentTimeMillis);
        switch (contributeType.toLowerCase()) {
            case "pr":
                list.add("is_pull_state_merged");
                list.add("pull_title");
                list.add("pull_url");
                list.add("pull_id_in_repo");
                break;
            case "issue":
                list.add("is_gitee_issue");
                list.add("issue_title");
                list.add("issue_url");
                list.add("issue_id_in_repo");
                break;
            case "comment":
                list.add("is_gitee_comment");
                list.add("body");
                list.add("sub_type");
                list.add("id");
                break;
            default:
                return null;
        }
        return list;
    }

    public String getUserContributeDetailsQuery(CustomPropertiesConfig queryConf, String sig, String label) {
        return null;
    }

    public String getAggUserListQueryStr(String queryJson, String group, String name) {
        if (group == null || name == null) {
            group = "*";
            name = "*";
        }
        return switch (group) {
            case "sig" -> String.format(queryJson, "sig_names.keyword", name);
            case "company" -> String.format(queryJson, "tag_user_company.keyword", name);
            case "*" -> String.format(queryJson, "user_login.keyword", name);
            default -> null;
        };
    }


    protected static String queryStrFormat(String queryJson, Object... args) {
        return String.format(queryJson, args);
    }

    protected long getPastTime(String timeRange) {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        switch (timeRange.toLowerCase()) {
            case "lastonemonth":
                c.add(Calendar.MONTH, -1);
                break;
            case "lasthalfyear":
                c.add(Calendar.MONTH, -6);
                break;
            case "lastoneyear":
                c.add(Calendar.YEAR, -1);
                break;
            default:
                c.setTimeInMillis(0);
        }
        return c.getTimeInMillis();
    }

    public ArrayList<String> getTermQuery(String term, JsonNode companyQueryMap) {
        ArrayList<String> params = new ArrayList<>();
        if (term.equalsIgnoreCase("sig")) {
            params.add("sig_names.keyword");
            params.add("*");
        } else if (term.equalsIgnoreCase("repo")) {
            params.add("gitee_repo.keyword");
            params.add("*");
        } else if (companyQueryMap.has(term)) {
            params.add("tag_user_company.keyword");
            params.add(companyQueryMap.get(term).asText());
        }
        return params;
    }

    public String convertList2QueryStr(ArrayList<String> res) {
        if (res == null) {
            return "*";
        }
        String names = "(";
        for (String r : res) {
            names = names + "\\\"" + r + "\\\",";
        }
        names = names + ")";
        return names;
    }

    public String getCompanyContributorsQuery(CustomPropertiesConfig queryConf, String community, String contributeType,
            String timeRange, String version, String repo, String sig) {
        String contributesQueryStr = null;
        if (timeRange != null && version == null) {
            contributesQueryStr = getAggCountQueryStr(queryConf, "company", contributeType, timeRange, community, repo,
                    sig);
        } else if (version != null) {
            contributesQueryStr = getVersionContributeQuery(contributeType, version);
        }
        return contributesQueryStr;
    }

    public String getVersionContributeQuery(String contributeType, String version) {
        String contributesQueryStr = "";
        String versionQueryStr = version.equalsIgnoreCase("all") ? "*" : version;
        if (contributeType.equalsIgnoreCase("cloc")) {
            contributesQueryStr = String.format(getCompanyVersionClocQuery(), versionQueryStr);
        }
        if (contributeType.equalsIgnoreCase("pr")) {
            contributesQueryStr = String.format(getCompanyVersionPrQuery(), versionQueryStr);
        }
        return contributesQueryStr;
    }

    public String getAggSigContributeQueryStr(CustomPropertiesConfig queryConf, String type, String timeRange) {
        String queryJson = getGiteeAggSigQueryStr();
        long currentTimeMillis = System.currentTimeMillis();
        long lastTimeMillis = getPastTime(timeRange);
        String typeJson = contributeTypeMap.getOrDefault(type.toLowerCase(), "");
        if (queryJson == null || StringUtils.isBlank(typeJson)) {
            return "";
        }
        return queryStrFormat(queryJson, lastTimeMillis, currentTimeMillis, typeJson);
    }

    public String getAggIssueQueryStr(CustomPropertiesConfig queryConf, String groupField, String timeRange, String type) {
        String queryJson = null;
        switch (type) {
            case "issue_cve":
                queryJson = getIssueCveQueryStr();
                break;
            case "issue_done":
                queryJson = getIssueDoneQueryStr();
                break;
            default:
                break;
        }
        if (queryJson == null) {
            return "";
        }
        long currentTimeMillis = System.currentTimeMillis();
        long lastTimeMillis = getPastTime(timeRange);
        // 判断搜索结果是按照公司进行排序还是按照SIG组进行排序
        String group = groupFieldMap.getOrDefault(groupField.toLowerCase(), "");
        if (StringUtils.isBlank(group)) {
            return "";
        }
        return queryStrFormat(queryJson, lastTimeMillis, currentTimeMillis, group);
    }

    public List<String> getProjectRepos(CustomPropertiesConfig queryConf, String projectName) {
        YamlUtil yamlUtil = new YamlUtil();
        InnovationItemYaml items = yamlUtil.readLocalYaml(queryConf.getInnovationItemAddress(), 
                InnovationItemYaml.class);
        List<String> repos = new ArrayList<>();
        for (InnovationItemInfo item : items.getInnovation_projects()) {
            String name = item.getProject_name().trim();
            if (projectName.equals(name)) {
                repos.addAll(item.getRepos());
                break;
            }
        }
        return repos;
    }

    public String getAggProjectPrQueryStr(CustomPropertiesConfig queryConf, String timeRange, String groupField, String projectName, String contributeType) {
        List<String> repos = getProjectRepos(queryConf, projectName);
        // 判断根据项目找到的仓库是否存在
        if (repos.size() == 0) {
            return "";
        }
        // 拼接查询仓库的字符串
        String url = queryConf.getRepoUrlPrefix();
        String repoStr = getRepoList(repos, url);
        // 拼接查询项目字符串
        String queryJson = null;
        switch (contributeType) {
            case "pr":
                queryJson = getProjectPrQueryStr();
                break;
            case "issue":
                queryJson = getProjectIssueQueryStr();
                break;
            case "comment":
                queryJson = getProjectCommentQueryStr();
                break;
            case "issue_cve":
                queryJson = getProjectIssueCveQueryStr();
                break;
            case "issue_done":
                queryJson = getProjectIssueDoneQueryStr();
                break;
            default:
                break;
        }
        if (queryJson == null) {
            return "";
        }
        long currentTimeMillis = System.currentTimeMillis();
        long lastTimeMillis = getPastTime(timeRange);
        String group = groupFieldMap.getOrDefault(groupField.toLowerCase(), "");
        if (StringUtils.isBlank(group)) {
            return "";
        }
        return queryStrFormat(queryJson, lastTimeMillis, currentTimeMillis, repoStr, group);
    }

    /**
     * 拼接查询语句中仓库字符串
     * @param repos
     * @param url
     * @return
     */
    public String getRepoList(List<String> repos, String url) {
        StringBuilder repoSb = new StringBuilder("(\\\"");
        for (int i = 0; i < repos.size(); i++) {
            repoSb.append(url);
            repoSb.append(repos.get(i));
            repoSb.append("\\\"");
            if (i < repos.size() - 1) {
                repoSb.append(", \\\"");
            }
        }
        repoSb.append(")");
        return repoSb.toString();
    }

    public String getAggSigDefectQueryStr(CustomPropertiesConfig queryConf, String timeRange, String sigName, String type) {
        // 拼接查询项目字符串
        String queryJson = null;
        String issueRange = null;
        switch (type) {
            case "allIssue":
                issueRange = "(closed, rejected, open, progressing)";
                queryJson = getSigIssueQueryStr();
                break;
            case "closedIssue":
                issueRange = "(closed, rejected)";
                queryJson = getSigIssueQueryStr();
                break;
            case "allCve":
                issueRange = "(CVE/FIXED, CVE/UNFIXED, CVE/UNAFFECTED, CVE/PENDING)";
                queryJson = getSigCveQueryStr();
                break;
            case "fixedCve":
                issueRange = "(CVE/FIXED, CVE/UNAFFECTED)";
                queryJson = getSigCveQueryStr();
                break;
            default:
                break;
        }
        if (queryJson == null) {
            return "";
        }
        long currentTimeMillis = System.currentTimeMillis();
        long lastTimeMillis = getPastTime(timeRange);
        return queryStrFormat(queryJson, lastTimeMillis, currentTimeMillis, issueRange, sigName);
    }

    public String getAggCompanyFeatureQueryStr(CustomPropertiesConfig queryConf, String version, String groupField) {
        String queryJson = getCompanyFeatureQueryStr();
        String verisonQuery = "all".equals(version) ? "*" : version;
        if (queryJson == null) {
            return "";
        }
        // 判断搜索结果是按照公司进行排序还是按照SIG组进行排序
        String group = groupFieldMap.getOrDefault(groupField.toLowerCase(), "");
        if (StringUtils.isBlank(group)) {
            return "";
        }
        return queryStrFormat(queryJson, verisonQuery, group);
    }

    public String getAggSigVersionQuery(CustomPropertiesConfig queryConf, String type, String version) {
        String queryJson = getSigVersionPrQueryStr();
        String verisonQuery = "all".equals(version) ? "*" : version;
        if (queryJson == null) {
            return "";
        }
        return queryStrFormat(queryJson, verisonQuery);
    }
}
