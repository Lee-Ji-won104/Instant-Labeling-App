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

package org.tensorflow.lite.examples.detection.env;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Xml;

import androidx.annotation.NonNull;

import org.tensorflow.lite.examples.detection.DetectorActivity;
import org.tensorflow.lite.examples.detection.tflite.Detector;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.List;
import java.util.Objects;

/** Utility class for manipulating images. */
public class ImageUtils {
  // This value is 2 ^ 18 - 1, and is used to clamp the RGB values before their ranges
  // are normalized to eight bits.
  static final int kMaxChannelValue = 262143;

  @SuppressWarnings("unused")
  private static final Logger LOGGER = new Logger();

  /**
   * Utility method to compute the allocated size in bytes of a YUV420SP image of the given
   * dimensions.
   */
  public static int getYUVByteSize(final int width, final int height) {
    // The luminance plane requires 1 byte per pixel.
    final int ySize = width * height;

    // The UV plane works on 2x2 blocks, so dimensions with odd size must be rounded up.
    // Each 2x2 block takes 2 bytes to encode, one each for U and V.
    final int uvSize = ((width + 1) / 2) * ((height + 1) / 2) * 2;

    return ySize + uvSize;
  }

  /**
   * Saves a Bitmap object to disk for analysis.
   *
   * @param bitmap The bitmap to save.
   */
  public static void saveBitmap(final Bitmap bitmap) {
    saveBitmap(bitmap, "preview.png");
  }

  /**
   * Saves a Bitmap object to disk for analysis.
   *
   * @param bitmap The bitmap to save.
   * @param filename The location to save the bitmap to.
   */
  public static void saveBitmap(final Bitmap bitmap, final String filename) {
    final String root =
        Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "tensorflow";
    LOGGER.i("Saving %dx%d bitmap to %s.", bitmap.getWidth(), bitmap.getHeight(), root);
    final File myDir = new File(root);

    if (!myDir.mkdirs()) {
      LOGGER.i("Make dir failed");
    }

    final String fname = filename;
    final File file = new File(myDir, fname);
    if (file.exists()) {
      file.delete();
    }
    try {
      final FileOutputStream out = new FileOutputStream(file);
      bitmap.compress(Bitmap.CompressFormat.PNG, 99, out);
      out.flush();
      out.close();
    } catch (final Exception e) {
      LOGGER.e(e, "Exception!");
    }
  }

  public static void saveImage(Bitmap bitmap, int name, @NonNull String date, Context context) throws IOException {
    OutputStream fos;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

      ContentResolver resolver = context.getContentResolver();
      ContentValues contentValues = new ContentValues();
      contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, date+"_"+name + ".jpg");
      contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
      contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/"+"InstantLabeling/" +date+"/");

      Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
      fos = resolver.openOutputStream(Objects.requireNonNull(imageUri));
      bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
      Objects.requireNonNull(fos).close();
    }
    else {

      final String root = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +"DCIM/"+ "InstantLabeling/" +date+"/";
      final File myDir = new File(root);

      if (!myDir.mkdirs()) {
      }

      final String fname = date+"_"+name+ ".jpg";
      final File file = new File(myDir, fname);
      if (file.exists()) {
        file.delete();
      }
      try {
        final FileOutputStream out = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 99, out);
        out.flush();
        out.close();
      } catch (final Exception e) {

      }
    }
  }

  //TODO
  //xml 저장하는 코드 작성해야 한다.
  public static void saveXML(int name, String date, int height, int width, List<Detector.Recognition> mappedRecognitions, Context context ) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

      ContentValues contentValues = new ContentValues();
      contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, date + "_" + name + ".xml");
      contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/xml");
      contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + File.separator + "InstantLabeling/" + date + "/");

      ContentResolver resolver = context.getContentResolver();
      Uri xmlUri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues);
      try {

        resolver.openOutputStream(Objects.requireNonNull(xmlUri)).write(writeXML(name, height, width, mappedRecognitions).getBytes());

      } catch (Exception e) {
        e.printStackTrace();
      }

    }
  }


  public static String writeXML(int name, int height, int width, List<Detector.Recognition> mappedRecognitions) {

    StringWriter writer=new StringWriter();

    try {

      //we create a XmlSerializer in order to write xml data
      XmlSerializer serializer = Xml.newSerializer();


      //we set the FileOutputStream as output for the serializer, using UTF-8 encoding
      serializer.setOutput(writer);

      //Write <?xml declaration with encoding (if encoding not null) and standalone flag (if standalone not null)
      serializer.startDocument("UTF-8", true);

      //set indentation option
      //serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);

      //start a tag called "root"
      serializer.startTag(null, "annotation");

      //folder
      serializer.startTag(null, "folder");
      serializer.text("InstantLabeling");
      serializer.endTag(null, "folder");

      //filename
      serializer.startTag(null, "filename");
      serializer.text(name+".jpg");
      serializer.endTag(null, "filename");

      //path
      serializer.startTag(null, "path");
      serializer.text("InstantLabeling");
      serializer.endTag(null, "path");

      //source
      serializer.startTag(null, "source");

      serializer.startTag(null,"database");
      serializer.text("Unknown");
      serializer.endTag(null,"database");

      serializer.endTag(null, "source");


      //size
      serializer.startTag(null, "size");

      serializer.startTag(null,"width");
      serializer.text(String.valueOf(width));
      serializer.endTag(null,"width");

      serializer.startTag(null,"height");
      serializer.text(String.valueOf(height));
      serializer.endTag(null,"height");

      serializer.startTag(null,"depth");
      serializer.text("3");
      serializer.endTag(null,"depth");

      serializer.endTag(null, "size");

      //segmented
      serializer.startTag(null, "segmented");
      serializer.text("0");
      serializer.endTag(null, "segmented");


      //TODO
      //make for-loop for object tag
      for (final Detector.Recognition result: mappedRecognitions){
        //object
        serializer.startTag(null, "object");

        serializer.startTag(null,"name");
        serializer.text(result.getTitle());
        serializer.endTag(null,"name");

        serializer.startTag(null,"pose");
        serializer.text("Unspecified");
        serializer.endTag(null,"pose");

        serializer.startTag(null,"truncated");
        serializer.text("0");
        serializer.endTag(null,"truncated");

        serializer.startTag(null,"difficult");
        serializer.text("0");
        serializer.endTag(null,"difficult");

        serializer.startTag(null,"bndbox");

        /////xmin
        serializer.startTag(null,"xmin");
        if (result.getLocation().left<0) {
          result.getLocation().left=0;
          serializer.text(String.valueOf(0));
        }
        else{
          serializer.text(String.valueOf(result.getLocation().left));
        }

        serializer.endTag(null,"xmin");

        //////ymin
        serializer.startTag(null,"ymin");
        if(result.getLocation().top<0){
          result.getLocation().top=0;
          serializer.text(String.valueOf(0));
        }
        else{
          serializer.text(String.valueOf(result.getLocation().top));
        }

        serializer.endTag(null,"ymin");

        //////xmax
        serializer.startTag(null,"xmax");
        if(result.getLocation().right> DetectorActivity.DESIRED_PREVIEW_SIZE.getWidth()){
          result.getLocation().right=DetectorActivity.DESIRED_PREVIEW_SIZE.getWidth();
          serializer.text(String.valueOf(DetectorActivity.DESIRED_PREVIEW_SIZE.getWidth()));
        }
        else{
          serializer.text(String.valueOf(result.getLocation().right));
        }

        serializer.endTag(null,"xmax");

        //////ymax
        serializer.startTag(null,"ymax");
        if(result.getLocation().bottom>DetectorActivity.DESIRED_PREVIEW_SIZE.getHeight()){
          result.getLocation().bottom=DetectorActivity.DESIRED_PREVIEW_SIZE.getHeight();
          serializer.text(String.valueOf(DetectorActivity.DESIRED_PREVIEW_SIZE.getHeight()));
        }
        else{
          serializer.text(String.valueOf(result.getLocation().bottom));
        }

        serializer.endTag(null,"ymax");

        ////////
        serializer.endTag(null,"bndbox");

        serializer.endTag(null, "object");

      }

      serializer.endTag(null, "annotation");

      serializer.endDocument();

      //write xml data into the FileOutputStream

      serializer.flush();

      //finally we close the file stream



    } catch (Exception e) {

      Log.e("Exception","error occurred while creating xml file");

    }
    return writer.toString();
  }


  public static void convertYUV420SPToARGB8888(byte[] input, int width, int height, int[] output) {
    final int frameSize = width * height;
    for (int j = 0, yp = 0; j < height; j++) {
      int uvp = frameSize + (j >> 1) * width;
      int u = 0;
      int v = 0;

      for (int i = 0; i < width; i++, yp++) {
        int y = 0xff & input[yp];
        if ((i & 1) == 0) {
          v = 0xff & input[uvp++];
          u = 0xff & input[uvp++];
        }

        output[yp] = YUV2RGB(y, u, v);
      }
    }
  }

  private static int YUV2RGB(int y, int u, int v) {
    // Adjust and check YUV values
    y = (y - 16) < 0 ? 0 : (y - 16);
    u -= 128;
    v -= 128;

    // This is the floating point equivalent. We do the conversion in integer
    // because some Android devices do not have floating point in hardware.
    // nR = (int)(1.164 * nY + 2.018 * nU);
    // nG = (int)(1.164 * nY - 0.813 * nV - 0.391 * nU);
    // nB = (int)(1.164 * nY + 1.596 * nV);
    int y1192 = 1192 * y;
    int r = (y1192 + 1634 * v);
    int g = (y1192 - 833 * v - 400 * u);
    int b = (y1192 + 2066 * u);

    // Clipping RGB values to be inside boundaries [ 0 , kMaxChannelValue ]
    r = r > kMaxChannelValue ? kMaxChannelValue : (r < 0 ? 0 : r);
    g = g > kMaxChannelValue ? kMaxChannelValue : (g < 0 ? 0 : g);
    b = b > kMaxChannelValue ? kMaxChannelValue : (b < 0 ? 0 : b);

    return 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
  }

  public static void convertYUV420ToARGB8888(
      byte[] yData,
      byte[] uData,
      byte[] vData,
      int width,
      int height,
      int yRowStride,
      int uvRowStride,
      int uvPixelStride,
      int[] out) {
    int yp = 0;
    for (int j = 0; j < height; j++) {
      int pY = yRowStride * j;
      int pUV = uvRowStride * (j >> 1);

      for (int i = 0; i < width; i++) {
        int uv_offset = pUV + (i >> 1) * uvPixelStride;

        out[yp++] = YUV2RGB(0xff & yData[pY + i], 0xff & uData[uv_offset], 0xff & vData[uv_offset]);
      }
    }
  }

  /**
   * Returns a transformation matrix from one reference frame into another. Handles cropping (if
   * maintaining aspect ratio is desired) and rotation.
   *
   * @param srcWidth Width of source frame.
   * @param srcHeight Height of source frame.
   * @param dstWidth Width of destination frame.
   * @param dstHeight Height of destination frame.
   * @param applyRotation Amount of rotation to apply from one frame to another. Must be a multiple
   *     of 90.
   * @param maintainAspectRatio If true, will ensure that scaling in x and y remains constant,
   *     cropping the image if necessary.
   * @return The transformation fulfilling the desired requirements.
   */
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
        LOGGER.w("Rotation of %d % 90 != 0", applyRotation);
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
}
