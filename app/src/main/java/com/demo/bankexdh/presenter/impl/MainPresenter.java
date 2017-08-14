package com.demo.bankexdh.presenter.impl;

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

import com.demo.bankexdh.model.ImageManager;
import com.demo.bankexdh.model.event.DeviceIdUpdateEvent;
import com.demo.bankexdh.model.rest.RegisterBody;
import com.demo.bankexdh.model.rest.RegisterData;
import com.demo.bankexdh.model.rest.RestHelper;
import com.demo.bankexdh.model.rest.Shake;
import com.demo.bankexdh.model.rest.api.Register;
import com.demo.bankexdh.model.store.DeviceModel;
import com.demo.bankexdh.model.store.UserModel;
import com.demo.bankexdh.presenter.base.AbstractPresenter;
import com.demo.bankexdh.presenter.base.NotificationView;
import com.demo.bankexdh.utils.Const;
import com.demo.bankexdh.utils.ShakeDetector;
import com.devicehive.rest.ApiClient;
import com.devicehive.rest.api.DeviceNotificationApi;
import com.devicehive.rest.auth.ApiKeyAuth;
import com.devicehive.rest.model.DeviceNotificationWrapper;
import com.devicehive.rest.model.InsertNotification;
import com.devicehive.rest.model.JsonStringWrapper;
import com.google.gson.Gson;
import com.ipaulpro.afilechooser.utils.FileUtils;

import org.greenrobot.eventbus.EventBus;
import org.joda.time.DateTime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class MainPresenter extends AbstractPresenter<NotificationView> implements ShakeDetector.Listener {

    public static final String ACCELEROMETER = "ACCELEROMETER";
    public static final String NOTIFICATION_TITLE = "SHAKE IT BABY";
    private final ApiClient client;

    private DeviceNotificationApi deviceNotificationApi;
    private Call<InsertNotification> notificationCallInsert;

    private boolean enabled;
    private boolean executed = true;
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);

    public static final String FILEDATA = "filedata";
    public static final String IMAGE_TYPE = "image/*";
    private String currentPhotoPath;
    private String absolutImagePath;
    public static final int FILE_SIZE_UNITS = 1024;
    public static final int MAX_FILE_SIZE = 9;

    public MainPresenter() {
        client = RestHelper.getInstance().getApiClient();
        enabled = isEnabled();
    }

    public void prepare() {
        if (isDeviceRegistered()) {
            setupPresenter();
        } else {
            register();
        }

    }

    private void setupPresenter() {
        String token = getToken();
        addAuth(token);
        createServices();
    }

    private void register() {
        Register registerCall = client.createService(Register.class);
        RegisterBody body = getRegisterBody();
        registerCall.register(Const.REGISTER_URL, body).enqueue(new Callback<RegisterData>() {
            @Override
            public void onResponse(Call<RegisterData> call, Response<RegisterData> response) {
                Timber.d(response.toString());
                if (response.isSuccessful()) {
                    Timber.d(response.body().toString());
                    RegisterData data = response.body();

                    insertUserModel(data);
                    insertDevice(data);
                    addAuth(data.getToken().getAccessToken());
                    createServices();
                    EventBus.getDefault().post(DeviceIdUpdateEvent.newInstance());
                }
            }

            @Override
            public void onFailure(Call<RegisterData> call, Throwable t) {
                Timber.d(t.getMessage());
            }
        });
    }

    private RegisterBody getRegisterBody() {
        String uiid = UUID.randomUUID().toString();
        RegisterBody body = new RegisterBody();
        body.setKey(null);
        body.setId(uiid);
        body.setName(Const.DEVICE_NAME);
        return body;
    }


    private void createServices() {
        deviceNotificationApi = client.createService(DeviceNotificationApi.class);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        try (Realm realm = Realm.getDefaultInstance()) {
            UserModel model = realm.where(UserModel.class).equalTo(UserModel.ID, UserModel.DEFAULT_ID).findFirst();
            if (model != null) {
                realm.executeTransaction(t -> {
                    model.setIsEnabled(enabled);
                    t.copyToRealmOrUpdate(model);
                });
            }
        }
    }

    public boolean isEnabled() {
        try (Realm realm = Realm.getDefaultInstance()) {
            UserModel model = realm.where(UserModel.class).equalTo(UserModel.ID, UserModel.DEFAULT_ID).findFirst();
            if (model != null) {
                return model.getIsEnabled();
            }
        }
        return false;
    }

    private void addAuth(String accessToken) {
        client.addAuthorization(ApiClient.AUTH_API_KEY,
                ApiKeyAuth.newInstance(accessToken));
    }

    private void insertUserModel(RegisterData registerData) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(t -> {
                UserModel model = new UserModel();
                model.setId(UserModel.DEFAULT_ID);
                model.setAccessToken(registerData.getToken().getAccessToken());
                model.setRefreshToken(registerData.getToken().getRefreshToken());
                model.setIsEnabled(enabled);
                t.copyToRealmOrUpdate(model);
            });
        }
    }


    private void insertDevice(RegisterData registerData) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(t -> {
                DeviceModel newModel = new DeviceModel();
                newModel.setId(DeviceModel.DEFAULT_ID);
                newModel.setDeviceId(registerData.getDeviceId());
                newModel.setName(ACCELEROMETER);
                t.copyToRealmOrUpdate(newModel);
            });
        }
    }

    private boolean isDeviceRegistered() {
        try (Realm realm = Realm.getDefaultInstance()) {
            DeviceModel model = realm.where(DeviceModel.class)
                    .equalTo(DeviceModel.ID, DeviceModel.DEFAULT_ID).findFirst();
            return model != null;
        }
    }

    private String getToken() {
        try (Realm realm = Realm.getDefaultInstance()) {
            UserModel model = realm.where(UserModel.class).equalTo(UserModel.ID, UserModel.DEFAULT_ID).findFirst();
            return model.getAccessToken();
        }
    }

    public String getDeviceId() {
        try (Realm realm = Realm.getDefaultInstance()) {
            DeviceModel model = realm.where(DeviceModel.class)
                    .equalTo(DeviceModel.ID,
                            DeviceModel.DEFAULT_ID).findFirst();
            if (model == null) {
                return null;
            } else {
                return model.getDeviceId();
            }
        }
    }

    private DeviceNotificationWrapper getShakeNotification(String shakeMessage) {
        DeviceNotificationWrapper wrapper = new DeviceNotificationWrapper();
        JsonStringWrapper jsonStringWrapper = new JsonStringWrapper();
        Shake shake = new Shake();
        shake.setShake(String.valueOf(shakeMessage));
//        shake.setFakeData(genetateFakeData());
        shake.setShakenAt(DateTime.now().toString());
        jsonStringWrapper.setJsonString(new Gson().toJson(shake));
        wrapper.setParameters(jsonStringWrapper);
        wrapper.setNotification(NOTIFICATION_TITLE);
        return wrapper;
    }

    private List<Long> genetateFakeData() {
        List<Long> result = new ArrayList<>();
        Random random = new Random();
        int max = random.nextInt(200);
        for (int i = 0; i < max; i++) {
            result.add(random.nextLong());
        }
        return result;
    }

    @Override
    public void hearShake(String timestamp) {
        if (enabled && executed) {
            executed = false;
            if (!TextUtils.isEmpty(getDeviceId())) {
                notificationCallInsert = deviceNotificationApi.insert(getDeviceId(), getShakeNotification("Shaked"));
                notificationCallInsert.enqueue(new Callback<InsertNotification>() {
                    @Override
                    public void onResponse(@NonNull Call<InsertNotification> call, @NonNull Response<InsertNotification> response) {
                        Timber.d("NOTIFICATION INSERT RESPONSE " + response.code());
                        if (response.isSuccessful()) {
                            if (!isViewNull()) {
                                view.onNotificationSent();
                            }
                            isRegistrationInProgress.set(false);
                        } else {
                            if (!isViewNull()) view.onError();
                            if (response.code() == 401) {
                                if (!isRegistrationInProgress.get()) {
                                    isRegistrationInProgress.set(true);
                                    register();
                                }
                            }
                        }
                        executed = true;
                    }

                    @Override
                    public void onFailure(@NonNull Call<InsertNotification> call, @NonNull Throwable t) {
                        Timber.d("NOTIFICATION INSERT FAIL " + t.getMessage());
                        if (!isViewNull()) view.onError();
                        executed = true;
                    }
                });
            }
        }
    }

    public void uploadFile(Uri filePath, Context context) {
        Timber.d(filePath + "");
        Observable.just(context)
                .observeOn(Schedulers.io())
                .map((ctx) -> {
                    InputStream imageStream = ctx.getContentResolver().openInputStream(filePath);
                    int imageLength = imageStream.available();
                    String imageUrl = ImageManager.getInstance()
                            .uploadImage(filePath.getLastPathSegment(), imageStream, imageLength);

                    return imageUrl;
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe((s) -> Timber.d(s), Throwable::printStackTrace);

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
        Timber.d(storageDir.getAbsolutePath());
        File image = File.createTempFile(imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */);

        // Save a file: path for use with ACTION_VIEW intents
        absolutImagePath = image.getAbsolutePath();
        currentPhotoPath = "file:" + absolutImagePath;
        return image;
    }

    public void setOrientation(Context context, Uri uri) throws IOException {
        File file = prepareFile(context, uri);
        if (file != null) {
            absolutImagePath = file.getAbsolutePath();
            setOrientation();
        }
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

    private MultipartBody.Part getBody(Context context, Uri uri) {

        File file = prepareFile(context, uri);
        if (file == null) {
            return null;
        }

        RequestBody requestFile =
                RequestBody.create(
                        MediaType.parse(IMAGE_TYPE),
                        file
                );

        return MultipartBody.Part.createFormData(FILEDATA, file.getName(), requestFile);
    }

    private File prepareFile(Context context, Uri uri) {
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

    private String compressImage(Context context, @NonNull String filePath) throws IOException {
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

    @Override
    protected void onDestroyed() {
    }


}
