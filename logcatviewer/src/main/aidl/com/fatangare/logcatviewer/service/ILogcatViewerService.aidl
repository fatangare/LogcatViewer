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

/**
 * AIDL for LogcatViewerService service.
 */
interface ILogcatViewerService {

	/**
         * Change logcat source buffer
         * @param buffer it can be 'main', 'radio', 'events' Check {@link com.fatangare.logcatviewer.utils.Constants} class.
         */
        void changeLogcatSource(String buffer);

        /**
         * Restart {@link com.fatangare.logcatviewer.service.LogcatViewerService} service.
         */
        void restart();

        /**
         * Stop {@link com.fatangare.logcatviewer.service.LogcatViewerService} service.
         */
        void stop();

        /**
         * Start saving logcat logs to given file for given filter-text.
         * File is stored in android.os.Environment.DIRECTORY_DOWNLOADS+ "/LogcatViewer/"+ getPackageName() directory.
         * Logs are saved to file after every {@link com.fatangare.logcatviewer.service.LogcatViewerService#LOG_SAVING_INTERVAL}.
         * @param logFilename file to which logs are saved.
         * @param filterText text by which logs should be filtered. It can be tag, package or some text.
         */
        void startRecording(String logFilename, String filterText);

        /**
         * Stop saving logcat logs.
         */
        void stopRecording();

        /**
         * Is 'saving logcat logs to file' active?
         * @return true if yes else false.
         */
        boolean isRecording();

        /**
         * Stop listening to logcat logs.
         */
        void pause();

        /**
         * Resume listening to logcat logs.
         */
        void resume();
}
