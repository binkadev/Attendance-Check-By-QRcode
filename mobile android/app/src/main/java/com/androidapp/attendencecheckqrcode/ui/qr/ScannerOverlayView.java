package com.androidapp.attendencecheckqrcode.ui.qr;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

public class ScannerOverlayView extends View {
    private Paint scrimPaint;
    private Paint eraserPaint;
    private Paint cornerPaint;
    private RectF scanRect;
    private final float cornerLength = 60f;
    private final float cornerWidth = 12f;
    private final float cornerRadius = 30f;

    public ScannerOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        scrimPaint = new Paint();
        scrimPaint.setColor(Color.parseColor("#99000000")); // Đen mờ 60%

        eraserPaint = new Paint();
        eraserPaint.setAntiAlias(true);
        eraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        cornerPaint = new Paint();
        cornerPaint.setColor(Color.WHITE);
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(cornerWidth);
        cornerPaint.setStrokeCap(Paint.Cap.ROUND);
        cornerPaint.setAntiAlias(true);

        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0, 0, getWidth(), getHeight(), scrimPaint);

        float size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 280, getResources().getDisplayMetrics());
        float left = (getWidth() - size) / 2;
        float top = getHeight() * 0.45f - (size / 2);
        scanRect = new RectF(left, top, left + size, top + size);

        canvas.drawRoundRect(scanRect, cornerRadius, cornerRadius, eraserPaint);
        drawCorners(canvas, scanRect);
    }

    private void drawCorners(Canvas canvas, RectF rect) {
        Path path = new Path();

        path.moveTo(rect.left, rect.top + cornerLength);
        path.lineTo(rect.left, rect.top + cornerRadius);
        path.quadTo(rect.left, rect.top, rect.left + cornerRadius, rect.top);
        path.lineTo(rect.left + cornerLength, rect.top);

        path.moveTo(rect.right - cornerLength, rect.top);
        path.lineTo(rect.right - cornerRadius, rect.top);
        path.quadTo(rect.right, rect.top, rect.right, rect.top + cornerRadius);
        path.lineTo(rect.right, rect.top + cornerLength);

        path.moveTo(rect.left, rect.bottom - cornerLength);
        path.lineTo(rect.left, rect.bottom - cornerRadius);
        path.quadTo(rect.left, rect.bottom, rect.left + cornerRadius, rect.bottom);
        path.lineTo(rect.left + cornerLength, rect.bottom);

        path.moveTo(rect.right - cornerLength, rect.bottom);
        path.lineTo(rect.right - cornerRadius, rect.bottom);
        path.quadTo(rect.right, rect.bottom, rect.right, rect.bottom - cornerRadius);
        path.lineTo(rect.right, rect.bottom - cornerLength);

        canvas.drawPath(path, cornerPaint);
    }
}
