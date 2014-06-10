package com.peteschmitz.android.pocketwikipedia.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.peteschmitz.android.pocketwikipedia.R;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Pete Schmitz on 4/1/2014.
 */
public class WikiTextUtils {

    @NotNull
    public static String getMergedSplit(String text, String regex, int max){
        String[] sentences= text.split(regex, max + 1);

        if (sentences.length < max + 1){
            return text;
        }

        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < sentences.length - 1; i++){
            stringBuilder.append(sentences[i]);
        }

        return stringBuilder.toString();
    }

    @NotNull
    public static String removeOmissions(String text, String[] omissions){
        for (String omission : omissions){
            text = text.replaceAll(omission, "");
        }

        return text;
    }

    private static final String EDIT_REGEX = "\\[(.*?)edit(.*?)\\]";

    @NotNull
    public static String removeEdits(@NotNull String text){
        return text.replaceAll(EDIT_REGEX, "");
    }

    private static final String CITATION_REGEX = "\\[(.*?)\\]";

    @NotNull
    public static String removeCitations(@NotNull String text){
        return text.replaceAll(CITATION_REGEX, "");
    }

    private static final String IMAGE_REGEX = "<img(.*?)>";

    @NotNull
    public static String removeImages(@NotNull String text){
        return text.replaceAll(IMAGE_REGEX, "");
    }

    public static void applySpannableLinkStyle(@NotNull TextView textView, final int color){
        applySpannableLinkStyle(textView, color, null);
    }

    public static void applySpannableLinkStyle(@NotNull TextView textView, final int color, @Nullable WikiLinkUtils.LinkListener linkListener){

        if (TextUtils.isEmpty(textView.getText())) return;
        SpannableString span = new SpannableString(textView.getText());

        applySpannableLinkStyle(span, color, linkListener);

        textView.setText(span);
    }

    public static void applySpannableLinkStyle(@NotNull SpannableString span, final int color, @Nullable final WikiLinkUtils.LinkListener linkListener){

        for (final URLSpan urlSpan : span.getSpans(0, span.length(), URLSpan.class)){
            int startSpan = span.getSpanStart(urlSpan);
            int endSpan = span.getSpanEnd(urlSpan);
            ClickableSpan spanStyle = new ClickableSpan(){
                @Override
                public void updateDrawState(@NotNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false);
                    ds.setColor(color);
                }

                @Override
                public void onClick(View widget) {
                    if (linkListener != null){
                        linkListener.onClick(urlSpan.getURL());
                    }
                }
            };

            span.removeSpan(urlSpan);
            span.setSpan(spanStyle, startSpan, endSpan, 0);
        }
    }

    public static void applySpannableLinkStyle(@NotNull SpannableString span, @NotNull ClickableSpan clickableSpan){

        for (URLSpan urlSpan : span.getSpans(0, span.length(), URLSpan.class)){
            int startSpan = span.getSpanStart(urlSpan);
            int endSpan = span.getSpanEnd(urlSpan);
            span.setSpan(clickableSpan, startSpan, endSpan, 0);
        }
    }

    @NotNull
    public static Pair<String, String> removeVCard(@NotNull String span){

        String[] split = span.split("<table class=\"infobox(.*?)>", 2);

        if (split.length > 1){
            String[] splitCard = split[1].split("</table>");

            if (splitCard.length > 0){
                return new Pair<String, String>(splitCard[1], splitCard[0]);
            } else {
                return new Pair<String, String>(span, null);
            }
        } else {
            return new Pair<String, String>(span, null);
        }
    }

    public static void copyToClipboard(@NotNull String text, @NotNull Context context, boolean showToast){
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData data = ClipData.newPlainText("Pocket Wikipedia", text);
        clipboard.setPrimaryClip(data);

        if (showToast){
            Toast.makeText(
                    context,
                    context.getResources().getString(R.string.copied_to_clipboard) + ":" + text,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    /**
     * Remove supplied {@code tag} from {@code target} and returns the modified {@code target} string.
     *
     * @param tag
     *      HTML tag to remove. Shouldn't include less-than/greater-than characters, e.g. just pass "table"
     * @param target
     *      Target text to remove instances of the {@code tag} from.
     * @return
     *      List of splits, presented with a paired bool to indicate if the split was a tag removal
     */
    public static List<Pair<String, Boolean>> removeTags(String tag, String target){

        // Get opening tag indices
        Matcher matcher = Pattern.compile("<" + tag + "(.*?)>").matcher(target);
        List<IntPair> tags = new ArrayList<IntPair>();
        while (matcher.find()){
            tags.add(new IntPair(matcher.start(), matcher.end(), true));
        }

        // Get closing tag indices
        matcher = Pattern.compile("</" + tag + ">").matcher(target);
        while (matcher.find()){
            tags.add(new IntPair(matcher.start(), matcher.end(), false));
        }

        // Return if no tags found
        if (tags.isEmpty()) {
            //noinspection unchecked
            return Arrays.asList(new Pair<String, Boolean>(target, false));
        }

        Collections.sort(tags, new Comparator<IntPair>() {
            @Override
            public int compare(IntPair o1, IntPair o2) {
                if (o1.start == o2.start) return 0;
                return o1.start < o2.start ? -1 : 1;
            }
        });

        List<Pair<String, Boolean>> sections = new ArrayList<Pair<String, Boolean>>();
        int tier = 0;
        IntPair lastCloseIndex = null;
        IntPair lastOpenIndex = null;
        int lastAddedIndex = 0;
        for (int i = 0; i < tags.size(); i++){
            IntPair t = tags.get(i);

            if (t.open){
                if (tier == 0){

                    if (!t.first){
                        continue;
                    }

                    lastOpenIndex = t;
                    String section = target.substring(lastCloseIndex == null ? 0 : lastCloseIndex.end, lastOpenIndex.start);
                    sections.add(new Pair<String, Boolean>(section, false));
                    lastAddedIndex = lastOpenIndex.start;
                }

                ++ tier;
            } else if (tier > 0){
                -- tier;

                if (tier == 0){
                    lastCloseIndex = t;

                    if (lastOpenIndex != null){
                        String section = target.substring(lastOpenIndex.start, lastCloseIndex.end);
                        sections.add(new Pair<String, Boolean>(section, true));
                        lastAddedIndex = lastCloseIndex.end;
                    }

                    if (i == tags.size() - 1){
                        String section = target.substring(lastCloseIndex.end, target.length());
                        sections.add(new Pair<String, Boolean>(section, false));
                        lastAddedIndex = target.length();
                    }
                }
            }
        }

        if (lastAddedIndex != target.length()){
            String section = target.substring(lastAddedIndex, target.length());
            sections.add(new Pair<String, Boolean>(section, false));
        }

        return sections;
    }

    public static List<Pair<String, Boolean>> removeTagsSpecial(String opening, String closing, String target, String first){

        boolean enforceFirst = !TextUtils.isEmpty(first);

        // Get opening tag indices
        Matcher matcher = Pattern.compile(opening).matcher(target);
        List<IntPair> tags = new ArrayList<IntPair>();
        while (matcher.find()){
            tags.add(new IntPair(matcher.start(), matcher.end(), true, !enforceFirst));
        }

        // Get closing tag indices
        matcher = Pattern.compile(closing).matcher(target);
        while (matcher.find()){
            tags.add(new IntPair(matcher.start(), matcher.end(), false, false));
        }

        // Get starter tags
        if (enforceFirst){
            matcher = Pattern.compile(first).matcher(target);
            while (matcher.find()){
                tags.add(new IntPair(matcher.start(), matcher.end(), true, true));
            }
        }

        // Return if no tags found
        if (tags.isEmpty()){
            //noinspection unchecked
            return Arrays.asList(new Pair<String, Boolean>(target, false));
        }

        Collections.sort(tags, new Comparator<IntPair>() {
            @Override
            public int compare(IntPair o1, IntPair o2) {
                if (o1.start == o2.start) return 0;
                return o1.start < o2.start ? -1 : 1;
            }
        });

        List<Pair<String, Boolean>> sections = new ArrayList<Pair<String, Boolean>>();
        int tier = 0;
        IntPair lastCloseIndex = null;
        IntPair lastOpenIndex = null;
        int lastAddedIndex = 0;
        for (int i = 0; i < tags.size(); i++){
            IntPair t = tags.get(i);

            if (t.open){
                if (tier == 0){

                    if (!t.first){
                        continue;
                    }

                    lastOpenIndex = t;

                    String section = target.substring(lastCloseIndex == null ? 0 : lastCloseIndex.end, lastOpenIndex.start);
                    sections.add(new Pair<String, Boolean>(section, false));
                    lastAddedIndex = lastOpenIndex.start;
                }

                ++ tier;
            } else if (tier > 0){
                -- tier;

                if (tier == 0){
                    lastCloseIndex = t;

                    if (lastOpenIndex != null){
                        String section = target.substring(lastOpenIndex.start, lastCloseIndex.end);
                        sections.add(new Pair<String, Boolean>(section, true));
                        lastAddedIndex = lastCloseIndex.end;
                    }

                    if (i == tags.size() - 1){
                        String section = target.substring(lastCloseIndex.end, target.length());
                        sections.add(new Pair<String, Boolean>(section, false));
                        lastAddedIndex = target.length();
                    }
                }
            }
        }

        if (lastAddedIndex != target.length()){
            String section = target.substring(lastAddedIndex, target.length());
            sections.add(new Pair<String, Boolean>(section, false));
        }


        return sections;
    }

    private static class IntPair{

        int start;
        int end;
        boolean open;
        boolean first;

        public IntPair(int start, int end, boolean isOpen){
            this.start = start;
            this.end = end;
            this.open = isOpen;
            this.first = true;
        }

        public IntPair(int start, int end, boolean isOpen, boolean first){
            this.start = start;
            this.end = end;
            this.open = isOpen;
            this.first = first;
        }
    }
}
