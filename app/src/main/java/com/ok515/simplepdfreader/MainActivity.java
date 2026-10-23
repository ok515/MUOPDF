package com.ok515.simplepdfreader;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.view.Gravity;
import android.view.View;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Typeface;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends Activity {
    private static final int PICK_PDF = 100;
    private PdfRenderer renderer;
    private ParcelFileDescriptor descriptor;
    private PdfPageView pageView;
    private TextView pageInfo, fileTitle, hint;
    private int pageIndex = 0;
    private Uri currentUri;
    private String currentName = "لم يتم فتح ملف";
    private final int navy = Color.rgb(24, 43, 70);
    private final int teal = Color.rgb(16, 126, 137);
    private final int bg = Color.rgb(241, 245, 249);

    private int dp(float n) {
        return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(navy);
        getWindow().setNavigationBarColor(navy);
        buildUI();
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextSize(14);
        b.setAllCaps(false);
        return b;
    }

    private void buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(8), dp(12), dp(8));
        root.setBackgroundColor(bg);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(4), dp(5), dp(4), dp(12));

        TextView logo = text("▤", 32, teal);
        header.addView(logo);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(12), 0, 0, 0);

        TextView brand = text("Simple PDF", 22, navy);
        brand.setTypeface(null, Typeface.BOLD);
        titles.addView(brand);
        titles.addView(text("اقرأ براحتك، في أي وقت", 13,
                Color.rgb(105, 119, 137)));
        header.addView(titles, new LinearLayout.LayoutParams(0,
                -2, 1));
        root.addView(header);

        Button open = button("＋   فتح ملف PDF");
        open.setTextColor(Color.WHITE);
        open.setBackgroundTintList(android.content.res.ColorStateList.valueOf(teal));
        open.setOnClickListener(v -> choosePdf());
        root.addView(open);

        fileTitle = text(currentName, 14, navy);
        fileTitle.setGravity(Gravity.CENTER);
        fileTitle.setPadding(0, dp(8), 0, dp(6));
        root.addView(fileTitle);

        pageView = new PdfPageView();
        pageView.setBackgroundColor(Color.rgb(226, 233, 240));
        LinearLayout.LayoutParams pageParams = new LinearLayout.LayoutParams(
                -1, 0, 1);
        pageParams.setMargins(0, dp(5), 0, dp(7));
        root.addView(pageView, pageParams);

        hint = text("افتح كتابًا أو ملف PDF للبدء", 14,
                Color.rgb(90, 105, 123));
        hint.setGravity(Gravity.CENTER);
        root.addView(hint);

        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        Button prev = button("❮ السابق");
        Button next = button("التالي ❯");
        pageInfo = text("— / —", 15, navy);
        pageInfo.setGravity(Gravity.CENTER);
        nav.addView(prev);
        nav.addView(pageInfo, new LinearLayout.LayoutParams(0, -2, 1));
        nav.addView(next);
        root.addView(nav);

        LinearLayout zoomBar = new LinearLayout(this);
        zoomBar.setGravity(Gravity.CENTER);
        Button minus = button("−");
        Button reset = button("ملاءمة الشاشة");
        Button plus = button("+");
        zoomBar.addView(minus, new LinearLayout.LayoutParams(0, -2, 1));
        zoomBar.addView(reset, new LinearLayout.LayoutParams(0, -2, 2));
        zoomBar.addView(plus, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(zoomBar);

        prev.setOnClickListener(v -> showPage(pageIndex - 1));
        next.setOnClickListener(v -> showPage(pageIndex + 1));
        minus.setOnClickListener(v -> pageView.changeZoom(0.8f));
        plus.setOnClickListener(v -> pageView.changeZoom(1.25f));
        reset.setOnClickListener(v -> pageView.resetZoom());

        setContentView(root);
    }

    private void choosePdf() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/pdf");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(i, PICK_PDF);
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK_PDF || result != RESULT_OK ||
                data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            closePdf();
            currentUri = uri;
            String name = uri.getLastPathSegment();
            currentName = name == null ? "ملف PDF" : name;
            fileTitle.setText(currentName);
            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) throw new Exception("تعذر فتح الملف");
            File f = new File(getCacheDir(), "reader.pdf");
            try (FileOutputStream out = new FileOutputStream(f)) {
                byte[] buffer = new byte[16384];
                int n;
                while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            }
            in.close();
            descriptor = ParcelFileDescriptor.open(f,
                    ParcelFileDescriptor.MODE_READ_ONLY);
            renderer = new PdfRenderer(descriptor);
            pageIndex = 0;
            hint.setText("");
            showPage(0);
        } catch (Exception e) {
            Toast.makeText(this, "تعذر فتح الملف: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showPage(int index) {
        if (renderer == null) {
            Toast.makeText(this, "افتح PDF أولًا", Toast.LENGTH_SHORT).show();
            return;
        }
        if (index < 0 || index >= renderer.getPageCount()) return;
        try (PdfRenderer.Page page = renderer.openPage(index)) {
            int width = Math.min(1800, Math.max(1000,
                    getResources().getDisplayMetrics().widthPixels * 2));
            int height = Math.max(1, width * page.getHeight() / page.getWidth());
            Bitmap bmp = Bitmap.createBitmap(width, height,
                    Bitmap.Config.ARGB_8888);
            bmp.eraseColor(Color.WHITE);
            page.render(bmp, null, null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            pageIndex = index;
            pageView.setBitmap(bmp);
            pageInfo.setText((index + 1) + " / " + renderer.getPageCount());
        } catch (OutOfMemoryError e) {
            Toast.makeText(this, "الملف كبير جدًا للعرض بهذه الجودة",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "خطأ في عرض الصفحة",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void closePdf() {
        if (renderer != null) {
            renderer.close();
            renderer = null;
        }
        if (descriptor != null) {
            try { descriptor.close(); } catch (Exception ignored) {}
            descriptor = null;
        }
    }

    @Override
    protected void onDestroy() {
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
        private int pointerCount;

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
            if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
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
            canvas.drawColor(Color.rgb(226, 233, 240));
            if (bitmap == null || bitmap.isRecycled()) return;

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
                    pointerCount = 1;
                    return true;
                case MotionEvent.ACTION_POINTER_DOWN:
                    pointerCount++;
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
                case MotionEvent.ACTION_POINTER_UP:
                    pointerCount = Math.max(1, pointerCount - 1);
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
