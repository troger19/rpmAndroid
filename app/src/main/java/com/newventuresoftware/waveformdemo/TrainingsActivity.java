package com.newventuresoftware.waveformdemo;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.newventuresoftware.waveformdemo.MainActivity.BASE_URL;

public class TrainingsActivity extends AppCompatActivity {
    private static final String TAG = TrainingsActivity.class.getSimpleName();
    private TrainingAdapter adapter;
    private RecyclerView recyclerView;
    private JsonPlaceHolderApi jsonPlaceHolderApi;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trainings);

        // REST client initialization
        AuthenticationInterceptor interceptor = new AuthenticationInterceptor();
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        jsonPlaceHolderApi = retrofit.create(JsonPlaceHolderApi.class);
        recyclerView = findViewById(R.id.recycler_view_training_list);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(TrainingsActivity.this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        getAllTrainings();
    }

    private void getAllTrainings() {
        final ProgressDialog progressDialog = new ProgressDialog(TrainingsActivity.this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Please Wait");
        progressDialog.show();

        ArrayList<TrainingDto> trainingDtos = new ArrayList<>();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String name = preferences.getString("prf_username", "");
        Call<List<TrainingDto>> call = jsonPlaceHolderApi.getTrainingsByName(name);
        call.enqueue(new Callback<List<TrainingDto>>() {
            @Override
            public void onResponse(Call<List<TrainingDto>> call, Response<List<TrainingDto>> response) {
                if (!response.isSuccessful()) {
                    try {
                        Log.e(TAG, "Error calling Get All Trainings: " + Objects.requireNonNull(response.errorBody()).string());
                    } catch (IOException e) {
                        Log.e(TAG, "IOException: " + e);
                    }
                    progressDialog.dismiss();
                }
                Log.i(TAG, "Trainings Retrieved " + response.message());
                if (response.body() != null) {
                    trainingDtos.addAll(response.body());
                }
                progressDialog.dismiss();
                recyclerView.setAdapter(new TrainingAdapter(trainingDtos));
            }

            @Override
            public void onFailure(Call<List<TrainingDto>> call, Throwable t) {
                Toast.makeText(TrainingsActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
                progressDialog.dismiss();
            }
        });
    }
}
