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

package com.datastat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

import com.datastat.dao.RedisDao;
import com.datastat.model.CustomPropertiesConfig;
import com.datastat.util.EsAsyncHttpUtil;
import com.datastat.util.PageUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;



/**
 * @author xiazhonghai
 * @date 2021/3/8 17:25
 * @description:
 */
@Service
public class VersionService {
    @Autowired
    RedisDao redisDao;

    @Autowired
    EsAsyncHttpUtil asyncHttpUtil;

    @Autowired
    private Environment env;

    private static final String redisKey = "VERSION";

    protected static String esUrl;

    private static ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        esUrl = String.format("%s://%s:%s/", env.getProperty("es.scheme"), env.getProperty("es.host"), env.getProperty("es.port"));
    }


    /***
     * 功能描述:
     * @param community:
     * @param repo:
     * @param pageSize:
     * @param currentPage:
     * @return: java.util.List
     * @Author: xiazhonghai
     * @Date: 2021/3/22 10:18
     */

    public String getVersionByRepoBranch(CustomPropertiesConfig queryConf, String community, String repo, int pageSize,
            int currentPage) {
        try {
            String data = (String) redisDao.get(community + redisKey);
            if (StringUtils.isBlank(data)) {
                String url = esUrl + queryConf.getGiteeAllIndex() + "/_search";
                long expire = Long.valueOf(env.getProperty("redis.keyExpire"));

                RequestBuilder builder = asyncHttpUtil.getBuilder();
                String bodyData = "{\"query\":{\"bool\":{\"filter\":[{\"query_string\":{\"analyze_wildcard\":true,\"query\":\"is_gitee_repo:1\"}}]}},\"_source\":[\"branch_detail\"],\"size\":10000}";
                Request request = builder.setUrl(url).setBody(bodyData).build();
                ListenableFuture<Response> future = EsAsyncHttpUtil.getClient().executeRequest(request);
                Response response = future.get();
                String responseBody = response.getResponseBody(StandardCharsets.UTF_8);
                redisDao.set(community + redisKey, responseBody, expire);
                Map map = assemblyData(repo, responseBody, currentPage, pageSize);
                return resultJsonStr(200, objectMapper.valueToTree(map.get("data")), map.get("total"), "SUCCESS");
            } else {
                Map map = assemblyData(repo, data, currentPage, pageSize);
                return resultJsonStr(200, objectMapper.valueToTree(map.get("data")), map.get("total"), "SUCCESS");
            }
        } catch (Exception e) {
            return resultJsonStr(400, null, 0, "Failed");
        }

    }

    public String resultJsonStr(int code, Object data, Object total, String msg) {
        return "{\"code\":" + code + ",\"data\":" + data + ",\"total\":" + total + ",\"msg\":\"" + msg + "\"}";
    }

    /***
     * 功能描述:组装过滤数据
     * @param data: 待处理数据，使用json 反序列化
     * @param page: 分页
     * @param pageSize:一页多少数据
     * @return: java.util.List
     * @Author: xiazhonghai
     * @Date: 2021/3/22 11:27
     */
    private Map assemblyData(String repo, String data, int page, int pageSize) throws JsonProcessingException {
        Map allDataMap = objectMapper.readValue(data, Map.class);
        List<Map> datas = (List) (((Map) allDataMap.get("hits")).get("hits"));
        Stream<Map> source = datas.stream().filter(map -> {
            Object source1 = map.get("_source");
            if (map == null || ((Map) source1).size() <= 0) {
                return false;
            }
            return true;
        });
        if (StringUtils.isNotBlank(repo)) {
            source = source.filter(map -> {
                String id = map.get("_id").toString();
                if (!id.contains("src-openeuler")){
                 return false;
                }
                String repoName = id.substring(id.lastIndexOf("/") + 1);
                if (repo.equals(repoName)) {
                    return true;
                } else {
                    return false;
                }
            });
        }
        List<Map> collect = source.collect(Collectors.toList());
        for (Map map : collect) {
            map.remove("_type");
            map.remove("_score");
            map.remove("_index");
            String id = map.remove("_id").toString();
            String repoName = id.substring(id.lastIndexOf("/") + 1);
            Object sourceItem = map.remove("_source");
            map.put(repoName, sourceItem);
            //对description 进行下类型转换
            List branches = (List) ((Map) sourceItem).get("branches");
            if (branches != null && branches.size() > 0) {
                for (Map branch : (List<Map>) branches) {
                    Object de = branch.remove("description");
                    List description = null;
                    if (de instanceof List){
                        description = (List) de;
                    } else if (de instanceof  String){
                        description = new ArrayList();
                        description.add(de);
                    }
                    if (description == null) {
                        branch.put("description", "");
                    } else {
                        branch.put("description", description.get(0));
                    }

                }
            }
        }
        if(pageSize==0||page==0){
            Map map = new HashMap();
            map.put("data",collect);
            map.put("total",collect.size());
            return map;
        }else {
            Map dataByPage = PageUtils.getDataByPage(page, pageSize, collect);
            return dataByPage;
        }
    }

    /***
     * 功能描述:
     * @param datas:传入数据
     * @param repo: 仓库名称
     * @param branch:分支名称
     * @return: java.util.List
     * @Author: xiazhonghai
     * @Date: 2021/3/9 9:38
     */
    public static List filterDataByConditions(List<Map> datas, String repo, String branch) {
        //如果为空返回对应的全部数据 eg repo 为空返回该社区的所有repo对应的version数据
        Stream<Map> resultData = datas.stream().filter(map -> {
            boolean returnAllRepo = false;
            boolean returnAllBranch = false;
            String repoName = map.get("repo").toString();
            String repoBranch = map.get("branch").toString();
            if (StringUtils.isBlank(repo)) {
                returnAllRepo = true;
            }
            if (StringUtils.isBlank(branch)) {
                returnAllBranch = true;
            }
            if (returnAllBranch && returnAllRepo) {
                //repo 不为空，branch 为空 返回此branch的所有数据/
                return true;
            } else if (returnAllBranch) {
                //repo 不为空，branch 为空 返回此branch的所有数据
                if (repoName.equals(repo)) {
                    return true;
                }
            } else {
                //repo 不为空 branch 不为空，返回指定repo 中指定branch的数据。

                if (repoName.equals(repo) && repoBranch.equals(branch)) {
                    return true;
                }
            }
            return false;
        });
        List<Map> collect = resultData.collect(Collectors.toList());
        return collect;
    }
}