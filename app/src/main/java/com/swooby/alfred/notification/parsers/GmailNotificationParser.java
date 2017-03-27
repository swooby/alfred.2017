package com.swooby.alfred.notification.parsers;

import android.app.Notification;
import android.app.Notification.BigTextStyle;
import android.app.Notification.InboxStyle;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.style.TextAppearanceSpan;

import com.smartfoo.android.core.FooString;
import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.texttospeech.FooTextToSpeechBuilder;
import com.swooby.alfred.BuildConfig;

import java.util.Arrays;

public class GmailNotificationParser
        extends AbstractNotificationParser
{
    private static final String TAG = FooLog.TAG(GmailNotificationParser.class);

    public GmailNotificationParser(@NonNull NotificationParserCallbacks callbacks)
    {
        super(callbacks);//, "G mail");
    }

    @Override
    public String getPackageName()
    {
        return "com.google.android.gm";
    }

    @Override
    public NotificationParseResult onNotificationPosted(StatusBarNotification sbn)
    {
        FooLog.i(TAG, "---- #GMAIL ----");
        if (BuildConfig.DEBUG)
        {
            super.onNotificationPosted(sbn);
        }

        Notification notification = sbn.getNotification();
        if (notification == null)
        {
            FooLog.w(TAG, "onNotificationPosted: notification == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        Bundle extras = notification.extras;
        if (extras == null)
        {
            FooLog.w(TAG, "onNotificationPosted: extras == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }

/*
example1:
onNotificationPosted: notification=Notification(pri=0 contentView=com.google.android.gm/0x1090085 vibrate=null sound=null tick defaults=0x4 flags=0x19 color=0xffdb4437 category=email actions=2 vis=PRIVATE publicVersion=Notification(pri=0 contentView=com.google.android.gm/0x1090085 vibrate=null sound=null defaults=0x0 flags=0x0 color=0xffdb4437 category=email vis=PUBLIC))
onNotificationPosted: bigContentView
walkView: view=android.widget.FrameLayout{c4cb098 V.E...... ......I. 0,0-0,0 #102036c android:id/status_bar_latest_event_content}
walkView: view=android.widget.FrameLayout{805b0f1 V.E...... ......I. 0,0-0,0 #102036b android:id/icon_group}
walkView: view=android.widget.ImageView{1bbf4d6 V.ED..... ......I. 0,0-0,0 #1020006 android:id/icon}
walkView: view=android.widget.ImageView{524a357 G.ED..... ......I. 0,0-0,0 #1020040 android:id/right_icon}
walkView: view=android.widget.LinearLayout{6a83e44 V.E...... ......I. 0,0-0,0 #102036d android:id/notification_main_column}
walkView: view=android.widget.LinearLayout{5336b2d V.E...... ......I. 0,0-0,0 #102037e android:id/line1}
walkView: view=android.widget.TextView{35a8862 V.ED..... ......ID 0,0-0,0 #1020016 android:id/title}
walkView: view=android.view.ViewStub{e61df3 G.E...... ......I. 0,0-0,0 #102008c android:id/time}
walkView: view=android.view.ViewStub{c0cfab0 G.E...... ......I. 0,0-0,0 #102037d android:id/chronometer}
walkView: view=android.widget.LinearLayout{a088d29 V.E...... ......I. 0,0-0,0}
walkView: view=android.widget.TextView{75cc8ae G.ED..... ......I. 0,0-0,0 #1020015 android:id/text2}
walkView: view=android.widget.ImageView{c3aa64f G.ED..... ......I. 0,0-0,0 #102037f android:id/profile_badge_line2}
walkView: view=android.view.ViewStub{3bf11dc G.E...... ......I. 0,0-0,0 #102000d android:id/progress}
walkView: view=android.widget.LinearLayout{bf3d2e5 V.E...... ......I. 0,0-0,0}
walkView: view=android.widget.TextView{330c1ba G.ED..... ......I. 0,0-0,0 #102036e android:id/big_text}
walkView: view=android.widget.ImageView{6a5586b G.ED..... ......I. 0,0-0,0 #102036f android:id/profile_badge_large_template}
walkView: view=android.widget.ImageView{3196fc8 G.ED..... ......I. 0,0-0,0 #1020370 android:id/action_divider}
walkView: view=android.widget.LinearLayout{e93b861 G.E...... ......I. 0,0-0,0 #1020367 android:id/actions}
walkView: view=android.widget.ImageView{3193f86 V.ED..... ......I. 0,0-0,0 #1020373 android:id/overflow_divider}
walkView: view=android.widget.LinearLayout{abf1047 V.E...... ......I. 0,0-0,0 #1020380 android:id/line3}
walkView: view=android.widget.TextView{839c074 V.ED..... ......ID 0,0-0,0 #1020075 android:id/text}
walkView: view=android.widget.TextView{6f2799d V.ED..... ......ID 0,0-0,0 #1020381 android:id/info}
walkView: view=android.widget.ImageView{2b9ce12 G.ED..... ......I. 0,0-0,0 #1020382 android:id/profile_badge_line3}
onNotificationPosted: --------
onNotificationPosted: contentView
walkView: view=android.widget.FrameLayout{b58aa0c V.E...... ......I. 0,0-0,0 #102036c android:id/status_bar_latest_event_content}
walkView: view=android.widget.FrameLayout{9d83f55 V.E...... ......I. 0,0-0,0 #102036b android:id/icon_group}
walkView: view=android.widget.ImageView{51d0d6a V.ED..... ......I. 0,0-0,0 #1020006 android:id/icon}
walkView: view=android.widget.ImageView{171325b G.ED..... ......I. 0,0-0,0 #1020040 android:id/right_icon}
walkView: view=android.widget.LinearLayout{7ec5af8 V.E...... ......I. 0,0-0,0 #102036d android:id/notification_main_column}
walkView: view=android.widget.LinearLayout{72a7bd1 V.E...... ......I. 0,0-0,0 #102037e android:id/line1}
walkView: view=android.widget.TextView{3769636 V.ED..... ......ID 0,0-0,0 #1020016 android:id/title}
walkView: view=android.view.ViewStub{f069937 G.E...... ......I. 0,0-0,0 #102008c android:id/time}
walkView: view=android.view.ViewStub{82ea4 G.E...... ......I. 0,0-0,0 #102037d android:id/chronometer}
walkView: view=android.widget.LinearLayout{88a040d V.E...... ......I. 0,0-0,0}
walkView: view=android.widget.TextView{f0ddfc2 G.ED..... ......I. 0,0-0,0 #1020015 android:id/text2}
walkView: view=android.widget.ImageView{52191d3 G.ED..... ......I. 0,0-0,0 #102037f android:id/profile_badge_line2}
walkView: view=android.view.ViewStub{5eb9110 G.E...... ......I. 0,0-0,0 #102000d android:id/progress}
walkView: view=android.widget.LinearLayout{ddfd409 V.E...... ......I. 0,0-0,0 #1020380 android:id/line3}
walkView: view=android.widget.TextView{2de360e V.ED..... ......ID 0,0-0,0 #1020075 android:id/text}
walkView: view=android.widget.TextView{9e9782f V.ED..... ......ID 0,0-0,0 #1020381 android:id/info}
walkView: view=android.widget.ImageView{e60ae3c G.ED..... ......I. 0,0-0,0 #1020382 android:id/profile_badge_line3}
onNotificationPosted: extras={"android.title"="TheLadders", "android.subText"="pv@swooby.com", "android.template"="android.app.Notification$BigTextStyle", "android.showChronometer"=false, "android.icon"=2130837846, "android.text"=Your exclusive pass to apply to a job for FREE, "android.progress"=0, "android.progressMax"=0, "android.showWhen"=true, "android.rebuild.applicationInfo"=ApplicationInfo{2a8a7c5 com.google.android.gm}, "android.people"=[Ljava.lang.String;@c4ba51a, "android.largeIcon"=android.graphics.Bitmap@49f684b, "android.bigText"=Your exclusive pass to apply to a job for FREE
    Your complimentary job application. Apply now!
    Hi Paul,
    Your profile matches this job recently posted by Manjit Singh at Nityo Infotech. Apply now for FREE.
    Learn more:
    Manjit Singh
    Recruiter
    at Nityo Infotech
    Software Design Engineer (Power BI)
    Redmond, WA
    Manjit said:
    Role: Software Design Engineer           Location: Redmond, WA  Duration: Long-Term   Full-Time or W2 Contract  
    You were matched based on your profile information:
    Edit Profile
    JOB TITLE:
    Software Design Engineer (Power BI)
    LOCATION:
    Redmond, WA
    JOB FUNCTION:
    Technology
    COMPENSATION:
    $60K to $80K
    You are receiving this email from our secure server at TheLadders.com because you signed up on 2009-02-03 from pv@swooby.com with the zip code 98011.
    To stop receiving Pipeline emails, unsubscribe. Click here to manage the emails you receive from TheLadders.
    Found this in your junk or spam folder? Add jobs@SalesLadder.com to your safe sender list.
    © 2014 TheLadders | Privacy | Terms of Use | Affiliates | Contact | Employers | Site Map
    TheLadders.com, Inc. | 137 Varick St., 8th Floor | New York, NY 10013, "android.infoText"=null, "android.wearable.EXTENSIONS"={"background"=android.graphics.Bitmap@ea87228, "actions"=[android.app.Notification$Action@370fb41, android.app.Notification$Action@8eef8e6, android.app.Notification$Action@57a3e27]}, "android.originatingUserId"=0, "android.progressIndeterminate"=false}

example2:
packageName="com.google.android.gm"
packageAppSpokenName="Gmail"
notification=Notification(pri=0 contentView=null vibrate=null sound=content://settings/system/notification_sound tick defaults=0x4 flags=0x219 color=0xffdb4437 category=email groupKey=content://com.android.gmail.ui/account%3A1872741928/account/content://com.android.gmail.ui/account%3A1872741928/label/%5Esq_ig_i_personal vis=PRIVATE publicVersion=Notification(pri=0 contentView=null vibrate=null sound=null defaults=0x0 flags=0x200 color=0xffdb4437 category=email groupKey=content://com.android.gmail.ui/account%3A1872741928/account/content://com.android.gmail.ui/account%3A1872741928/label/%5Esq_ig_i_personal vis=PUBLIC))
extras={"android.title"="4 new messages",
        "android.textLines"=[Ljava.lang.CharSequence;@e2b02bc,
        "android.subText"="pv@swooby.com",
        "android.template"="android.app.Notification$InboxStyle",
        "android.showChronometer"=false,
        "android.icon"=2130837932,
        "android.text"=null,
        "android.progress"=0,
        "android.progressMax"=0,
        "android.appInfo"=ApplicationInfo{c8da245 com.google.android.gm},
        "android.showWhen"=true,
        "android.people"=[Ljava.lang.String;@61d4d9a,
        "android.largeIcon"=null,
        "android.infoText"=null,
        "android.wearable.EXTENSIONS"={"background"=android.graphics.Bitmap@73b26cb},
        "android.originatingUserId"=0,
        "android.progressIndeterminate"=false,
        "android.remoteInputHistory"=null}
tickerText="pv@swooby.com"
---- bigContentView ----
walkView: view=null
---- contentView ----
walkView: view=null
*/
        CharSequence androidTitle = extras.getCharSequence(Notification.EXTRA_TITLE);
        FooLog.v(TAG, "onNotificationPosted: androidTitle == " + FooString.quote(androidTitle));
        CharSequence[] androidTextLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        FooLog.v(TAG, "onNotificationPosted: androidTextLines == " + Arrays.toString(androidTextLines));
        CharSequence androidSubText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
        FooLog.v(TAG, "onNotificationPosted: androidSubText == " + FooString.quote(androidSubText));
        String[] androidPeople = extras.getStringArray(Notification.EXTRA_PEOPLE);
        FooLog.v(TAG, "onNotificationPosted: androidPeople == " + Arrays.toString(androidPeople));
        ApplicationInfo androidAppInfo = extras.getParcelable("android.appInfo");
        FooLog.v(TAG, "onNotificationPosted: androidAppInfo == " + FooString.repr(androidAppInfo));
        String androidInfoText = extras.getString(Notification.EXTRA_INFO_TEXT);
        FooLog.v(TAG, "onNotificationPosted: androidInfoText == " + FooString.quote(androidInfoText));
        String androidTemplate = extras.getString(Notification.EXTRA_TEMPLATE);
        FooLog.v(TAG, "onNotificationPosted: androidTemplate == " + FooString.quote(androidTemplate));
        CharSequence androidText = extras.getCharSequence(Notification.EXTRA_TEXT);
        FooLog.v(TAG, "onNotificationPosted: androidText == " + FooString.quote(androidText));
        CharSequence androidBigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        FooLog.v(TAG, "onNotificationPosted: androidBigText == " + FooString.quote(androidBigText));

        FooTextToSpeechBuilder builder = new FooTextToSpeechBuilder();

        //
        // Account name...
        //
        if (androidSubText == null)
        {
            FooLog.w(TAG, "onNotificationPosted: accountName == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }
        builder.appendSpeech(androidSubText.toString());

        //
        // Notification type/style...
        //
        Class notificationClass = null;
        if (androidTemplate != null)
        {
            if (InboxStyle.class.getName().equals(androidTemplate))
            {
                notificationClass = InboxStyle.class;
            }
            else if (BigTextStyle.class.getName().equals(androidTemplate))
            {
                notificationClass = BigTextStyle.class;
            }
        }

        if (notificationClass == BigTextStyle.class)
        {
            return NotificationParseResult.ParsableIgnored;
        }
        else if (notificationClass == InboxStyle.class)
        {
            //
            // X New Messages...
            //
            if (androidTitle != null)
            {
                builder.appendSpeech(androidTitle.toString());
            }

            //
            // From + Subject...
            //
            if (androidTextLines != null)
            {
                for (CharSequence textLine : androidTextLines)
                {
                    FooLog.v(TAG, "onNotificationPosted: textLine=" + textLine);
                    boolean appended = false;

                    if (textLine instanceof Spannable)
                    {
                        Spannable spannable = (Spannable) textLine;
                        int next;
                        for (int i = 0, spannableLength = spannable.length(); i < spannableLength; i = next)
                        {
                            next = spannable.nextSpanTransition(i, spannableLength, TextAppearanceSpan.class);
                            CharSequence spanText = spannable.subSequence(i, next);
                            String text = spanText.toString().trim();
                            if (!FooString.isNullOrEmpty(text))
                            {
                                builder.appendSpeech(text);
                                appended = true;
                            }
                        }
                    }

                    if (!appended)
                    {
                        builder.appendSpeech(textLine.toString());
                    }
                }
            }
        }
        else
        {
            return NotificationParseResult.Unparsable;
        }

        getTextToSpeech().speak(builder);

        return NotificationParseResult.ParsableHandled;
    }
}
