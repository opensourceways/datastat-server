package com.datastat.ds;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import com.datastat.dao.UserIdDao;
import com.datastat.ds.common.CommonUtil;
import com.datastat.ds.common.ReadCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;

/**
 * QueryDaoTests
 */
@SpringBootTest
@AutoConfigureMockMvc
public class QueryDaoTests {
    @Test
	void contextLoads() {
	}

	@Autowired
	private WebApplicationContext webApplicationContext;

    @MockBean
    private UserIdDao userIdDao;

	private ObjectMapper mapper = new ObjectMapper();

	private MockMvc mockMvc;

	@BeforeEach
	public void setUp() throws Exception {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

    @Test
    void test_get_nps() throws Exception{
        MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<>();
		paramMap.add("community", "openeuler");
        paramMap.add("repo", "easy-software");
        paramMap.add("page", "1");
        paramMap.add("pageSize", "1000");
        paramMap.add("sort", "desc");    

        String token = "abc123";
        Cookie cookie = new Cookie("_Y_G_", token);
		
		when(userIdDao.getUserId(token)).thenReturn("50216");
        String res = CommonUtil.executeGetWithCookie(mockMvc, "/query/get/nps", paramMap, cookie);
		CommonUtil.assertOk(res);
    }

    @Test
    void test_globalnps_issue() throws Exception{
        MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<>();
		paramMap.add("community", "software");

        String token = "abc123";
        Cookie cookie = new Cookie("_Y_G_", token);
        
		when(userIdDao.getUserId(token)).thenReturn("50216");
		JsonNode cases = ReadCase.readFile("src/test/java/com/datastat/ds/case/TestCase.json");
		JsonNode testCases = cases.get("nps");
		for (JsonNode testCase : testCases) {
			String body = mapper.writeValueAsString(testCase);
			String res = CommonUtil.executePostWithCookie(mockMvc, "/query/globalnps/issue", paramMap, body, cookie);
			CommonUtil.assertOk(res);
		}
    }

}