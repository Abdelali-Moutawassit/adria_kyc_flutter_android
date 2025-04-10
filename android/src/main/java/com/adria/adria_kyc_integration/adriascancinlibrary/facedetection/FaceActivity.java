package com.adria.adria_kyc_integration.adriascancinlibrary.facedetection;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.Image;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.extensions.HdrImageCaptureExtender;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.adria.adria_kyc_integration.R;
import com.adria.adria_kyc_integration.adriascancinlibrary.ImageToBitmap;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

//import ir.mahdi.mzip.zip.ZipArchive;

public class FaceActivity extends AppCompatActivity {
    CountDownTimer timer;
    private Executor executor = Executors.newSingleThreadExecutor();
    private int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};
    public static List<Bitmap> frames;
    PreviewView mPreviewView;
    private final double OPEN_THRESHOLD = 0.85;
    private final double CLOSE_THRESHOLD = 0.5;
    private ImageView img1;
    int id = -1;
    boolean blinkDetected = false;
    private String cinPath,zipPath,selfiePath;
    TextView message;
    int eyesState = 0;
    LinearLayout loading;
    boolean recording = true;
    public static File file;
    ImageView cadre;
    ImageView bar;
    int i = 0, x = 0;
    FaceDetector detector;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face);
        frames = new ArrayList<Bitmap>();
        mPreviewView = findViewById(R.id.previewView);
        cinPath = this.getCacheDir().getAbsolutePath() + "/image_cin.jpg";
        zipPath = getBaseContext().getCacheDir().getAbsolutePath() + "/zipFolder.zip";
        selfiePath = getBaseContext().getCacheDir().getAbsolutePath() + "/frame0.jpeg";
        img1 = findViewById(R.id.img);
        message = findViewById(R.id.message);
        loading = findViewById(R.id.loading);
        cadre = findViewById(R.id.cadre);
        bar = findViewById(R.id.bar);
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .enableTracking()
                        .build();
        detector = FaceDetection.getClient(options);
        cadre.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                cadre.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int height = cadre.getHeight();
                final int width = cadre.getWidth();
                bar.setPadding(0, (8 * height) / 100, 0, (17 * height) / 100);
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
                layoutParams.setMargins((9 * width) / 100, 0, (9 * width) / 100, 0);
                bar.setLayoutParams(layoutParams);
            }
        });
        TranslateAnimation mAnimation = new TranslateAnimation(
                TranslateAnimation.RELATIVE_TO_SELF, -0.38f,
                TranslateAnimation.RELATIVE_TO_SELF, 0.38f,
                TranslateAnimation.ABSOLUTE, 0f,
                TranslateAnimation.ABSOLUTE, 0f);
        mAnimation.setDuration(2000);
        mAnimation.setRepeatCount(-1);
        mAnimation.setRepeatMode(Animation.REVERSE);
        mAnimation.setInterpolator(new LinearInterpolator());
        bar.setAnimation(mAnimation);
        timer = new CountDownTimer(1000, 1000) {
            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish() {
                Log.i("heyyyyyyyy", "hey");
                message.setVisibility(View.GONE);
            }
        };
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(FaceActivity.this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void startCamera() {

        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {

                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);

                } catch (ExecutionException | InterruptedException e) {
                    // No errors need to be handled for this Future.
                    // This should never be reached.
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void showMessage(String msg) {
        Log.i("heyyyyyyyy", "here");
        message.setVisibility(View.VISIBLE);
        message.setText(msg);
        timer.cancel();
        timer.start();
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {

        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        ImageCapture.Builder builder = new ImageCapture.Builder();

        //Vendor-Extensions (The CameraX extensions dependency in build.gradle)
        HdrImageCaptureExtender hdrImageCaptureExtender = HdrImageCaptureExtender.create(builder);

        // Query if extension is available (optional).
        if (hdrImageCaptureExtender.isExtensionAvailable(cameraSelector)) {
            // Enable the extension if available.
            hdrImageCaptureExtender.enableExtension(cameraSelector);
        }
        final ImageCapture imageCapture = builder
                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())
                .build();
        preview.setSurfaceProvider(mPreviewView.createSurfaceProvider());
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageAnalysis);
        imageAnalysis.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                   /* runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Bitmap btm = rotateImage(ImageToBitmapKt.toBitmap(imageProxy), 270);
                                img1.setImageBitmap(btm);

                        }
                    });*/
                //    if (((x++)%5)==0){

                //Bitmap btm = rotateImage(ImageToBitmapKt.toBitmap(image), 270);
                if (recording) {
                    @SuppressLint("UnsafeExperimentalUsageError") Image mediaImage = imageProxy.getImage();
                    if (mediaImage != null) {
                        InputImage image =
                                InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                        detector.process(image)
                                .addOnSuccessListener(firebaseVisionFaces -> {
                                    if (firebaseVisionFaces.size() == 1) {
                                        Face face = firebaseVisionFaces.get(0);
                                        Log.i("*********yy", "analyze: " + face.getHeadEulerAngleZ()); //+ " " + face.getHeadEulerAngleZ() + " " + face.getHeadEulerAngleX()
                                        if (face.getTrackingId() == id) {
                                            if (lookingForward(face) && isFaceNotRotated(face)) {
                                                if (!blinkDetected) {
                                                    showMessage(getResources().getString(R.string.blink));
                                                    blinkDetected = blinkDetected(face);
                                                } else {
                                                    if (eyesOpen(face)) {
                                                        hidemessage();
                                                        if (frames.size() < 3) {
                                                            Bitmap btm = rotateImage(ImageToBitmap.toBitmap(imageProxy), 270);
                                                            frames.add(btm);
                                                        } else {
                                                            recording = false;
                                                            showLoading();
                                                            new Thread(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    createzipFile();
                                                                    // Bitmap btm=Utils.yuv420ToBitmap(image.getImage());
                                                                }
                                                            }).run();
                                                        }
                                                    }
                                                }
                                            } else {
                                                showMessage(getResources().getString(R.string.look_straight));
                                            }

                                        } else {
                                            hidemessage();
                                            id = face.getTrackingId();
                                            startOver();
                                        }
                                    } else if (firebaseVisionFaces.size() > 1) {
                                        showMessage(getResources().getString(R.string.trop_visage));
                                    } else {
                                        showMessage(getResources().getString(R.string.no_face_detected));
                                    }
                                }).addOnCompleteListener(new OnCompleteListener<List<Face>>() {
                            @Override
                            public void onComplete(@NonNull Task<List<Face>> task) {
                                imageProxy.close();
                            }
                        });
                    }
                }
                /*}else {
                          image.close();
                      }*/

            }
        });


    }

    private boolean lookingForward(Face face) {
        return face.getHeadEulerAngleY() > -4 && face.getHeadEulerAngleY() < 4;
    }

    private boolean isFaceNotRotated(Face face){
        return face.getHeadEulerAngleZ() > -2 && face.getHeadEulerAngleZ() < 2;
    }

    private boolean isMouthShowing(Face face) {
        if (face.getContour(FaceContour.LOWER_LIP_BOTTOM) != null) {
            Log.d("*******mouth", "isMouthShowing: " + face.getContour(FaceContour.LOWER_LIP_BOTTOM));
            return true;
        }

        return false;
    }

    void createzipFile() {
//        ZipArchive zipArchive = new ZipArchive();
        //delete old zip file
        File fzp = new File(getBaseContext().getCacheDir().getAbsolutePath() + "/zipFolder.zip");
        if (fzp.exists()) fzp.delete();
        int i = 0;
        for (Bitmap bitmap : frames) {
            File file = new File(getBaseContext().getCacheDir().getAbsolutePath() +
                    "/frame" + i++
                    + ".jpeg");
            file.delete();
            // Bitmap bitmap = Utils.yuv420ToBitmap(image);
            try {
                try (FileOutputStream out = new FileOutputStream(file)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                zipArchive.zip(file.getAbsolutePath(), getBaseContext().getCacheDir().getAbsolutePath() +
//                                "/zipFolder.zip",
//                        "");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
//            zipArchive.zip(cinPath, Environment.getExternalStorageDirectory().getAbsolutePath() +
//        zipArchive.zip(cinPath, getBaseContext().getCacheDir().getAbsolutePath() +
//                        "/zipFolder.zip",
//                "");
//            new File(cinPath).delete();
        Intent returnIntent = new Intent();
        returnIntent.putExtra("zipFolder", zipPath);
        returnIntent.putExtra("selfiePath", selfiePath);
        this.setResult(Activity.RESULT_OK, returnIntent);
        this.finish();

    }

    private boolean blinkDetected(Face face) {
        Log.i("BlinkTracker", eyesState + " " + face.getLeftEyeOpenProbability());
        switch (eyesState) {
            case 0:
                if (eyesOpen(face)) {
                    // Both eyes are initially open
                    Log.i("BlinkTracker", "eye open");
                    eyesState = 1;
                }
                return false;

            case 1:
                if ((face.getLeftEyeOpenProbability() < CLOSE_THRESHOLD) && (face.getRightEyeOpenProbability() < CLOSE_THRESHOLD)) {
                    // Both eyes become closed
                    Log.i("BlinkTracker", "closed!");
                    eyesState = 2;
                }
                return false;

            case 2:
                if (eyesOpen(face)) {
                    // Both eyes are open again
                    Log.i("BlinkTracker", "blink occurred!");
                    eyesState = 0;
                    return true;
                }
                return false;
        }
        return false;
    }

    boolean eyesOpen(Face face) {
        Log.i("****************", "eyesOpen: " + face.getLeftEyeOpenProbability());
        return (face.getLeftEyeOpenProbability() > OPEN_THRESHOLD) && (face.getRightEyeOpenProbability() > OPEN_THRESHOLD);
    }

    private void hidemessage() {
        message.setVisibility(View.GONE);
    }

    void showLoading() {
        loading.setVisibility(View.VISIBLE);
    }

    private void startOver() {
        i = 0;
        blinkDetected = false;
        recording = true;
        frames = new ArrayList<Bitmap>();
        // queue=new LinkedList<byte[]>();
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

    private boolean allPermissionsGranted() {

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}