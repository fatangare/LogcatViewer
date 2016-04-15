# LogcatViewer
Android Logcat Viewer

LogcatViewer is utility library which will allow user to view the logcat logs of the application on phone itself. 
It will be useful for tester to provide logs along with defects and also to developer to do initial analysis of defects.
Moreever, if correct tags are used, it can also be useful for performance measurement, for monitoring network requests etc. etc.

How to plug LogcatViewer in your application?
Step 1: Add following code to launch LogcatViewer floating view.
LogcatViewer.showLogcatLoggerView(this);
Step 2: Add following services to AndroidManifest.xml
 <service android:name="com.fatangare.logcatviewer.service.LogcatViewerService"
            android:label="LogcatLoggerService"></service>
        <service android:name="com.fatangare.logcatviewer.service.LogcatViewerFloatingView"
            android:label="LogcatLoggerFloatingView" > </service>
            
That's all!

It will launch floating window which will show logcat logs of the application. 
Since it is floating window, user can play around with the application while viewing logcat logs.

Operations which can be done in LogcatViewer:
1. Pause - Pause listening to logcat logs
2. Resume - Resume listening to logcat logs
3. Start Recording - Start storing logcat logs in file. File is stored in android.os.Environment.DIRECTORY_DOWNLOADS+ "/LogcatViewer/"+ getPackageName() directory.
4. Stop Recording - Stop storing logcat logs in file.
5. Enter filter text - Filter logs by filter text. It can be tag, package name or some text.
6. Select log level - Filter logs by log level.
7. Reset - Show all logcat logs.            