package com.swooby.alfred

import android.content.Context
import android.media.AudioManager
import com.smartfoo.android.core.FooString
import com.smartfoo.android.core.content.FooPreferences

/** @noinspection unused
 */
class AppPreferences(applicationContext: Context?)
    : FooPreferences(applicationContext) {

    companion object {
        //const val DEFAULT_VOICE_NAME: String = "en-GB-language"
        const val DEFAULT_VOICE_NAME: String = "en-gb-x-rjs-local"

        private const val KEY_USER_VOICE_NAME = "pref_user_tts_voice_name"
        private const val KEY_USER_VOICE_AUDIO_STREAM_TYPE = "pref_user_tts_voice_audio_stream_type"
        private const val KEY_USER_PROFILE_TOKEN = "pref_user_profile_token"
        private const val KEY_USER_KEYPHRASE = "pref_user_keyphrase"
    }

    init {
        @Suppress("KotlinConstantConditions", "SimplifyBooleanWithConstants")
        if (BuildConfig.DEBUG && false)
        {
            clearAll()
        }
    }

    fun textToSpeechVoiceName(): String {
        return getString(
            FILE_NAME_USER,
            KEY_USER_VOICE_NAME,
            DEFAULT_VOICE_NAME
        )
    }
    fun setTextToSpeechVoiceName(value: String?) {
        setString(
            FILE_NAME_USER,
            KEY_USER_VOICE_NAME,
            if (!FooString.isNullOrEmpty(value)) value else DEFAULT_VOICE_NAME
        )
    }

    var textToSpeechAudioStreamType: Int
        get() = getInt(
            FILE_NAME_USER,
            KEY_USER_VOICE_AUDIO_STREAM_TYPE,
            AudioManager.STREAM_MUSIC
        )
        set(value) {
            setInt(
                FILE_NAME_USER,
                KEY_USER_VOICE_AUDIO_STREAM_TYPE,
                value
            )
        }

    fun profileToken(): String {
        return getString(
            FILE_NAME_USER,
            KEY_USER_PROFILE_TOKEN,
            ProfileManager.DEFAULT_PROFILE_TOKEN
        )
    }
    fun setProfileToken(value: String?) {
        @Suppress("NAME_SHADOWING")
        var value = value
        if (value.isNullOrEmpty()) {
            value = ProfileManager.DEFAULT_PROFILE_TOKEN
        }
        setString(
            FILE_NAME_USER,
            KEY_USER_PROFILE_TOKEN,
            value
        )
    }

    fun keyphrase(): String {
        return getString(
            FILE_NAME_USER,
            KEY_USER_KEYPHRASE,
            "alfred"
        )
    }
    fun setKeyphrase(value: String?) {
        setString(
            FILE_NAME_USER,
            KEY_USER_KEYPHRASE,
            value
        )
    }
}
