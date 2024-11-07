package com.datastat.ds.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus; 
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;



public class CommonUtil {
    private static ObjectMapper mapper = new ObjectMapper();
    
    public static String executeGet(MockMvc mockMvc, String url, MultiValueMap<String, String> paramMap) throws Exception {
        String content = "";
        if (null == paramMap) {
            content = mockMvc.perform(MockMvcRequestBuilders.get(url)
                    .accept(MediaType.APPLICATION_JSON))
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        } else {
            content = mockMvc.perform(MockMvcRequestBuilders.get(url)
                    .params(paramMap)
                    .accept(MediaType.APPLICATION_JSON))
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        }
        return content;
    }

    public static String executeGetWithCookie(MockMvc mockMvc, String url, MultiValueMap<String, String> paramMap, Cookie cookie) throws Exception {
        String content = "";
        if (null == paramMap) {
            content = mockMvc.perform(MockMvcRequestBuilders.get(url)
                    .cookie(cookie)
                    .accept(MediaType.APPLICATION_JSON))
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        } else {
            content = mockMvc.perform(MockMvcRequestBuilders.get(url)
                    .params(paramMap)
                    .cookie(cookie)
                    .accept(MediaType.APPLICATION_JSON))
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        }
        return content;
    }

    public static String executePost(MockMvc mockMvc, String url, MultiValueMap<String, String> paramMap, String jsonRequest) throws Exception {
        String content = mockMvc.perform(MockMvcRequestBuilders.post(url)
            .params(paramMap)
            .content(jsonRequest)
            .contentType(MediaType.APPLICATION_JSON))
            .andReturn().getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

        return content;
    }

    public static String executePostWithCookie(MockMvc mockMvc, String url, MultiValueMap<String, String> paramMap, String jsonRequest, Cookie cookie) throws Exception {
        String content = mockMvc.perform(MockMvcRequestBuilders.post(url)
            .params(paramMap)
            .cookie(cookie)
            .content(jsonRequest)
            .contentType(MediaType.APPLICATION_JSON))
            .andReturn().getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

        return content;
    }

    public static void assertOk(String res) throws JsonMappingException, JsonProcessingException {
        JsonNode jsonRes = mapper.readTree(res);
        assertEquals(jsonRes.get("code").asInt(), HttpStatus.OK.value());
    }

}

