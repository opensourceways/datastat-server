package com.datastat.model.yaml;

import java.util.List;

import lombok.Data;

@Data
public class InnovationItemInfo {
    private String projectName;
    private List<String> repos;
}
