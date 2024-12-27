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

package com.datastat.ds.unit;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import com.datastat.config.QueryConfig;
import com.datastat.config.context.QueryConfContext;
import com.datastat.dao.KafkaDao;
import com.datastat.dao.ObsDao;
import com.datastat.dao.QueryDao;
import com.datastat.dao.RedisDao;
import com.datastat.dao.context.QueryDaoContext;
import com.datastat.ds.common.CommonUtil;
import com.datastat.model.dto.RequestParams;
import com.datastat.util.EsAsyncHttpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
public class DaoUnitTests {
    @Test
    void contextLoads() {
    }

    @Mock
    QueryDaoContext queryDaoContext;

    @Mock
    QueryConfContext queryConfContext;

    @Mock
    ListenableFuture<Response> mockFuture;

    @Mock
    Response mockResponse;

    @MockBean
    KafkaDao kafkaDao;

    @MockBean
    ObsDao obsDao;

    @Mock
    private RedisDao redisDao;

    @Mock
    private QueryConfig queryConfig;

    @InjectMocks
    private QueryDao queryDao;

    @Mock
    private EsAsyncHttpUtil esAsyncHttpUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(queryDao, "objectMapper", objectMapper);
    }

    @Test()
    void testUserOwnerTypeDao() throws Exception {
        String respBody = "{\"aggregations\":{\"group_field\":{\"buckets\":[{\"key\":\"sig-python-modules\","
                + "\"user\":{\"buckets\":[{\"key\":\"myeuler\",\"doc_count\":1609,"
                + "\"type\":{\"buckets\":[{\"key\":\"maintainers\",\"doc_count\":1609}]}},"
                + "{\"key\":\"shinwell_hu\",\"type\":{\"buckets\":[{\"key\":\"maintainers\",\"doc_count\":1609}]}}]}}]}}}";

        when(esAsyncHttpUtil.executeSearch(anyString(), isNull(), isNull())).thenReturn(mockFuture);
        when(mockFuture.get()).thenReturn(mockResponse);
        when(mockResponse.getStatusCode()).thenReturn(200);
        when(mockResponse.getStatusText()).thenReturn("OK");
        when(mockResponse.getResponseBody(StandardCharsets.UTF_8)).thenReturn(respBody);
        String community = "openubmc";
        String user = "user";
        when(queryDaoContext.getQueryDao(community)).thenReturn(queryDao);
        when(queryConfContext.getQueryConfig(community)).thenReturn(queryConfig);
        String res = queryDao.queryUserOwnerType(queryConfig, user);
        CommonUtil.assertOk(res);

        community = "openeuler";
        when(queryDaoContext.getQueryDao(community)).thenReturn(queryDao);
        when(queryConfContext.getQueryConfig(community)).thenReturn(queryConfig);
        res = queryDao.queryUserOwnerType(queryConfig, user);
        CommonUtil.assertOk(res);
    }


    @Test()
    void testViewCountDao() throws Exception {
        String respBody = "{\"aggregations\":{\"group_field\":{\"buckets\":[{\"key\":\"3828\",\"doc_count\":1609}]}}}";
        when(esAsyncHttpUtil.executeSearch(anyString(), isNull(), anyString())).thenReturn(mockFuture);
        when(mockFuture.get()).thenReturn(mockResponse);
        when(mockResponse.getStatusCode()).thenReturn(200);
        when(mockResponse.getStatusText()).thenReturn("OK");
        when(mockResponse.getResponseBody(StandardCharsets.UTF_8)).thenReturn(respBody);
        String community = "foundry";
        RequestParams params = new RequestParams();
        params.setStart("2024-01-01");
        params.setEnd("2024-12-01");
        params.setRepoType("model");
        params.setRepoId("3828");

        when(queryDaoContext.getQueryDao(community)).thenReturn(queryDao);
        when(queryConfContext.getQueryConfig(community)).thenReturn(queryConfig);
        String query = "{\"size\":0,\"query\":{\"bool\":{\"filter\":[{\"range\":{\"created_at\":{\"gte\":\"%s\",\"lte\":\"%s\"}}},"
                + "{\"query_string\":{\"analyze_wildcard\":true,"
                + "\"query\":\"event.keyword:RV AND properties.module.keyword:%s AND properties.id.keyword:%s\"}}]}},"
                + "\"aggs\":{\"group_field\":{\"terms\":{\"field\":\"properties.id.keyword\",\"size\":50,"
                + "\"order\":{\"_count\":\"desc\"},\"min_doc_count\":1},\"aggs\":{}}}}";
        when(queryConfig.getRepoViewCountQueryStr()).thenReturn(query);
        String res = queryDao.getViewCount(queryConfig, params);
        CommonUtil.assertOk(res);
    }

}
