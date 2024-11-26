/* This project is licensed under the Mulan PSL v2.
 You can use this software according to the terms and conditions of the Mulan PSL v2.
 You may obtain a copy of Mulan PSL v2 at:
     http://license.coscl.org.cn/MulanPSL2
 THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR
 PURPOSE.
 See the Mulan PSL v2 for more details.
 Create: 2024
*/

package com.datastat.model.dto;

import com.datastat.constant.Constant;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RequestParams {
    /**
     * repo type
     */
    @Size(max = 20, message = "the length can not exceed 20")
    @Pattern(regexp = Constant.VALID_APPROVAL_REG, message = "Input repo type error")
    private String repoType = "model";

    /**
     * This parameter is used to indicate which environment the data comes from.
     */
    @Size(max = 50, message = "the length can not exceed 50")
    @Pattern(regexp = Constant.VALID_OPENMIND_ENV_REG, message = "Input path error")
    private String path = "pro";

    /**
     * repo id
     */
    @Size(max = 20, message = "the length can not exceed 20")
    @Pattern(regexp = Constant.VALID_REPO_ID, message = "Input repo id error")
    private String repoId = "*";

    /**
     * compute data from start time
     */
    @Size(max = 20, message = "the length can not exceed 20")
    @Pattern(regexp = Constant.VALID_DATE_REG, message = "Input start date error")
    private String start = Constant.START_DATE;

    /**
     * compute data to end time
     */
    @Size(max = 20, message = "the length can not exceed 20")
    @Pattern(regexp = Constant.VALID_DATE_REG, message = "Input end date error")
    private String end = Constant.END_DATE;

}
