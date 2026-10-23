package com.ok515.simplepdfreader;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.view.Gravity;
import android.view.View;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.DynamicColors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int PICK_PDF = 100;

    private PdfRenderer renderer;
    private ParcelFileDescriptor descriptor;
    private PdfPageView pageView;
    private LinearLayout root;
    private TextView pageInfo, fileTitle;
    private int pageIndex = 0;
    private Uri currentUri;
    private String currentName = "لم يتم فتح ملف";
    private Bitmap currentBitmap;
    private boolean readerMode = false;
    private boolean darkMode = false;
    private boolean favorite = false;

    private int primary = Color.rgb(82, 103, 217);
    private int background, surface, foreground, secondaryText, pageBackground;
    private SharedPreferences prefs;

    private int dp(float n) {
        return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("reader_prefs", MODE_PRIVATE);
        darkMode = prefs.getBoolean("dark", false);
        applyColors();

        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        getWindow().getDecorView().setSystemUiVisibility(
                darkMode ? 0 :
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR |
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        );

        buildHome();
    }

    private void applyColors() {
        if (darkMode) {
            background = Color.rgb(18, 19, 24);
            surface = Color.rgb( thirty(), thirty(), 38);
            foreground = Color.rgb(239, 240, 250);
            secondaryText = Color.rgb(177, 181, 199);
            pageBackground = Color.rgb(35, 37, 45);
        } else {
            background = Color.rgb(247, 247, 255);
            surface = Color.WHITE;
            foreground = Color.rgb(30, 32, 47);
            secondaryText = Color.rgb(105, 109, 129);
            pageBackground = Color.rgb(230, 233, 243);
        }
    }

    private int thirty() { return 30; }

    private TextView label(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private MaterialButton button(String value, boolean filled) {
        MaterialButton b = new MaterialButton(this);
        b.setText(value);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setCornerRadius(dp(18));
        b.setInsetTop(0);
        b.setInsetBottom(0);
        if (filled) {
            b.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(primary));
            b.setTextColor(Color.WHITE);
        } else {
            b.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(surface));
            b.setTextColor(foreground);
            b.setStrokeColor(android.content.res.ColorStateList.valueOf(
                    darkMode ? Color.rgb(65, 67, 82) :
                            Color.rgb(224, 226, 239)));
            b.setStrokeWidth(dp(1));
        }
        return b;
    }

    private MaterialCardView card() {
        MaterialCardView c = new MaterialCardView(this);
        c.setRadius(dp(24));
        c.setCardElevation(0);
        c.setCardBackgroundColor(surface);
        c.setStrokeWidth(dp(1));
        c.setStrokeColor(darkMode ? Color.rgb(55, 57, 70) :
                Color.rgb(232, 233, 245));
        return c;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private void buildHome() {
        readerMode = false;
        root = column();
        root.setPadding(dp(20), dp(18), dp(20), dp(16));
        root.setBackgroundColor(background);

        LinearLayout header = row();
        LinearLayout brandBox = column();

        TextView brand = label("Simple PDF", 27, foreground);
        brand.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        brandBox.addView(brand);
        brandBox.addView(label("مساحة هادئة للقراءة", 13, secondaryText));
        header.addView(brandBox, new LinearLayout.LayoutParams(0, -2, 1));

        MaterialButton theme = button(darkMode ? "☀" : "☾", false);
        header.addView(theme, new LinearLayout.LayoutParams(dp(52), dp(48)));
        theme.setOnClickListener(v -> {
            darkMode = !darkMode;
            prefs.edit().putBoolean("dark", darkMode).apply();
            applyColors();
            buildHome();
        });
        root.addView(header);

        TextView welcome = label("اقرأ كتبك براحتك", 22, foreground);
        welcome.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams welcomeP = new LinearLayout.LayoutParams(
                -1, -2);
        welcomeP.topMargin = dp(28);
        root.addView(welcome, welcomeP);
        root.addView(label("كل ملفات PDF في مكان واحد.", 14, secondaryText));

        MaterialCardView hero = card();
        LinearLayout heroInside = column();
        heroInside.setPadding(dp(20), dp(22), dp(20), dp(20));

        TextView icon = label("▤", 38, primary);
        heroInside.addView(icon);

        TextView heroTitle = label("ابدأ القراءة", 21, foreground);
        heroTitle.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams ht = new LinearLayout.LayoutParams(-1, -2);
        ht.topMargin = dp(10);
        heroInside.addView(heroTitle, ht);

        TextView heroSub = label(
                "افتح كتابًا أو مستندًا من هاتفك، واقرأه في واجهة بسيطة ومريحة.",
                14, secondaryText);
        heroSub.setGravity(Gravity.TOP);
        heroSub.setPadding(0, dp(6), 0, dp(16));
        heroInside.addView(heroSub);

        MaterialButton open = button("＋   فتح ملف PDF", true);
        heroInside.addView(open, new LinearLayout.LayoutParams(-1, dp(52)));
        open.setOnClickListener(v -> choosePdf());

        hero.addView(heroInside);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(22);
        root.addView(hero, hp);

        LinearLayout recentHeader = row();
        TextView recentTitle = label("متابعة القراءة", 18, foreground);
        recentTitle.setTypeface(Typeface.DEFAULT_BOLD);
        recentHeader.addView(recentTitle, new LinearLayout.LayoutParams(0, -2, 1));
        MaterialButton browse = button("فتح ملف", false);
        recentHeader.addView(browse);
        browse.setOnClickListener(v -> choosePdf());

        LinearLayout.LayoutParams rh = new LinearLayout.LayoutParams(-1, -2);
        rh.topMargin = dp(26);
        root.addView(recentHeader, rh);

        String lastUri = prefs.getString("last_uri", null);
        String lastName = prefs.getString("last_name", null);
        if (lastUri != null && lastName != null) {
            MaterialCardView recent = card();
            LinearLayout inside = row();
            inside.setPadding(dp(14), dp(12), dp(14), dp(12));
            TextView docIcon = label("▤", 28, primary);
            inside.addView(docIcon, new LinearLayout.LayoutParams(dp(44), dp(48)));

            LinearLayout meta = column();
            meta.addView(label(lastName, 14, foreground));
            meta.addView(label("آخر ملف مفتوح", 12, secondaryText));
            inside.addView(meta, new LinearLayout.LayoutParams(0, -2, 1));

            MaterialButton resume = button("فتح", true);
            inside.addView(resume);
            resume.setOnClickListener(v -> {
                try {
                    openPdf(Uri.parse(lastUri), lastName);
                } catch (Exception e) {
                    Toast.makeText(this, "اختر الملف من جديد", Toast.LENGTH_SHORT).show();
                    choosePdf();
                }
            });
            recent.addView(inside);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
            rp.topMargin = dp(10);
            root.addView(recent, rp);
        } else {
            TextView empty = label("مازال ما فتحت حتى ملف. ابدأ بفتح PDF.", 13,
                    secondaryText);
            LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-1, -2);
            ep.topMargin = dp(12);
            root.addView(empty, ep);
        }

        TextView privacy = label("ملفاتك تبقى على جهازك", 12, secondaryText);
        privacy.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, dp(40));
        pp.topMargin = dp(12);
        root.addView(privacy, pp);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(root);
        setContentView(scroll);
    }

    private void choosePdf() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/pdf");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, PICK_PDF);
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK_PDF || result != RESULT_OK ||
                data == null || data.getData() == null) return;

        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}
        String name = uri.getLastPathSegment();
        if (name == null || name.trim().isEmpty()) name = "ملف PDF";
        openPdf(uri, name);
    }

    private void openPdf(Uri uri, String name) {
        try {
            closePdf();
            currentUri = uri;
            currentName = name;

            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) throw new Exception("تعذر قراءة الملف");

            File f = new File(getCacheDir(), "reader-current.pdf");
            try (FileOutputStream out = new FileOutputStream(f)) {
                byte[] buffer = new byte[16384];
                int n;
                while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            }
            in.close();

            descriptor = ParcelFileDescriptor.open(f,
                    ParcelFileDescriptor.MODE_READ_ONLY);
            renderer = new PdfRenderer(descriptor);
            pageIndex = Math.max(0, Math.min(
                    prefs.getInt("page_" + uri.toString(), 0),
                    renderer.getPageCount() - 1));
            favorite = prefs.getBoolean("fav_" + uri.toString(), false);
            prefs.edit()
                    .putString("last_uri", uri.toString())
                    .putString("last_name", name)
                    .apply();
            buildReader();
            showPage(pageIndex);
        } catch (OutOfMemoryError e) {
            Toast.makeText(this, "الملف كبير جدًا للذاكرة المتاحة",
                    Toast.LENGTH_LONG).show();
            buildHome();
        } catch (Exception e) {
            Toast.makeText(this, "تعذر فتح الملف: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            buildHome();
        }
    }

    private void buildReader() {
        readerMode = true;
        root = column();
        root.setPadding(dp(12), dp(8), dp(12), dp(8));
        root.setBackgroundColor(background);

        LinearLayout top = row();
        MaterialButton back = button("‹", false);
        top.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        back.setOnClickListener(v -> {
            savePage();
            buildHome();
        });

        fileTitle = label(currentName, 14, foreground);
        fileTitle.setMaxLines(1);
        fileTitle.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        fileTitle.setPadding(dp(10), 0, dp(10), 0);
        top.addView(fileTitle, new LinearLayout.LayoutParams(0, -2, 1));

        MaterialButton fav = button(favorite ? "★" : "☆", false);
        top.addView(fav, new LinearLayout.LayoutParams(dp(48), dp(48)));
        fav.setOnClickListener(v -> {
            favorite = !favorite;
            prefs.edit().putBoolean("fav_" + currentUri.toString(), favorite).apply();
            fav.setText(favorite ? "★" : "☆");
            Toast.makeText(this, favorite ? "أُضيف للمفضلة" : "أُزيل من المفضلة",
                    Toast.LENGTH_SHORT).show();
        });
        root.addView(top);

        pageView = new PdfPageView();
        pageView.setBackgroundColor(pageBackground);
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(-1, 0, 1);
        vp.setMargins(0, dp(8), 0, dp(8));
        root.addView(pageView, vp);

        LinearLayout nav = row();
        MaterialButton prev = button("السابق", false);
        MaterialButton next = button("التالي", true);
        pageInfo = label("— / —", 14, foreground);
        pageInfo.setGravity(Gravity.CENTER);

        nav.addView(prev, new LinearLayout.LayoutParams(0, dp(48), 1));
        nav.addView(pageInfo, new LinearLayout.LayoutParams(0, dp(48), 1));
        nav.addView(next, new LinearLayout.LayoutParams(0, dp(48), 1));
        root.addView(nav);

        LinearLayout zoom = row();
        MaterialButton minus = button("−", false);
        MaterialButton fit = button("ملاءمة الصفحة", false);
        MaterialButton plus = button("+", false);
        zoom.addView(minus, new LinearLayout.LayoutParams(0, dp(46), 1));
        zoom.addView(fit, new LinearLayout.LayoutParams(0, dp(46), 2));
        zoom.addView(plus, new LinearLayout.LayoutParams(0, dp(46), 1));
        root.addView(zoom);

        prev.setOnClickListener(v -> showPage(pageIndex - 1));
        next.setOnClickListener(v -> showPage(pageIndex + 1));
        minus.setOnClickListener(v -> pageView.changeZoom(0.8f));
        plus.setOnClickListener(v -> pageView.changeZoom(1.25f));
        fit.setOnClickListener(v -> pageView.resetZoom());

        setContentView(root);
    }

    private void showPage(int index) {
        if (renderer == null || index < 0 || index >= renderer.getPageCount()) return;

        try (PdfRenderer.Page page = renderer.openPage(index)) {
            int screenW = getResources().getDisplayMetrics().widthPixels;
            int screenH = getResources().getDisplayMetrics().heightPixels;
            int width = Math.max(900, Math.min(1600, Math.max(screenW, screenH)));
            int height = Math.max(1,
                    (int)((long)width * page.getHeight() / page.getWidth()));

            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bmp.eraseColor(Color.WHITE);
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            if (currentBitmap != null && !currentBitmap.isRecycled()) {
                currentBitmap.recycle();
            }
            currentBitmap = bmp;
            pageIndex = index;

            if (pageView != null) pageView.setBitmap(bmp);
            if (pageInfo != null) {
                pageInfo.setText((index + 1) + " / " + renderer.getPageCount());
            }
            savePage();
        } catch (OutOfMemoryError e) {
            Toast.makeText(this, "الصفحة أكبر من الذاكرة المتاحة",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "تعذر عرض الصفحة", Toast.LENGTH_SHORT).show();
        }
    }

    private void savePage() {
        if (currentUri != null) {
            prefs.edit().putInt("page_" + currentUri.toString(), pageIndex).apply();
        }
    }

    private void closePdf() {
        if (renderer != null) {
            try { renderer.close(); } catch (Exception ignored) {}
            renderer = null;
        }
        if (descriptor != null) {
            try { descriptor.close(); } catch (Exception ignored) {}
            descriptor = null;
        }
        if (currentBitmap != null && !currentBitmap.isRecycled()) {
            currentBitmap.recycle();
            currentBitmap = null;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyColors();
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);

        if (readerMode) {
            buildReader();
            if (currentBitmap != null && !currentBitmap.isRecycled()) {
                pageView.setBitmap(currentBitmap);
            }
            if (pageInfo != null && renderer != null) {
                pageInfo.setText((pageIndex + 1) + " / " + renderer.getPageCount());
            }
        } else {
            buildHome();
        }
    }

    @Override
    public void onBackPressed() {
        if (readerMode) {
            savePage();
            buildHome();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        savePage();
        closePdf();
        super.onDestroy();
    }

    private class PdfPageView extends View {
        private Bitmap bitmap;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG |
                Paint.FILTER_BITMAP_FLAG);
        private final ScaleGestureDetector detector;
        private float zoom = 1f;
        private float offsetX, offsetY, lastX, lastY;

        PdfPageView() {
            super(MainActivity.this);
            detector = new ScaleGestureDetector(MainActivity.this,
                    new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector d) {
                    zoom = Math.max(1f, Math.min(6f,
                            zoom * d.getScaleFactor()));
                    invalidate();
                    return true;
                }
            });
        }

        void setBitmap(Bitmap b) {
            bitmap = b;
            resetZoom();
        }

        void changeZoom(float factor) {
            zoom = Math.max(1f, Math.min(6f, zoom * factor));
            if (zoom == 1f) offsetX = offsetY = 0;
            invalidate();
        }

        void resetZoom() {
            zoom = 1f;
            offsetX = offsetY = 0;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(pageBackground);
            if (bitmap == null || bitmap.isRecycled() ||
                    getWidth() <= 0 || getHeight() <= 0) return;

            float fit = Math.min((float)getWidth() / bitmap.getWidth(),
                    (float)getHeight() / bitmap.getHeight());
            float scale = fit * zoom;
            float left = (getWidth() - bitmap.getWidth() * scale) / 2f + offsetX;
            float top = (getHeight() - bitmap.getHeight() * scale) / 2f + offsetY;

            canvas.save();
            canvas.translate(left, top);
            canvas.scale(scale, scale);
            canvas.drawBitmap(bitmap, 0, 0, paint);
            canvas.restore();
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            detector.onTouchEvent(e);
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = e.getX();
                    lastY = e.getY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (e.getPointerCount() == 1 &&
                            !detector.isInProgress() && zoom > 1f) {
                        offsetX += e.getX() - lastX;
                        offsetY += e.getY() - lastY;
                        invalidate();
                    }
                    lastX = e.getX();
                    lastY = e.getY();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    performClick();
                    return true;
            }
            return true;
        }

        @Override
        public boolean performClick() {
            super.performClick();
            return true;
        }
    }
}
