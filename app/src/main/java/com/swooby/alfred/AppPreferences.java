package com.swooby.alfred;

import android.content.Context;
import android.speech.tts.Voice;

import com.smartfoo.android.core.content.FooPreferences;

public class AppPreferences
        extends FooPreferences
{
    private static final String KEY_USER_VOICE_NAME    = "pref_user_voice_name";
    private static final String KEY_USER_PROFILE_TOKEN = "pref_user_profile_token";
    private static final String KEY_USER_KEYPHRASE     = "pref_user_keyphrase";

    public AppPreferences(Context applicationContext)
    {
        super(applicationContext);
    }

    public String getVoiceName()
    {
        return getString(FILE_NAME_USER, KEY_USER_VOICE_NAME, null);
    }

    public void setVoice(Voice value)
    {
        setString(FILE_NAME_USER, KEY_USER_VOICE_NAME, value != null ? value.getName() : null);
    }

    public String getProfileToken()
    {
        return getString(FILE_NAME_USER, KEY_USER_PROFILE_TOKEN, null);
    }

    public void setProfileToken(String value)
    {
        setString(FILE_NAME_USER, KEY_USER_PROFILE_TOKEN, value);
    }

    public String getKeyphrase()
    {
        return getString(FILE_NAME_USER, KEY_USER_KEYPHRASE, "alfred");
    }

    public void setKeyphrase(String value)
    {
        setString(FILE_NAME_USER, KEY_USER_KEYPHRASE, value);
    }
}
