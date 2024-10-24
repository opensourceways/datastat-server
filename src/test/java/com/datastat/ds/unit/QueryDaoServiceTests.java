package com.datastat.ds.unit;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import com.datastat.dao.QueryDao;
import com.datastat.dao.RedisDao;
import com.datastat.dao.UserIdDao;
import com.datastat.ds.common.CommonUtil;
import com.datastat.model.dto.ContributeRequestParams;
import com.datastat.service.QueryService;

import jakarta.servlet.http.HttpServletRequest;


/**
 * QueryDaoTests
 */
@SpringBootTest
@AutoConfigureMockMvc
public class QueryDaoServiceTests {
    

    @Mock
    private QueryDao queryDao;

    @Mock
    private UserIdDao userIdDao;

    @Mock
    private RedisDao redisDao;

    @Spy
    @InjectMocks
    private QueryService queryService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testQueryGolbalIssues_hasRedisValue() throws Exception{
        HttpServletRequest request = mock(HttpServletRequest.class);
        String token = "ccca";
        ContributeRequestParams params = new ContributeRequestParams();
        params.setCommunity("openeuler");
        params.setRepo("easy-software");
        params.setSort("desc");
        params.setPage(1);;
        params.setPageSize(10);

        doReturn(true).when(queryService).checkCommunity(token); when(queryService.checkCommunity(token)).thenReturn(true);
        when(userIdDao.getUserId(token)).thenReturn("123456");
        String key = params.getCommunity() + params.getRepo() + params.getSort() + params.getFilter() + "globalfeedbackissue" + "123456";
        String result = "{\"code\":200,\"message\":\"Success\",\"data\":[{\"id\":1,\"name\":\"Alice\",\"age\":30},{\"id\":2,\"name\":\"Bob\",\"age\":25}]}";
        when(redisDao.get(key)).thenReturn(result);
        
        String res = queryService.queryGolbalIssues(request, token, params);

        CommonUtil.assertOk(res);
    }
}