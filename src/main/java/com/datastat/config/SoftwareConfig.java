package com.datastat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.datastat.model.CustomPropertiesConfig;

import lombok.Data;

@ConfigurationProperties(prefix = "software")
@PropertySource(value = {"file:${user.dir}/software.properties"})
@Configuration("softwareConf")
@Data
public class SoftwareConfig extends CustomPropertiesConfig {
    
}
