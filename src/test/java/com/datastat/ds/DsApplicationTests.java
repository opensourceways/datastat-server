package com.datastat.ds;

import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import com.datastat.ds.common.CommonUtil;
import com.datastat.ds.common.ReadCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class DsApplicationTests {

	@Test
	void contextLoads() {
	}

	@Autowired
	private WebApplicationContext webApplicationContext;

	private ObjectMapper mapper = new ObjectMapper();

	private MockMvc mockMvc;

	@BeforeEach
	public void setUp() throws Exception {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@Test
	void testNps() throws Exception {
		MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<>();
		paramMap.add("community", "openeuler");

		JsonNode cases = ReadCase.readFile("src/test/java/com/datastat/ds/case/TestCase.json");
		JsonNode testCases = cases.get("nps");
		for (JsonNode testCase : testCases) {
			String body = mapper.writeValueAsString(testCase);
			String res = CommonUtil.executePost(mockMvc, "/query/nps", paramMap, body);
			CommonUtil.assertOk(res);
		}

	}
}
