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

import com.datastat.model.CustomPropertiesConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@ConfigurationProperties(prefix = "openeuler")
@PropertySource(value = {"file:${config.path}/openeuler.properties"}, ignoreResourceNotFound = true, encoding = "UTF-8")
@Configuration("openeulerConf")
@Data
public class OpenEulerConfig extends CustomPropertiesConfig {
    @Override
    public String getAggCountQueryStr(CustomPropertiesConfig queryConf, String groupField, String contributeType, String timeRange, String community, String repo, String sig) {
        if (groupField.equals("company")) {
            sig = sig == null ? "*" : sig;
            return getQueryStrByType(contributeType, getGiteeAggCompanyQueryStr(), timeRange, sig);
        }

        return getQueryStrByType(contributeType, getGiteeAggUserQueryStr(), timeRange, null);
    }
}
