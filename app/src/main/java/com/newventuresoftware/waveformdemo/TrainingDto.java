package com.newventuresoftware.waveformdemo;


import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

public class TrainingDto {
    private Date date;
    private Map<Integer, Integer> rpm;
    private Integer duration;
    private String personName;
    private BigDecimal avgRpm;
    private BigDecimal avgRpmTime;


    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public BigDecimal getAvgRpm() {
        return avgRpm;
    }

    public void setAvgRpm(BigDecimal avgRpm) {
        this.avgRpm = avgRpm;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public BigDecimal getAvgRpmTime() {
        return avgRpmTime;
    }

    public void setAvgRpmTime(BigDecimal avgRpmTime) {
        this.avgRpmTime = avgRpmTime;
    }


    public Map<Integer, Integer> getRpm() {
        return rpm;
    }

    public void setRpm(Map<Integer, Integer> rpm) {
        this.rpm = rpm;
    }
}
