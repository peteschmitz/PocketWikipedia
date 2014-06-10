package com.peteschmitz.android.pocketwikipedia.language.abs;

import com.peteschmitz.android.pocketwikipedia.data.FrontPageSectionData;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by Pete Schmitz on 3/25/14.
 */
public interface Language {
    FrontPageSectionData[] buildFrontPageSections(String text);
    String getMainPage();
    String getSubDomain();
    void postModifications(@NotNull List<FrontPageSectionData> sections);
}
