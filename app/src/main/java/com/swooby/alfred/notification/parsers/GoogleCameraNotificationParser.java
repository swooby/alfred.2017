package com.swooby.alfred.notification.parsers;

import android.service.notification.StatusBarNotification;

import com.swooby.alfred.MainApplication;

public class GoogleCameraNotificationParser
        extends AbstractNotificationParser
{
    public GoogleCameraNotificationParser(MainApplication application)
    {
        super(application, "com.google.android.GoogleCamera", "Google Camera");
    }

    @Override
    public NotificationParseResult onNotificationPosted(StatusBarNotification sbn)
    {
        // TODO:(pv) This one is annoying whenever a photo is taken!
        // TODO:(pv) Make this same something like "Google Camera, Picture Taken"...ONCE PER PICTURE
/*
05-21 15:53:03.671 3388-3400/com.swooby.alfred V/AbstractNot…ationParser: T3400 onNotificationPosted: packageName="com.google.android.GoogleCamera"
05-21 15:53:03.678 3388-3400/com.swooby.alfred V/AbstractNot…ationParser: T3400 onNotificationPosted: packageAppSpokenName="Camera"
05-21 15:53:03.678 3388-3400/com.swooby.alfred V/AbstractNot…ationParser: T3400 onNotificationPosted: notification=Notification(pri=0 contentView=com.google.android.GoogleCamera/0x1090085 vibrate=null sound=null defaults=0x0 flags=0x62 color=0xff4285f4 vis=PRIVATE)
05-21 15:53:03.678 3388-3400/com.swooby.alfred V/AbstractNot…ationParser: T3400 onNotificationPosted: extras={"android.title"="Camera", "android.subText"=null, "android.showChronometer"=false, "android.icon"=2130837713, "android.text"=null, "android.progress"=0, "android.progressMax"=0, "android.rebuild.contentViewActionCount"=38, "android.showWhen"=true, "android.rebuild.applicationInfo"=ApplicationInfo{9125666 com.google.android.GoogleCamera}, "android.infoText"=null, "android.originatingUserId"=0, "android.progressIndeterminate"=false}
05-21 15:53:03.679 3388-3400/com.swooby.alfred V/AbstractNot…ationParser: T3400 onNotificationPosted: tickerText=null
05-21 15:53:03.682 3388-3400/com.swooby.alfred V/AbstractNot…ationParser: T3400 onNotificationPosted: ---- bigContentView ----
05-21 15:53:03.682 3388-3400/com.swooby.alfred V/AbstractNot…ationParser: T3400 walkView: view=null
05-21 15:53:03.682 3388-3400/com.swooby.alfred V/AbstractNot…ationParser: T3400 onNotificationPosted: ---- contentView ----
05-21 15:53:03.751 3388-3400/com.swooby.alfred V/AbstractNot…ationParser: T3400 walkView: view=android.widget.FrameLayout{5f17b5 V.E...... ......I. 0,0-0,0 #102036c android:id/status_bar_latest_event_content}
05-21 15:53:03.751 3388-3400/com.swooby.alfred V/AbstractNot…ationParser: T3400 walkView: view=android.widget.FrameLayout{cb46e4a V.E...... ......I. 0,0-0,0 #102036b android:id/icon_group}
05-21 15:53:03.751 3388-3400/com.swooby.alfred V/AbstractNot…ationParser: T3400 walkView: view=android.widget.ImageView{9a9b1bb V.ED..... ......I. 0,0-0,0 #1020006 android:id/icon}
05-21 15:53:03.751 3388-3400/com.swooby.alfred V/AbstractNot…ationParser: T3400 walkView: view=android.widget.ImageView{309d6d8 G.ED..... ......I. 0,0-0,0 #1020040 android:id/right_icon}
05-21 15:53:03.751 3388-3400/com.swooby.alfred V/AbstractNot…ationParser: T3400 walkView: view=android.widget.LinearLayout{db67a31 V.E...... ......I. 0,0-0,0 #102036d android:id/notification_main_column}
05-21 15:53:03.751 3388-3400/com.swooby.alfred V/AbstractNot…ationParser: T3400 walkView: view=android.widget.LinearLayout{2d32516 V.E...... ......I. 0,0-0,0 #102037e android:id/line1}
05-21 15:53:03.751 3388-3400/com.swooby.alfred V/AbstractNot…ationParser: T3400 walkView: view=android.widget.TextView{e37ae97 V.ED..... ......ID 0,0-0,0 #1020016 android:id/title}
05-21 15:53:03.751 3388-3400/com.swooby.alfred V/AbstractNot…ationParser: T3400 walkView: view=android.widget.DateTimeView{4b18884 V.ED..... ......ID 0,0-0,0 #102008c android:id/time}
05-21 15:53:03.752 3388-3400/com.swooby.alfred V/AbstractNot…ationParser: T3400 walkView: view=android.view.ViewStub{147886d G.E...... ......I. 0,0-0,0 #102037d android:id/chronometer}
05-21 15:53:03.752 3388-3400/com.swooby.alfred V/AbstractNot…ationParser: T3400 walkView: view=android.widget.LinearLayout{8547ca2 V.E...... ......I. 0,0-0,0}
05-21 15:53:03.752 3388-3400/com.swooby.alfred V/AbstractNot…ationParser: T3400 walkView: view=android.widget.TextView{13e9d33 G.ED..... ......I. 0,0-0,0 #1020015 android:id/text2}
05-21 15:53:03.752 3388-3400/com.swooby.alfred V/AbstractNot…ationParser: T3400 walkView: view=android.widget.ImageView{400a8f0 G.ED..... ......I. 0,0-0,0 #102037f android:id/profile_badge_line2}
05-21 15:53:03.752 3388-3400/com.swooby.alfred V/AbstractNot…ationParser: T3400 walkView: view=android.view.ViewStub{1613e69 G.E...... ......I. 0,0-0,0 #102000d android:id/progress}
05-21 15:53:03.752 3388-3400/com.swooby.alfred V/AbstractNot…ationParser: T3400 walkView: view=android.widget.LinearLayout{5a1c0ee G.E...... ......I. 0,0-0,0 #1020380 android:id/line3}
05-21 15:53:03.752 3388-3400/com.swooby.alfred V/AbstractNot…ationParser: T3400 walkView: view=android.widget.TextView{4bbd98f V.ED..... ......ID 0,0-0,0 #1020075 android:id/text}
05-21 15:53:03.753 3388-3400/com.swooby.alfred V/AbstractNot…ationParser: T3400 walkView: view=android.widget.TextView{3df641c G.ED..... ......ID 0,0-0,0 #1020381 android:id/info}
05-21 15:53:03.753 3388-3400/com.swooby.alfred V/AbstractNot…ationParser: T3400 walkView: view=android.widget.ImageView{465825 G.ED..... ......I. 0,0-0,0 #1020382 android:id/profile_badge_line3}
*/
        super.onNotificationPosted(sbn);

        return NotificationParseResult.ParsableIgnored;
    }
}
