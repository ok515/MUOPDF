package com.ok515.mefepdf;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
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
    private LinearLayout readerTop, readerNav, readerZoom;
    private boolean controlsVisible = true;
    private int pageIndex = 0;
    private Uri currentUri;
    private String currentName = tr("لم يتم فتح ملف", "No file opened", "Aucun fichier ouvert");
    private Bitmap currentBitmap;
    private boolean readerMode = false;
    private boolean darkMode = false;
    private boolean favorite = false;

    private int primary = Color.rgb(82, 103, 217);
    private int background, surface, foreground, secondaryText, pageBackground;
    private SharedPreferences prefs;

    private String tr(String ar, String en, String fr) {
        String lang = java.util.Locale.getDefault().getLanguage();
        if ("ar".equals(lang)) return ar;
        if ("fr".equals(lang)) return fr;
        return en;
    }

    private int dp(float n) {
        return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("reader_prefs", MODE_PRIVATE);
        darkMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        applyColors();
        applyDisplayCutoutMode();

        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        getWindow().getDecorView().setSystemUiVisibility(
                darkMode ? 0 :
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR |
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        );

        buildHome();
    }

    private void applyDisplayCutoutMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                params.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            } else {
                params.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }
            getWindow().setAttributes(params);
        }
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(true);
        }
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);
        getWindow().getDecorView().setSystemUiVisibility(
                darkMode ? 0 :
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR |
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        );
        root = column();
        root.setPadding(dp(20), dp(18), dp(20), dp(16));
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(background);

        MaterialButton open = button(tr("＋  فتح ملف PDF", "＋  Open PDF file", "＋  Ouvrir un fichier PDF"), true);
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(-1, dp(54));
        op.topMargin = dp(24);
        root.addView(open, op);
        open.setOnClickListener(v -> choosePdf());

        String lastUri = prefs.getString("last_uri", null);
        String lastName = prefs.getString("last_name", null);
        if (lastUri != null && lastName != null) {
            MaterialButton resume = button(tr("متابعة القراءة", "Continue reading", "Reprendre la lecture"), false);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, dp(48));
            rp.topMargin = dp(12);
            root.addView(resume, rp);
            resume.setOnClickListener(v -> openPdf(Uri.parse(lastUri), lastName));
        }

        TextView email = label("mesdh@outlook.sa", 14, primary);
        email.setGravity(Gravity.CENTER);
        email.setPaintFlags(email.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        email.setClickable(true);
        email.setFocusable(true);

        LinearLayout.LayoutParams emailParams =
                new LinearLayout.LayoutParams(-1, dp(44));
        emailParams.topMargin = 0;
        emailParams.gravity = Gravity.TOP;
        root.addView(email, emailParams);

        root.post(() -> {
            int targetY = (int) (root.getHeight() * 0.60f);
            int currentY = email.getTop();
            email.setTranslationY(targetY - currentY);
        });
        email.setOnClickListener(v -> openSupportEmail());

        setContentView(root);
    }

    private void openSupportEmail() {
        Uri mailUri = Uri.parse("mailto:mesdh@outlook.sa");
        Intent intent = new Intent(Intent.ACTION_SENDTO, mailUri);
        intent.setPackage("com.google.android.gm");

        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            intent.setPackage(null);
            try {
                startActivity(intent);
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this,
                        tr("ماكان حتى تطبيق بريد مثبت",
                           "No email app is installed",
                           "Aucune application e-mail installee"),
                        Toast.LENGTH_LONG).show();
            }
        }
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
        if (name == null || name.trim().isEmpty()) name = tr("ملف PDF", "PDF file", "Fichier PDF");
        openPdf(uri, name);
    }

    private void openPdf(Uri uri, String name) {
        try {
            closePdf();
            currentUri = uri;
            currentName = name;

            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) throw new Exception(tr("تعذر قراءة الملف", "Unable to read the file", "Impossible de lire le fichier"));

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
            Toast.makeText(this, tr("الملف كبير جدًا للذاكرة المتاحة", "The file is too large for available memory", "Le fichier est trop volumineux pour la mémoire disponible"),
                    Toast.LENGTH_LONG).show();
            buildHome();
        } catch (Exception e) {
            Toast.makeText(this, tr("تعذر فتح الملف: ", "Could not open file: ", "Impossible d’ouvrir le fichier : ") + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            buildHome();
        }
    }

    private void buildReader() {
        readerMode = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }
        applyDisplayCutoutMode();
        root = column();
        root.setPadding(dp(12), dp(8), dp(12), dp(8));
        root.setBackgroundColor(background);

        LinearLayout top = row();
        readerTop = top;
        fileTitle = label(currentName, 14, foreground);
        fileTitle.setMaxLines(1);
        fileTitle.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        fileTitle.setPadding(dp(10), 0, dp(10), 0);
        top.addView(fileTitle, new LinearLayout.LayoutParams(-1, -2));
        root.addView(top);

        pageView = new PdfPageView();
        pageView.setBackgroundColor(pageBackground);
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(-1, 0, 1);
        vp.setMargins(0, dp(8), 0, dp(8));
        root.addView(pageView, vp);

        LinearLayout nav = row();
        readerNav = nav;
        MaterialButton prev = button(tr("السابق", "Previous", "Précédent"), false);
        MaterialButton next = button(tr("التالي", "Next", "Suivant"), true);
        pageInfo = label("— / —", 14, foreground);
        pageInfo.setGravity(Gravity.CENTER);

        nav.addView(prev, new LinearLayout.LayoutParams(0, dp(48), 1));
        nav.addView(pageInfo, new LinearLayout.LayoutParams(0, dp(48), 1));
        nav.addView(next, new LinearLayout.LayoutParams(0, dp(48), 1));
        root.addView(nav);

        LinearLayout zoom = row();
        readerZoom = zoom;
        MaterialButton minus = button("−", false);
        MaterialButton fit = button(tr("ملاءمة الصفحة", "Fit page", "Ajuster à la page"), false);
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
        controlsVisible = false;
        updateReaderControls();
        pageView.setOnClickListener(v -> {
            controlsVisible = !controlsVisible;
            updateReaderControls();
        });
    }

    private void updateReaderControls() {
        boolean immersive = !controlsVisible;

        int visibility = immersive ? View.GONE : View.VISIBLE;
        if (readerTop != null) readerTop.setVisibility(visibility);
        if (readerNav != null) readerNav.setVisibility(visibility);
        if (readerZoom != null) readerZoom.setVisibility(visibility);

        if (root != null) {
            root.setPadding(0, 0, 0, 0);
        }

        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

        if (immersive) {
            flags |= View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        } else if (!darkMode) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }

        applyDisplayCutoutMode();
        if (immersive) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        } else {
            getWindow().setStatusBarColor(background);
            getWindow().setNavigationBarColor(background);
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);
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
            Toast.makeText(this, tr("الصفحة أكبر من الذاكرة المتاحة", "Page is too large for available memory", "La page dépasse la mémoire disponible"),
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, tr("تعذر عرض الصفحة", "Unable to display page", "Impossible d’afficher la page"), Toast.LENGTH_SHORT).show();
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
        darkMode = (newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        applyColors();
        getWindow().setStatusBarColor(background);
        getWindow().setNavigationBarColor(background);

        if (readerMode) {
            controlsVisible = newConfig.orientation != Configuration.ORIENTATION_LANDSCAPE;
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
        private final Paint paint = new Paint(
                Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final ScaleGestureDetector detector;
        private float zoom = 1f;
        private float offsetX, offsetY;
        private float lastX, lastY, downX, downY;
        private boolean moved, multiTouch;
        private static final float MAX_ZOOM = 12f;
        private static final float TOUCH_SLOP = 12f;

        PdfPageView() {
            super(MainActivity.this);
            setClickable(true);

            detector = new ScaleGestureDetector(MainActivity.this,
                    new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector d) {
                    float oldZoom = zoom;
                    float newZoom = Math.max(1f, Math.min(MAX_ZOOM,
                            oldZoom * d.getScaleFactor()));

                    float fx = d.getFocusX();
                    float fy = d.getFocusY();
                    float cx = getWidth() / 2f;
                    float cy = getHeight() / 2f;
                    float ratio = newZoom / oldZoom;

                    // Keep the PDF point beneath the fingers anchored.
                    offsetX = (fx - cx) - ((fx - cx) - offsetX) * ratio;
                    offsetY = (fy - cy) - ((fy - cy) - offsetY) * ratio;
                    zoom = newZoom;

                    if (zoom <= 1.001f) {
                        zoom = 1f;
                        offsetX = offsetY = 0f;
                    }

                    clampOffsets();
                    invalidate();
                    return true;
                }
            });
        }

        void setBitmap(Bitmap b) {
            bitmap = b;
            resetZoom();
        }

        private float fitScale() {
            if (bitmap == null || getWidth() == 0 || getHeight() == 0) {
                return 1f;
            }
            return Math.min((float) getWidth() / bitmap.getWidth(),
                    (float) getHeight() / bitmap.getHeight());
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (w > 0 && h > 0 && (w != oldw || h != oldh)) {
                clampOffsets();
                invalidate();
            }
        }

        void changeZoom(float factor) {
            float oldZoom = zoom;
            float newZoom = Math.max(1f, Math.min(MAX_ZOOM, zoom * factor));
            float ratio = newZoom / oldZoom;

            // Button zoom is centered and does not jump toward a corner.
            offsetX *= ratio;
            offsetY *= ratio;
            zoom = newZoom;

            if (zoom == 1f) offsetX = offsetY = 0f;
            clampOffsets();
            invalidate();
        }

        void resetZoom() {
            zoom = 1f;
            offsetX = offsetY = 0f;
            invalidate();
        }

        private void clampOffsets() {
            if (bitmap == null || getWidth() <= 0 || getHeight() <= 0) return;

            float scale = fitScale() * zoom;
            float w = bitmap.getWidth() * scale;
            float h = bitmap.getHeight() * scale;
            float limitX = Math.max(0f, (w - getWidth()) / 2f);
            float limitY = Math.max(0f, (h - getHeight()) / 2f);

            offsetX = Math.max(-limitX, Math.min(limitX, offsetX));
            offsetY = Math.max(-limitY, Math.min(limitY, offsetY));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(pageBackground);

            if (bitmap == null || bitmap.isRecycled() ||
                    getWidth() <= 0 || getHeight() <= 0) return;

            float scale = fitScale() * zoom;
            float left = (getWidth() - bitmap.getWidth() * scale) / 2f + offsetX;
            float top = (getHeight() - bitmap.getHeight() * scale) / 2f + offsetY;

            canvas.save();
            canvas.translate(left, top);
            canvas.scale(scale, scale);
            canvas.drawBitmap(bitmap, 0f, 0f, paint);
            canvas.restore();
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            detector.onTouchEvent(e);

            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = lastX = e.getX();
                    downY = lastY = e.getY();
                    moved = false;
                    multiTouch = false;
                    return true;

                case MotionEvent.ACTION_POINTER_DOWN:
                    multiTouch = true;
                    moved = true;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float dx = e.getX() - lastX;
                    float dy = e.getY() - lastY;

                    if (Math.abs(e.getX() - downX) > TOUCH_SLOP ||
                            Math.abs(e.getY() - downY) > TOUCH_SLOP) {
                        moved = true;
                    }

                    // One-finger pan is reserved for zoomed pages.
                    if (e.getPointerCount() == 1 &&
                            !detector.isInProgress() && zoom > 1.001f) {
                        offsetX += dx;
                        offsetY += dy;
                        clampOffsets();
                        invalidate();
                    }

                    lastX = e.getX();
                    lastY = e.getY();
                    return true;

                case MotionEvent.ACTION_POINTER_UP:
                    multiTouch = true;
                    moved = true;
                    return true;

                case MotionEvent.ACTION_UP:
                    float totalX = e.getX() - downX;
                    float totalY = e.getY() - downY;

                    if (!moved && !multiTouch && !detector.isInProgress()) {
                        performClick();
                    } else if (!multiTouch && zoom <= 1.001f &&
                            Math.abs(totalX) > dp(65) &&
                            Math.abs(totalX) > Math.abs(totalY) * 1.25f) {
                        if (totalX < 0) showPage(pageIndex + 1);
                        else showPage(pageIndex - 1);
                    }
                    return true;

                case MotionEvent.ACTION_CANCEL:
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
