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

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

public class RecordingThread {
    private static final String LOG_TAG = RecordingThread.class.getSimpleName();
    private static final int SAMPLE_RATE = 16000;
    private boolean mShouldContinue;
    private AudioDataReceivedListener mListener;
    private Thread mThread;
    private long previousTimeMillis;
    private Handler mUpdateUIHandler;

    // Message type code.
    private final static int MESSAGE_UPDATE_TEXT_CHILD_THREAD = 1;
    private TextView textView;

    public RecordingThread(AudioDataReceivedListener listener, Handler updateUIHandler) {
        mListener = listener;
        mUpdateUIHandler = updateUIHandler;
    }

    public boolean recording() {
        return mThread != null;
    }

    public void startRecording() {
        if (mThread != null)
            return;

        mShouldContinue = true;
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                record();

                // Build message object.
                Message message = new Message();
                // Set message type.
                message.what = MESSAGE_UPDATE_TEXT_CHILD_THREAD;
                // Send message to main thread Handler.
                mUpdateUIHandler.sendMessage(message);

            }

        });
        mThread.start();
    }

    public void stopRecording() {
        if (mThread == null)
            return;

        mShouldContinue = false;
        mThread = null;
    }

    private void record() {
        Log.v(LOG_TAG, "Start");
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

        // buffer size in bytes
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }

        short[] audioBuffer = new short[bufferSize / 2];

        AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!");
            return;
        }
        record.startRecording();

        Log.v(LOG_TAG, "Start recording");

        long shortsRead = 0;
        while (mShouldContinue) {
            int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);
            shortsRead += numberOfShort;
            long currentTimeMillis = System.currentTimeMillis();
            short max = getMax(audioBuffer);
//            Log.v("janoo", String.format("max : %d, timestamp : %d ", max, currentTimeMillis));
            if (max > 8000 && (currentTimeMillis - previousTimeMillis) > 300) {
                long difference = currentTimeMillis - previousTimeMillis;
                Log.v("janoo", String.format("Ubehlo %d sekund od posledneho beepu", difference));

                previousTimeMillis = currentTimeMillis;
                // Notify waveform
                mListener.onAudioDataReceived(audioBuffer, difference);
            }

        }

        record.stop();
        record.release();

        Log.v(LOG_TAG, String.format("Recording stopped. Samples read: %d", shortsRead));
    }

    // Method for getting the maximum value
    public static short getMax(short[] inputArray) {
        short maxValue = inputArray[0];
        for (int i = 1; i < inputArray.length; i++) {
            if (inputArray[i] > maxValue) {
                maxValue = inputArray[i];
            }
        }
        return maxValue;
    }
}
