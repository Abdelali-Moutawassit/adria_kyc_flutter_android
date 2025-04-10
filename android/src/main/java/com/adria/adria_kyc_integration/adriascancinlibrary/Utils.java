/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.adria.adria_kyc_integration.adriascancinlibrary;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.Image;

import androidx.core.math.MathUtils;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

/** Utility class for manipulating images. */
public class Utils {

  public static Matrix getTransformationMatrix(
      final int srcWidth,
      final int srcHeight,
      final int dstWidth,
      final int dstHeight,
      final int applyRotation,
      final boolean maintainAspectRatio) {
    final Matrix matrix = new Matrix();

    if (applyRotation != 0) {
      if (applyRotation % 90 != 0) {
      }

      // Translate so center of image is at origin.
      matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

      // Rotate around origin.
      matrix.postRotate(applyRotation);
    }

    // Account for the already applied rotation, if any, and then determine how
    // much scaling is needed for each axis.
    final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;

    final int inWidth = transpose ? srcHeight : srcWidth;
    final int inHeight = transpose ? srcWidth : srcHeight;

    // Apply scaling if necessary.
    if (inWidth != dstWidth || inHeight != dstHeight) {
      final float scaleFactorX = dstWidth / (float) inWidth;
      final float scaleFactorY = dstHeight / (float) inHeight;

      if (maintainAspectRatio) {
        // Scale by minimum factor so that dst is filled completely while
        // maintaining the aspect ratio. Some image may fall off the edge.
        final float scaleFactor = Math.max(scaleFactorX, scaleFactorY);
        matrix.postScale(scaleFactor, scaleFactor);
      } else {
        // Scale exactly to fill dst from src.
        matrix.postScale(scaleFactorX, scaleFactorY);
      }
    }

    if (applyRotation != 0) {
      // Translate back from origin centered reference to destination frame.
      matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
    }

    return matrix;
  }

  public static Bitmap yuv420ToBitmap(Image image) {
    int imageWidth = image.getWidth();
    int imageHeight = image.getHeight();
    // sRGB array needed by Bitmap static factory method I use below.
    int[] argbArray = new int[imageWidth * imageHeight];
    ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
    yBuffer.position(0);

    // This is specific to YUV420SP format where U & V planes are interleaved
    // so you can access them directly from one ByteBuffer. The data is saved as
    // UVUVUVUVU... for NV12 format and VUVUVUVUV... for NV21 format.
    //
    // The alternative way to handle this would be refer U & V as separate
    // `ByteBuffer`s and then use PixelStride and RowStride to find the right
    // index of the U or V value per pixel.
    ByteBuffer uvBuffer = image.getPlanes()[1].getBuffer();
    uvBuffer.position(0);
    int r, g, b;
    int yValue, uValue, vValue;

    for (int y = 0; y < imageHeight - 2; y++) {
      for (int x = 0; x < imageWidth - 2; x++) {
        int yIndex = y * imageWidth + x;
        // Y plane should have positive values belonging to [0...255]
        yValue = (yBuffer.get(yIndex) & 0xff);

        int uvx = x / 2;
        int uvy = y / 2;
        // Remember UV values are common for four pixel values.
        // So the actual formula if U & V were in separate plane would be:
        // `pos (for u or v) = (y / 2) * (width / 2) + (x / 2)`
        // But since they are in single plane interleaved the position becomes:
        // `u = 2 * pos`
        // `v = 2 * pos + 1`, if the image is in NV12 format, else reverse.
        int uIndex = uvy * imageWidth + 2 * uvx;
        // ^ Note that here `uvy = y / 2` and `uvx = x / 2`
        int vIndex = uIndex + 1;

        uValue = (uvBuffer.get(uIndex) & 0xff) - 128;
        vValue = (uvBuffer.get(vIndex) & 0xff) - 128;
        r = (int) (yValue + 1.370705f * vValue);
        g = (int) (yValue - (0.698001f * vValue) - (0.337633f * uValue));
        b = (int) (yValue + 1.732446f * uValue);
        r = MathUtils.clamp(r, 0, 255);
        g = MathUtils.clamp(g, 0, 255);
        b = MathUtils.clamp(b, 0, 255);
        // Use 255 for alpha value, no transparency. ARGB values are
        // positioned in each byte of a single 4 byte integer
        // [AAAAAAAARRRRRRRRGGGGGGGGBBBBBBBB]
        argbArray[yIndex] = (255 << 24) | (r & 255) << 16 | (g & 255) << 8 | (b & 255);
      }
    }

    return Bitmap.createBitmap(argbArray, imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
  }

  public static Bitmap resizeBitmap(Bitmap source, int maxLength) {
    try {
      if (source.getHeight() >= source.getWidth()) {
        int targetHeight = maxLength;
        if (source.getHeight() <= targetHeight) { // if image already smaller than the required height
          return source;
        }

        double aspectRatio = (double) source.getWidth() / (double) source.getHeight();
        int targetWidth = (int) (targetHeight * aspectRatio);

        Bitmap result = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, false);
        if (result != source) {
        }
        return result;
      } else {
        int targetWidth = maxLength;

        if (source.getWidth() <= targetWidth) { // if image already smaller than the required height
          return source;
        }

        double aspectRatio = ((double) source.getHeight()) / ((double) source.getWidth());
        int targetHeight = (int) (targetWidth * aspectRatio);

        Bitmap result = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, false);
        if (result != source) {
        }
        return result;

      }
    } catch (Exception e) {
      return source;
    }
  }

  public static boolean similarity(String s1, String s2) {
    String longer = s1, shorter = s2;
    if (s1.length() < s2.length()) { // longer should always have greater length
      longer = s2;
      shorter = s1;
    }
    int longerLength = longer.length();
    if (longerLength == 0) {
      return true; /* both strings are zero length */
    }
    /* // If you have Apache Commons Text, you can use it to calculate the edit distance:
    LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
    return (longerLength - levenshteinDistance.apply(longer, shorter)) / (double) longerLength; */
    return (longerLength - editDistance(longer, shorter)) / (double) longerLength>0.7;
  }

  public static int editDistance(String s1, String s2) {
    s1 = s1.toLowerCase();
    s2 = s2.toLowerCase();

    int[] costs = new int[s2.length() + 1];
    for (int i = 0; i <= s1.length(); i++) {
      int lastValue = i;
      for (int j = 0; j <= s2.length(); j++) {
        if (i == 0)
          costs[j] = j;
        else {
          if (j > 0) {
            int newValue = costs[j - 1];
            if (s1.charAt(i - 1) != s2.charAt(j - 1))
              newValue = Math.min(Math.min(newValue, lastValue),
                      costs[j]) + 1;
            costs[j - 1] = lastValue;
            lastValue = newValue;
          }
        }
      }
      if (i > 0)
        costs[s2.length()] = lastValue;
    }
    return costs[s2.length()];
  }


  public static int countWords(String sentence) {
    if (sentence == null || sentence.isEmpty()) {
      return 0;
    }
    StringTokenizer tokens = new StringTokenizer(sentence);

    return tokens.countTokens();
  }

  public static int checkDay(String date){
    int result = 0;
    try {
      Date date1=new SimpleDateFormat("dd.MM.yyyy").parse(date);
      Date todayDate = new Date();
      result = date1.compareTo(todayDate);

    } catch (ParseException e) {
      e.printStackTrace();
    }
    return result;
  }

  public static Boolean checkIfCIN(String txt){
    if (txt.contains("NATIONALE D'IDENTITE") || txt.contains("NATIONALE") || txt.contains("IDENTITE"))
      return true;
    else
      return false;
  }
}
