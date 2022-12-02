/**
 * Copyright (C) 2016  Sandeep Fatangare <sandeep@fatangare.info>
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.fatangare.logcatviewer.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.fatangare.logcatviewer.utils.Constants;
import com.fatangare.logcatviewer.service.ILogcatViewerCallback;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Vector;


/**
 * Service to listen logcat logs.
 */
public class LogcatViewerService extends Service {
    private static final String LOG_TAG = "LogcatViewerService";


    private static Handler mHandler;
    private ILogcatViewerCallback mCallback;

    /**
     * Logcat source buffer.
     */
    private String mLogcatSource = Constants.LOGCAT_SOURCE_BUFFER_MAIN;

    /**
     * Filter to apply for logcat.
     */
    private String[] mLogcatFilter = null;

    /**
     * Message filter to apply for logcat.
     */
    private String mLogcatMessageFilter = null;

    //Saving logs to file
    /**
     * File to which logs to be saved.
     */
    private String mRecordingFilename;

    /**
     * Logs are saved to file after every {@link LogcatViewerService#LOG_SAVING_INTERVAL} interval.
     * Hence log data should be saved for {@link LogcatViewerService#LOG_SAVING_INTERVAL} interval.
     * It should be cleared when data is saved to file.
     */
    private Vector<String> mRecordingData;

    /**
     * Log entries saved in log file.
     */
    private int mRecordedLogEntriesCount;

    //Threads
    private volatile boolean mShouldLogcatRunnableBeKilled = false;
    private volatile boolean mIsLogcatRunnableRunning = false;

    Process mProcess = null;

    //Status
    /**
     * Is recording logs active?
     */
    private boolean mIsRecording = false;

    /**
     * Is listening to logcat paused?
     */
    private boolean mIsPaused = false;

    private String mFilterText;

    /**
     * Interval after which logs are saved to file.
     */
    private static final int LOG_SAVING_INTERVAL = 5000; //5s



    // Handler Messages

    /**
     * Message indicating that executing logcat command is failed.
     */
    public static final int MSG_LOGCAT_RUN_FAILURE = 1;

    /**
     * Message indicating that reading logs for logcat is failed.
     */
    public static final int MSG_LOGCAT_READ_FAILURE = 2;

    /**
     * Message indicating that new log entry from logcat is received.
     */
    public static final int MSG_NEW_LOG_ENTRY = 3;


    private Runnable mLogcatRunnable = new Runnable() {
        @Override
        public void run() {
            mIsLogcatRunnableRunning = true;
            //Run logcat subscriber to subscribe for logcat log entries
            runLogcatSubscriber();
            //If reached here, it means thread is killed.
            mIsLogcatRunnableRunning = false;
            mProcess = null;
            return;
        }
    };

    private Runnable mRecordLogEntryRunnable = new Runnable() {
        @Override
        public void run() {
            //save log entries
            recordLogData();
            //wait for LOG_SAVING_INTERVAL before next 'record' operation.
            if(mHandler != null) mHandler.postDelayed(mRecordLogEntryRunnable, LOG_SAVING_INTERVAL);
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int i = super.onStartCommand(intent, flags, startId);
        Log.i(LOG_TAG, "onStartCommand:service is started.");
        return i;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        //Kill mLogcatRunnable thread.
        requestToKillLogcatRunnableThread();

        //Till thread is not killed, wait
        while (mIsLogcatRunnableRunning) {
            Log.d(LOG_TAG, "onUnbind:Waiting to kill LogcatRunnable thread");
        }

        //Stop LogcatViewerService service.
        stopSelf();
        return false;
    }

    /**
     * Handler to sendMessage messages from service to View.
     * @param handler handler from view
     */
    public static void setHandler(Handler handler) {
        mHandler = handler;
    }

    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Subscribe logcat to listen logcat log entries.
     */
    private void runLogcatSubscriber() {

        //Execute logcat system command
        try {
            ArrayList<String> args = new ArrayList<String>();
            args.add("/system/bin/logcat");
            if(mLogcatSource != null) {
                args.add("-b");
                args.add(mLogcatSource);
            }
            if(mLogcatMessageFilter != null) {
                args.add("-e");
                args.add(mLogcatMessageFilter);
            }
            if(mLogcatFilter != null) {
                args.addAll(Arrays.asList(mLogcatFilter));
            }
            String[] cmdarray = new String[args.size()];
            mProcess = Runtime.getRuntime().exec(args.toArray(cmdarray));
        } catch (IOException e) {
            sendMessage(MSG_LOGCAT_RUN_FAILURE);
        }

        //Read logcat log entries
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));

            String logEntry;

            //Till request to kill thread is not received, keep reading log entries
            while (!shouldLogcatRunnableBeKilled()) {
                //if paused, ignore entries.
                if (mIsPaused) {
                    continue;
                }

                //Read log entry.
                logEntry = reader.readLine();

                if (logEntry == null) {
                    continue;
                }

                //Send log entry to view.
                sendLogEntry(logEntry);

                //If recording is on, save log entries in mRecordingData in order to save them
                // after every LOG_SAVING_INTERVAL interval
                if (mIsRecording) {
                    if(TextUtils.isEmpty(mFilterText) ||
                            (!TextUtils.isEmpty(mFilterText) && logEntry.toLowerCase().contains(mFilterText.toLowerCase()))) {
                        mRecordingData.add(logEntry);
                    }
                }
            }

            Log.d(LOG_TAG, "Preparing to terminate LogcatRunnable thread");
            //If recording is on, save log entries and reset recording related fields.
            if (mIsRecording) {
                recordLogData();

                if(mHandler != null) mHandler.removeCallbacks(mRecordLogEntryRunnable);
                mIsRecording = false;
                mRecordingData.removeAllElements();
                mRecordingFilename = null;

            }

            //Release resources
            reader.close();
            mProcess.destroy();

        } catch (IOException e) {
            //Fail to read logcat log entries
            sendMessage(MSG_LOGCAT_READ_FAILURE);
        }

        Log.d(LOG_TAG, "Terminating LogcatRunnable thread");
        return;
    }

    /**
     * Make request to kill LogcatRunnable thread.
     */
    private synchronized void requestToKillLogcatRunnableThread() {
        mShouldLogcatRunnableBeKilled = true;
        if(mProcess != null) mProcess.destroy();
    }

    /**
     * Check if request to kill LogcatRunnable thread is made.
     * @return true if request is made else false.
     */
    private synchronized boolean shouldLogcatRunnableBeKilled() {
        return mShouldLogcatRunnableBeKilled;
    }

    /**
     * Send handler messages to view - communication from service to view.
     * @param msg message constant - starting with MSG_
     */
    private void sendMessage(int msg) {
        if(mHandler != null) {
            Message.obtain(mHandler, msg, "error").sendToTarget();
        }
        if(mCallback != null) {
            try {
                mCallback.sendMessage(msg);
            } catch(RemoteException e) {
                Log.d(LOG_TAG, "sendMessage() failed: " + e.toString());
            }
        }
    }

    /**
     * Send logcat log entry to view.
     * @param logEntry log entry.
     */
    private void sendLogEntry(String logEntry) {
        if(mHandler != null) {
            Message.obtain(mHandler, MSG_NEW_LOG_ENTRY, logEntry).sendToTarget();
        }
        if(mCallback != null) {
            try {
                mCallback.sendLogEntry(logEntry);
            } catch(RemoteException e) {
                Log.d(LOG_TAG, "sendLogEntry() failed: " + e.toString());
            }
        }
    }

    /**
     * Save log data to file
     */
    private void recordLogData() {
        try {
            int size = mRecordingData.size();
            //no entry to save, so return
            if(size == 0){
                return;
            }

            //Since logcat keeps adding logentries to mRecordingData, keep it in local field.
            Vector<String> recordingData = new Vector<>(mRecordingData);

            //Get log directory.
            File logDir = Constants.getRecordDir(this);
            logDir.mkdirs();

            //Get log file.
            File logFile = new File(logDir, mRecordingFilename);

            //Get writer to write in log file. Enable 'Append' mode.
            FileWriter logFileWriter = new FileWriter(logFile, true);

            //Write to log file.
            for (int i = 0; i < size; i++) {
                logFileWriter.append(recordingData.elementAt(i) + "\n");
                //Once saved, delete it from mRecordingData.
                mRecordingData.removeElementAt(0);
            }

            //Release resources.
            recordingData.removeAllElements();
            logFileWriter.close();

        } catch (Exception e) {
            Log.e(LOG_TAG, "recordLogData:Error writing the log to file. Exception: " + e.toString());
        }
    }


    //AIDL to communicate from view to service.
    private final ILogcatViewerService.Stub mBinder = new ILogcatViewerService.Stub() {

        public void changeLogcatSource(String logcatSource) {
            mLogcatSource = logcatSource;
            restart();
        }

        public void changeLogcatFilter(String[] filter_spec) {
            mLogcatFilter = filter_spec.clone();
            restart();
        }

        public void changeLogcatMessageFilter(String expression) {
            mLogcatMessageFilter = expression;
            restart();
        }

        public void setCallback(ILogcatViewerCallback callback) {
            mCallback = callback;
        }

        public void restart() {
            //request to kill thread
            requestToKillLogcatRunnableThread();

            //Till thread is not killed, wait
            while (mIsLogcatRunnableRunning) {
                Log.d(LOG_TAG, "restart:Waiting to kill LogcatRunnable thread");
            }

            //since request to kill is completed, set mShouldLogcatRunnableBeKilled to false
            mShouldLogcatRunnableBeKilled = false;

            //Start new LogcatRunnable thread.
            Thread thr = new Thread(mLogcatRunnable);
            thr.start();
        }

        public void stop() {
            Log.d(LOG_TAG, "stop:request to stop LogcatViewerService service is made.");
            //Kill mLogcatRunnable thread.
            requestToKillLogcatRunnableThread();

            //Till thread is not killed, wait
            while (mIsLogcatRunnableRunning) {
                Log.d(LOG_TAG, "stop:Waiting to kill LogcatRunnable thread");
            }

            //Stop LogcatViewerService service.
            stopSelf();
        }

        public void startRecording(String recordingFilename, String filterText) {
            mRecordingData = new Vector<>();
            mIsRecording = true;
            mRecordingFilename = recordingFilename;
            mFilterText = filterText;
            if(mHandler != null) mHandler.postDelayed(mRecordLogEntryRunnable, LOG_SAVING_INTERVAL);
        }

        public void stopRecording() {
            if(mHandler != null) mHandler.removeCallbacks(mRecordLogEntryRunnable);
            mIsRecording = false;
            recordLogData();
            mRecordingData.removeAllElements();
            mRecordingFilename = null;
        }

        public boolean isRecording() {
            return mIsRecording;
        }

        public void pause() {
            mIsPaused = true;
        }

        public void resume() {
            mIsPaused = false;
        }
    };
}
