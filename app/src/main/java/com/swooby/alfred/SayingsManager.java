package com.swooby.alfred;

import android.content.Context;
import android.icu.util.Calendar;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.smartfoo.android.core.texttospeech.FooTextToSpeechBuilder;

import java.util.Random;

public class SayingsManager
{
    private static final Formality FORMALITY_DEFAULT = Formality.Formal;

    /**
     * https://www.altalang.com/beyond-words/2012/05/01/how-did-that-register-five-levels-of-formality-in-language/
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
    private final Random  mRandom;

    SayingsManager(@NonNull Context context)
    {
        mContext = context;
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
        Male,
        Female,
    }

    private String userPronoun(Formality formality)
    {
        formality = formalityOrRandomFormality(formality);

        // TODO:(pv) Settable or pull from settings...
        Gender gender = Gender.Male;

        switch (gender)
        {
            case Male:
                return "Sir";
            case Female:
                switch (formality)
                {
                    case Frozen:
                    case Formal:
                        return "Madam";
                    default:
                        return "Ma'am";
                }
            default:
                throw new IllegalArgumentException("Unexpected gender == " + gender);
        }
    }

    private String userName()
    {
        // TODO:(pv) Settable or pull from settings...
        return "Paul";
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
                return userName();
            default:
                throw new IllegalArgumentException("Unexpected formality == " + formality);
        }
    }

    FooTextToSpeechBuilder goodPartOfDayUserNoun()
    {
        FooTextToSpeechBuilder builder;
        int hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        // TODO:(pv) Make these settable?
        if (hourOfDay > 18) // 6PM
        {
            builder = goodEveningUserNoun();
        }
        else if (hourOfDay > 12) // 12PM
        {
            builder = goodAfternoonUserNoun();
        }
        else
        {
            builder = goodMorningUserNoun();
        }
        return builder;
    }

    private FooTextToSpeechBuilder goodMorningUserNoun()
    {
        return new FooTextToSpeechBuilder(mContext)
                .appendSpeech("Good morning " + userNoun(FORMALITY_DEFAULT));
    }

    private FooTextToSpeechBuilder goodAfternoonUserNoun()
    {
        return new FooTextToSpeechBuilder(mContext)
                .appendSpeech("Good afternoon " + userNoun(FORMALITY_DEFAULT));
    }

    private FooTextToSpeechBuilder goodEveningUserNoun()
    {
        return new FooTextToSpeechBuilder(mContext)
                .appendSpeech("Good evening " + userNoun(FORMALITY_DEFAULT));
    }
}
