package com.ptithcm.attendapp.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

public class QROverlayView extends View {

    private Paint maskPaint, borderPaint, laserPaint;
    private RectF framingRect;       // Khung hiện tại đang vẽ
    private RectF defaultFramingRect;// Khung vuông mặc định ở giữa màn hình

    private float laserY = 0;
    private ValueAnimator laserAnimator;
    private boolean isTargetLocked = false; // Trạng thái đã chốt mã QR

    public QROverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskPaint.setColor(Color.parseColor("#99000000")); // Đen mờ 60%

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.parseColor("#EF4444")); // Đỏ
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(14); // Cho góc dày hơn một chút
        borderPaint.setStrokeJoin(Paint.Join.ROUND);
        borderPaint.setStrokeCap(Paint.Cap.ROUND);
        // ĐÂY LÀ CHÌA KHÓA CHO GÓC BO TRÒN MỀM MẠI
        borderPaint.setPathEffect(new CornerPathEffect(30));

        laserPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        laserPaint.setColor(Color.parseColor("#EF4444"));
        laserPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Tạo khung vuông mặc định 260dp ở giữa màn hình
        float size = 260 * getResources().getDisplayMetrics().density;
        float left = (w - size) / 2;
        float top = (h - size) / 2;

        defaultFramingRect = new RectF(left, top, left + size, top + size);
        framingRect = new RectF(defaultFramingRect); // Ban đầu bằng mặc định

        startLaserAnimation();
    }

    private void startLaserAnimation() {
        if (laserAnimator != null) laserAnimator.cancel();

        laserY = framingRect.top;
        laserAnimator = ValueAnimator.ofFloat(framingRect.top, framingRect.bottom);
        laserAnimator.setDuration(2500);
        laserAnimator.setRepeatCount(ValueAnimator.INFINITE);
        laserAnimator.setRepeatMode(ValueAnimator.REVERSE); // Laser quét lên quét xuống mượt hơn
        laserAnimator.setInterpolator(new LinearInterpolator());
        laserAnimator.addUpdateListener(animation -> {
            laserY = (float) animation.getAnimatedValue();
            invalidate();
        });
        laserAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (framingRect == null) return;

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // 1. VẼ MẶT NẠ MỜ (Chừa lỗ thủng cho khung framingRect)
        canvas.drawRect(0, 0, width, framingRect.top, maskPaint);
        canvas.drawRect(0, framingRect.top, framingRect.left, framingRect.bottom, maskPaint);
        canvas.drawRect(framingRect.right, framingRect.top, width, framingRect.bottom, maskPaint);
        canvas.drawRect(0, framingRect.bottom, width, height, maskPaint);

        // 2. VẼ 4 GÓC BO TRÒN MỀM MẠI BẰNG PATH
        float borderLength = 100;
        Path path = new Path();

        // Góc Trái - Trên
        path.moveTo(framingRect.left, framingRect.top + borderLength);
        path.lineTo(framingRect.left, framingRect.top);
        path.lineTo(framingRect.left + borderLength, framingRect.top);

        // Góc Phải - Trên
        path.moveTo(framingRect.right - borderLength, framingRect.top);
        path.lineTo(framingRect.right, framingRect.top);
        path.lineTo(framingRect.right, framingRect.top + borderLength);

        // Góc Phải - Dưới
        path.moveTo(framingRect.right, framingRect.bottom - borderLength);
        path.lineTo(framingRect.right, framingRect.bottom);
        path.lineTo(framingRect.right - borderLength, framingRect.bottom);

        // Góc Trái - Dưới
        path.moveTo(framingRect.left + borderLength, framingRect.bottom);
        path.lineTo(framingRect.left, framingRect.bottom);
        path.lineTo(framingRect.left, framingRect.bottom - borderLength);

        canvas.drawPath(path, borderPaint);

        // 3. VẼ TIA LASER (Chỉ vẽ khi chưa chốt mã QR)
        if (!isTargetLocked) {
            canvas.drawRect(framingRect.left + 16, laserY - 3, framingRect.right - 16, laserY + 3, laserPaint);
        } else {
            // Khi đã chốt, đổi khung viền thành màu Xanh lá báo hiệu thành công
            borderPaint.setColor(Color.parseColor("#10B981"));
        }
    }

    // =======================================================
    // HÀM GỌI TỪ ACTIVITY: BÓP KHUNG THEO TỌA ĐỘ QR CODE
    // =======================================================
    public void animateToBarcode(android.graphics.Rect barcodeBoundingBox) {
        if (isTargetLocked || barcodeBoundingBox == null) return;
        isTargetLocked = true;

        if (laserAnimator != null) {
            laserAnimator.cancel(); // Dừng laser
        }

        // Chuyển Rect nguyên thủy thành RectF (tọa độ thập phân cho mượt)
        // Lưu ý: Tọa độ của ML Kit trả về cần phải căn chỉnh lại với màn hình (Thường bị lệc do tỉ lệ Camera),
        // ở đây ta sẽ mô phỏng việc "bóp" lại 1 chút cho giống Zalo thay vì khớp 100%.

        RectF targetRect = new RectF(
                defaultFramingRect.left + 50,
                defaultFramingRect.top + 50,
                defaultFramingRect.right - 50,
                defaultFramingRect.bottom - 50
        );

        // Animation "bóp" khung
        ValueAnimator shrinkAnimator = ValueAnimator.ofFloat(0, 1);
        shrinkAnimator.setDuration(300); // Rút nhanh trong 300ms
        shrinkAnimator.setInterpolator(new DecelerateInterpolator());

        final float startLeft = framingRect.left;
        final float startTop = framingRect.top;
        final float startRight = framingRect.right;
        final float startBottom = framingRect.bottom;

        shrinkAnimator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            framingRect.left = startLeft + (targetRect.left - startLeft) * fraction;
            framingRect.top = startTop + (targetRect.top - startTop) * fraction;
            framingRect.right = startRight + (targetRect.right - startRight) * fraction;
            framingRect.bottom = startBottom + (targetRect.bottom - startBottom) * fraction;
            invalidate();
        });

        shrinkAnimator.start();
    }
}