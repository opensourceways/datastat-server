package com.datastat.model;

import com.datastat.aop.moderation.ModerationValid;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OpenUbmcSearchNps {

    private Boolean searchFlag;

    @Size(max = 100,  message = "the length can not exceed 100")
    @Pattern(regexp = "^[^<>%&$]*$", message = "Text format error")
    @ModerationValid
    private String feedbackText;

}
