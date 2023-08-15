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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import lombok.Data;

@Data
public class MeetupApplyForm {
    private String company;
    private String topic;
    private String date;
    private SurveyAnswer duration;
    private String city;
    private String meetupSize;
    private String principalUser;
    private String principalCompany;
    private String principalPhone;
    private String principalEmail;
    private SurveyAnswer meetupFormat;
    private ArrayList<SurveyAnswer> supports;
    private String details;

    public ArrayList<String> validMeetupApplyFormField() {
        ArrayList<String> errorMesseges = new ArrayList<>();
        if (!validStringLength()) {
            errorMesseges.add("text error");
        }
        if (!validDate()) {
            errorMesseges.add("date format error");
        }
        if (!validPhone()) {
            errorMesseges.add("phone format error");
        }
        if (!validEmail()) {
            errorMesseges.add("email format error");
        }
        if (!validDuration(getDuration())) {
            errorMesseges.add("duration error");
        }
        if (!validMeetupFormat(getMeetupFormat())) {
            errorMesseges.add("meetup format error");
        }
        return errorMesseges;
    }

    public Boolean validStringLength() {
        if (getCompany().length() <= 20
                && getTopic().length() <= 50
                && getCity().length() <= 10
                && getPrincipalUser().length() <= 10
                && getPrincipalCompany().length() <= 20
                && getDetails().length() <= 500) {
            return true;
        }
        return false;
    }

    public Boolean validDate(){
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate.parse(getDate(), formatter);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public Boolean validDuration(SurveyAnswer value){
        List<String> valueList = Arrays.asList("半天", "全天", "其他");
        String duration = value.getOptional();
        if (valueList.contains(duration)) {
            return true;
        }
        return false;
    }

    public Boolean validPhone(){
        if (getPrincipalPhone().length() >= 20) {
            return false;
        }
        return getPrincipalPhone().matches("\\d+");
    }

    public Boolean validEmail(){
        String[] parts = getPrincipalEmail().split("@");
        if (parts.length == 2 && parts[0].length() <=20 && parts[1].length() <=10) {
            return true;
        }
        return false;
    }

    public Boolean validMeetupFormat(SurveyAnswer value){
        List<String> valueList = Arrays.asList("线上活动", "线下活动", "线上+线下", "其他");
        String format = value.getOptional();
        if (valueList.contains(format)) {
            return true;
        }
        return false;
    }

}
