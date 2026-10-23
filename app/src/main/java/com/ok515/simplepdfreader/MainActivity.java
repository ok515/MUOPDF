package com.ok515.simplepdfreader;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends Activity {
    private static final int PICK_PDF = 100;
    private PdfRenderer renderer;
    private ParcelFileDescriptor descriptor;
    private PdfPageView pageView;
    private TextView pageInfo;
    private int pageIndex = 0;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(242, 245, 250));

        TextView title = new TextView(this);
        title.setText("Simple PDF Reader");
        title.setTextSize(22);
        title.setTextColor(Color.rgb(25, 45, 75));
        title.setGravity(Gravity.CENTER);
        title.setPadding(8, 14, 8, 14);
        root.addView(title);

        Button open = new Button(this);
        open.setText("فتح ملف PDF");
        open.setOnClickListener(v -> choosePdf());
        root.addView(open);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        Button previous = new Button(this);
        previous.setText("السابق");
        Button next = new Button(this);
        next.setText("التالي");
        pageInfo = new TextView(this);
        pageInfo.setText("افتح ملفًا للبدء");
        pageInfo.setTextSize(15);
        pageInfo.setGravity(Gravity.CENTER);
        controls.addView(previous);
        controls.addView(pageInfo, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        controls.addView(next);
        root.addView(controls);

        pageView = new PdfPageView();
        root.addView(pageView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        previous.setOnClickListener(v -> showPage(pageIndex - 1));
        next.setOnClickListener(v -> showPage(pageIndex + 1));

        setContentView(root);
    }

    private void choosePdf() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        startActivityForResult(intent, PICK_PDF);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_PDF || resultCode != RESULT_OK ||
                data == null || data.getData() == null) return;

        try {
            closePdf();
            Uri uri = data.getData();
            InputStream input = getContentResolver().openInputStream(uri);
            if (input == null) throw new Exception("تعذر قراءة الملف");

            File file = new File(getCacheDir(), "document.pdf");
            try (FileOutputStream output = new FileOutputStream(file)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
            }
            input.close();

            descriptor = ParcelFileDescriptor.open(
                    file, ParcelFileDescriptor.MODE_READ_ONLY);
            renderer = new PdfRenderer(descriptor);
            pageIndex = 0;
            showPage(0);
        } catch (Exception e) {
            Toast.makeText(this, "تعذر فتح PDF: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showPage(int index) {
        if (renderer == null) {
            Toast.makeText(this, "افتح ملف PDF أولًا",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (index < 0 || index >= renderer.getPageCount()) return;

        try (PdfRenderer.Page page = renderer.openPage(index)) {
            int width = 1200;
            int height = Math.max(1, width * page.getHeight() / page.getWidth());
            Bitmap bitmap = Bitmap.createBitmap(
                    width, height, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(Color.WHITE);
            page.render(bitmap, null, null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            pageIndex = index;
            pageView.setBitmap(bitmap);
            pageInfo.setText((index + 1) + " / " + renderer.getPageCount());
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
            try {
                descriptor.close();
            } catch (Exception ignored) {
            }
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
        private final ScaleGestureDetector scaleDetector;
        private float zoom = 1f;
        private float offsetX, offsetY, lastX, lastY;

        PdfPageView() {
            super(MainActivity.this);
            scaleDetector = new ScaleGestureDetector(MainActivity.this,
                    new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    zoom = Math.max(1f, Math.min(5f,
                            zoom * detector.getScaleFactor()));
                    invalidate();
                    return true;
                }
            });
        }

        void setBitmap(Bitmap value) {
            if (bitmap != null) bitmap.recycle();
            bitmap = value;
            zoom = 1f;
            offsetX = offsetY = 0;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(Color.rgb(225, 230, 237));
            if (bitmap == null) return;

            float fit = Math.min(
                    (float) getWidth() / bitmap.getWidth(),
                    (float) getHeight() / bitmap.getHeight());
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
        public boolean onTouchEvent(MotionEvent event) {
            scaleDetector.onTouchEvent(event);

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = event.getX();
                    lastY = event.getY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (!scaleDetector.isInProgress() && zoom > 1f) {
                        offsetX += event.getX() - lastX;
                        offsetY += event.getY() - lastY;
                        invalidate();
                    }
                    lastX = event.getX();
                    lastY = event.getY();
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
