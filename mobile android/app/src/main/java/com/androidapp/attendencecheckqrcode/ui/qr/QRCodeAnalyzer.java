package com.androidapp.attendencecheckqrcode.ui.qr;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.media.Image;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

public class QRCodeAnalyzer implements ImageAnalysis.Analyzer {

    private final QRCodeListener listener;
    private final BarcodeScanner scanner;

    // Cập nhật interface để truyền thêm Rect và Kích thước ảnh
    public interface QRCodeListener {
        void onQRCodeFound(String qrCode, Rect boundingBox, int imageWidth, int imageHeight);
    }

    public QRCodeAnalyzer(QRCodeListener listener) {
        this.listener = listener;
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        scanner = BarcodeScanning.getClient(options);
    }

    @SuppressLint("UnsafeOptInUsageError")
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            scanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        for (Barcode barcode : barcodes) {
                            String rawValue = barcode.getRawValue();
                            Rect boundingBox = barcode.getBoundingBox();

                            if (rawValue != null && boundingBox != null) {
                                // Truyền thêm chiều rộng và chiều cao của ảnh (đã xoay chiều)
                                listener.onQRCodeFound(rawValue, boundingBox, image.getWidth(), image.getHeight());
                                break;
                            }
                        }
                    })
                    .addOnCompleteListener(task -> imageProxy.close());
        } else {
            imageProxy.close();
        }
    }
}