package com.peteschmitz.android.pocketwikipedia.language;

import android.text.TextUtils;
import android.util.Log;

import com.peteschmitz.android.pocketwikipedia.data.FrontPageSectionData;
import com.peteschmitz.android.pocketwikipedia.language.abs.Language;
import com.peteschmitz.android.pocketwikipedia.util.WikiTextUtils;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Pete Schmitz on 3/25/14.
 */
public class English implements Language {

    private enum SupportedHeader{
        FEATURED_ARTICLE("tfa", "Today's Featured Article", true),
        FEATURED_PICTURE("tfp", "Today's Featured Picture", false),
        FEATURED_LIST("tfl", "Today's Featured List", false),
        DID_YOU_KNOW("dyk", "Did you know...", true),
        IN_THE_NEWS("itn", "In the news...", true),
        ON_THIS_DAY("otd", "On this day...", true);

        private String mId;
        private String mTitle;
        private boolean mImagesFromPages;

        private SupportedHeader(String id, String title, boolean imagesFromPages){
            mId = id;
            mTitle = title;
            mImagesFromPages = imagesFromPages;
        }

        public String getId(){
            return mId;
        }

        public String getTitle(){
            return mTitle;
        }

        public boolean useImagesFromPages(){ return mImagesFromPages; }
    }

    private static int MAX_SENTENCES = 3;
    private static String[] OMISSIONS = new String[]{"\\(pictured\\)", "\\(detail pictured\\)"};

    @Override
    public FrontPageSectionData[] buildFrontPageSections(String text) {
        if (TextUtils.isEmpty(text)){
            Log.e("pwiki", "Attempted to build front page selections with empty text.");
            return null;
        }

        ArrayList<FrontPageSectionData> sections = new ArrayList<FrontPageSectionData>();

        String[] sectionsById = text.split("id=\"mp-");

        lp:
        for (SupportedHeader supportedHeader : SupportedHeader.values()){
            String id = supportedHeader.getId() + "\"";

            for (String sectionById : sectionsById){
                if (id.equals(sectionById.substring(0, id.length()))){
                    FrontPageSectionData section = new FrontPageSectionData();
                    section.line = supportedHeader.getTitle();
                    section.id = supportedHeader.getId();
                    section.div = sectionById.split(">", 2)[1];
                    section.text = Jsoup.parse(section.div).text();
                    section.span = removeImages(section.div);

                    if (section.text != null){

                    }
                    section.build(!supportedHeader.useImagesFromPages());

                    sections.add(section);

                    continue lp;
                }
            }
        }

        postModifications(sections);

        return sections.toArray(new FrontPageSectionData[sections.size()]);
    }

    private String removeImages(String string){
        return string.replaceAll("<img.*?>", "");
    }

    @Override
    public void postModifications(@NotNull List<FrontPageSectionData> sections) {
        for (FrontPageSectionData section : sections){

            if (section.items.isEmpty()){
                if (!TextUtils.isEmpty(section.text)){
                    section.text = WikiTextUtils.getMergedSplit(section.text, "(?<=\\.)", MAX_SENTENCES);
                    section.text = WikiTextUtils.removeOmissions(section.text, OMISSIONS);
                }
            } else {
                for (FrontPageSectionData.ListItem listItem : section.items){
                    listItem.text = WikiTextUtils.removeOmissions(listItem.text, OMISSIONS);
                }
            }

        }
    }

    @Override
    public String getMainPage() {
        return "Main_Page";
    }

    @Override
    public String getSubDomain() {
        return "en";
    }
}
