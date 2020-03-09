package com.newventuresoftware.waveformdemo;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.joda.time.LocalDateTime;
import org.joda.time.Seconds;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RpmUtil {

    public static String getTrainingTime(long aSeconds) {
        StringBuilder s = new StringBuilder();
        long minutes = TimeUnit.SECONDS.toMinutes(aSeconds) - (TimeUnit.SECONDS.toHours(aSeconds) * 60);
        long seconds = TimeUnit.SECONDS.toSeconds(aSeconds) - (TimeUnit.SECONDS.toMinutes(aSeconds) * 60);
        return s.append(minutes).append(" min. ").append(seconds).append(" sec.").toString();
    }

    /**
     * method extract from recorded object and transform it to training representation - > DTO
     *
     * @param statistics the recorded object with timestamp and RPM
     */
    protected TrainingDto convertToTrainingDto(ArrayList<OverallStatistics> statistics, Context context) {
        if (statistics.size() == 0) {
            return null;
        }
        List<Integer> rpm = new ArrayList<>();
        LocalDateTime startTime = statistics.get(0).getLocalDateTime();
        Map<Integer, Integer> rpm1 = new HashMap<>();
        for (OverallStatistics overallStatistics : statistics) {
            rpm.add((int) overallStatistics.getRpm());
            int seconds = Seconds.secondsBetween(startTime, overallStatistics.getLocalDateTime()).getSeconds();
            rpm1.put(seconds, (int) overallStatistics.getRpm());
        }

        LocalDateTime endTime = statistics.get(statistics.size() - 1).getLocalDateTime();
        int durationInSeconds = Seconds.secondsBetween(startTime, endTime).getSeconds();

        BigDecimal averageRpmByTime = calculateAverageByTime(rpm, durationInSeconds);
        BigDecimal averageRpm = calculateRpmAverage(rpm);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String name = preferences.getString("prf_username", "");

        TrainingDto trainingDto = new TrainingDto();
        trainingDto.setPersonName(name);
        trainingDto.setRpm(rpm1);
        trainingDto.setDuration(durationInSeconds);
        trainingDto.setAvgRpm(averageRpm);
        trainingDto.setAvgRpmTime(averageRpmByTime);

        return trainingDto;
    }

    /**
     * Calculate average RPM through out the whole activity
     *
     * @param rpms              revolutions
     * @param durationInSeconds duration of a training
     * @return average RPM by mean of time
     */
    private static BigDecimal calculateAverageByTime(List<Integer> rpms, int durationInSeconds) {
        Integer sum = 0;
        if (!rpms.isEmpty()) {
            for (Integer mark : rpms) {
                sum += mark;
            }
            BigDecimal averageRpmByTime = BigDecimal.valueOf(sum.doubleValue() / durationInSeconds);
            return averageRpmByTime.setScale(1, BigDecimal.ROUND_HALF_UP);
        }
        return BigDecimal.valueOf(sum);
    }

    /**
     * Calculate average RPM from the list of RPM
     *
     * @param rpms revolutions
     * @return average revolution
     */
    private static BigDecimal calculateRpmAverage(List<Integer> rpms) {
        Integer sum = 0;
        if (!rpms.isEmpty()) {
            for (Integer mark : rpms) {
                sum += mark;
            }
            BigDecimal averageRpm = BigDecimal.valueOf(sum.doubleValue() / rpms.size());
            return averageRpm.setScale(1, BigDecimal.ROUND_HALF_UP);
        }
        return BigDecimal.valueOf(sum);
    }

    public void saveTraining(String TAG, JsonPlaceHolderApi jsonPlaceHolderApi, TrainingDto trainingDto, Context context, boolean finished) {
        Log.i(TAG, "idecko je : " + MainActivity.id);
        Call<Long> call = jsonPlaceHolderApi.updateTraining(MainActivity.id, trainingDto);
        call.enqueue(new Callback<Long>() {
            @Override
            public void onResponse(Call<Long> call, Response<Long> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(context, "Code: " + response.code(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error calling Save Training: " + response.message());
                    return;
                }
                Log.i(TAG, "Saving training: " + response.body());
                Toast.makeText(context, "Training Saved", Toast.LENGTH_LONG).show();
                if (finished) {
                    DisplayGraphActivity.getBtnSave().setEnabled(false);
                }
            }

            @Override
            public void onFailure(Call<Long> call, Throwable t) {
                Toast.makeText(context, t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public int sumRpm(List<OverallStatistics> overall) {
        int sum = 0;
        for (OverallStatistics overallStatistic : overall) {
            sum += (int) overallStatistic.getRpm();
        }
        return sum;
    }
}
