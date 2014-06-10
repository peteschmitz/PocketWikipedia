package com.peteschmitz.android.pocketwikipedia.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import android.util.Log;

import com.peteschmitz.android.pocketwikipedia.constant.Wikipedia;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Data container for a Wikipedia language-specific landing page.
 * <p/>
 * Created by Pete Schmitz on 3/25/14.
 */
public class FrontPageSectionData implements Parcelable {

    public static enum LayoutType {
        SINGLE, LIST;

        public static LayoutType evaluate(FrontPageSectionData section) {

            return section.items.size() > 1 ? LIST : SINGLE;
        }
    }

    public static
    @Nullable
    FrontPageSectionData[] parseSections(@NotNull JSONObject jsonObject) {

        try {
            return Wikipedia.LANGUAGE.buildFrontPageSections(
                    jsonObject.getJSONObject("parse").getJSONObject("text").getString("*")
            );
        } catch (JSONException e) {
            Log.e("pwiki", e.getMessage());
        }

        return null;
    }

    public static final Parcelable.Creator<FrontPageSectionData> CREATOR
            = new Parcelable.Creator<FrontPageSectionData>() {

        public FrontPageSectionData createFromParcel(Parcel in) {
            return new FrontPageSectionData(in);
        }

        public FrontPageSectionData[] newArray(int size) {
            return new FrontPageSectionData[size];
        }
    };

    public String id = "";
    public String line = "";
    public String text = "";
    public String div = "";
    public String span = "";
    public Spanned spanned = new SpannedString("");

    public List<String> images = new LinkedList<String>();
    public List<String> pages = new LinkedList<String>();
    public List<ListItem> items = new LinkedList<ListItem>();
    public List<String> importantPages = new LinkedList<String>();

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(line);
        dest.writeString(text);
        dest.writeString(div);
        dest.writeString(spanned.toString());

        dest.writeStringList(images);
        dest.writeStringList(pages);
        dest.writeTypedList(items);
        dest.writeStringList(importantPages);
    }

    public FrontPageSectionData(Parcel in) {
        id = in.readString();
        line = in.readString();
        text = in.readString();
        div = in.readString();
        spanned = new SpannedString(in.readString());

        in.readStringList(images);
        in.readStringList(pages);
        in.readTypedList(items, ListItem.CREATOR);
        in.readStringList(importantPages);
    }

    public FrontPageSectionData() {

    }

    public void build(boolean getImages) {
        buildSectionItems();

        if (items.isEmpty()) {
            if (getImages) buildSectionImages();
            buildSectionPages();
        } else {

            // Dump text
            text = "";
            span = "";
        }
    }

    public void buildSpanned() {
        if (TextUtils.isEmpty(span)) return;

        spanned = Html.fromHtml(span);
    }

    public String getSuggestedArticle() {
        if (importantPages.size() >= 3) return importantPages.get(0).split("%23|#")[0];

        if (!pages.isEmpty()) return pages.get(0).split("%23|#")[0];

        return "";
    }

    private void buildSectionItems() {

        Pattern listPattern = Pattern.compile("<li>(.*?)</li>");
        Matcher listMatcher = listPattern.matcher(div);

        Pattern linkPattern = Pattern.compile("href=\"(.*?)\"");
        Matcher linkMatcher;

        Pattern importantPattern = Pattern.compile("<b><a href=\"(.*?)\"");
        Matcher importantMatcher;

        while (listMatcher.find()) {
            String list = listMatcher.group(1);
            ListItem item = new ListItem();
            item.text = Jsoup.parse(list).text();
            item.span = list;

            linkMatcher = linkPattern.matcher(list);

            while (linkMatcher.find()) {
                String[] split = linkMatcher.group(1).split("/wiki/", 2);

                if (split.length > 1 && !TextUtils.isEmpty(split[1])) {
                    item.pages.add(split[1]);
                }

            }

            importantMatcher = importantPattern.matcher(list);
            while (importantMatcher.find()) {
                String[] split = importantMatcher.group(1).split("/wiki/", 2);

                if (split.length > 1 && !TextUtils.isEmpty(split[1])) {
                    item.importantPages.add(split[1]);
                }

            }

            items.add(item);
        }
    }

    private void buildSectionPages() {

        Pattern pattern = Pattern.compile("<a (.*?)>");
        Matcher matcher = pattern.matcher(div);

        Pattern pagePattern = Pattern.compile("href=\"(.*?)\"");
        Matcher pageMatcher;

        while (matcher.find()) {
            String link = matcher.group(1);

            if (!link.contains("class=\"image\"")) {
                pageMatcher = pagePattern.matcher(link);

                if (pageMatcher.find()) {
                    String[] split = pageMatcher.group(1).split("/wiki/", 2);

                    if (split.length > 1 && !TextUtils.isEmpty(split[1])) {
                        pages.add(split[1]);
                    }
                }
            }
        }

        Pattern importantPattern = Pattern.compile("<b><a (.*?)>");
        matcher = importantPattern.matcher(div);

        while (matcher.find()) {
            String link = matcher.group(1);

            if (!link.contains("class=\"image\"")) {
                pageMatcher = pagePattern.matcher(link);

                if (pageMatcher.find()) {
                    String[] split = pageMatcher.group(1).split("/wiki/", 2);

                    if (split.length > 1 && !TextUtils.isEmpty(split[1])) {
                        importantPages.add(split[1]);
                    }
                }
            }
        }
    }

    private void buildSectionImages() {

        Pattern pattern = Pattern.compile("srcset=\"(.*?) ");
        Matcher matcher = pattern.matcher(div);

        while (matcher.find()) {
            images.add("http:" + matcher.group(1));
        }
    }

    public static class ListItem implements Parcelable {

        public static final Creator<ListItem> CREATOR
                = new Creator<ListItem>() {

            public ListItem createFromParcel(Parcel in) {
                return new ListItem(in);
            }

            public ListItem[] newArray(int size) {
                return new ListItem[size];
            }
        };

        public String text = "";
        public String span = "";
        public Spanned spanned = new SpannedString("");
        public ArrayList<String> pages = new ArrayList<String>();
        public ArrayList<String> importantPages = new ArrayList<String>();

        public ListItem() {

        }

        public ListItem(Parcel in) {
            text = in.readString();
            span = in.readString();
            spanned = new SpannedString(in.readString());
            pages = in.createStringArrayList();
            importantPages = in.createStringArrayList();

        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(text);
            dest.writeString(span);
            dest.writeString(spanned.toString());
            dest.writeStringList(pages);
            dest.writeStringList(importantPages);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public boolean buildSpanned() {
            if (TextUtils.isEmpty(span)) return false;

            spanned = Html.fromHtml(span);

            return true;
        }

        public String getSuggestedArticle() {
            if (!importantPages.isEmpty()) return importantPages.get(0);

            if (!pages.isEmpty()) return pages.get(0);

            return "";
        }
    }
}
