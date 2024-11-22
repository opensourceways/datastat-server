package com.datastat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.datastat.model.CustomPropertiesConfig;

import lombok.Data;

@ConfigurationProperties(prefix = "openubmc")
@PropertySource(value = {"file:${config.path}/openubmc.properties"}, ignoreResourceNotFound = true, encoding = "UTF-8")
@Configuration("openubmcConf")
@Data
public class OpenUbmcConfig extends CustomPropertiesConfig {
    
}
