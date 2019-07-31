package com.newventuresoftware.waveformdemo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.joda.time.LocalDateTime;
import org.joda.time.Seconds;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DisplayGraphActivity extends AppCompatActivity {
    private static final String TAG = DisplayGraphActivity.class.getSimpleName();
    private TextView txtAverageRpm, txtDuration;
    private JsonPlaceHolderApi jsonPlaceHolderApi;
    private TrainingDto trainingDto;
    private static final String BASE_URL = MainActivity.BASE_URL;
    private Button btnSave;
    private boolean exists;
    private String name;
    SharedPreferences preferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_graph);
        btnSave = findViewById(R.id.btnSaveTraining);
        txtAverageRpm = findViewById(R.id.txtAverageRpm);
        txtDuration = findViewById(R.id.txtDuration);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        name = preferences.getString("prf_username", "");

        Intent intent = getIntent();
        ArrayList<OverallStatistics> list = (ArrayList<OverallStatistics>) intent.getSerializableExtra(MainActivity.EXTRA_MESSAGE);

        //draw a chart
        LineChart lineChart = findViewById(R.id.chart);
        LineDataSet lineDataSet = new LineDataSet(createDataSet(list), "Training series");
        ArrayList<ILineDataSet> dataSet = new ArrayList<>();
        dataSet.add(lineDataSet);
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.invalidate();

        // REST client initialization
        AuthenticationInterceptor interceptor = new AuthenticationInterceptor();
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        jsonPlaceHolderApi = retrofit.create(JsonPlaceHolderApi.class);
        convertToTrainingDto(list);
    }

    /**
     * The method create dataset in format (X,Y)
     *
     * @param list the recorded object with timestamp and RPM
     * @return List of Entries in format (X,Y)
     */
    private ArrayList<Entry> createDataSet(ArrayList<OverallStatistics> list) {
        ArrayList<Entry> data = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            data.add(new Entry(i, list.get(i).getRpm()));
        }
        return data;
    }

    /**
     * method extract from recorded object and transform it to training representation - > DTO
     *
     * @param statistics the recorded object with timestamp and RPM
     */
    private void convertToTrainingDto(ArrayList<OverallStatistics> statistics) {
        if (statistics.size() == 0) {
            return;
        }
        List<Integer> rpm = new ArrayList<>();
        for (OverallStatistics overallStatistics : statistics) {
            rpm.add((int) overallStatistics.getRpm());
        }

        LocalDateTime startTime = statistics.get(0).getLocalDateTime();
        LocalDateTime endTime = statistics.get(statistics.size() - 1).getLocalDateTime();
        int durationInSeconds = Seconds.secondsBetween(startTime, endTime).getSeconds(); //TODO replace with seconds

        double average = calculateAverage(rpm, durationInSeconds); //TODO Overall average?? rpms.size() -> durationInSecondes  / sumOfRpms
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String name = preferences.getString("prf_username", "");

        trainingDto = new TrainingDto();
        trainingDto.setPersonName(name);
        trainingDto.setRpm(rpm);
        trainingDto.setDuration(durationInSeconds);
        trainingDto.setAverage(average);

        txtAverageRpm.setText(String.valueOf(BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP)));
        txtDuration.setText(String.valueOf(durationInSeconds) + getString(R.string.minutes));
    }

    /**
     * REST call to backend servis to save the training
     *
     * @param view this
     */
    public void saveTraining(View view) {
        isExistingUser();
        if (!exists) {
            Toast.makeText(DisplayGraphActivity.this, "User " + name + " does not exists", Toast.LENGTH_LONG).show();
            return;
        }
        Call<ResponseBody> call = jsonPlaceHolderApi.createTraining(trainingDto);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(DisplayGraphActivity.this, "Code: " + response.code(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error calling Save Training: " + response.message());
                    return;
                }
                Log.i(TAG, "Saving training: " + response.message());
                Toast.makeText(DisplayGraphActivity.this, "Training Saved", Toast.LENGTH_LONG).show();
                btnSave.setEnabled(false);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(DisplayGraphActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean isExistingUser() {
        name = preferences.getString("prf_username", "");
        Call<PersonDto> call = jsonPlaceHolderApi.getPerson(name);

        call.enqueue(new Callback<PersonDto>() {
            @Override
            public void onResponse(Call<PersonDto> call, Response<PersonDto> response) {
                if (!response.isSuccessful()) {
                    try {
                        Log.e(TAG, "Error calling Get Person: " + Objects.requireNonNull(response.errorBody()).string());
                        Intent intent = new Intent(DisplayGraphActivity.this, SettingsActivity.class);
                        startActivity(intent);
                    } catch (IOException e) {
                        Log.e(TAG, "IOException: " + e);
                        ;
                    }
                    return;
                }
                Log.i(TAG, "Person exists " + response.message());
                exists = response.body() != null;
            }

            @Override
            public void onFailure(Call<PersonDto> call, Throwable t) {
                Toast.makeText(DisplayGraphActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        return false;
    }

    private static double calculateAverage(List<Integer> rpms, int durationInSeconds) {
        Integer sum = 0;
        if (!rpms.isEmpty()) {
            for (Integer mark : rpms) {
                sum += mark;
            }
//            return sum.doubleValue() / rpms.size();
            return sum.doubleValue() / durationInSeconds;
        }
        return sum;
    }
}
