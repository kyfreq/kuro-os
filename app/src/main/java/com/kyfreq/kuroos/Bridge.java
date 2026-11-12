package com.kyfreq.kuroos;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Base64;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Everything the page can ask the phone for, exposed to JavaScript as window.Android. */
public class Bridge {
    private final Activity activity;
    private final WebView web;

    Bridge(Activity activity, WebView web) {
        this.activity = activity;
        this.web = web;
    }

    // ---------- apps ----------

    /** Lists installed apps with icons in the background, then calls kuroApps(list) in the page. */
    @JavascriptInterface
    public void loadApps() {
        new Thread(() -> {
            PackageManager pm = activity.getPackageManager();
            Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> found = pm.queryIntentActivities(main, 0);
            List<JSONObject> apps = new ArrayList<>();
            for (ResolveInfo r : found) {
                String pkg = r.activityInfo.packageName;
                if (pkg.equals(activity.getPackageName())) continue;
                try {
                    JSONObject o = new JSONObject();
                    o.put("name", r.loadLabel(pm).toString());
                    o.put("pkg", pkg);
                    o.put("icon", iconData(r.loadIcon(pm)));
                    apps.add(o);
                } catch (Exception ignored) { }
            }
            Collections.sort(apps, (a, b) -> a.optString("name").compareToIgnoreCase(b.optString("name")));
            JSONArray arr = new JSONArray();
            for (JSONObject o : apps) arr.put(o);
            String js = "window.kuroApps&&kuroApps(" + arr + ")";
            activity.runOnUiThread(() -> web.evaluateJavascript(js, null));
        }).start();
    }

    private String iconData(Drawable d) {
        int size = 72;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        d.setBounds(0, 0, size, size);
        d.draw(c);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        return "data:image/png;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
    }

    @JavascriptInterface
    public boolean launch(String pkg) {
        Intent i = activity.getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) return false;
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.runOnUiThread(() -> activity.startActivity(i));
        return true;
    }

    // ---------- music (Spotify or any player) ----------

    private MediaController controller() {
        MediaSessionManager m = (MediaSessionManager) activity.getSystemService(Context.MEDIA_SESSION_SERVICE);
        List<MediaController> list = m.getActiveSessions(new ComponentName(activity, NotifListener.class));
        if (list == null || list.isEmpty()) return null;
        for (MediaController c : list) {
            PlaybackState s = c.getPlaybackState();
            if (s != null && s.getState() == PlaybackState.STATE_PLAYING) return c;
        }
        return list.get(0);
    }

    /** Returns JSON: {title, artist, app, dur, pos, playing} or {needs:true} or {none:true}. */
    @JavascriptInterface
    public String getMedia() {
        try {
            MediaController c = controller();
            JSONObject o = new JSONObject();
            if (c == null) return o.put("none", true).toString();
            MediaMetadata md = c.getMetadata();
            PlaybackState ps = c.getPlaybackState();
            boolean playing = ps != null && ps.getState() == PlaybackState.STATE_PLAYING;
            long pos = 0;
            if (ps != null) {
                pos = ps.getPosition();
                if (playing) pos += (long) ((SystemClock.elapsedRealtime() - ps.getLastPositionUpdateTime()) * ps.getPlaybackSpeed());
            }
            String app = c.getPackageName();
            if (app.contains("spotify")) app = "Spotify";
            else if (app.contains("youtube")) app = "YT";
            else app = "MUSIC";
            o.put("title", md != null ? md.getString(MediaMetadata.METADATA_KEY_TITLE) : "");
            o.put("artist", md != null ? md.getString(MediaMetadata.METADATA_KEY_ARTIST) : "");
            o.put("dur", md != null ? md.getLong(MediaMetadata.METADATA_KEY_DURATION) : 0);
            o.put("pos", Math.max(0, pos));
            o.put("playing", playing);
            o.put("app", app);
            return o.toString();
        } catch (SecurityException e) {
            return "{\"needs\":true}";
        } catch (Exception e) {
            return "{\"none\":true}";
        }
    }

    @JavascriptInterface
    public void mediaCmd(String cmd) {
        MediaController c = null;
        try { c = controller(); } catch (Exception ignored) { }
        if (c == null) {
            // Nothing is playing yet: send a media key so the last player (usually Spotify) wakes up.
            int key = cmd.equals("next") ? KeyEvent.KEYCODE_MEDIA_NEXT
                    : cmd.equals("prev") ? KeyEvent.KEYCODE_MEDIA_PREVIOUS : KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
            AudioManager am = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
            am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, key));
            am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, key));
            return;
        }
        MediaController.TransportControls t = c.getTransportControls();
        PlaybackState s = c.getPlaybackState();
        switch (cmd) {
            case "next": t.skipToNext(); break;
            case "prev": t.skipToPrevious(); break;
            default:
                if (s != null && s.getState() == PlaybackState.STATE_PLAYING) t.pause(); else t.play();
        }
    }

    @JavascriptInterface
    public void openNotifAccess() {
        Intent i = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.runOnUiThread(() -> activity.startActivity(i));
    }

    // ---------- phone ----------

    /** Returns "level,charging" e.g. "21,0". */
    @JavascriptInterface
    public String getBattery() {
        Intent b = activity.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (b == null) return "100,0";
        int level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, 100);
        int scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int status = b.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
        return Math.round(level * 100f / scale) + "," + (charging ? 1 : 0);
    }

    @JavascriptInterface
    public void vibrate(int ms) {
        Vibrator v = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) v.vibrate(VibrationEffect.createOneShot(Math.max(1, ms), VibrationEffect.DEFAULT_AMPLITUDE));
    }

    @JavascriptInterface
    public void openSettings() {
        Intent i = new Intent(Settings.ACTION_SETTINGS);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.runOnUiThread(() -> activity.startActivity(i));
    }
}
