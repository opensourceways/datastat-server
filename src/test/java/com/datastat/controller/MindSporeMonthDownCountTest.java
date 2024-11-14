package com.datastat.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.datastat.ds.DsApplication;

@SpringBootTest(classes = DsApplication.class)
@AutoConfigureMockMvc
public class MindSporeMonthDownCountTest {

  @Autowired
  private WebApplicationContext webApplicationContext;

  private MockMvc mockMvc;

  @BeforeEach
  public void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
  }

  @Test
  public void test() throws Exception {
    String content = mockMvc.perform(MockMvcRequestBuilders.get("/query/monthdowncount/openmind/30387")
        .accept(MediaType.APPLICATION_JSON_VALUE))
        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

    System.out.println("Conetent: " + content);
    assertNotNull(content);
    assertTrue(content.contains("data"));
  }
}