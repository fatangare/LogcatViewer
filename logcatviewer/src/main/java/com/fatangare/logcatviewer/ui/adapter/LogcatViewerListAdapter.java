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

package com.fatangare.logcatviewer.ui.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.fatangare.logcatviewer.R;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This is the list adapter for the logcat log entries.
 */
public class LogcatViewerListAdapter extends BaseAdapter {
    private LayoutInflater mInflater;

    //Logcat log entries

    /**
     * Actual log entries.
     */
    private ArrayList<String> mLogcatData;

    /**
     * Filtered log entries - filtered by priority-level and filter-text.
     */
    private ArrayList<String> mFilteredLogcatData;

    /**
     * Current priority level. It can be any of {@link #mPriorityLevels} values.
     * Empty level means verbose by default.
     */
    private String mLogPriorityLevel = "";

    /**
     * Current filter text. It can be tag, package name or some text.
     */
    private String mLogFilterText = "";

    /**
     * Color map to add text-color to log-entry according to its priority level.
     */
    private HashMap<String, Integer> mPriorityLevelColorMap;

    /**
     * Priority levels for logcat log-entry.
     */
    final String[] mPriorityLevels = {PRIORITY_LEVEL_VERBOSE, PRIORITY_LEVEL_DEBUG,
            PRIORITY_LEVEL_INFO, PRIORITY_LEVEL_WARNING, PRIORITY_LEVEL_ERROR};

    /**
     * Verbose priority level.
     */
    public static final String PRIORITY_LEVEL_VERBOSE = " V ";
    /**
     * Debug priority level.
     */
    public static final String PRIORITY_LEVEL_DEBUG = " D ";
    /**
     * Info priority level.
     */
    public static final String PRIORITY_LEVEL_INFO = " I ";
    /**
     * Warning priority level.
     */
    public static final String PRIORITY_LEVEL_WARNING = " W ";
    /**
     * Error priority level.
     */
    public static final String PRIORITY_LEVEL_ERROR = " E ";

    /**
     * Constructor
     *
     * @param context
     */
    public LogcatViewerListAdapter(Context context) {
        mLogcatData = new ArrayList<String>();
        mFilteredLogcatData = new ArrayList<>();

        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mPriorityLevelColorMap = new HashMap<>();
        mPriorityLevelColorMap.put(PRIORITY_LEVEL_VERBOSE, 0xffcccccc);
        mPriorityLevelColorMap.put(PRIORITY_LEVEL_DEBUG, 0xff0f9d58);
        mPriorityLevelColorMap.put(PRIORITY_LEVEL_INFO, 0xff4285f4);
        mPriorityLevelColorMap.put(PRIORITY_LEVEL_WARNING, 0xffff9800);
        mPriorityLevelColorMap.put(PRIORITY_LEVEL_ERROR, 0xffb93221);
    }

    /**
     * Set current filter text.
     *
     * @param logFilterText filter text.
     */
    public void setLogFilterText(String logFilterText) {
        //If new and old values are same then return.
        if (mLogFilterText.equals(logFilterText)) {
            return;
        }

        //Set current filter text.
        mLogFilterText = logFilterText;
        //Filter log entries based on filter-text.
        filterLogcatData();
    }

    /**
     * Set current priority level.
     *
     * @param logPriorityLevel priority level
     */
    public void setLogPriorityLevel(String logPriorityLevel) {
        //If new and old values are same then return.
        if (mLogPriorityLevel.equals(logPriorityLevel)) {
            return;
        }

        //Set current priority level.
        mLogPriorityLevel = logPriorityLevel;
        //Filter log-entries based on priority level.
        filterLogcatData();
    }

    /**
     * Return current filter text.
     * @return current filter text.
     */
    public String getLogFilterText(){
        return mLogFilterText;
    }

    /**
     * Return current priority level.
     * @return current priority level.
     */
    public String getLogPriorityLevel(){
        return mLogPriorityLevel;
    }

    @Override
    public int getCount() {
        return mFilteredLogcatData.size();
    }

    @Override
    public long getItemId(int pos) {
        return pos;
    }

    @Override
    public Object getItem(int pos) {
        return mFilteredLogcatData.get(pos);
    }

    @Override
    public boolean isEmpty() {
        return mFilteredLogcatData.size() == 0;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        TextView holder;
        String logcatEntry = mFilteredLogcatData.get(pos);

        if (convertView == null) {
            //inflate the view here because there's no existing view object.
            convertView = mInflater.inflate(R.layout.logentry_listitem, parent, false);

            holder = (TextView) convertView.findViewById(R.id.logEntry);
            holder.setTypeface(Typeface.MONOSPACE);

            convertView.setTag(holder);
        } else { //reuse existing view
            holder = (TextView) convertView.getTag();
        }

        holder.setText(logcatEntry);
        holder.setTextColor(getTextColorForLogcatEntry(logcatEntry));

        final boolean autoscroll =
                (parent.getScrollY() + parent.getHeight() >= parent.getBottom());

        if (autoscroll) {
            ((ListView) parent).setSelection(mFilteredLogcatData.size() - 1);
        }

        return convertView;
    }

    /**
     * add new log-entry to list.
     * @param logEnry new log-entry
     */
    public void addLogEntry(String logEnry) {
        mLogcatData.add(logEnry);
        addFilterLogcatEntry(logEnry);
        notifyDataSetChanged();
    }

    /**
     * Reset log-entries.
     * Set filter text to empty and priority level to verbose and show all log-entries.
     */
    public void reset() {
        mLogFilterText = "";
        mLogPriorityLevel = "";
        mFilteredLogcatData.clear();
        mFilteredLogcatData.addAll(mLogcatData);
        notifyDataSetChanged();
    }

    /**
     * Filter log-entries based on {@link #mLogFilterText} and {@link #mLogPriorityLevel}.
     */
    private void filterLogcatData() {
        mFilteredLogcatData.clear();

        //If filter-text is empty and priority level is empty then filtering is not required.
        if (TextUtils.isEmpty(mLogPriorityLevel) && TextUtils.isEmpty(mLogFilterText)) {
            mFilteredLogcatData = new ArrayList<>(mLogcatData);
        }

        //Filter each log-entry
        for (String logcatEntry : mLogcatData) {
            if ((TextUtils.isEmpty(mLogPriorityLevel) || (!TextUtils.isEmpty(mLogPriorityLevel) && priorityLevelConditionForFiltering(logcatEntry))) &&
                    (TextUtils.isEmpty(mLogFilterText) || (!TextUtils.isEmpty(mLogFilterText) &&
                            logcatEntry.toLowerCase().contains(mLogFilterText.toLowerCase())))) {
                mFilteredLogcatData.add(logcatEntry);
            }
        }
    }

    /**
     * Filter log-entry based on priority levels.
     * All log-entries for current priority level and below it should be shown.
     * e.g. If {@link #PRIORITY_LEVEL_WARNING} is set, log-entries for warning and error priority levels should be shown.
     * @param logcatEntry log-entry
     * @return true if log-entry is in given priority level else false.
     */
    private boolean priorityLevelConditionForFiltering(String logcatEntry) {
        int size = mPriorityLevels.length;
        for (int i = 0; i < size; i++) {
            //if current priority level is found
            if (mPriorityLevels[i].equals(mLogPriorityLevel)) {
                //Check if either current priority level or below it in hierarchy is part of given log-entry.
                for (int j = i; j < size; j++) {
                    if (logcatEntry.contains(mPriorityLevels[j])) {
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }

    /**
     * First filter new log-entry and if it is filtered, add it to list.
     * @param logcatEntry
     */
    private void addFilterLogcatEntry(String logcatEntry) {
        if ((TextUtils.isEmpty(mLogPriorityLevel) || (!TextUtils.isEmpty(mLogPriorityLevel) && priorityLevelConditionForFiltering(logcatEntry))) &&
                (TextUtils.isEmpty(mLogFilterText) || (!TextUtils.isEmpty(mLogFilterText) &&
                        logcatEntry.toLowerCase().contains(mLogFilterText.toLowerCase())))) {
            mFilteredLogcatData.add(logcatEntry);
        }
    }

    /**
     * Get text-color for log-entry based on its priority level.
     * @param logcatEntry log entry.
     * @return text-color for log entry.
     */
    private int getTextColorForLogcatEntry(String logcatEntry) {
        String priorityLevel = PRIORITY_LEVEL_VERBOSE; //default:verbose

        //get priority level of log-entry.
        for (String key : mPriorityLevelColorMap.keySet()) {
            String key1 = key.trim() + "/";
            if (logcatEntry.contains(key) || logcatEntry.contains(key1)) {
                priorityLevel = key;
                break;
            }
        }

        //return text-color
        return mPriorityLevelColorMap.get(priorityLevel);
    }
}

