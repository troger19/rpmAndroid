package com.newventuresoftware.waveformdemo;

import java.util.concurrent.TimeUnit;

public class RpmUtil {

    public static String getTrainingTime(long aSeconds) {
        StringBuilder s = new StringBuilder();
        long minutes = TimeUnit.SECONDS.toMinutes(aSeconds) - (TimeUnit.SECONDS.toHours(aSeconds) * 60);
        long seconds = TimeUnit.SECONDS.toSeconds(aSeconds) - (TimeUnit.SECONDS.toMinutes(aSeconds) * 60);
        return s.append(minutes).append(" min. ").append(seconds).append(" sec.").toString();
    }
}
