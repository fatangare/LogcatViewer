package com.fatangare.logcatviewer.service;

/**
 * AIDL for LogcatViewerService callback.
 */
interface ILogcatViewerCallback {
    /**
     * Message indicating that executing logcat command is failed.
     */
    const int MSG_LOGCAT_RUN_FAILURE = 1;

    /**
     * Message indicating that reading logs for logcat is failed.
     */
    const int MSG_LOGCAT_READ_FAILURE = 2;

    /**
     * Used by LogcatViewerService to send status information
     * @param buffer either MSG_LOGCAT_RUN_FAILURE or MSG_LOGCAT_READ_FAILURE
     */
    void sendMessage(int msg);

    /**
     * Used by LogcatViewerService to send a logcat message
     * @param logEntry log entry from logcat
     */
     void sendLogEntry(String logEntry);
}
