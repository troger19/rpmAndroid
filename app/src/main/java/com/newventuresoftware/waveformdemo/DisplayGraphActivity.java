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

import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DisplayGraphActivity extends AppCompatActivity {
    private static final String TAG = DisplayGraphActivity.class.getSimpleName();
    private TextView txtAverageRpm, txtAverageRpmTime, txtDuration;
    private JsonPlaceHolderApi jsonPlaceHolderApi;
    private TrainingDto trainingDto;
    private static final String BASE_URL = MainActivity.BASE_URL;
    public static Button btnSave;
    private RpmUtil rpmUtil;
    SharedPreferences preferences;

    public static Button getBtnSave() {
        return btnSave;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_graph);
        btnSave = findViewById(R.id.btnSaveTraining);
        txtAverageRpm = findViewById(R.id.txtAverageRpm);
        txtAverageRpmTime = findViewById(R.id.txtAverageRpmTime);
        txtDuration = findViewById(R.id.txtDuration);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        rpmUtil = new RpmUtil();

        Intent intent = getIntent();
        ArrayList<OverallStatistics> list = (ArrayList<OverallStatistics>) intent.getSerializableExtra(MainActivity.EXTRA_MESSAGE);

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
        wakeupPing();
        trainingDto = rpmUtil.convertToTrainingDto(list, DisplayGraphActivity.this);
        txtAverageRpm.setText(String.valueOf(trainingDto.getAvgRpm()));
        txtAverageRpmTime.setText(String.valueOf(trainingDto.getAvgRpmTime()));
        txtDuration.setText(RpmUtil.getTrainingTime(trainingDto.getDuration()));
    }

    private void wakeupPing() {
        Call<ResponseBody> call = jsonPlaceHolderApi.healthCheck();
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.i(TAG, "Health check " + response.message());
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(DisplayGraphActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
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
     * REST call to backend service to save the training
     *
     * @param view this
     */
    public void saveTraining(View view) {
        rpmUtil.saveTraining(TAG, jsonPlaceHolderApi, trainingDto, DisplayGraphActivity.this, true);
    }
}
