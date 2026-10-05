package io.github.sonagi130.anima;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/** 无障碍服务：读屏（当前界面文本）+ 模拟点击/滑动 —— 小工的腿 */
public class HearthAccess extends AccessibilityService {
    private static HearthAccess INSTANCE = null;
    public static String lastScreen = "";

    public static HearthAccess get() { return INSTANCE; }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        INSTANCE = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || e.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            lastScreen = dump(getRootInActiveWindow());
        }
    }

    @Override
    public void onInterrupt() {}

    @Override
    public boolean onUnbind(Intent intent) {
        INSTANCE = null;
        return super.onUnbind(intent);
    }

    /** 把节点树 dump 成文本，网页可读 */
    private String dump(AccessibilityNodeInfo root) {
        if (root == null) return "";
        StringBuilder sb = new StringBuilder();
        walk(root, sb, 0);
        return sb.toString();
    }

    private void walk(AccessibilityNodeInfo n, StringBuilder sb, int depth) {
        if (n == null) return;
        CharSequence t = n.getText();
        CharSequence d = n.getContentDescription();
        if ((t != null && t.length() > 0) || (d != null && d.length() > 0)) {
            String s = (t != null ? t : d).toString().trim();
            if (!s.isEmpty()) {
                if (sb.length() > 0) sb.append('\n');
                sb.append(s);
            }
        }
        for (int i = 0; i < n.getChildCount(); i++) {
            walk(n.getChild(i), sb, depth + 1);
        }
    }

    /** 点击某个文本所在的节点 */
    public boolean tapText(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        AccessibilityNodeInfo target = findText(root, text);
        if (target == null) return false;
        return target.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    private AccessibilityNodeInfo findText(AccessibilityNodeInfo n, String text) {
        if (n == null) return null;
        CharSequence t = n.getText();
        if (t != null && t.toString().contains(text)) return n;
        for (int i = 0; i < n.getChildCount(); i++) {
            AccessibilityNodeInfo r = findText(n.getChild(i), text);
            if (r != null) return r;
        }
        return null;
    }

    /** 模拟点击坐标 */
    public boolean tapAt(float x, float y) {
        Path p = new Path();
        p.moveTo(x, y);
        GestureDescription g = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(p, 0, 80))
                .build();
        return dispatchGesture(g, null, null);
    }

    /** 模拟滑动 */
    public boolean swipe(float x1, float y1, float x2, float y2) {
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        GestureDescription g = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(p, 0, 200))
                .build();
        return dispatchGesture(g, null, null);
    }
}