/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

import com.github.anastr.speedviewlib.SpeedView;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "OverallStatistics";
    private RecordingThread mRecordingThread;
    private PlaybackThread mPlaybackThread;
    private static final int REQUEST_RECORD_AUDIO = 13;
    // This is the activity main thread Handler.
    private Handler updateUIHandler = null;
    private ArrayList<OverallStatistics> overall = new ArrayList<>();
    private JsonPlaceHolderApi jsonPlaceHolderApi;
    private static final String TAG = MainActivity.class.getSimpleName();
    public static final String BASE_URL = "https://rpmbackend.herokuapp.com/";  //https://rpmbackend.herokuapp.com/   //http://192.168.100.41:8081/

    // Message type code.
    private final static int MESSAGE_UPDATE_TEXT_CHILD_THREAD = 1;
    private TextView textView;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final int btnStartColor = ContextCompat.getColor(getBaseContext(), R.color.button_start);
        final int btnStopColor = ContextCompat.getColor(getBaseContext(), R.color.button_stop);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        name = preferences.getString("prf_username", "");

        // Initialize Handler.
        createUpdateUiHandler();
        mRecordingThread = new RecordingThread(new AudioDataReceivedListener() {
            @Override
            public void onAudioDataReceived(short[] data, long difference) {
                double rpm = (60 / (double) difference) * 1000;
                long value[] = {difference, (long) rpm};
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
                startAudioRecordingSafe();
                overall = new ArrayList<>();
                textView.setText(R.string.activity_started);
                btnStartStop.setBackgroundColor(btnStopColor);
                btnStartStop.setText(R.string.stop);
                startChronometer();
            } else {
                stop();
                mRecordingThread.stopRecording();
                textView.setText(welcomeUserMessage);
                btnStartStop.setBackgroundColor(btnStartColor);
                btnStartStop.setText(R.string.start);
                pauseChronometer();

            }
        });

        btnTrainings = findViewById(R.id.btnTrainings);
        speedometer = findViewById(R.id.speedView);
        speedometer.speedTo(50);
        textView = findViewById(R.id.textView2);
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
        filterExtremeValues();
        sendMessage();
    }

    /**
     * Help method for output real time values
     *
     * @param text
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

    private void filterExtremeValues() {
        overall.remove(1);
        Iterator<OverallStatistics> iter = overall.iterator();
        while (iter.hasNext()) {
            OverallStatistics statistics = iter.next();
            if (statistics.getRpm() > minRPM || statistics.getRpm() < maxRPM) {
                iter.remove();
            }
        }
    }

    public void sendMessage() {
        Intent intent = new Intent(this, DisplayGraphActivity.class);
        intent.putExtra(EXTRA_MESSAGE, overall);
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

    public void pauseChronometer() {
        if (running) {
            chronometer.stop();
            chronometer.setBase(SystemClock.elapsedRealtime());
            running = false;
            chronometer.setVisibility(View.INVISIBLE);
            textView.setText(welcomeUserMessage);
            btnTrainings.setVisibility(View.VISIBLE);
        }
    }

    private double rpmAverage() {
        double total = 0;
        for (Long element : allRpms) {
            total += element;
        }
        return total / allRpms.size();
    }

}
