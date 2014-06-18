package com.peteschmitz.android.pocketwikipedia.constant;

import com.peteschmitz.android.pocketwikipedia.language.abs.Language;

import org.jetbrains.annotations.NotNull;

public class Wikipedia {

    public static final int NAMESPACE_MAIN = 0;

    public static final String FILE = "File:";
    public static final String ACTION = "&action=";
    public static final String SECTION = "&section=";
    public static final String PAGE = "&page=";
    public static final String PROPERTIES = "&prop=";
    public static final String IMAGE_INFO_PROPERTIES = "&iiprop=";
    public static final String ACTION_PARSE = "&action=parse";
    public static final String SECTION_FIRST = "&section=0";
    public static final String SEARCH = "&search=";
    public static final String ACTION_OPENSEARCH = "&action=opensearch";
    public static final String ACTION_QUERY = "&action=query";
    public static final String TITLES = "&titles=";
    public static final String IMAGE_LIMIT = "&imlimit=";
    public static final String MOBILE_FORMAT = "&mobileformat";
    public static final String TEXT = "text";
    public static final String IMAGES = "images";
    public static final String IMAGE_INFO = "imageinfo";
    public static final String URL = "url";
    public static final String REDIRECTS = "&redirects";
    public static final String LIST = "&list=";
    public static final String SIZE = "size";
    private static final String SPECIAL_RANDOM = "Special:Random";
    public static final String RANDOM = "random";
    public static final String RANDOM_LIMIT = "&rnlimit=";
    public static final String RANDOM_NAMESPACE = "&rnnamespace=";
    public static Language LANGUAGE;
    public static String BASE_API_URL;
    private static final String UPLOAD_URL = "http://upload.wikimedia.org/wikipedia/";
    public static final String UPLOAD_COMMONS_URL = "http://upload.wikimedia.org/wikipedia/commons/";
    public static String UPLOAD_LANG_URL;
    public static String PAGE_MAIN;

    public static void setLanguage(Language language) {
        LANGUAGE = language;
        BASE_API_URL = "http://" + LANGUAGE.getSubDomain() + ".wikipedia.org/w/api.php?format=json";
        UPLOAD_LANG_URL = "http://upload.wikimedia.org/wikipedia/" + LANGUAGE.getSubDomain() + "/";
        PAGE_MAIN = "&page=" + LANGUAGE.getMainPage();
    }

    @NotNull
    public static String getBaseArticleURL() {
        languageCheck();

        return getBaseURL() + "/wiki/";
    }

    @NotNull
    public static String getBaseURL(){
        languageCheck();

        return "http://" + LANGUAGE.getSubDomain() + ".wikipedia.org";
    }

    @NotNull
    public static String getOpensearchURL(String query){
        languageCheck();

        return BASE_API_URL + SEARCH + query + ACTION_OPENSEARCH;
    }
    
    @NotNull
    public static String getRandomArticleURL(){
        return getBaseArticleURL() + SPECIAL_RANDOM;
    }

    private static void languageCheck(){
        if (LANGUAGE == null){
            throw new IllegalStateException("Language hasn't been set yet. See #setLanguage()");
        }
    }

    private Wikipedia() {
        throw new AssertionError("Class is reserved for static usage only");
    }
}