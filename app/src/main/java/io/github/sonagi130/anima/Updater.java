package io.github.sonagi130.anima;

import android.content.Context;
import android.os.Environment;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Fatum Echo 热更新器
 * 启动时检查远程 version.json，版本号比本地大 → 下载 web.zip 解压到 filesDir/anima_www/
 * WebView 优先读 anima_www/，缺失回退 assets/anima/
 */
public class Updater {
    public static final String REMOTE_VERSION_URL = "https://sonagi130.github.io/anima/version.json";
    public static final String REMOTE_ZIP_URL = "https://sonagi130.github.io/anima/web.zip";

    private final Context ctx;

    public Updater(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    /** 返回本地当前版本号 */
    public int localVersion() {
        try {
            InputStream in = ctx.getAssets().open("anima/version.json");
            byte[] buf = new byte[512];
            int n = in.read(buf);
            String s = new String(buf, 0, n, "UTF-8");
            return new JSONObject(s).optInt("version", 0);
        } catch (Exception e) {
            return 0;
        }
    }

    /** 请求远程版本号，失败返回 -1 */
    public int remoteVersion() {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(REMOTE_VERSION_URL).openConnection();
            c.setConnectTimeout(6000);
            c.setReadTimeout(6000);
            c.setRequestMethod("GET");
            int code = c.getResponseCode();
            if (code != 200) return -1;
            BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            r.close();
            return new JSONObject(sb.toString()).optInt("version", -1);
        } catch (Exception e) {
            return -1;
        }
    }

    /** 更新入口：本地版本 < 远程版本 → 下载解压；返回是否更新成功 */
    public boolean updateIfNeeded() {
        int local = localVersion();
        int remote = remoteVersion();
        if (remote <= local) return false; // 已是最新
        try {
            File dir = webDir();
            dir.mkdirs();
            // 下载 zip
            File zip = new File(ctx.getFilesDir(), "fatum_update.zip");
            HttpURLConnection c = (HttpURLConnection) new URL(REMOTE_ZIP_URL).openConnection();
            c.setConnectTimeout(10000);
            c.setReadTimeout(30000);
            c.setRequestMethod("GET");
            int code = c.getResponseCode();
            if (code != 200) return false;
            InputStream in = new BufferedInputStream(c.getInputStream());
            FileOutputStream fo = new FileOutputStream(zip);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) fo.write(buf, 0, n);
            fo.close();
            in.close();
            // 解压覆盖（先清旧目录，避免残留）
            deleteRecursive(dir);
            dir.mkdirs();
            ZipInputStream zi = new ZipInputStream(new BufferedInputStream(new java.io.FileInputStream(zip)));
            ZipEntry e;
            while ((e = zi.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                File out = new File(dir, e.getName());
                File parent = out.getParentFile();
                if (parent != null) parent.mkdirs();
                FileOutputStream o = new FileOutputStream(out);
                byte[] b2 = new byte[8192];
                int m;
                while ((m = zi.read(b2)) > 0) o.write(b2, 0, m);
                o.close();
                zi.closeEntry();
            }
            zi.close();
            zip.delete();
            // 落盘远程版本号
            File vf = new File(dir, "version.json");
            FileOutputStream vo = new FileOutputStream(vf);
            vo.write(("{\"version\":" + remote + ",\"note\":\"hot-updated\"}").getBytes("UTF-8"));
            vo.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** WebView 读取资源的根目录：更新过用 filesDir/anima_www，否则 null 表示用 assets */
    public File webDir() {
        File dir = new File(ctx.getFilesDir(), "anima_www");
        if (dir.exists() && new File(dir, "index.html").exists()) return dir;
        return null;
    }

    /** 判断文件是否存在于更新目录 */
    public boolean hasFile(String path) {
        File d = webDir();
        return d != null && new File(d, path).exists();
    }

    /** 读取更新目录里的文件，不存在返回 null */
    public InputStream openFile(String path) {
        File d = webDir();
        if (d == null) return null;
        try {
            return new java.io.FileInputStream(new File(d, path));
        } catch (Exception e) {
            return null;
        }
    }

    private static void deleteRecursive(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) for (File k : kids) deleteRecursive(k);
        }
        f.delete();
    }
}
