package com.datastat.model;

import com.datastat.aop.moderation.ModerationValid;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NpsBody {
    @Size(max = 500,  message = "the length can not exceed 500")
    @Pattern(regexp = "^((http|https)://)[^<>{}\\[\\])]*$", message = "Url format error")
    private String feedbackPageUrl;

    @Min(value = 0, message = "Value must be greater than or equal to 0")
    @Max(value = 10, message = "Value must be less than or equal to 10")
    private int feedbackValue;

    @Size(max = 500,  message = "the length can not exceed 500")
    @Pattern(regexp = "^[^<>%&$]*$", message = "Text format error")
    @NotBlank
    @ModerationValid
    private String feedbackText;
}
