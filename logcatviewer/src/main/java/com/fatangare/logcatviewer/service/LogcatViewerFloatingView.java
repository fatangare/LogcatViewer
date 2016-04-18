/**
 * Copyright (C) 2016  Sandeep Fatangare <sandeep@fatangare.info>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.fatangare.logcatviewer.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;

import com.fatangare.logcatviewer.R;
import com.fatangare.logcatviewer.ui.adapter.LogcatViewerListAdapter;

import wei.mark.standout.StandOutWindow;
import wei.mark.standout.constants.StandOutFlags;
import wei.mark.standout.ui.Window;

/**
 * Floating view to show logcat logs.
 */
public class LogcatViewerFloatingView extends StandOutWindow {
    private static final String LOG_TAG = "LogcatFloatingView";

    //Views
    private ListView mListView;
    private LogcatViewerListAdapter mAdapter;

    private LinearLayout mMenuOptionLayout;
    private LinearLayout mFilterLayout;
    private RadioGroup mPriorityLevelRadioGroup;

    //Service
    private ILogcatViewerService mLogcatViewerService;

    //Service connection
    private ServiceConnection mLogcatViewerServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mLogcatViewerService = ILogcatViewerService.Stub.asInterface(service);
            LogcatViewerService.setHandler(mHandler);

            try {
                mLogcatViewerService.restart();
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "Could not start LogcatViewerService service");
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.i(LOG_TAG, "onServiceDisconnected has been called");
            mLogcatViewerService = null;
        }
    };

    /**
     * Handle service to view communication.
     */
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LogcatViewerService.MSG_LOGCAT_READ_FAILURE:
                    Log.d(LOG_TAG, "Reading logs for logcat is failed.");
                    break;
                case LogcatViewerService.MSG_LOGCAT_RUN_FAILURE:
                    Log.d(LOG_TAG, "Executing logcat command is failed.");
                    break;
                case LogcatViewerService.MSG_NEW_LOG_ENTRY:
                    mAdapter.addLogEntry((String) msg.obj);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    @Override
    public String getAppName() {
        return getString(getApplicationInfo().labelRes);
    }

    @Override
    public int getAppIcon() {
        return getApplicationInfo().icon;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //bind service
        bindService(new Intent(this, LogcatViewerService.class), mLogcatViewerServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        //if recording, stop recording before unbinding service.
        try {
            if (mLogcatViewerService.isRecording()) {
                stopRecording();
            }
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "isRecording() failed");
        }

        unbindService(mLogcatViewerServiceConnection);
        super.onDestroy();
    }

    @Override
    public void createAndAttachView(int id, FrameLayout frame) {
        // create a new layout from body.xml
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final View rootView = inflater.inflate(R.layout.main, frame, true);

        //Main layout for showing views specific to menu-action e.g. filterLayout or PriorityLevelRadioGroup.
        mMenuOptionLayout = (LinearLayout) rootView.findViewById(R.id.menuOptionsLayout);
        //Layout for "Enter filter text"
        mFilterLayout = (LinearLayout) rootView.findViewById(R.id.filterLayout);
        //Radio group containing different priority levels.
        mPriorityLevelRadioGroup = (RadioGroup) rootView.findViewById(R.id.rgPriorityLevels);

        setupLogListView(rootView);
        setupBottomBarView(rootView);
        setupFilterTextView(rootView);
        setupPriorityLevelView();
    }

    // the window will be centered
    @Override
    public StandOutLayoutParams getParams(int id, Window window) {
        DisplayMetrics displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
        return new StandOutLayoutParams(id, displayMetrics.widthPixels * 5 / 6, displayMetrics.heightPixels / 2,
                StandOutLayoutParams.RIGHT, StandOutLayoutParams.BOTTOM, 400, 300);
    }

    // move the window by dragging the view
    @Override
    public int getFlags(int id) {
        return StandOutFlags.FLAG_DECORATION_SYSTEM
                | StandOutFlags.FLAG_BODY_MOVE_ENABLE
                | StandOutFlags.FLAG_WINDOW_HIDE_ENABLE
                | StandOutFlags.FLAG_WINDOW_BRING_TO_FRONT_ON_TAP
                | StandOutFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE
                | StandOutFlags.FLAG_WINDOW_PINCH_RESIZE_ENABLE;
    }

    @Override
    public String getPersistentNotificationMessage(int id) {
        return "Show the LogcatViewerFloatingView for "+getAppName()+ " application";
    }

    @Override
    public Intent getPersistentNotificationIntent(int id) {
        return StandOutWindow.getShowIntent(this, LogcatViewerFloatingView.class, id);
    }


    /**
     * Stop recording log-entries
     */
    private void stopRecording() {
        try {
            mLogcatViewerService.stopRecording();
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "StopRecording:Trouble writing the log to a file");
        }
    }

    /**
     * Pause listening to logcat logs.
     */
    private void pauseLogging() {
        try {
            mLogcatViewerService.pause();
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Pausing logcat failed");
        }
    }

    /**
     * Resume listening to logcat logs.
     */
    private void resumeLogging() {
        try {
            mLogcatViewerService.resume();
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Resuming logcat failed");
        }
    }

    /**
     * Hide all menu specific views.
     */
    private void resetMenuOptionLayout() {
        mFilterLayout.setVisibility(View.GONE);
        mPriorityLevelRadioGroup.setVisibility(View.GONE);
        mMenuOptionLayout.setVisibility(View.GONE);
    }

    /**
     * Setup list view to show logcat log-entries.
     * @param rootView root view.
     */
    private void setupLogListView(final View rootView){
        //Log entry list view
        mListView = (ListView) rootView.findViewById(R.id.list);
        mListView.setStackFromBottom(true);
        mListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        mAdapter = new LogcatViewerListAdapter(getApplicationContext());
        mListView.setAdapter(mAdapter);
    }

    /**
     * Setup bottombar view to show action buttons.
     * @param rootView root view.
     */
    private void setupBottomBarView(final View rootView){
        //Pause button
        rootView.findViewById(R.id.pause).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pauseLogging();
                view.setVisibility(View.GONE);
                rootView.findViewById(R.id.play).setVisibility(View.VISIBLE);
            }
        });

        //Play button
        rootView.findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resumeLogging();
                view.setVisibility(View.GONE);
                rootView.findViewById(R.id.pause).setVisibility(View.VISIBLE);
            }
        });

        //'Start Recording' button
        rootView.findViewById(R.id.record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setVisibility(View.GONE);
                rootView.findViewById(R.id.recordOn).setVisibility(View.VISIBLE);
                try {
                    String logFilename = "log_" + System.currentTimeMillis() + ".txt";
                    mLogcatViewerService.startRecording(logFilename, mAdapter.getLogFilterText());
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "StartRecording:Trouble writing the log to a file");
                }
            }
        });

        //'Stop Recording' button
        rootView.findViewById(R.id.recordOn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setVisibility(View.GONE);
                rootView.findViewById(R.id.record).setVisibility(View.VISIBLE);
                try {
                    mLogcatViewerService.stopRecording();
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "StopRecording:Trouble writing the log to a file");
                }
            }
        });

        //'Enter filter text' button
        rootView.findViewById(R.id.find).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mFilterLayout.getVisibility() == View.GONE) {
                    resetMenuOptionLayout();
                    mFilterLayout.setVisibility(View.VISIBLE);
                    mMenuOptionLayout.setVisibility(View.VISIBLE);
                } else {
                    resetMenuOptionLayout();
                }
            }
        });

        //'Select priority level' button
        rootView.findViewById(R.id.btnPriorityLevel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPriorityLevelRadioGroup.getVisibility() == View.GONE) {
                    resetMenuOptionLayout();
                    mPriorityLevelRadioGroup.setVisibility(View.VISIBLE);
                    mMenuOptionLayout.setVisibility(View.VISIBLE);
                } else {
                    resetMenuOptionLayout();
                }
            }
        });

        //'Reset log-entries' button
        rootView.findViewById(R.id.btnReset).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pauseLogging();
                mAdapter.reset();
                resumeLogging();
            }
        });
    }

    /**
     * Setup 'Enter filter text' layout.
     * @param rootView root view.
     */
    private void setupFilterTextView(final View rootView){
        rootView.findViewById(R.id.btnLogFilter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String filterText = ((EditText) rootView.findViewById(R.id.etLogFilter)).getText().toString().trim();
                resetMenuOptionLayout();

                pauseLogging();
                mAdapter.setLogFilterText(filterText);
                resumeLogging();
            }
        });
    }

    /**
     * Setup 'Select priority level' view
     */
    private  void setupPriorityLevelView(){
        mPriorityLevelRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                //Get priority level based on selection.
                String priorityLevel;
                if (checkedId == R.id.radioDebug) {
                    priorityLevel = LogcatViewerListAdapter.PRIORITY_LEVEL_DEBUG;

                } else if (checkedId == R.id.radioInfo) {
                    priorityLevel = LogcatViewerListAdapter.PRIORITY_LEVEL_INFO;

                } else if (checkedId == R.id.radioWarning) {
                    priorityLevel = LogcatViewerListAdapter.PRIORITY_LEVEL_WARNING;

                } else if (checkedId == R.id.radioError) {
                    priorityLevel = LogcatViewerListAdapter.PRIORITY_LEVEL_ERROR;

                } else {
                    priorityLevel = "";

                }
                //Set current priority level.
                mAdapter.setLogPriorityLevel(priorityLevel);
                //Hide all menu option layouts.
                resetMenuOptionLayout();
            }
        });
    }

}
