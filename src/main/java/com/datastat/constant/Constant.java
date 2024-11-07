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
     * openmind社区.
     */
    public static final String OPENMIND_COMMUNITY = "openmind";

    /**
     * 支持性能数据上传的社区.
     */
    public static final List<String> PERF_DATA_COMMUNITY = Collections.unmodifiableList(new ArrayList<>() {
        {
            add(OPENMIND_COMMUNITY);
        }
    });
}