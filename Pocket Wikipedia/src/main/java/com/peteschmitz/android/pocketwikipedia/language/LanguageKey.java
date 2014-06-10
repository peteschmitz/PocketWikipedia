package com.peteschmitz.android.pocketwikipedia.language;

import com.peteschmitz.android.pocketwikipedia.constant.Wikipedia;
import com.peteschmitz.android.pocketwikipedia.language.abs.Language;

/**
 * Created by Pete Schmitz on 3/25/14.
 */
public enum LanguageKey {
    ENGLISH("english", English.class);

    public static void setLanguage(String languageId){
        LanguageKey targetLanguageKey = getLanguageKeyFromId(languageId);
        Wikipedia.setLanguage(targetLanguageKey.getLanguageInstance());
    }

    public static LanguageKey getLanguageKeyFromId(String languageKey){
        for (LanguageKey language : LanguageKey.values()){
            if (language.getLanguageKey().equals(languageKey)) return language;
        }

        throw new IllegalArgumentException("Language key \"" + languageKey + "\" is not supported.");
    }


    private String mLanguageKey;
    private Class<? extends Language> mLanguageClass;

    private LanguageKey(String languageKey, Class<? extends Language> languageClass){
        mLanguageKey = languageKey;
        mLanguageClass = languageClass;
    }

    public String getLanguageKey(){
        return mLanguageKey;
    }

    public Language getLanguageInstance(){
        try {
            return mLanguageClass.newInstance();
        } catch (InstantiationException e) {
            throw new IllegalStateException(
                    "Language class \"" + mLanguageClass + "\" could not be instantiated.");
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "Language class \"" + mLanguageClass + "\" could not be instantiated.");
        }
    }
}
