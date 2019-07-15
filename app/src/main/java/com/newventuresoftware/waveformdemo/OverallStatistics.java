package com.newventuresoftware.waveformdemo;

import org.joda.time.LocalDateTime;

import java.io.Serializable;

public class OverallStatistics  implements Serializable {
    private org.joda.time.LocalDateTime localDateTime;
    private long rpm;

    public OverallStatistics(LocalDateTime localDateTime, long rpm) {
        this.localDateTime = localDateTime;
        this.rpm = rpm;
    }

    public org.joda.time.LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(org.joda.time.LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    public long getRpm() {
        return rpm;
    }

    public void setRpm(long rpm) {
        this.rpm = rpm;
    }
}
