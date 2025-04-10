package com.adria.adria_kyc_integration.adriascancinlibrary;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
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
import com.adria.adria_kyc_integration.adriascancinlibrary.ui.AutoFitTextureView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public abstract class CameraXActivity extends AppCompatActivity {

    private int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};
    private PreviewView mPreviewView;
    private Executor executor;
    private Handler handler;
    private HandlerThread handlerThread;

    ProcessCameraProvider cameraProvider;
    CameraSelector cameraSelector;
    ImageAnalysis imageAnalysis;
    Preview preview;
    ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private LinearLayout frame;
    private LinearLayout left_bg, right_bg, bottom_bg, top_bg, top1_bg, bottom1_bg;
    private AutoFitTextureView textureView;
    private LinearLayout scannerEffect;
    private Boolean frameSet = false;
    private AppCompatActivity activity;
    protected TextRecognizer recognizer;

    protected boolean isVerso = false;
    private ImageProxy imageProxy;

    private ExecutorService cameraExecutor;

    protected Boolean startDetecteingFace = false, stopAnalyzing = false;

    protected CameraXActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_x);
        activity = this;

        mPreviewView = findViewById(R.id.previewView);
        frame = findViewById(R.id.ic_frame);
        left_bg = findViewById(R.id.left_bg);
        right_bg = findViewById(R.id.right_bg);
        top_bg = findViewById(R.id.top_bg);
        top1_bg = findViewById(R.id.top1_bg);
        bottom1_bg = findViewById(R.id.bottom1_bg);
        bottom_bg = findViewById(R.id.bottom_bg);
        scannerEffect = findViewById(R.id.iv_scanner_effect);
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        textureView = (AutoFitTextureView) findViewById(R.id.texture);

        if(allPermissionsGranted()){
            startCamera();
        } else{
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public synchronized void onPause() {

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            Log.i("++++", e.getLocalizedMessage());
        }

        super.onPause();
    }
    private boolean allPermissionsGranted(){

        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_CODE_PERMISSIONS){
            if(allPermissionsGranted()){
                startCamera();
            } else{
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
    }

    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        //cameraProviderFuture = ProcessCameraProvider.getInstance(this);

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

    @SuppressLint("RestrictedApi")
    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        preview = new Preview.Builder()
                //.setTargetAspectRatio(AspectRatio.RATIO_4_3)
                //.setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                .build();

        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                //.setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                .build();

        ImageCapture.Builder builder = new ImageCapture.Builder();

        final ImageCapture imageCapture = builder
                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())
                .build();

        //Vendor-Extensions (The CameraX extensions dependency in build.gradle)
        HdrImageCaptureExtender hdrImageCaptureExtender = HdrImageCaptureExtender.create(builder);

        // Query if extension is available (optional).
        if (hdrImageCaptureExtender.isExtensionAvailable(cameraSelector)) {
            // Enable the extension if available.
            hdrImageCaptureExtender.enableExtension(cameraSelector);
        }
        preview.setSurfaceProvider(mPreviewView.createSurfaceProvider());
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageAnalysis);

        imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @SuppressLint("UnsafeExperimentalUsageError")
            @Override
            public void analyze(@NonNull ImageProxy image) {
                imageProxy = image;
                if (!frameSet){
                    textureView.setAspectRatio(image.getWidth(), image.getHeight(),activity);
                    activity.runOnUiThread(()->{
                        setFrame();
                    });
                }
                if (!stopAnalyzing){
                    if (!isVerso){
                        if (!startDetecteingFace)
                            processRectoFrames(image);
                        else
                            checkFaceImage(image);
                    }
                    else {
                        processVersoFrames(image);
                    }
                }
            }
        });

    }

    private void setFrame() {
        frameSet = true;
        ViewGroup.LayoutParams frameParams = frame.getLayoutParams();
        ViewGroup.LayoutParams layoutParamsLeft = left_bg.getLayoutParams();
        ViewGroup.LayoutParams layoutParamsRight = right_bg.getLayoutParams();
        ViewGroup.LayoutParams layoutParamsTop = top_bg.getLayoutParams();
        ViewGroup.LayoutParams layoutParamsBottom = bottom_bg.getLayoutParams();


        frameParams.height = (int) (textureView.getShownHeight() * 0.9);
        frameParams.width = (int) (textureView.getShownHeight() * 1.45);
        frame.setVisibility(View.VISIBLE);

        Animation animation = AnimationUtils.loadAnimation(this, R.anim.scan_anim);
        this.runOnUiThread(()->{
            scannerEffect.startAnimation(animation);
        });

        layoutParamsLeft.width = (textureView.getShowWidth()-frameParams.width)*1/2;
        layoutParamsRight.width = (textureView.getShowWidth()-frameParams.width)*1/2;

        layoutParamsBottom.height = (textureView.getShownHeight () - frameParams.height)*1/2;
        layoutParamsTop.height = (textureView.getShownHeight () - frameParams.height)*1/2;

        bottom1_bg.setPadding(layoutParamsLeft.width,0,layoutParamsLeft.width,0);
        top1_bg.setPadding(layoutParamsLeft.width,0,layoutParamsLeft.width,0);


        left_bg.setLayoutParams(layoutParamsLeft);
        left_bg.requestLayout();
        left_bg.setVisibility(View.VISIBLE);

        right_bg.setLayoutParams(layoutParamsRight);
        right_bg.requestLayout();
        right_bg.setVisibility(View.VISIBLE);

        top_bg.setLayoutParams(layoutParamsTop);
        top_bg.requestLayout();
        top_bg.setVisibility(View.VISIBLE);

        bottom_bg.setLayoutParams(layoutParamsBottom);
        bottom_bg.requestLayout();
        bottom_bg.setVisibility(View.VISIBLE);
    }

    public void rebindPreview(){
        imageProxy.close();
    }
    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    protected abstract void processRectoFrames(ImageProxy imageProxy);
    protected abstract void processVersoFrames(ImageProxy imageProxy);
    protected abstract void checkFaceImage(ImageProxy imageProxy);
}