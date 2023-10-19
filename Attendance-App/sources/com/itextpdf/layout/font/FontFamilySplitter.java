package com.itextpdf.layout.font;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Deprecated
public final class FontFamilySplitter {
    private static final Pattern FONT_FAMILY_PATTERN = Pattern.compile("^ *([\\w-]+) *$");
    private static final Pattern FONT_FAMILY_PATTERN_QUOTED = Pattern.compile("^ *(('[\\w -]+')|(\"[\\w -]+\")) *$");
    private static final Pattern FONT_FAMILY_PATTERN_QUOTED_SELECT = Pattern.compile("[\\w-]+( +[\\w-]+)*");

    public static List<String> splitFontFamily(String fontFamilies) {
        if (fontFamilies == null) {
            return null;
        }
        String[] names = fontFamilies.split(",");
        List<String> result = new ArrayList<>(names.length);
        for (String name : names) {
            if (FONT_FAMILY_PATTERN.matcher(name).matches()) {
                result.add(name.trim());
            } else if (FONT_FAMILY_PATTERN_QUOTED.matcher(name).matches()) {
                Matcher selectMatcher = FONT_FAMILY_PATTERN_QUOTED_SELECT.matcher(name);
                if (selectMatcher.find()) {
                    result.add(selectMatcher.group());
                }
            }
        }
        return result;
    }

    public static String removeQuotes(String fontFamily) {
        Matcher selectMatcher = FONT_FAMILY_PATTERN_QUOTED_SELECT.matcher(fontFamily);
        if (selectMatcher.find()) {
            return selectMatcher.group();
        }
        return null;
    }
}
