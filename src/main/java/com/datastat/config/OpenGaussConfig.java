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

package com.datastat.config;

import com.datastat.dao.QueryDao;
import com.datastat.model.CustomPropertiesConfig;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@ConfigurationProperties(prefix = "opengauss")
@PropertySource(value = {"file:${user.dir}/openGauss.properties"})
@Configuration("opengaussConf")
@Data
public class OpenGaussConfig extends CustomPropertiesConfig {
    @Autowired
    Environment env;

    @Override
    public String getAggCountQueryStr(CustomPropertiesConfig queryConf, String groupField, String contributeType, String timeRange, String community, String repo, String sig) {
        String queryJson = groupField.equals("company") ? queryConf.getGiteeAggCompanyQueryStr() : queryConf.getGiteeAggUserQueryStr();
        if (repo == null || repo == "") {
            repo = "*";
        } else if (repo.equals("coreRepo")) {
            String repoListStr = queryConf.getCoreRepo();
            ArrayList<String> repoList = new ArrayList<>(Arrays.asList(repoListStr.split(",")));
            repo = convertList2QueryStr(repoList);
        } else {
            repo = String.format(env.getProperty("gitee.url"), repo);
        }

        return getQueryStrByType(contributeType, queryJson, timeRange, repo);
    }

    @Override
    public String getAggCommentQueryStr(CustomPropertiesConfig queryConf, String groupField, String timeRange, String repo) {
        String group = groupField.equals("company") ? "tag_user_company" : "user_login";
        String queryJson = getAggGroupCommentQueryStr();
        if (queryJson == null) return null;
        repo = repo == null ? "*" : String.format(env.getProperty("gitee.url"), repo);
        long currentTimeMillis = System.currentTimeMillis();
        long lastTimeMillis = getPastTime(timeRange);
        return queryStrFormat(queryJson, lastTimeMillis, currentTimeMillis, repo, group);
    }

    @Override
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

        String orDefault = contributeTypeMap.getOrDefault(contributeType, "");
        if (StringUtils.isBlank(orDefault)) return null;
        return getQueryStrWithTimeRange(queryJson, timeRange, group, label, orDefault);
    }

    @Override
    public String getUserContributeDetailsQuery(CustomPropertiesConfig queryConf, String sig, String label) {
        if (sig != null && sig.equals("Others")) sig = "No-SIG";
        sig = sig == null ? "*" : sig;

        return String.format(queryConf.getSigCountQuery(), sig, label);
    }

}
