package com.datastat.constant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Constant {

    private Constant() {
        throw new AssertionError("Constant class cannot be instantiated.");
    }

    public static final String SRC_OPENEULER = "src-openeuler";

    public static final String FEEDBACK_OWNER = "openeuler";

    public static final String FEEDBACK_REPO = "easy-software";

    /**
     * VALID_APPROVAL_REG used to match input string.
     */
    public static final String VALID_APPROVAL_REG = "model|dataset|space|^\\*$";

    /**
     * VALID_DATE_REG used to match input string.
     */
    public static final String VALID_DATE_REG = "^(\\d{4})-(\\d{2})-(\\d{2})$";

    /**
     * START_DATE used to match input string.
     */
    public static final String START_DATE = "1970-01-01";

    /**
     * END_DATE used to match input string.
     */
    public static final String END_DATE = "2050-01-01";

    /**
     * VALID_OPENMIND_ENV_REG used to match input string.
     */
    public static final String VALID_OPENMIND_ENV_REG = "pro|tianyi|sh";

    /**
     * VALID_REPO_ID used to match input string.
     */
    public static final String VALID_REPO_ID = "^\\d+$|^\\*$";

    /**
     * openmind社区.
     */
    public static final String OPENMIND_COMMUNITY = "openmind";

    /**
     * The name of github platform.
     */
    public static final String GITHUB_PLATFORM = "github";

    /**
     * The name of gutee platform.
     */
    public static final String GITEE_PLATFORM = "GITEE";

    /**
     * 支持性能数据上传的社区.
     */
    public static final List<String> PERF_DATA_COMMUNITY = Collections.unmodifiableList(new ArrayList<>() {
        {
            add(OPENMIND_COMMUNITY);
        }
    });
}