package com.adria.adria_kyc_integration.adriascancinlibrary;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;

import androidx.camera.core.ImageProxy;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import kotlin._Assertions;
import kotlin.jvm.internal.Intrinsics;

public class ImageToBitmap {
    @org.jetbrains.annotations.Nullable
    public static final Bitmap toBitmap(@NotNull ImageProxy $this$toBitmap) {
        Intrinsics.checkParameterIsNotNull($this$toBitmap, "$this$toBitmap");
        byte[] nv21 = yuv420888ToNv21($this$toBitmap);
        YuvImage yuvImage = new YuvImage(nv21, 17, $this$toBitmap.getWidth(), $this$toBitmap.getHeight(), (int[])null);
        return toBitmap(yuvImage);
    }

    private static final Bitmap toBitmap(@NotNull YuvImage $this$toBitmap) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!$this$toBitmap.compressToJpeg(new Rect(0, 0, $this$toBitmap.getWidth(), $this$toBitmap.getHeight()), 100, (OutputStream)out)) {
            return null;
        } else {
            byte[] var10000 = out.toByteArray();
            Intrinsics.checkExpressionValueIsNotNull(var10000, "out.toByteArray()");
            byte[] imageBytes = var10000;
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        }
    }

    private static final byte[] yuv420888ToNv21(ImageProxy image) {
        int pixelCount = image.getCropRect().width() * image.getCropRect().height();
        @SuppressLint("WrongConstant") int pixelSizeBits = ImageFormat.getBitsPerPixel(35);
        byte[] outputBuffer = new byte[pixelCount * pixelSizeBits / 8];
        imageToByteBuffer(image, outputBuffer, pixelCount);
        return outputBuffer;
    }

    private static final void imageToByteBuffer(ImageProxy image, byte[] outputBuffer, int pixelCount) {
        boolean var3 = image.getFormat() == 35;
        boolean var4 = false;
        boolean var5 = false;
        boolean $i$f$forEachIndexed;
        if (_Assertions.ENABLED && !var3) {
            $i$f$forEachIndexed = false;
            String var31 = "Assertion failed";
            try {
                throw (Throwable)(new AssertionError(var31));
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        } else {
            Rect var10000 = image.getCropRect();
            Intrinsics.checkExpressionValueIsNotNull(var10000, "image.cropRect");
            Rect imageCrop = var10000;
            ImageProxy.PlaneProxy[] var33 = image.getPlanes();
            Intrinsics.checkExpressionValueIsNotNull(var33, "image.planes");
            ImageProxy.PlaneProxy[] imagePlanes = var33;
            $i$f$forEachIndexed = false;
            int index$iv = 0;
            ImageProxy.PlaneProxy[] var8 = imagePlanes;
            int var9 = imagePlanes.length;

            for(int var10 = 0; var10 < var9; ++var10) {
                Object item$iv = var8[var10];
                int planeIndex = index$iv++;
                byte outputStride;
                int outputOffset;
                switch(planeIndex) {
                    case 0:
                        outputStride = 1;
                        outputOffset = 0;
                        break;
                    case 1:
                        outputStride = 2;
                        outputOffset = pixelCount + 1;
                        break;
                    case 2:
                        outputStride = 2;
                        outputOffset = pixelCount;
                        break;
                    default:
                        continue;
                }

                Intrinsics.checkExpressionValueIsNotNull(item$iv, "plane");
                ByteBuffer var36 = ((ImageProxy.PlaneProxy) item$iv).getBuffer();
                Intrinsics.checkExpressionValueIsNotNull(var36, "plane.buffer");
                ByteBuffer planeBuffer = var36;
                int rowStride = ((ImageProxy.PlaneProxy) item$iv).getRowStride();
                int pixelStride = ((ImageProxy.PlaneProxy) item$iv).getPixelStride();
                Rect planeCrop = planeIndex == 0 ? imageCrop : new Rect(imageCrop.left / 2, imageCrop.top / 2, imageCrop.right / 2, imageCrop.bottom / 2);
                int planeWidth = planeCrop.width();
                int planeHeight = planeCrop.height();
                byte[] rowBuffer = new byte[((ImageProxy.PlaneProxy) item$iv).getRowStride()];
                int rowLength = pixelStride == 1 && outputStride == 1 ? planeWidth : (planeWidth - 1) * pixelStride + 1;
                int row = 0;

                for(int var26 = planeHeight; row < var26; ++row) {
                    planeBuffer.position((row + planeCrop.top) * rowStride + planeCrop.left * pixelStride);
                    if (pixelStride == 1 && outputStride == 1) {
                        planeBuffer.get(outputBuffer, outputOffset, rowLength);
                        outputOffset += rowLength;
                    } else {
                        planeBuffer.get(rowBuffer, 0, rowLength);
                        int col = 0;

                        for(int var28 = planeWidth; col < var28; ++col) {
                            outputBuffer[outputOffset] = rowBuffer[col * pixelStride];
                            outputOffset += outputStride;
                        }
                    }
                }
            }

        }
    }
}