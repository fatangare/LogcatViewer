[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-LogcatViewer-green.svg?style=true)](https://android-arsenal.com/details/1/3463)  [![](https://jitpack.io/v/fatangare/LogcatViewer.svg)](https://jitpack.io/#fatangare/LogcatViewer)

# LogcatViewer - Android Logcat Viewer

## Purpose
LogcatViewer is utility library which will allow user to view the logcat logs of the application on phone itself.   
It will be useful for tester to provide logs along with defects and also to developer to do initial analysis of defects.  
Moreever, if correct tags are used, it can also be useful for performance measurement, for monitoring network requests etc. etc.  

#### How to plug LogcatViewer in your application?

1. Add following line to build.gradle.
```
repositories {
    maven {     
        url "https://jitpack.io"    
    }    
}  

dependencies { 
    compile 'com.github.fatangare.LogcatViewer:logcatviewer:aadf092447'
    }
```
2. Add following code to launch LogcatViewer floating view. - To launch logcatviewer floating view.
```java
LogcatViewer.showLogcatLoggerView(this);
```
3. Add following services to AndroidManifest.xml. - To register services.
```xml
<service android:name="com.fatangare.logcatviewer.service.LogcatViewerService"
            android:label="LogcatLoggerService"></service>
 <service android:name="com.fatangare.logcatviewer.service.LogcatViewerFloatingView"
            android:label="LogcatLoggerFloatingView" > </service>
 ```

*That's all!*  
  
It will launch floating window which will show logcat logs of the application.   
![mainview](https://github.com/fatangare/LogcatViewer/blob/master/Images/mainview.png)  
Since it is floating window, user can play around with the application while viewing logcat logs.  

#### Operations which can be done in LogcatViewer:
1. Pause - Pause listening to logcat logs
2. Resume - Resume listening to logcat logs
3. Start Recording - Start storing logcat logs in file. File is stored in android.os.Environment.DIRECTORY_DOWNLOADS+ "/LogcatViewer/"+ getPackageName() directory.
It take filter-text used before recording is started. Any change to filter-text during recording will not update filter-text used for recording.
To apply new filter-text for recording, new recording should be started.
4. Stop Recording - Stop storing logcat logs in file.
5. Enter filter text - Filter logs by filter text. It can be tag, package name or some text.
6. Select log level - Filter logs by log level.
7. Reset - Show all logcat logs.      

#### More screenshots
###### Enter Filter text       
![filter](https://github.com/fatangare/LogcatViewer/blob/master/Images/filter.png)   
###### Select log level
![loglevel](https://github.com/fatangare/LogcatViewer/blob/master/Images/loglevel.png)  
###### Notification
![loglevel](https://github.com/fatangare/LogcatViewer/blob/master/Images/notification.png)    
