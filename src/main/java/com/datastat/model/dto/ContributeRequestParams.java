package com.datastat.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ContributeRequestParams {
    @NotBlank
    @Size(max = 20, message = "the length can not exceed 20")
    private String community;

    @NotBlank
    @Size(max = 50, message = "the length can not exceed 50")
    private String repo;

    @Size(max = 20, message = "the length can not exceed 20")
    private String filter;

    @Size(max = 20, message = "the length can not exceed 20")
    private String sort;

    @Min(value = 1, message = "Value must be greater than or equal to 1")
    private Integer page;
    
    @Min(value = 5, message = "Value must be greater than or equal to 5")
    @Max(value = 100, message = "Value must be less than or equal to 100")
    private Integer pageSize;
}
