/* This project is licensed under the Mulan PSL v2.
 You can use this software according to the terms and conditions of the Mulan PSL v2.
 You may obtain a copy of Mulan PSL v2 at:
     http://license.coscl.org.cn/MulanPSL2
 THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR
 PURPOSE.
 See the Mulan PSL v2 for more details.
 Create: 2022
*/

package com.datastat.controller;

import com.datastat.interceptor.authentication.UserLoginToken;
import com.datastat.interceptor.oneid.OneidToken;
import com.datastat.interceptor.oneid.SigToken;
import com.datastat.model.DatastatRequestBody;
import com.datastat.model.NpsBody;
import com.datastat.model.QaBotRequestBody;
import com.datastat.model.meetup.MeetupApplyForm;
import com.datastat.model.vo.*;
import com.datastat.service.QueryService;
import jakarta.servlet.http.HttpServletRequest;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/query")
public class QueryController {
    @Autowired
    QueryService queryService;

    @RequestMapping("/contributors")
    public String queryContributors(HttpServletRequest request,
                                    @RequestParam(value = "community") String community) {
        return queryService.queryContributors(request, community);
    }

    @RequestMapping("/avgduration")
    public String queryDurationAggFromProjectHostArchPackage(HttpServletRequest request,
                                                             @RequestParam(value = "community") String community) {
        return queryService.queryDurationAggFromProjectHostArchPackage(request, community);
    }

    @RequestMapping("/sigs")
    public String querySigs(HttpServletRequest request,
                            @RequestParam(value = "community") String community) {
        return queryService.querySigs(request, community);
    }

    @RequestMapping("/users")
    public String queryUsers(HttpServletRequest request,
                             @RequestParam(value = "community") String community) {
        return queryService.queryUsers(request, community);
    }

    @RequestMapping("/noticeusers")
    public String queryNoticeUsers(HttpServletRequest request,
                                   @RequestParam(value = "community") String community) {
        return queryService.queryNoticeUsers(request, community);
    }

    @RequestMapping("/modulenums")
    public String queryModuleNums(HttpServletRequest request,
                                  @RequestParam(value = "community") String community) {
        return queryService.queryModuleNums(request, community);
    }

    @RequestMapping("/businessosv")
    public String queryBusinessOsv(HttpServletRequest request,
                                   @RequestParam(value = "community") String community) {
        return queryService.queryBusinessOsv(request, community);
    }

    @RequestMapping("/communitymembers")
    public String queryCommunityMembers(HttpServletRequest request,
                                        @RequestParam(value = "community") String community) {
        return queryService.queryCommunityMembers(request, community);
    }

    @RequestMapping("/downloads")
    public String queryDownloads(HttpServletRequest request,
                                 @RequestParam(value = "community") String community) {
        return queryService.queryDownload(request, community);
    }

    @RequestMapping("/all")
    public String queryAll(HttpServletRequest request,
                           @RequestParam(value = "community") String community) throws Exception {
        return queryService.queryAll(request, community);
    }

    @RequestMapping("/stars")
    public String queryStars(HttpServletRequest request,
                             @RequestParam(value = "community") String community) {
        return queryService.queryCount(request, community, "stars");
    }

    @RequestMapping("/issues")
    public String queryIssues(HttpServletRequest request,
                              @RequestParam(value = "community") String community) {
        return queryService.queryCount(request, community, "issues");
    }

    @RequestMapping("/prs")
    public String queryPrs(HttpServletRequest request,
                           @RequestParam(value = "community") String community) {
        return queryService.queryCount(request, community, "prs");
    }

    @RequestMapping(value = "/blueZone/contributes", method = RequestMethod.POST)
    public String queryBlueZoneContributes(HttpServletRequest request,
                                           @RequestBody BlueZoneContributeVo body) {
        return queryService.queryBlueZoneContributes(request, body);
    }

    @RequestMapping(value = "/blueZone/users", method = RequestMethod.POST)
    public String putBlueZoneUser(HttpServletRequest request,
                                  @RequestBody BlueZoneUserVo userVo) {
        return queryService.putBlueZoneUser(request, userVo);
    }

    @RequestMapping(value = "/starFork", method = RequestMethod.GET)
    public String queryOrgStarAndFork(HttpServletRequest request,
                                      @RequestParam(value = "community") String community) {
        return queryService.queryOrgStarAndFork(request, community);
    }

    @UserLoginToken
    @RequestMapping(value = "/cveDetails", method = RequestMethod.GET)
    public String queryCveDetails(HttpServletRequest request,
                                  @RequestParam(value = "community") String community,
                                  @RequestParam(value = "lastCursor", required = false) String lastCursor,
                                  @RequestParam(value = "pageSize", required = false) String pageSize) {
        return queryService.queryCveDetails(request, community, lastCursor, pageSize);
    }

    @RequestMapping("/newYear/report")
    public String queryNewYear(HttpServletRequest request,
                               @CookieValue(value = "_oauth2_proxy", required = true) String oauth2_proxy) {
        return queryService.queryNewYearPer(request, oauth2_proxy);
    }

    @RequestMapping("/newYear/monthcount")
    public String queryNewYearMonthCount(HttpServletRequest request,
                                         @CookieValue(value = "_oauth2_proxy", required = true) String oauth2_proxy) {
        return queryService.queryNewYearMonthCount(request, oauth2_proxy);
    }

    @RequestMapping("/bugQuestionnaires")
    public String queryBugQuestionnaires(HttpServletRequest request,
                                         @RequestParam(value = "community") String community,
                                         @RequestParam(value = "lastCursor", required = false) String lastCursor,
                                         @RequestParam(value = "pageSize", required = false) String pageSize) {
        return queryService.queryBugQuestionnaire(request, community, lastCursor, pageSize);
    }

    @RequestMapping(value = "add/bugquestionnaire", method = RequestMethod.POST)
    public String addBugQuestionnaire(HttpServletRequest request,
            @RequestParam String community,
            @RequestParam(value = "lang", required = false) String lang,
            @RequestBody BugQuestionnaireVo bugQuestionnaireVo) {
        return queryService.putBugQuestionnaire(request, community, lang, bugQuestionnaireVo);
    }

    @RequestMapping("/obsDetails")
    public String queryObsDetails(HttpServletRequest request,
                                  @RequestParam(value = "community") String community,
                                  @RequestParam(value = "branch") String branch,
                                  @RequestParam(value = "limit", required = false) String limit) {
        return queryService.queryObsDetails(request, branch, limit);
    }

    @RequestMapping(value = "/isoBuildTimes", method = RequestMethod.POST)
    public String queryIsoBuildTimes(HttpServletRequest request,
                                     @RequestBody IsoBuildTimesVo body) {
        return queryService.queryIsoBuildTimes(request, body);
    }

    @RequestMapping(value = "/sigDetails", method = RequestMethod.POST)
    public String querySigDetails(HttpServletRequest request,
                                  @RequestBody SigDetailsVo body) {
        return queryService.querySigDetails(request, body);
    }

    @RequestMapping("/company/contribute")
    public String queryCompanyContributors(HttpServletRequest request,
                                           @RequestParam(value = "community") String community,
                                           @RequestParam(value = "contributeType") String contributeType,
                                           @RequestParam(value = "timeRange", required = false) String timeRange,
                                           @RequestParam(value = "version", required = false) String version,
                                           @RequestParam(value = "repo", required = false) String repo) {
        return queryService.queryCompanyContributors(request, community, contributeType, timeRange, version, repo);
    }

    @RequestMapping("/user/contribute")
    public String queryUserContributors(HttpServletRequest request,
                                        @RequestParam(value = "community") String community,
                                        @RequestParam(value = "contributeType") String contributeType,
                                        @RequestParam(value = "timeRange") String timeRange,
                                        @RequestParam(value = "repo", required = false) String repo) {
        return queryService.queryUserContributors(request, community, contributeType, timeRange, repo);
    }

    @RequestMapping(value = "/issueScore", method = RequestMethod.GET)
    public String queryIssueScore(HttpServletRequest request,
                                  @RequestParam(value = "community") String community,
                                  @RequestParam(value = "start_date", required = false) String startDate,
                                  @RequestParam(value = "end_date", required = false) String endDate) {
        return queryService.queryIssueScore(request, startDate, endDate);
    }

    @RequestMapping(value = "/buildCheckInfo", method = RequestMethod.POST)
    public String queryBuildCheckInfo(HttpServletRequest request,
                                      @RequestBody BuildCheckInfoQueryVo queryBody,
                                      @RequestParam(value = "lastCursor", required = false) String lastCursor,
                                      @RequestParam(value = "pageSize", required = false) String pageSize) {
        return queryService.queryBuildCheckInfo(request, queryBody, lastCursor, pageSize);
    }

    @RequestMapping(value = "/track", method = RequestMethod.GET)
    public String putUserActionsInfo(HttpServletRequest request,
                                     @RequestParam(value = "community") String community,
                                     @RequestParam(value = "data") String data,
                                     @RequestParam(value = "ext") String ext) {
        return queryService.putUserActionsInfo(request, community, data);
    }

    @RequestMapping("/sig/name")
    public String querySigName(HttpServletRequest request,
                               @RequestParam(value = "community") String community,
                               @RequestParam(value = "lang", required = false) String lang) {
        return queryService.querySigName(request, community, lang);
    }

    @RequestMapping("/sig/info")
    public String querySigInfo(HttpServletRequest request,
                               @RequestParam(value = "community") String community,
                               @RequestParam(value = "sig", required = false) String sig,
                               @RequestParam(value = "repo", required = false) String repo,
                               @RequestParam(value = "user", required = false) String user,
                               @RequestParam(value = "search", required = false) String search,
                               @RequestParam(value = "page", required = false) String page,
                               @RequestParam(value = "pageSize", required = false) String pageSize) throws Exception {
        return queryService.querySigInfo(request, community, sig, repo, user, search, page, pageSize);
    }

    @RequestMapping("/sig/repo")
    public String querySigRepo(HttpServletRequest request,
                               @RequestParam(value = "community") String community,
                               @RequestParam(value = "sig", required = false) String sig,
                               @RequestParam(value = "page", required = false) String page,
                               @RequestParam(value = "pageSize", required = false) String pageSize) throws Exception {
        return queryService.querySigRepo(request, community, sig, page, pageSize);
    }

    @RequestMapping("sig/company/contribute")
    public String querySigCompanyContributors(HttpServletRequest request,
                                              @RequestParam(value = "community") String community,
                                              @RequestParam(value = "contributeType") String contributeType,
                                              @RequestParam(value = "timeRange") String timeRange,
                                              @RequestParam(value = "sig", required = false) String sig) {
        return queryService.querySigCompanyContributors(request, community, contributeType, timeRange, sig);
    }

    @RequestMapping("/company/name")
    public String queryCompanyName(HttpServletRequest request,
                                   @RequestParam(value = "community") String community) {
        return queryService.queryCompanyName(request, community);
    }

    @OneidToken
    @RequestMapping("/company/usercontribute")
    public String queryCompanyUserContribute(HttpServletRequest request,
                                             @RequestParam(value = "community") String community,
                                             @RequestParam(value = "company") String company,
                                             @RequestParam(value = "contributeType") String contributeType,
                                             @RequestParam(value = "timeRange") String timeRange,
                                             @CookieValue(value = "_Y_G_", required = false) String token) {
        return queryService.queryCompanyUserContribute(request, community, company, contributeType, timeRange, token);
    }

    @OneidToken
    @RequestMapping("/company/sigcontribute")
    public String queryCompanySigcontribute(HttpServletRequest request,
                                            @RequestParam(value = "community") String community,
                                            @RequestParam(value = "company") String company,
                                            @RequestParam(value = "contributeType") String contributeType,
                                            @RequestParam(value = "timeRange") String timeRange,
                                            @CookieValue(value = "_Y_G_", required = false) String token) {
        return queryService.queryCompanySigContribute(request, community, company, contributeType, timeRange, token);
    }

    @OneidToken
    @RequestMapping("/company/sigdetails")
    public String queryCompanySigDetails(HttpServletRequest request,
                                         @RequestParam(value = "community") String community,
                                         @RequestParam(value = "company") String company,
                                         @RequestParam(value = "timeRange") String timeRange,
                                         @CookieValue(value = "_Y_G_", required = false) String token) {
        return queryService.queryCompanySigDetails(request, community, company, timeRange, token);
    }

    @RequestMapping("/sig/usercontribute")
    public String querySigUserTypeCount(HttpServletRequest request,
                                        @RequestParam(value = "community") String community,
                                        @RequestParam(value = "sig") String sig,
                                        @RequestParam(value = "contributeType") String contributeType,
                                        @RequestParam(value = "timeRange") String timeRange) {
        return queryService.querySigUserTypeCount(request, community, sig, contributeType, timeRange);
    }

    @OneidToken
    @RequestMapping("/company/users")
    public String queryCompanyUsers(HttpServletRequest request,
                                    @RequestParam(value = "community") String community,
                                    @RequestParam(value = "company") String company,
                                    @RequestParam(value = "timeRange") String timeRange,
                                    @CookieValue(value = "_Y_G_", required = false) String token) {
        return queryService.queryCompanyUsers(request, community, company, timeRange, token);
    }

    @RequestMapping("/community/repos")
    public String queryRepos(HttpServletRequest request,
                             @RequestParam(value = "community") String community) {
        return queryService.queryCommunityRepos(request, community);
    }

    @OneidToken
    @SigToken
    @RequestMapping("/sig/score")
    public String querySigScore(HttpServletRequest request,
                                @RequestParam(value = "community") String community,
                                @RequestParam(value = "sig") String sig,
                                @RequestParam(value = "timeRange") String timeRange) {
        return queryService.querySigScore(request, community, sig, timeRange);
    }

    @RequestMapping("/sig/scoreAll")
    public String querySigScoreAll(HttpServletRequest request,
                                   @RequestParam(value = "community") String community) {
        return queryService.querySigScoreAll(request, community);
    }

    @OneidToken
    @SigToken
    @RequestMapping("/sig/radarscore")
    public String querySigRadarScore(HttpServletRequest request,
                                     @RequestParam(value = "community") String community,
                                     @RequestParam(value = "sig") String sig,
                                     @RequestParam(value = "timeRange") String timeRange) {
        return queryService.querySigRadarScore(request, community, sig, timeRange);
    }

    @RequestMapping("/company/sigs")
    public String queryCompanySigs(HttpServletRequest request,
                                   @RequestParam(value = "community") String community,
                                   @RequestParam(value = "timeRange") String timeRange) {
        return queryService.queryCompanySigs(request, community, timeRange);
    }

    @RequestMapping("/TC/sigs")
    public String querySigsOfTCOwners(HttpServletRequest request,
                                      @RequestParam(value = "community") String community) {
        return queryService.querySigsOfTCOwners(request, community);
    }

    @RequestMapping("/user/sigcontribute")
    public String queryUserSigContribute(HttpServletRequest request,
                                         @RequestParam(value = "community") String community,
                                         @RequestParam(value = "user") String user,
                                         @RequestParam(value = "contributeType") String contributeType,
                                         @RequestParam(value = "timeRange") String timeRange) {
        return queryService.queryUserSigContribute(request, community, user, contributeType, timeRange);
    }

    @RequestMapping("/user/ownertype")
    public String queryUserOwnerType(HttpServletRequest request,
                                     @RequestParam(value = "community") String community,
                                     @RequestParam(value = "user") String user) {
        return queryService.queryUserOwnerType(request, community, user);
    }

    @RequestMapping("/user/contribute/details")
    public String queryUserContributeDetails(HttpServletRequest request,
                                             @RequestParam(value = "community") String community,
                                             @RequestParam(value = "user") String user,
                                             @RequestParam(value = "sig", required = false) String sig,
                                             @RequestParam(value = "comment_type", required = false) String comment_type,
                                             @RequestParam(value = "filter", required = false) String filter,
                                             @RequestParam(value = "contributeType") String contributeType,
                                             @RequestParam(value = "timeRange") String timeRange,
                                             @RequestParam(value = "page", required = false) String page,
                                             @RequestParam(value = "pageSize", required = false) String pageSize) throws Exception {
        return queryService.queryUserContributeDetails(request, community, user, sig, contributeType, timeRange, page, pageSize, comment_type, filter);
    }


    @RequestMapping(value = "/gitee/webhook", method = RequestMethod.POST)
    public String giteeWebhook(HttpServletRequest request,
                               @RequestBody String requestBody) {
        return queryService.putGiteeHookUser(request, requestBody);
    }

    @RequestMapping("/userlist")
    public String queryUserLists(HttpServletRequest request,
                                 @RequestParam(value = "community") String community,
                                 @RequestParam(value = "group", required = false) String group,
                                 @RequestParam(value = "name", required = false) String name) {
        return queryService.queryUserLists(request, community, group, name);
    }

    @RequestMapping("/sig/repo/committers")
    public String querySigRepoCommitters(HttpServletRequest request,
                                         @RequestParam(value = "community") String community,
                                         @RequestParam(value = "sig") String sig) {
        return queryService.querySigRepoCommitters(request, community, sig);
    }

    @RequestMapping(value = "/metrics/data", method = RequestMethod.POST)
    public String queryMetricsData(HttpServletRequest request,
                                   @RequestParam(value = "community") String community,
                                   @RequestBody DatastatRequestBody body) {
        String res = queryService.queryMetricsData(request, community, body);
        return res;
    }

    @RequestMapping("/sig/pr/state")
    public String querySigPrDetails(HttpServletRequest request,
                                   @RequestParam(value = "community") String community,
                                   @RequestParam(value = "sig", required = false) String sig,
                                   @RequestParam(value = "timestamp", required = false) Long ts) {
        return queryService.querySigPrStateCount(request, community, sig, ts);
    }

    @RequestMapping("/cla/name")
    public String queryClaName(HttpServletRequest request,
                                   @RequestParam(value = "community") String community,
                                   @RequestParam(value = "timestamp", required = false) Long ts) {
        return queryService.queryClaName(request, community, ts);
    }

    @RequestMapping(value = "ecosystem/repo/info")
    public String getEcosystemRepoInfo(HttpServletRequest request,
            @RequestParam(value = "community") String community,
            @RequestParam(value = "ecosystem_type") String ecosystemType,
            @RequestParam(value = "lang", required = false) String lang,
            @RequestParam(value = "sort_type", required = false) String sortType,
            @RequestParam(value = "sort_order", required = false) String sortOrder,
            @RequestParam(value = "page", required = false) String page,
            @RequestParam(value = "pageSize", required = false) String pageSize) {
        return queryService.getEcosystemRepoInfo(request, community, ecosystemType, lang, sortType, sortOrder, page,
                pageSize);
    }

    @RequestMapping(value = "sig/readme")
    public String getSigReadme(HttpServletRequest request,
            @RequestParam(value = "community") String community,
            @RequestParam(value = "sig", required = false) String sig,
            @RequestParam(value = "lang", required = false) String lang) {
        return queryService.getSigReadme(request, community, sig, lang);
    }

    @RequestMapping("/test")
    public String test() throws InterruptedException {
        Thread.sleep(5000);
        return "time out";
    }

    @OneidToken
    @RequestMapping(value = "/meetupApplyForm", method = RequestMethod.POST)
    public String addMeetupApplyForm(HttpServletRequest request, 
            @RequestParam String community,
            @RequestBody MeetupApplyForm meetupApplyForm,
            @CookieValue(value = "_Y_G_", required = false) String token) {
        String res = queryService.putMeetupApplyForm(request, community, meetupApplyForm, token);
        return res;
    }
    
    @RequestMapping("/isv")
    public String queryCommunityIsv(HttpServletRequest request,
            @RequestParam(value = "community") String community,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "softwareType", required = false) String softwareType,
            @RequestParam(value = "company", required = false) String company) {
        return queryService.queryCommunityIsv(request, community, name, softwareType, company);
    }

    @RequestMapping(value = "/versions")
    public String queryCommunityVersions(HttpServletRequest request, @RequestParam String community) {
        String res = queryService.queryCommunityVersions(request, community);
        return res;
    }

    @RequestMapping(value = "repo/readme")
    public String getRepoReadme(HttpServletRequest request,
            @RequestParam(value = "community") String community,
            @RequestParam(value = "name") String name) {
        return queryService.getRepoReadme(request, community, name);
    }

    @RequestMapping(value = "user/permission/apply")
    public String putUserPermissionApply(HttpServletRequest request,
            @RequestParam(value = "community") String community,
            @RequestParam(value = "username") String username) {
        return queryService.putUserPermissionApply(request, community, username);
    }

    @RequestMapping(value = "qabot/chat", method = RequestMethod.POST)
    public String QaBotChat(HttpServletRequest request,
            @RequestBody QaBotRequestBody body) {
        return queryService.QaBotChat(request, body);
    }

    @RequestMapping(value = "qabot/suggestions", method = RequestMethod.POST)
    public String QaBotSuggestions(HttpServletRequest request,
            @RequestBody QaBotRequestBody body) {
        return queryService.QaBotSuggestions(request, body);
    }

    @RequestMapping(value = "qabot/satisfaction", method = RequestMethod.POST)
    public String QaBotSatisfaction(HttpServletRequest request,
            @RequestBody QaBotRequestBody body) {
        return queryService.QaBotSatisfaction(request, body);
    }

    @RequestMapping(value = "qabot/user_feedback", method = RequestMethod.POST)
    public String QaBotUserFeedback(HttpServletRequest request,
            @RequestBody QaBotRequestBody body) {
        return queryService.QaBotUserFeedback(request, body);
    }

    @RequestMapping(value = "/reviewer/recommend", method = RequestMethod.POST)
    public ResponseEntity queryReviewerRecommend(@RequestBody PrReviewerVo input) {
        ResponseEntity res = queryService.queryReviewerRecommend(input);
        return res;
    }

    @RequestMapping(value = "/innovation_items")
    public String queryInnovationItems(HttpServletRequest request, @RequestParam(value = "community") String community) {
        return queryService.queryInnovationItems(request, community);
    }

    @RequestMapping(value = "/list")
    public String queryAllProjects(HttpServletRequest request,
            @RequestParam(value = "community") String community,
            @RequestParam(value = "timeRange") String timeRange,
            @RequestParam(value = "groupField") String groupField,
            @RequestParam(value = "type") String type) {
        return queryService.queryAllProjects(request, community, timeRange, groupField, type);
    }

    @RequestMapping(value = "/project")
    public String queryByProjectName(HttpServletRequest request,
            @RequestParam(value = "community") String community,
            @RequestParam(value = "timeRange") String timeRange,
            @RequestParam(value = "groupField") String groupField,
            @RequestParam(value = "projectName") String projectName,
            @RequestParam(value = "type") String type) {
        return queryService.queryByProjectName(request, community, timeRange, groupField, projectName, type);
    }
  
    @RequestMapping(value = "/nps", method = RequestMethod.POST)
    public String getNps(HttpServletRequest request, @RequestParam(value = "community") String community,
            @RequestBody NpsBody body) {
        return queryService.getNps(request, community, body);
    }

    @RequestMapping(value = "/sig_defect")
    public String querySigDefect(HttpServletRequest request,
            @RequestParam(value = "community") String community,
            @RequestParam(value = "timeRange") String timeRange,
            @RequestParam(value = "sigName") String sigName) {
        return queryService.querySigDefect(request, community, timeRange, sigName);
    }

    @RequestMapping(value = "/sig/contribute")
    public String querySigContribute(HttpServletRequest request,
            @RequestParam(value = "community") String community,
            @RequestParam(value = "timeRange", required = false) String timeRange,
            @RequestParam(value = "projectName", required = false) String projectName,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "version", required = false) String version) {
        return queryService.querySigContribute(request, community, timeRange, projectName, type, version);
    }

    @RequestMapping(value = "/modelfoundry/download")
    public String queryModelFoundry(HttpServletRequest request,
            @RequestParam(value = "repo_id") String repo) {
        return queryService.queryModelFoundry(request, repo);
    }

    @RequestMapping(value = "/modelfoundry/download/trends")
    public String queryModelFoundryTrends(HttpServletRequest request,
            @RequestParam(value = "repo_id") String repo) {
        return queryService.queryModelFoundryTrends(request, repo);
    }

}