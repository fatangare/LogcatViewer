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

package com.fatangare.logcatviewer.utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;

/**
 * It contains constants used in the project.
 */
public class Constants {

    //Logcat source buffer. Note that values are case-sensitive.
    public static final String LOGCAT_SOURCE_BUFFER_MAIN = "main";
    public static final String LOGCAT_SOURCE_BUFFER_RADIO = "radio";
    public static final String LOGCAT_SOURCE_BUFFER_EVENTS = "events";

    /**
     * Top Logcat logger directory to save log entries.
     */
    private static final String LOG_RECORD_DIR = "/LogcatViewer/";

    /**
     * Get directory where logs are saved.
     *
     * @param context application context.
     * @return directory File object.
     */
    public static File getRecordDir(Context context) {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + LOG_RECORD_DIR + context.getPackageName());
    }

}
