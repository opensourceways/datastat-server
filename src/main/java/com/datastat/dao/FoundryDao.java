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

import com.datastat.model.CustomPropertiesConfig;
import com.datastat.util.ResultUtil;

import lombok.SneakyThrows;
import org.springframework.stereotype.Repository;


@Repository("foundryDao")
public class FoundryDao extends QueryDao {
    @SneakyThrows
    @Override
    public String queryContributors(CustomPropertiesConfig queryConf, String item) {
        return ResultUtil.resultJsonStr(404, item, 0, "Not Found");
    }

    @SneakyThrows
    @Override
    public String queryUsers(CustomPropertiesConfig queryConf, String item) {
        return ResultUtil.resultJsonStr(404, item, 0, "Not Found");
    }

    @SneakyThrows
    @Override
    public String querySigs(CustomPropertiesConfig queryConf, String item) {
        return ResultUtil.resultJsonStr(404, item, 0, "Not Found");
    }

    @Override
    public String queryDownload(CustomPropertiesConfig queryConf, String item) {
        return ResultUtil.resultJsonStr(404, item, 0, "Not Found");
    }
}
