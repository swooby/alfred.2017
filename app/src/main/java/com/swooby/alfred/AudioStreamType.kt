package com.swooby.alfred

import android.content.Context
import android.media.AudioManager
import com.smartfoo.android.core.FooRun
import com.smartfoo.android.core.R

class AudioStreamType
private constructor(private val mName: String, val audioStreamType: Int) {
    override fun toString(): String {
        return mName
    }

    companion object {
        private var sTypes: ArrayList<AudioStreamType>? = null

        fun getTypes(context: Context): List<AudioStreamType> {
            if (sTypes == null) {
                FooRun.throwIllegalArgumentExceptionIfNull(context, "context")
                sTypes = ArrayList()
                sTypes!!.add(
                    AudioStreamType(
                        context.getString(R.string.audio_stream_notification),
                        AudioManager.STREAM_NOTIFICATION
                    )
                )
                sTypes!!.add(
                    AudioStreamType(
                        context.getString(R.string.audio_stream_media),
                        AudioManager.STREAM_MUSIC
                    )
                )
                sTypes!!.add(
                    AudioStreamType(
                        context.getString(R.string.audio_stream_alarm),
                        AudioManager.STREAM_ALARM
                    )
                )
            }
            return sTypes!!
        }
    }
}
