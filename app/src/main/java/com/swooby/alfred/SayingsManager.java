package com.swooby.alfred;

import android.content.Context;
import android.icu.util.Calendar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.smartfoo.android.core.texttospeech.FooTextToSpeechBuilder;
import com.smartfoo.android.core.FooString;

import java.util.Random;

public class SayingsManager
{
    private static final Formality FORMALITY_DEFAULT = Formality.Formal;

    /**
     * <a href="https://www.altalang.com/beyond-words/2012/05/01/how-did-that-register-five-levels-of-formality-in-language/">...</a>
     */
    public enum Formality
    {
        Frozen,
        Formal,
        Consultative,
        Casual,
        Intimate,
    }

    private final Context mContext;
    private final AppPreferences mAppPreferences;
    private final Random  mRandom;

    SayingsManager(@NonNull Context context, @NonNull AppPreferences appPreferences)
    {
        mContext = context;
        mAppPreferences = appPreferences;
        mRandom = new Random();
    }

    @NonNull
    public String getString(@StringRes int resId, Object... formatArgs)
    {
        return mContext.getString(resId, formatArgs);
    }

    @NonNull
    Formality randomFormality()
    {
        return randomFormality(null);
    }

    /** @noinspection SameParameterValue*/
    @NonNull
    Formality randomFormality(Formality minimum)
    {
        if (minimum == null)
        {
            minimum = FORMALITY_DEFAULT;
        }

        Formality[] formalities = Formality.values();
        int indexMax = 0;
        for (Formality formality : formalities)
        {
            if (formality == minimum)
            {
                break;
            }
            indexMax++;
        }

        int indexRandom = mRandom.nextInt(indexMax);

        return formalities[indexRandom];
    }

    Formality formalityOrRandomFormality(Formality formality)
    {
        return formality != null ? formality : randomFormality();
    }

    public enum Gender
    {
        Unspecified(R.string.user_gender_unspecified),
        Male(R.string.user_gender_male),
        Female(R.string.user_gender_female);

        @StringRes
        private final int mDisplayNameResId;

        Gender(@StringRes int displayNameResId)
        {
            mDisplayNameResId = displayNameResId;
        }

        @StringRes
        int getDisplayNameResId()
        {
            return mDisplayNameResId;
        }
    }

    private String userPronoun(Formality formality)
    {
        formality = formalityOrRandomFormality(formality);

        Gender gender = mAppPreferences.userGender();

        switch (gender)
        {
            case Male:
                return mContext.getString(R.string.user_pronoun_male);
            case Female:
                switch (formality)
                {
                    case Frozen:
                    case Formal:
                        return mContext.getString(R.string.user_pronoun_female_formal);
                    default:
                        return mContext.getString(R.string.user_pronoun_female_casual);
                }
            case Unspecified:
                return mContext.getString(R.string.user_pronoun_unspecified);
            default:
                throw new IllegalArgumentException("Unexpected gender == " + gender);
        }
    }

    private String userName()
    {
        return mAppPreferences.userName();
    }

    private String userNoun()
    {
        return userNoun(FORMALITY_DEFAULT);
    }

    private String userNoun(Formality formality)
    {
        formality = formalityOrRandomFormality(formality);
        switch (formality)
        {
            case Frozen:
            case Formal:
            case Consultative:
                return userPronoun(formality);
            case Casual:
            case Intimate:
                String userName = userName();
                if (FooString.isNullOrEmpty(userName))
                {
                    return userPronoun(formality);
                }
                return userName;
            default:
                throw new IllegalArgumentException("Unexpected formality == " + formality);
        }
    }

    FooTextToSpeechBuilder goodPartOfDayUserNoun()
    {
        return goodPartOfDayUser(userNoun(FORMALITY_DEFAULT));
    }

    FooTextToSpeechBuilder goodPartOfDayUserName(@Nullable String userName)
    {
        if (userName == null)
        {
            return goodPartOfDayUserNoun();
        }

        String trimmedUserName = userName.trim();
        if (trimmedUserName.isEmpty())
        {
            return goodPartOfDayUserNoun();
        }

        return goodPartOfDayUser(trimmedUserName);
    }

    private FooTextToSpeechBuilder goodPartOfDayUser(@NonNull String user)
    {
        FooTextToSpeechBuilder builder;
        int hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        // TODO:(pv) Make these settable?
        if (hourOfDay > 18) // 6PM
        {
            builder = goodEveningUser(user);
        }
        else if (hourOfDay > 12) // 12PM
        {
            builder = goodAfternoonUser(user);
        }
        else
        {
            builder = goodMorningUser(user);
        }
        return builder;
    }

    private FooTextToSpeechBuilder goodMorningUserNoun()
    {
        return goodMorningUser(userNoun(FORMALITY_DEFAULT));
    }

    private FooTextToSpeechBuilder goodAfternoonUserNoun()
    {
        return goodAfternoonUser(userNoun(FORMALITY_DEFAULT));
    }

    private FooTextToSpeechBuilder goodEveningUserNoun()
    {
        return goodEveningUser(userNoun(FORMALITY_DEFAULT));
    }

    private FooTextToSpeechBuilder goodMorningUser(@NonNull String user)
    {
        return new FooTextToSpeechBuilder(mContext)
                .appendSpeech("Good morning " + user);
    }

    private FooTextToSpeechBuilder goodAfternoonUser(@NonNull String user)
    {
        return new FooTextToSpeechBuilder(mContext)
                .appendSpeech("Good afternoon " + user);
    }

    private FooTextToSpeechBuilder goodEveningUser(@NonNull String user)
    {
        return new FooTextToSpeechBuilder(mContext)
                .appendSpeech("Good evening " + user);
    }
}
