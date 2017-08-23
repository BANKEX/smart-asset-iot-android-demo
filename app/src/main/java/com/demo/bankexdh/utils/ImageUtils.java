package com.demo.bankexdh.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.ipaulpro.afilechooser.utils.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class ImageUtils {

    public static final String FILEDATA = "filedata";
    public static final String IMAGE_TYPE = "image/*";
    private String currentPhotoPath;
    private String absolutImagePath;
    private static final int FILE_SIZE_UNITS = 1024;
    private static final int MAX_FILE_SIZE = 9;

    private static class InstanceHolder {
        static final ImageUtils INSTANCE = new ImageUtils();
    }

    public static ImageUtils getInstance() {
        return ImageUtils.InstanceHolder.INSTANCE;
    }

    public String getCurrentPhotoPath() {
        return currentPhotoPath;
    }

    public File createImageFile(Context context) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(context.getExternalCacheDir() + "/photos");
        if (!storageDir.exists()) {
            boolean success = storageDir.mkdir();
            Timber.d("Success " + success);
        }
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        // Save a file: path for use with ACTION_VIEW intents
        absolutImagePath = image.getAbsolutePath();
        currentPhotoPath = "file:" + absolutImagePath;
        return image;
    }

    public boolean renameFile(Context context) throws IOException, NoSuchAlgorithmException {
        File file = new File(absolutImagePath);
        String newName = getName(absolutImagePath);

        File storageDir = new File(context.getExternalCacheDir() + "/photos");
        if (!storageDir.exists()) {
            boolean success = storageDir.mkdir();
            Timber.d("Success " + success);
        }
        File newFile = new File(storageDir.getAbsolutePath() + "/" + newName);
        boolean isRenamed = file.renameTo(newFile);
        absolutImagePath = newFile.getAbsolutePath();
        currentPhotoPath = "file:" + absolutImagePath;
        return isRenamed;
    }

    public void setOrientation(Context context, Uri uri) throws IOException, NoSuchAlgorithmException {
        File file = prepareFile(context, uri);
        if (file != null) {
            absolutImagePath = file.getAbsolutePath();
            setOrientation();
        }
    }

    private String getName(String path) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        FileInputStream fis = new FileInputStream(path);

        byte[] dataBytes = new byte[1024];

        int nread = 0;
        while ((nread = fis.read(dataBytes)) != -1) {
            md.update(dataBytes, 0, nread);
        }
        ;
        byte[] mdbytes = md.digest();

        //convert the byte to hex format method 1
        StringBuilder sb = new StringBuilder();
        for (byte mdbyte : mdbytes) {
            sb.append(Integer.toString((mdbyte & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

    //Due to Samsung mobile specific issue with Photo Rotation we need to get Exif data, check orientation
    // and change it if needed
    public void setOrientation() throws IOException {
        if (TextUtils.isEmpty(absolutImagePath)) {
            return;
        }
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(absolutImagePath, bmOptions);

        ExifInterface ei = new ExifInterface(absolutImagePath);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);

        switch (orientation) {

            case ExifInterface.ORIENTATION_ROTATE_90:
                writeFile(rotateImage(bitmap, 90));
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                writeFile(rotateImage(bitmap, 180));
                break;

            case ExifInterface.ORIENTATION_ROTATE_270:
                writeFile(rotateImage(bitmap, 270));
                break;

            case ExifInterface.ORIENTATION_NORMAL:

            default:
                break;
        }
    }

    private File prepareFile(Context context, Uri uri) throws NoSuchAlgorithmException {
        File file = FileUtils.getFile(context, uri);
        File compressedFile;
        try {
            long bytes = file.length();
            if (bytes < FILE_SIZE_UNITS) {
                return file;
            }

            if (isCompressNeeded(bytes)) {
                long start = System.currentTimeMillis();
                Timber.d("Compression started..." + start);
                String path = compressImage(context, file.getAbsolutePath());
                compressedFile = new File(path);
                Timber.d("Compression finished in " + (System.currentTimeMillis() - start));
                return compressedFile;
            } else {
                return file;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean isCompressNeeded(long bytes) {
        long fileSizeInKB = bytes / FILE_SIZE_UNITS;
        long fileSizeInMB = fileSizeInKB / FILE_SIZE_UNITS;
        Timber.d(fileSizeInMB + " MB");
        return fileSizeInMB >= MAX_FILE_SIZE;
    }


    private void writeFile(Bitmap bitmap) {
        try {
            File file = new File(absolutImagePath);
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    private String compressImage(Context context, @NonNull String filePath) throws IOException, NoSuchAlgorithmException {
        Bitmap scaledBitmap = null;

        BitmapFactory.Options options = new BitmapFactory.Options();

//      by setting this field as true, the actual bitmap pixels are not loaded in the memory. Just the bounds are loaded. If
//      you try the use the bitmap here, you will get null.
        options.inJustDecodeBounds = true;
        Bitmap bmp = BitmapFactory.decodeFile(filePath, options);

        int actualHeight = options.outHeight;
        int actualWidth = options.outWidth;

//      max Height and width values of the compressed image is taken as 1024x768 1600X1200

        float maxHeight = 1600.0f;
        float maxWidth = 1200.0f;
        float imgRatio = actualWidth / actualHeight;
        float maxRatio = maxWidth / maxHeight;
        Timber.d("imgRatio " + imgRatio + " maxRatio " + maxRatio + "actualWidth " + actualWidth + " actualHeight " + actualHeight);
//      width and height values are set maintaining the aspect ratio of the image

        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            if (imgRatio < maxRatio) {
                imgRatio = maxHeight / actualHeight;
                actualWidth = (int) (imgRatio * actualWidth);
                actualHeight = (int) maxHeight;
            } else if (imgRatio > maxRatio) {
                imgRatio = maxWidth / actualWidth;
                actualHeight = (int) (imgRatio * actualHeight);
                actualWidth = (int) maxWidth;
            } else {
                actualHeight = (int) maxHeight;
                actualWidth = (int) maxWidth;

            }
        }
        Timber.d("imgRatio " + imgRatio + " maxRatio " + maxRatio + "actualWidth " + actualWidth + " actualHeight " + actualHeight);

//      setting inSampleSize value allows to load a scaled down version of the original image

        options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight);

//      inJustDecodeBounds set to false to load the actual bitmap
        options.inJustDecodeBounds = false;

//      this options allow android to claim the bitmap memory if it runs low on memory
        options.inTempStorage = new byte[16 * 1024];

        try {
//          load the bitmap from its path
            bmp = BitmapFactory.decodeFile(filePath, options);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();

        }
        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();
        }

        float ratioX = actualWidth / (float) options.outWidth;
        float ratioY = actualHeight / (float) options.outHeight;
        float middleX = actualWidth / 2.0f;
        float middleY = actualHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bmp, middleX - bmp.getWidth() / 2, middleY - bmp.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

//      check the rotation of the image and display it properly
        ExifInterface exif;
        try {
            exif = new ExifInterface(filePath);

            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, 0);
            Log.d("EXIF", "Exif: " + orientation);
            Matrix matrix = new Matrix();
            if (orientation == 6) {
                matrix.postRotate(90);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 3) {
                matrix.postRotate(180);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 8) {
                matrix.postRotate(270);
                Log.d("EXIF", "Exif: " + orientation);
            }
            scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0,
                    scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix,
                    true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileOutputStream out = null;
        File image = createImageFile(context);

        try {
            out = new FileOutputStream(absolutImagePath);
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return image.getAbsolutePath();

    }


    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        final float totalPixels = width * height;
        final float totalReqPixelsCap = reqWidth * reqHeight * 2;
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++;
        }

        return inSampleSize;
    }
}
