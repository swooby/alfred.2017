package com.swooby.alfred.notification.parsers;

import android.app.Notification;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;

import com.smartfoo.android.core.logging.FooLog;
import com.smartfoo.android.core.texttospeech.FooTextToSpeechBuilder;
import com.swooby.alfred.R;

import java.util.LinkedList;
import java.util.List;

public class GoogleHangoutsNotificationParser
        extends AbstractNotificationParser
{
    private static final String TAG = FooLog.TAG(GoogleHangoutsNotificationParser.class);

    private static class TextMessage
    {
        final String mFrom;
        final String mMessage;

        public TextMessage(String from, String message)
        {
            mFrom = from;
            mMessage = message;
        }

        @Override
        public boolean equals(Object o)
        {
            if (o instanceof TextMessage)
            {
                return equals((TextMessage) o);
            }
            return super.equals(o);
        }

        public boolean equals(TextMessage o)
        {
            return mFrom.equals(o.mFrom) && mMessage.equals(o.mMessage);
        }

        @Override
        public int hashCode()
        {
            return mFrom.hashCode() + mMessage.hashCode();
        }
    }

    private final List<TextMessage> mTextMessages;

    public GoogleHangoutsNotificationParser(@NonNull NotificationParserCallbacks callbacks)
    {
        super(callbacks);//, application.getString(R.string.hangouts_package_app_spoken_name));

        mTextMessages = new LinkedList<>();
    }

    @Override
    public String getPackageName()
    {
        return "com.google.android.talk";
    }

    private TextMessage addTextMessage(String from, String message)
    {
        TextMessage textMessage = new TextMessage(from, message);
        if (mTextMessages.contains(textMessage))
        {
            return null;
        }

        mTextMessages.add(textMessage);

        return textMessage;
    }

    @Override
    public NotificationParseResult onNotificationPosted(StatusBarNotification sbn)
    {
        super.onNotificationPosted(sbn);

        Notification notification = sbn.getNotification();
        if (notification == null)
        {
            FooLog.w(TAG, "onNotificationPosted: textViewStation == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        Bundle extras = notification.extras;
        if (extras == null)
        {
            FooLog.w(TAG, "onNotificationPosted: extras == null; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        List<TextMessage> textMessages = new LinkedList<>();

        CharSequence androidTitle = extras.getCharSequence("android.title", "Unknown User");
        CharSequence androidText = extras.getCharSequence("android.text", "Unknown Text");
        CharSequence[] androidTextLines = extras.getCharSequenceArray("android.textLines");

        if (androidTextLines != null)
        {
            for (CharSequence textLine : androidTextLines)
            {
                String[] parts = textLine.toString().split("  ");
                String from = parts[0];
                String message = parts[1];
                TextMessage textMessage = addTextMessage(from, message);
                if (textMessage != null)
                {
                    textMessages.add(textMessage);
                }
            }
        }
        else
        {
            String from = androidTitle.toString();
            String message = androidText.toString();
            TextMessage textMessage = addTextMessage(from, message);
            if (textMessage != null)
            {
                textMessages.add(textMessage);
            }
        }

        // TODO:(pv) I think there is perhaps a better even less verbatim way to speak the notification info...

        int count = textMessages.size();
        if (count == 0)
        {
            FooLog.w(TAG, "onNotificationPosted: textMessages.size() == 0; Unparsable");
            return NotificationParseResult.Unparsable;
        }

        Context context = getContext();
        Resources resources = context.getResources();
        String title = resources.getQuantityString(R.plurals.X_new_messages, count, count);
        FooTextToSpeechBuilder builder = new FooTextToSpeechBuilder(title);
        for (TextMessage textMessage : textMessages)
        {
            builder.appendSilence(750);
            builder.appendSpeech(context.getString(R.string.X_says, textMessage.mFrom));
            //builder.appendSpeech("to " + to);
            builder.appendSilence(500);
            builder.appendSpeech(textMessage.mMessage);
        }

        getTextToSpeech().speak(builder);

        return NotificationParseResult.ParsableHandled;
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn)
    {
        mTextMessages.clear();
    }
}
