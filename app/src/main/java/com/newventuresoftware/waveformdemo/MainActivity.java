package com.newventuresoftware.waveformdemo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.github.anastr.speedviewlib.SpeedView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "OverallStatistics";
    private static final int firstDelay = 60_000;
    private static final int repeatPeriod = 60_000;
    private RecordingThread mRecordingThread;
    private static final int REQUEST_RECORD_AUDIO = 13;
    public static Long id;
    private boolean exists;
    // This is the activity main thread Handler.
    private Handler updateUIHandler = null;
    private ArrayList<OverallStatistics> overall = new ArrayList<>();
    private JsonPlaceHolderApi jsonPlaceHolderApi;
    private static final String TAG = MainActivity.class.getSimpleName();
    public static final String BASE_URL = "https://rpmbackend.herokuapp.com/";  //https://rpmbackend.herokuapp.com/   //http://192.168.100.41:8081/   //http://192.168.43.52:8081/

    // Message type code.
    private final static int MESSAGE_UPDATE_TEXT_CHILD_THREAD = 1;
    private TextView textView, txtCompare;
    private SpeedView speedometer;
    private Button btnStartStop, btnTrainings;
    private String name;
    private boolean running;
    private Chronometer chronometer;
    String welcomeUserMessage;
    ArrayDeque<Long> allRpms = new ArrayDeque<>();
    private SharedPreferences preferences;
    private static final int minRPM = 120;
    private static final int maxRPM = 15;
    private RpmUtil rpmUtil;
    int btnStartColor, btnStopColor;

    Timer timer;
    TimerTask timerTask;
    final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        btnStartColor = ContextCompat.getColor(getBaseContext(), R.color.button_start);
        btnStopColor = ContextCompat.getColor(getBaseContext(), R.color.button_stop);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        name = preferences.getString("prf_username", "");
        allRpms.add(0L);
        saveMap();

        createUpdateUiHandler();
        mRecordingThread = new RecordingThread(new AudioDataReceivedListener() {
            @Override
            public void onAudioDataReceived(short[] data, long difference) {
                double rpm = (60 / (double) difference) * 1000;
                long[] value = {difference, (long) rpm};
                OverallStatistics overallStatistics = new OverallStatistics(org.joda.time.LocalDateTime.now(), (long) rpm);
                overall.add(overallStatistics);
                // Build message object.
                Message message = new Message();
                // Set message type.
                message.what = MESSAGE_UPDATE_TEXT_CHILD_THREAD;
                Bundle data1 = new Bundle();
                data1.putLongArray("data", value);
                message.setData(data1);
                // Send message to main thread Handler.
                updateUIHandler.sendMessage(message);
            }
        }, updateUIHandler);


        btnStartStop = findViewById(R.id.start_stop);
        welcomeUserMessage = MessageFormat.format("{0}{1}", getString(R.string.welcome_user_message), name);
        btnStartStop.setOnClickListener(v -> {
            if (!mRecordingThread.recording()) {
                isExistingUser();
//                if (exists) {
//                    startTraining(btnStopColor);
//                }
            } else {
                stop();
                mRecordingThread.stopRecording();
                textView.setText(welcomeUserMessage);
                btnStartStop.setBackgroundColor(btnStartColor);
                btnStartStop.setText(R.string.start);
                stopChronometer();
                stopTimerTask();
            }
        });

        btnTrainings = findViewById(R.id.btnTrainings);
        speedometer = findViewById(R.id.speedView);
        speedometer.speedTo(50);
        textView = findViewById(R.id.textView2);
        textView.setVisibility(View.INVISIBLE);
        txtCompare = findViewById(R.id.txtCompare);
        chronometer = findViewById(R.id.chronometer);
        chronometer.setFormat("%s");
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.setVisibility(View.INVISIBLE);


        textView.setText(welcomeUserMessage);

        // REST client initialization
        AuthenticationInterceptor interceptor = new AuthenticationInterceptor();
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        jsonPlaceHolderApi = retrofit.create(JsonPlaceHolderApi.class);

        rpmUtil = new RpmUtil();
    }

    private void startTraining(int btnStopColor) {
        callSaveFirst();
        startAudioRecordingSafe();
        overall = new ArrayList<>();
        textView.setText(R.string.activity_started);
        btnStartStop.setBackgroundColor(btnStopColor);
        btnStartStop.setText(R.string.stop);
        startChronometer();
        startTimer();
    }

    @Override
    public void onRestart() {
        super.onRestart();
        name = preferences.getString("prf_username", "");
        textView.setText(MessageFormat.format("{0}{1}", getString(R.string.welcome_user_message), name));
    }

    protected void stop() {
        super.onStop();
        mRecordingThread.stopRecording();
//        filterExtremeValues();
        displayTrainingSummary();
    }

    /**
     * Help method for output the real time values
     *
     * @param text text
     */
    private void updateText(String text) {
        textView.setText(text);
    }

    /* Create Handler object in main thread. */
    private void createUpdateUiHandler() {
        if (updateUIHandler == null) {
            updateUIHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    // Means the message is sent from child thread.
                    if (msg.what == MESSAGE_UPDATE_TEXT_CHILD_THREAD) {
                        long[] timeRpm = (long[]) msg.getData().get("data");
                        // Update ui in main thread.
                        if (timeRpm == null) {
                            return;
                        }
                        BigDecimal timeFromLastRevolve = new BigDecimal(timeRpm[0] / 1000d);
                        timeFromLastRevolve = timeFromLastRevolve.setScale(2, BigDecimal.ROUND_HALF_UP);
//                        updateText(" time = " + timeFromLastRevolve + " rpm = " + timeRpm[1]);

                        if (timeRpm[1] > 15 && timeRpm[1] < 120) {
                            allRpms.add(timeRpm[1]);
                        }
                        updateText(String.valueOf(rpmAverage()));
                        speedometer.speedTo(timeRpm[1], 1000);
                    }
                }
            };
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startAudioRecordingSafe() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            mRecordingThread.startRecording();

        } else {
            requestMicrophonePermission();
        }
    }

    private void requestMicrophonePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.RECORD_AUDIO)) {
            // Show dialog explaining why we need record audio
            Snackbar.make(textView, "Microphone access is required in order to record audio",
                    Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                            android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
                }
            }).show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mRecordingThread.stopRecording();
        }
    }

    private ArrayList<OverallStatistics> filterExtremeValues() {
//        if (!overall.isEmpty() &&(overall.get(0).getRpm() > minRPM || overallStatistics.getRpm() < maxRPM)) {
//                overall.remove(0);
//            }
//        }
//        Iterator<OverallStatistics> iter = overall.iterator();
//        while (iter.hasNext()) {
//            OverallStatistics statistics = iter.next();
//            if (statistics.getRpm() > minRPM || statistics.getRpm() < maxRPM) {
//                iter.remove();
//            }
//        }
        CopyOnWriteArrayList<OverallStatistics> copyOnWriteArrayList = new CopyOnWriteArrayList(overall);

        for (OverallStatistics overallStatistics : copyOnWriteArrayList) {
            if (overallStatistics.getRpm() > minRPM || overallStatistics.getRpm() < maxRPM) {
                copyOnWriteArrayList.remove(overallStatistics);
            }
        }

        return new ArrayList<>(copyOnWriteArrayList);
    }

    public void displayTrainingSummary() {
        Intent intent = new Intent(this, DisplayGraphActivity.class);
        intent.putExtra(EXTRA_MESSAGE, filterExtremeValues());
        startActivity(intent);
    }

    public void trainings(View view) {
        Intent intent = new Intent(this, TrainingsActivity.class);
        startActivity(intent);
    }

    public void startChronometer() {
        if (!running) {
            chronometer.setVisibility(View.VISIBLE);
            chronometer.setBase(SystemClock.elapsedRealtime());
            chronometer.start();
            running = true;
            btnTrainings.setVisibility(View.INVISIBLE);
        }
    }

    public void stopChronometer() {
        if (running) {
            chronometer.stop();
            chronometer.setBase(SystemClock.elapsedRealtime());
            running = false;
            chronometer.setVisibility(View.INVISIBLE);
            textView.setText(welcomeUserMessage);
            btnTrainings.setVisibility(View.VISIBLE);
        }
    }

    private BigDecimal rpmAverage() {
        double total = 0;
        for (Long element : allRpms) {
            total += element;
        }
        BigDecimal averageRpm = BigDecimal.valueOf(total / allRpms.size());
        return averageRpm.setScale(1, BigDecimal.ROUND_HALF_UP);
    }

    private void callSaveFirst() {
        if (!exists) {
            Toast.makeText(MainActivity.this, "User " + name + " does not exists", Toast.LENGTH_LONG).show();
            return;
        }
        TrainingDto trainingDto = new TrainingDto();
        trainingDto.setPersonName(name);
        Call<Long> call = jsonPlaceHolderApi.createTraining(trainingDto);
        call.enqueue(new Callback<Long>() {
            @Override
            public void onResponse(Call<Long> call, Response<Long> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Code: " + response.code(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error calling Save Training: " + response.message());
                    return;
                }
                Log.i(TAG, "Saving training with ID : " + response.body());
                Toast.makeText(MainActivity.this, "Training Saved", Toast.LENGTH_LONG).show();
                id = response.body();
            }

            @Override
            public void onFailure(Call<Long> call, Throwable t) {
                Toast.makeText(MainActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void isExistingUser() {
        Call<PersonDto> call = jsonPlaceHolderApi.getPerson(name);
        call.enqueue(new Callback<PersonDto>() {
            @Override
            public void onResponse(Call<PersonDto> call, Response<PersonDto> response) {
                if (!response.isSuccessful()) {
                    try {
                        Log.e(TAG, "Error calling Get Person: " + Objects.requireNonNull(response.errorBody()).string());
                        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivity(intent);
                    } catch (IOException e) {
                        Log.e(TAG, "IOException: " + e);
                    }
                    return;
                }
                Log.i(TAG, "Person exists " + response.message());
                exists = response.body() != null;
                startTraining(btnStopColor);
            }

            @Override
            public void onFailure(Call<PersonDto> call, Throwable t) {
                Toast.makeText(MainActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public void startTimer() {
        timer = new Timer();
        initializeTimerTask();
        //schedule the timer, after the first 60s the TimerTask will run every 60s

//        timer.schedule(timerTask, firstDelay, repeatPeriod); //
        timer.schedule(timerTask, 10_000, 10_000); //
    }

    public void stopTimerTask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                handler.post(() -> {
                    TrainingDto trainingDto = rpmUtil.convertToTrainingDto(filterExtremeValues(), MainActivity.this);
                    rpmUtil.saveTraining(TAG, jsonPlaceHolderApi, trainingDto, MainActivity.this, false);
                    Toast.makeText(getApplicationContext(), "Saving...", Toast.LENGTH_SHORT).show();
                    int actualRpmSum = rpmUtil.sumRpm(overall);
                    int elapsedSeconds = (int) (SystemClock.elapsedRealtime() - chronometer.getBase()) / 1000;
                    Integer recordValue = getRecordValue(elapsedSeconds);
                    txtCompare.setText(String.valueOf(actualRpmSum));
                    if (actualRpmSum > recordValue) {
                        txtCompare.setTextColor(btnStartColor);
                    } else {
                        txtCompare.setTextColor(btnStopColor);
                    }
                });
            }
        };
    }

    private void saveMap() {
        Map<Integer, Integer> record = new HashMap<>();
        record.put(10, 500);
        record.put(20, 1000);
        record.put(30, 1500);
        record.put(40, 2000);
        record.put(50, 2500);
        record.put(60, 3000);

        String converted = new Gson().toJson(record);

        preferences.edit().putString("REFERENCED_TRAINING", converted).apply();
    }


    private Integer getRecordValue(Integer second) {
        String jano = preferences.getString("REFERENCED_TRAINING", "");
        Gson gson = new Gson();
        Type type = new TypeToken<Map<Integer, Integer>>() {
        }.getType();
        Map<Integer, Integer> map = gson.fromJson(jano, type);
        Integer sumOfRpm = map.get(second);
        if (sumOfRpm == null) {
            Log.e(TAG, "No reference sum was found for second: " + second);
            return 0;
        }
        return sumOfRpm;
    }
}
