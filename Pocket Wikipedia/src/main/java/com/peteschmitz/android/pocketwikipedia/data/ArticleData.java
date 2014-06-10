package com.peteschmitz.android.pocketwikipedia.data;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.Pair;

import com.peteschmitz.android.pocketwikipedia.R;
import com.peteschmitz.android.pocketwikipedia.util.WikiLinkUtils;
import com.peteschmitz.android.pocketwikipedia.util.WikiTextUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Data container for a Wikipedia article.
 * <p/>
 * Created by Pete Schmitz on 4/16/2014.
 */
public class ArticleData implements Parcelable {

    public interface OnItemBuiltListener {
        void onItemBuilt(@NotNull ArticleData item);
    }

    public static enum EmbedType {
        TABLE, DIV
    }

    public static enum ArticleDataLevel {
        ZERO("(?=<h2><span class=\"mw-headline\")", R.layout.article_data_header_one, R.layout.article_drawer_view_one),
        ONE("(?=<h2><span class=\"mw-headline\")", R.layout.article_data_header_one, R.layout.article_drawer_view_one),
        TWO("(?=<h3><span class=\"mw-headline\")", R.layout.article_data_header_two, R.layout.article_drawer_view_two),
        THREE("(?=<h4><span class=\"mw-headline\")", R.layout.article_data_header_three, R.layout.article_drawer_view_three);

        private String mRegex;
        private int mHeaderLayoutResource;
        private int mDrawerLayoutResource;

        private ArticleDataLevel(@NotNull String regex, int headerLayoutResource, int drawerLayoutResource) {
            mRegex = regex;
            mHeaderLayoutResource = headerLayoutResource;
            mDrawerLayoutResource = drawerLayoutResource;
        }

        public
        @NotNull
        String getRegex() {
            return mRegex;
        }

        public boolean hasNext() {
            return ordinal() < values().length - 1;
        }

        public
        @NotNull
        ArticleDataLevel next() {
            return values()[ordinal() + 1];
        }

        public int getHeaderLayoutResource() {
            return mHeaderLayoutResource;
        }

        public int getDrawerLayoutResource() {
            return mDrawerLayoutResource;
        }

        public static boolean isRootLevel(ArticleDataLevel level) {
            return level != null && level.equals(ONE) || level != null && level.equals(ZERO);
        }
    }

    public static final Creator<ArticleData> CREATOR
            = new Creator<ArticleData>() {

        public ArticleData createFromParcel(Parcel in) {
            return new ArticleData(in);
        }

        public ArticleData[] newArray(int size) {
            return new ArticleData[size];
        }
    };

    private static final Pattern TITLE_PATTERN = Pattern.compile("<span class=\"mw-headline\"(.*?)]", Pattern.MULTILINE);

    private static final Pattern CLASS_PATTERN = Pattern.compile("class=\"(.*?)\"");
    private static final String NO_LINE_BREAKS = "\n";
    private static final String NO_IMAGES = "<img(.*?)>";
    private static final String NO_BRACKETS = "\\[(.*?)]";
    private static final String NO_SOURCES = "<sup(.*?)>(.*?)</sup>";
    private static final String NO_PARAGRAPHS = "<p>|</p>";

    public static final String MODIFICATION_REGEX =
            NO_LINE_BREAKS + "|" +
                    NO_IMAGES + "|" +
                    NO_SOURCES + "|" +
                    NO_PARAGRAPHS;

    public String data = "";
    public SpannableString rawSpan = new SpannableString("");
    public String title = "";
    public SpannableString span = new SpannableString("");
    public ArticleDataLevel level;
    public List<ArticleData> items = new LinkedList<ArticleData>();
    public SectionIndex index;
    public String table = "";
    public String div = "";

    private OnItemBuiltListener mListener;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(data);
        dest.writeString(title);
        dest.writeString(table);
        dest.writeString(div);
        dest.writeString(rawSpan.toString());
        dest.writeString(span.toString());
        dest.writeSerializable(level);
        dest.writeTypedList(items);
        dest.writeParcelable(index, flags);
    }

    public ArticleData(Parcel in) {

        data = in.readString();
        title = in.readString();
        table = in.readString();
        div = in.readString();
        rawSpan = new SpannableString(in.readString());
        span = new SpannableString(in.readString());
        level = (ArticleDataLevel) in.readSerializable();

        in.readTypedList(items, ArticleData.CREATOR);
        index = in.readParcelable(SectionIndex.class.getClassLoader());
    }

    public ArticleData(@NotNull Context context,
                       @NotNull String data,
                       @NotNull OnItemBuiltListener listener,
                       @NotNull ImageEvaluation.BuildListener evaluationListener,
                       @Nullable WikiLinkUtils.LinkListener linkListener,
                       int linkColor,
                       int lowlightColor) {
        mListener = listener;

        index = new SectionIndex(ArticleDataLevel.ZERO, null, 1);
        postTitle(context.getResources().getString(R.string.summary), ArticleDataLevel.ZERO, index);

        buildLevelItems(ArticleDataLevel.ZERO, data, new SectionIndex(ArticleDataLevel.ZERO, index, 2), evaluationListener, linkListener, linkColor, lowlightColor);
    }

    public ArticleData() {
    }

    private void buildLevelItems(@NotNull ArticleDataLevel level,
                                 @NotNull String data,
                                 @NotNull SectionIndex index,
                                 @NotNull ImageEvaluation.BuildListener evaluationListener,
                                 @Nullable WikiLinkUtils.LinkListener linkListener,
                                 int linkColor,
                                 int lowlightColor) {

        ArticleDataLevel nextLevel = level.hasNext() ? level.next() : level;

        String[] splits = data.split(nextLevel.getRegex());

        for (int i = 0; i < splits.length; i++) {
            String split = splits[i];

            if (i == 0 || !level.hasNext()) {

                Matcher titleMatcher = TITLE_PATTERN.matcher(split);
                if (titleMatcher.find()) {
                    String title = titleMatcher.group(0);
                    title = title.replaceAll("<(.*?)>|" + NO_BRACKETS, "");
                    postTitle(title, level, index);

                    split = split.substring(titleMatcher.end(0));
                }

                List<Pair<String, Boolean>> tables = WikiTextUtils.removeTags("table", split);
                for (Pair<String, Boolean> table : tables) {
                    split = table.first;

                    if (table.second) {
                        postTable(split, linkColor);
                    } else {

                        List<Pair<String, Boolean>> divs = WikiTextUtils.removeTagsSpecial("<div>", "</div>", split, "<div (.*?)>");
                        for (Pair<String, Boolean> div : divs) {
                            split = div.first;

                            if (div.second) {
                                postDiv(split, evaluationListener, lowlightColor, linkListener);
                            } else {


                                String[] paragraphs = split.split("</p>");
                                for (String paragraph : paragraphs) {
                                    postData(paragraph.replaceAll("<p>", ""), level, index, linkColor, linkListener);
                                }
                            }
                        }
                    }
                }
            } else {
                buildLevelItems(nextLevel, split, new SectionIndex(nextLevel, index, i), evaluationListener, linkListener, linkColor, lowlightColor);
            }
        }
    }


    private void postTitle(@NotNull String title, @NotNull ArticleDataLevel level, @NotNull SectionIndex index) {

        ArticleData titleData = new ArticleData();
        titleData.title = title;
        titleData.level = level;
        titleData.index = index;

        mListener.onItemBuilt(titleData);
    }

    private void postData(@NotNull String span,
                          @NotNull ArticleDataLevel level,
                          @NotNull SectionIndex index,
                          int linkColor,
                          WikiLinkUtils.LinkListener linkListener) {

        ArticleData articleData = new ArticleData();
        articleData.data = span;
        articleData.level = level;
        articleData.index = index;
        articleData.span = new SpannableString(Html.fromHtml(applySpanModifications(span)));

        if (TextUtils.isEmpty(articleData.span)) return;

        WikiTextUtils.applySpannableLinkStyle(articleData.span, linkColor, linkListener);

        mListener.onItemBuilt(articleData);
    }

    private void postTable(@NotNull String span, int linkColor) {
        span = applyTableModification(span, linkColor);
        if (TextUtils.isEmpty(span)) return;

        ArticleData embedData = new ArticleData();
        embedData.table = span;

        mListener.onItemBuilt(embedData);
    }

    private void postDiv(@NotNull String div, @NotNull ImageEvaluation.BuildListener evaluationListener,
                         int linkColor,
                         WikiLinkUtils.LinkListener linkListener) {
        div = div.replaceAll("src=\"//", "src=\"http://");

        String divClass = getDivClass(div);

        if (!TextUtils.isEmpty(divClass) && divClass.contains("thumb")) {
            final ImageEvaluation image = ImageEvaluation.createFromDiv(div);


            if (image != null) {
                WikiTextUtils.applySpannableLinkStyle(image.getDescription(), linkColor, linkListener);
                ArticleData divData = new ArticleData();
                divData.div = image.getOriginalName();

                evaluationListener.onEvaluationBuilt(image);
                mListener.onItemBuilt(divData);
            }
        }
    }

    @NotNull
    private static String applySpanModifications(@NotNull String span) {

        // TODO: Preserve citations
        span = span.replaceAll(NO_BRACKETS, "");
        span = span.replace("<li>", "\u2022 ");
        span = span.replace("</li>", "<br/>");

        return span;
    }

    public String getURL() {
        if (TextUtils.isEmpty(title) || level.equals(ArticleDataLevel.ZERO)) return "";

        return Uri.encode(title.replace(" ", "_"));
    }

    @Nullable
    private static String getDivClass(String div) {
        Matcher matcher = CLASS_PATTERN.matcher(div);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    @NotNull
    private static String applyTableModification(@NotNull String table, int linkColor) {

        String color = String.format("#%06X", (0xFFFFFF & linkColor));

        StringBuilder style = new StringBuilder();
        style.append(" border=\"1\"");
        style.append(" cellpadding=\"4\"");
        style.append(" cellspacing=\"0\"");
        style.append(" style=\"float: middle; margin: 0 auto; border:1px solid #ccc; font-size:110%;\"");

        Matcher tableMatcher = Pattern.compile("<table(.*?)>").matcher(table);
        if (tableMatcher.find()) {
            Matcher classMatcher = Pattern.compile("class=\"(.*?)\"").matcher(tableMatcher.group(1));
            if (classMatcher.find()) {
                if (isBannedTable(classMatcher.group(1))) {
                    return "";
                }
            }
            table = table.substring(0, tableMatcher.start(1)) +
                    style.toString() +
                    table.substring(tableMatcher.end(1), table.length());
        }

        String head = "<head><style>" +
                "@font-face {font-family: 'aleo';src: url('file:///android_asset/fonts/aleo.otf');}body {font-family: 'aleo';}" +
                "a:link { color: #222;}" +
                "a:visited { color: #222;}" +
                "</style></head>";

        return "<html>" + head + "<body><center>" + table + "</center></body></html>";
    }

    private static boolean isBannedTable(String tableClass) {
        return tableClass.contains("metadata");
    }

}
