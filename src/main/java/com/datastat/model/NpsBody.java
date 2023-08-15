package com.datastat.model;

import lombok.Data;

@Data
public class NpsBody {
    private String community;
    private String page;
    private int score;
    private String suggest;
}
