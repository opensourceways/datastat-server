/* This project is licensed under the Mulan PSL v2.
 You can use this software according to the terms and conditions of the Mulan PSL v2.
 You may obtain a copy of Mulan PSL v2 at:
     http://license.coscl.org.cn/MulanPSL2
 THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR
 PURPOSE.
 See the Mulan PSL v2 for more details.
 Create: 2022
*/

package com.datastat.model.meetup;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SurveyAnswer {
    @Size(max = 50, message = "the length can not exceed 50")
    @Pattern(regexp = "^[^<>%&$]*$", message = "Text format error")
    private String optional;

    @Size(max = 100, message = "the length can not exceed 100")
    @Pattern(regexp = "^[^<>%&$]*$", message = "Text format error")
    private String comment;
}
