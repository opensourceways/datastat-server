package com.datastat.ds.unit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import com.datastat.config.QueryConfig;
import com.datastat.config.context.QueryConfContext;
import com.datastat.dao.FoundryDao;
import com.datastat.dao.KafkaDao;
import com.datastat.dao.ObsDao;
import com.datastat.dao.QueryDao;
import com.datastat.dao.RedisDao;
import com.datastat.dao.context.QueryDaoContext;
import com.datastat.ds.common.CommonUtil;
import com.datastat.model.dto.RequestParams;
import com.datastat.service.QueryService;
import jakarta.servlet.http.HttpServletRequest;

@SpringBootTest
@AutoConfigureMockMvc
public class ServiceUnitTests {
    @Test
    void contextLoads() {
    }

    @Mock
    QueryDaoContext queryDaoContext;

    @Mock
    QueryConfContext queryConfContext;

    @MockBean
    KafkaDao kafkaDao;

    @MockBean
    ObsDao obsDao;

    @Mock
    private RedisDao redisDao;

    @InjectMocks
    private QueryService queryService;

    @Mock
    private QueryConfig queryConfig;

    @Mock
    private FoundryDao foundryDao;

    @Mock
    private QueryDao queryDao;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(queryService, "communityList", Arrays.asList("openeuler"));
    }

    @Test()
    void testDownloadCountService() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        RequestParams params = new RequestParams();
        params.setRepoType("model");

        StringBuilder sb = new StringBuilder("modelfoundrycownload_repo_count_");
        sb.append(params.getPath())
                .append(params.getRepoType())
                .append(params.getRepoId())
                .append(params.getStart())
                .append(params.getEnd());
        String key = sb.toString();
        String result = "{\"code\":200,\"msg\":\"ok\",\"data\":[{\"repo_id\":\"1313\",\"repo\":\"Qwen\",\"download\":30}]}";
        when(redisDao.get(key)).thenReturn(result);
        String serviceRes = queryService.queryModelFoundryCountPath(request, params);
        CommonUtil.assertOk(serviceRes);

        when(redisDao.get(key)).thenReturn(null);
        when(queryDaoContext.getQueryDao("queryDao")).thenReturn(foundryDao);
        when(queryConfContext.getQueryConfig("foundryConf")).thenReturn(queryConfig);
        when(foundryDao.queryModelFoundryCountPath(queryConfig, params)).thenReturn(result);
        when(redisDao.set(key, result, 1l)).thenReturn(true);
        String res = queryService.queryModelFoundryCountPath(request, params);
        CommonUtil.assertOk(res);
    }

    @Test()
    void testUserOwnerTypeService() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        String user = "user";
        String community = "openeuler";
        String key = community.toLowerCase() + user + "ownertype";
        String result = "{\"code\":200,\"data\":[{\"sig\":\"infrastructrue\",\"type\":[\"committers\"]}],\"msg\":\"success\"}";
        when(redisDao.get(key)).thenReturn(result);
        String serviceRes = queryService.queryUserOwnerType(request, community, user);
        CommonUtil.assertOk(serviceRes);

        when(redisDao.get(key)).thenReturn(null);
        when(queryDaoContext.getQueryDao("queryDao")).thenReturn(queryDao);
        when(queryConfContext.getQueryConfig("queryConf")).thenReturn(queryConfig);
        when(queryDao.queryUserOwnerType(queryConfig, user)).thenReturn(result);
        when(redisDao.set(key, result, 1l)).thenReturn(true);
        String res = queryService.queryUserOwnerType(request, community, user);
        CommonUtil.assertOk(res);
    }
}
