package com.autoskip.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Watches the YouTube app and presses the "Skip" button as soon as it is on
 * screen. It does exactly what a finger would: it cannot skip an ad before
 * YouTube offers the button, and it does nothing for unskippable ads.
 */
public class SkipService extends AccessibilityService {

    private static final String TAG = "AutoSkip";

    /** YouTube fires many events per second while playing; look at most this often. */
    private static final long SCAN_DELAY_MS = 80;
    /** After a press, wait this long before pressing again. */
    private static final long PRESS_COOLDOWN_MS = 600;
    /** A button still on screen this soon after a press means the press did not land. */
    private static final long RETRY_WINDOW_MS = 3000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable scan = new Runnable() {
        @Override
        public void run() {
            scanPending = false;
            scanNow();
        }
    };

    private boolean scanPending;
    private long lastPressAt = -RETRY_WINDOW_MS;

    /** True when AutoSkip is switched on under Settings > Accessibility. */
    static boolean isSwitchedOn(Context context) {
        String enabled = Settings.Secure.getString(
            context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        ComponentName me = new ComponentName(context, SkipService.class);
        for (String entry : enabled.split(":")) {
            if (me.equals(ComponentName.unflattenFromString(entry))) return true;
        }
        return false;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (scanPending || !Prefs.isEnabled(this)) return;
        scheduleScan(SCAN_DELAY_MS);
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        handler.removeCallbacks(scan);
        super.onDestroy();
    }

    private void scheduleScan(long delayMs) {
        handler.removeCallbacks(scan);
        scanPending = true;
        handler.postDelayed(scan, delayMs);
    }

    private void scanNow() {
        long sincePress = SystemClock.uptimeMillis() - lastPressAt;
        if (sincePress < PRESS_COOLDOWN_MS) {
            scheduleScan(PRESS_COOLDOWN_MS - sincePress);
            return;
        }

        AccessibilityNodeInfo button = null;
        try {
            for (AccessibilityNodeInfo root : youtubeRoots()) {
                button = findSkipButton(root);
                if (button != null) break;
            }
        } catch (RuntimeException e) {
            // The screen changed underneath us; the next event triggers a new scan.
            Log.w(TAG, "Scan failed", e);
        }
        if (button == null) return;

        // If we pressed a moment ago and the button is still there, the polite
        // click was ignored, so fall back to a real tap at its position.
        boolean retry = sincePress < RETRY_WINDOW_MS;
        boolean pressed = retry ? tap(button) : (click(button) || tap(button));
        if (!pressed) return;

        lastPressAt = SystemClock.uptimeMillis();
        if (!retry) {
            Prefs.addSkip(this);
            Log.i(TAG, "Skipped an ad");
        }
        // Check once more after the cooldown in case the press did not land.
        scheduleScan(PRESS_COOLDOWN_MS);
    }

    private List<AccessibilityNodeInfo> youtubeRoots() {
        List<AccessibilityNodeInfo> roots = new ArrayList<>();
        for (AccessibilityWindowInfo window : getWindows()) {
            AccessibilityNodeInfo root = window.getRoot();
            if (isYoutube(root)) roots.add(root);
        }
        if (roots.isEmpty()) {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (isYoutube(root)) roots.add(root);
        }
        return roots;
    }

    private static boolean isYoutube(AccessibilityNodeInfo node) {
        return node != null && SkipMatcher.YOUTUBE_PACKAGE.contentEquals(
            node.getPackageName() == null ? "" : node.getPackageName());
    }

    private static AccessibilityNodeInfo findSkipButton(AccessibilityNodeInfo root) {
        for (String id : SkipMatcher.VIEW_IDS) {
            for (AccessibilityNodeInfo node : root.findAccessibilityNodeInfosByViewId(id)) {
                if (isOnScreen(node)) return node;
            }
        }
        for (String term : SkipMatcher.SEARCH_TERMS) {
            for (AccessibilityNodeInfo node : root.findAccessibilityNodeInfosByText(term)) {
                boolean labelled = SkipMatcher.isSkipLabel(node.getText())
                    || SkipMatcher.isSkipLabel(node.getContentDescription());
                if (labelled && isOnScreen(node)) return node;
            }
        }
        return null;
    }

    /** The button exists (hidden) during the countdown; only count it once it shows. */
    private static boolean isOnScreen(AccessibilityNodeInfo node) {
        if (node == null || !node.isVisibleToUser() || !node.isEnabled()) return false;
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        return !bounds.isEmpty();
    }

    /**
     * Clicks the node, or a clickable parent just above it (the label often sits
     * inside the button). It stops after two levels so it can never end up
     * clicking the video player that contains the button.
     */
    private static boolean click(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo target = node;
        for (int depth = 0; target != null && depth < 3; depth++) {
            if (target.isClickable()) {
                return target.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            target = target.getParent();
        }
        return false;
    }

    /** Taps the middle of the node the way a finger would. */
    private boolean tap(AccessibilityNodeInfo node) {
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        if (bounds.isEmpty()) return false;
        Path path = new Path();
        path.moveTo(bounds.exactCenterX(), bounds.exactCenterY());
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(path, 0, 50))
            .build();
        return dispatchGesture(gesture, null, null);
    }
}
