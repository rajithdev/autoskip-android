package com.autoskip.app;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Decides whether a piece of on-screen text is YouTube's "skip ad" button.
 * Pure Java (no Android classes) so it can be checked on a desktop JVM.
 */
final class SkipMatcher {

    static final String YOUTUBE_PACKAGE = "com.google.android.youtube";

    /** Resource ids YouTube has used for the skip button. Language independent. */
    static final String[] VIEW_IDS = {
        YOUTUBE_PACKAGE + ":id/skip_ad_button",
        YOUTUBE_PACKAGE + ":id/modern_skip_ad_text",
        YOUTUBE_PACKAGE + ":id/skip_ad_button_text",
    };

    /** Substrings to search the screen for; hits are then checked with {@link #isSkipLabel}. */
    static final String[] SEARCH_TERMS = {"Skip"};

    /**
     * The whole label must equal one of these (after normalising), so a video
     * titled "Don't skip this" or a "Skip navigation" link is never tapped.
     * Add translated labels here if your YouTube is not in English.
     */
    private static final Set<String> LABELS = new HashSet<>(Arrays.asList(
        "skip",
        "skip ad",
        "skip ads",
        "skip this ad"
    ));

    private SkipMatcher() {}

    static boolean isSkipLabel(CharSequence text) {
        return text != null && LABELS.contains(normalize(text));
    }

    /** Lower-case, collapse whitespace, and drop icons/punctuation around the words. */
    static String normalize(CharSequence text) {
        StringBuilder out = new StringBuilder(text.length());
        boolean pendingSpace = false;
        for (int i = 0; i < text.length(); ) {
            int cp = Character.codePointAt(text, i);
            i += Character.charCount(cp);
            int type = Character.getType(cp);
            boolean partOfWord = Character.isLetterOrDigit(cp)
                || type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK;
            if (partOfWord) {
                if (pendingSpace && out.length() > 0) out.append(' ');
                pendingSpace = false;
                out.appendCodePoint(cp);
            } else {
                pendingSpace = true;
            }
        }
        return out.toString().toLowerCase(Locale.ROOT);
    }
}
