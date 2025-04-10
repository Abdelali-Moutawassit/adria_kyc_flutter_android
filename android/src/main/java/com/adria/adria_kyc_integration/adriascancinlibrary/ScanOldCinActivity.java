package com.adria.adria_kyc_integration.adriascancinlibrary;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.ImageProxy;

import com.adria.adria_kyc_integration.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.text.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScanOldCinActivity extends CameraXActivity {

    List<String> res = Arrays.asList("", "", "", "", "", "");
    List<String> versoRes = Arrays.asList("", "", "", "", "");
    List<DetectedValues> rectoOccurences = new ArrayList<>(), versoOccurences = new ArrayList<>();
    public static final String SHOW_SDK_VERSION = "showVersion";

    Matcher matcher;
    Pattern patternDates, patternAdr;

    private List<String> upperCaseValues;
    private List<String> dates;
    Boolean pauseAnalyse = false;
    Intent returnIntent;

    String regexCIN = "[A-Z]{1,2}[0-9]{3,6}";
    String regexDateNaissance = "([0-9]{2}[.][0-9]{2}[.][0-9]{4})|([0-9]{2}[.][0-9]{2}[\\s]{1,2}[0-9]{4})|([0-9]{4})";
    String regexDate = "([0-9]{2}[.][0-9]{2}[.][0-9]{4})";
    String regexName = "([A-Z']{2,}[\\s|-]{1,2}+([A-Z']{2,}([\\s|-]{1,2}+[A-Z']{2,})*))|([A-Z']{3,})";

    String regexFisDe = "(\\bF[A-Za-z]+s\\b)|(\\bf[A-Za-z]+s\\b)";
    String regexFilleDe = "(\\bF[A-Za-z]+e\\b)|(\\bf[A-Za-z]+e\\b)";
    String regexMother = "(([A-Z]{2,}[\\s|-]{1,2})*[A-Z]{3,})[\\s|-]{1,2}bent[\\s|-]{1,2}(([A-Z]{2,}[\\s|-]{1,2})*[A-Z]{3,})|(([A-Z]{2,}[\\s|-]{1,2})*[A-Z]{3,})";
    String regexFather = "(([A-Z]{2,}[\\s|-]{1,2})*[A-Z]{3,})[\\s|-]{1,2}ben[\\s|-]{1,2}(([A-Z]{2,}[\\s|-]{1,2})*[A-Z]{3,})|(([A-Z]{2,}[\\s|-]{1,2})*[A-Z]{3,})";


    Matcher matcherFisDe, matcherFilleDe, matcherFather, matcherMother, matcherVersoCIN;
    Pattern patternFisDe, patternFilleDe, patternFather, patternMother, patternVersoCIN;

    private CountDownTimer countDownTimer;
    private final int countDownInSec = 25;
    private Bitmap bitmap;
    private String adrNaissance = "";
    private int countIsCIN = 0;
    private Boolean rectoScanned = true;
    private ArrayList<String> listPays;
    private int attemptsNbr = 0;

    Resources langResources;
    private Bitmap[] bitmaps = new Bitmap[2];
    private FaceDetector detector;
    private ImageProxy imageProxy;
    private CountDownTimer detectFaceCountDownTimer;
    private Boolean countDownStarted = false;

    private FaceDetector faceDetector;
    private int nbrFramesForFace = 0;
    private Boolean detectingFace = true;
    private TextView sdkVersionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        langResources = getResources();

            FaceDetectorOptions options =
                    new FaceDetectorOptions.Builder()
                            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                            .enableTracking()
                            .build();
            faceDetector = FaceDetection.getClient(options);

        patternDates = Pattern.compile(regexDateNaissance);
        patternAdr = Pattern.compile(regexName);

        patternVersoCIN = Pattern.compile(regexCIN);
        patternFilleDe = Pattern.compile(regexFilleDe);
        patternFisDe = Pattern.compile(regexFisDe);
        patternFather = Pattern.compile(regexFather);
        patternMother = Pattern.compile(regexMother);

        returnIntent = new Intent();

        sdkVersionText = findViewById(R.id.sdkVersion);

        Intent intent = getIntent();
        if(intent != null){
            boolean showSdkNumber = intent.getBooleanExtra(SHOW_SDK_VERSION, false);
            if(showSdkNumber){
                sdkVersionText.setVisibility(View.VISIBLE);
            } else {
                sdkVersionText.setVisibility(View.GONE);
            }
        } else {
            sdkVersionText.setVisibility(View.GONE);
        }

        reset();
    }

    public void reset() {
        res = Arrays.asList("", "", "", "", "", "");
        versoRes = Arrays.asList("", "", "", "", "");
        for (int j = 0; j < 5; j++) {
            versoOccurences.add(new DetectedValues("", 0));
        }
        for (int i = 0; i < 6; i++) {
            rectoOccurences.add(new DetectedValues("", 0));
        }
    }

    private void initCountDown() {
        final int[] i = {0};
        countDownTimer = new CountDownTimer(countDownInSec * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                switch (i[0]) {
                    case 10:
                        Toast.makeText(ScanOldCinActivity.this, R.string.demande_de_recadrage, Toast.LENGTH_SHORT).show();
                        break;
                    case 15:
                        Toast.makeText(ScanOldCinActivity.this, R.string.scan_msg_1, Toast.LENGTH_SHORT).show();
                        break;
                    case 20:
                        Toast.makeText(ScanOldCinActivity.this, R.string.scan_msg_2, Toast.LENGTH_SHORT).show();
                        break;
                }
                i[0]++;
            }

            @Override
            public void onFinish() {
                Log.i("----", "" + countIsCIN);
                pauseAnalyse = true;
                Intent returnIntentCanceled = new Intent();
                if (!isVerso) {
                    if (countIsCIN < 1) {
                        returnIntentCanceled.putExtra("is_a_cin", 0);
                        setResult(Activity.RESULT_CANCELED, returnIntentCanceled);
                        finish();
                    } else{
                        if (checkDetectedValues()>=2){
                            startDetecteingFace = true;
                            detectFace(bitmap);
                            imageProxy.close();
                            //returnRectoResult(res, bitmap);
                        }else {
                            returnIntentCanceled.putExtra("is_a_cin", 1);
                            setResult(Activity.RESULT_CANCELED, returnIntentCanceled);
                            finish();
                        }
                    }
                } else {
                    if(!versoRes.get(4).equals("") && !versoRes.get(3).equals("")){
                        showFinalResult(versoRes, bitmap);
                    }   else {
                        returnIntentCanceled.putExtra("is_a_cin", 1);
                        setResult(Activity.RESULT_CANCELED, returnIntentCanceled);
                        finish();
                    }

                }
            }
        }.start();
    }

    private void initDetectFaceCountDown(){
        detectFaceCountDownTimer = new CountDownTimer(5 * 1000, 1000) {
            @Override
            public void onTick(long l) {
            }

            @Override
            public void onFinish() {
                pauseAnalyse = true;
                stopAnalyzing = true;
                runOnUiThread(() -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ScanOldCinActivity.this);
                    LayoutInflater layoutInflaterAndroid = LayoutInflater.from(ScanOldCinActivity.this);
                    View view2 = layoutInflaterAndroid.inflate(R.layout.cam_dialog, null);
                    builder.setView(view2);
                    builder.setCancelable(true);
                    final AlertDialog alertDialog = builder.create();
                    alertDialog.setCancelable(false);

                    if (alertDialog.getWindow() != null)
                        alertDialog.getWindow().getDecorView().setBackgroundColor(Color.TRANSPARENT);
                    if (!alertDialog.isShowing())
                        alertDialog.show();

                    ImageView imageView = view2.findViewById(R.id.iv_emo);
                    imageView.setImageResource(R.drawable.sademoji);
                    TextView textView1 = view2.findViewById(R.id.tv1);
                    view2.findViewById(R.id.tv2).setVisibility(View.GONE);
                    textView1.setText(getString(R.string.visage_non_detectee));


                    TextView btnContinuer = view2.findViewById(R.id.iv_continuer);
                    btnContinuer.setText(getString(R.string.rescanner));

                    view2.findViewById(R.id.iv_annuler).setOnClickListener(v1 -> {
                        alertDialog.dismiss();
                        reset();
                        Intent returnIntentCanceled = new Intent();
                        //returnIntentCanceled.putExtra("is_a_cin",is_a_card);
                        setResult(Activity.RESULT_CANCELED, returnIntentCanceled);
                        finish();
                    });
                    view2.findViewById(R.id.iv_continuer).setOnClickListener(v12 -> {
                        reset();
                        stopAnalyzing = false;
                        pauseAnalyse = false;
                        countDownStarted = false;
                        startDetecteingFace = false;
                        rebindPreview();
                        alertDialog.dismiss();
                    });
                });
            }
        }.start();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        initCountDown();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    public int checkDetectedValues(){
        int nbrValues = 0;
        for (String s : res){
            if (!s.equals(""))
                nbrValues++;
        }
        return nbrValues;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("UnsafeExperimentalUsageError")
    @Override
    @OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    protected void processRectoFrames(ImageProxy image) {
        this.imageProxy = image;
        bitmap = ImageToBitmap.toBitmap(image);
        if (doubleChecked(res) && rectoScanned) {
//            pauseAnalyse = true;
            if (detectingFace)
                detectFace(bitmap);

//            image.close();
            //checkFaceImage(image);
            //returnRectoResult(res,bitmap);
        } else if (!pauseAnalyse) {
            upperCaseValues = new ArrayList<>();
            dates = new ArrayList<>();
            runInBackground(() -> {
                Image mediaImage = image.getImage();
                if (mediaImage != null) {
                    InputImage inputImage =
                            InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());
                    recognizer.process(inputImage)
                            .addOnSuccessListener(mlText -> {
                                if (Utils.checkIfCIN(mlText.getText()))
                                    countIsCIN++;

                                String[] results = mlText.getText().split("\\n");
                                Log.i("++++", mlText.getText());

                                int j = 0;
                                for (String result : results) {
                                    matcher = patternAdr.matcher(result);
                                    if (matcher.find()) {
                                        upperCaseValues.add(result);
                                    }

                                    if (res.get(3).equals("")) {
                                        if (result.startsWith("a ") || result.startsWith("à ") || result.startsWith("i ")){
                                            if (result.length() > 6) {
                                                adrNaissance = result.substring(2).toUpperCase();
                                                if (results[j + 1].matches(regexName))
                                                    adrNaissance = adrNaissance + " " + results[j + 1];
                                                check(adrNaissance, regexName, 3);
                                            }
                                        }
                                        if (result.contains("jusqu") || result.contains("jusqu'au")){
                                            matcher = patternAdr.matcher(results[j--]);
                                            if (matcher.find()){
                                                check(matcher.group(), regexName, 3);
                                            }
                                        }
                                    }

                                    if (res.get(5).equals("")) {
                                        Pattern pattern = Pattern.compile(regexCIN);
                                        Matcher matcher = pattern.matcher(result);
                                        if (matcher.find()) {
                                            check(matcher.group(), regexCIN, 5);
                                        }
                                    }
                                    j++;
                                }

                                if (res.get(0).equals("") || res.get(1).equals("")) {
                                    boolean getnames = false;
                                    for (int i = 0; i < upperCaseValues.size(); i++) {
                                        final String txt = upperCaseValues.get(i);
                                        if (txt.contains("CARTE NATIONALE D'IDENTITE")
                                                || txt.contains("NATIONALE D'IDENTITE")
                                                || txt.contains("NATIONALE")
                                                || txt.contains("IDENTITE")) {
                                            getnames = true;
                                            continue;
                                        }
                                        if (getnames) {
                                            if (res.get(0).equals("")) {
                                                check(upperCaseValues.get(i), regexName, 0);
                                            }
                                            if (res.get(1).equals("")) {
                                                if (upperCaseValues.size() > i + 1)
                                                    if (Utils.countWords(upperCaseValues.get(i + 1)) < 4)
                                                        check(upperCaseValues.get(i + 1), regexName, 1);
                                            } /*else if (res.get(3).equals("")) {
                                                Log.i("----", upperCaseValues.get(i));
                                                String adresse = "";
                                                if (upperCaseValues.get(i + 2).length() >= 6)
                                                    adresse = upperCaseValues.get(i + 2).substring(2);
                                                if (upperCaseValues.size() >= i + 4)
                                                    if (!upperCaseValues.get(i + 3).equals("ROYAUME DU MAROC") && !upperCaseValues.get(i + 3).equals(upperCaseValues.get(i + 1)))
                                                        adresse = adresse + " " + upperCaseValues.get(i + 3);

                                                check(adresse.toUpperCase(), regexName, 3);
                                            }*/
                                            break;
                                        }
                                    }
                                }

                                int indexDates = 0;
                                if (res.get(2).equals("") || res.get(4).equals("")) {
                                    matcher = patternDates.matcher(mlText.getText());
                                    while (matcher.find()) {
                                        Log.i("++++Date", matcher.group());
                                        if (res.get(2).equals("") || res.get(4).equals("")) {
                                            if (indexDates == 0) {
                                                String birthDate = matcher.group().replace(" ", ".");
                                                if (birthDate.length() == 4) {
                                                    check(birthDate, regexDateNaissance, 2);
                                                } else {
                                                    if (Utils.checkDay(birthDate) < 0)
                                                        check(birthDate, regexDate, 2);
                                                }
                                                indexDates++;
                                            } else if (matcher.group().length() > 4) {
                                                String expDate = matcher.group().replace(" ", ".");
                                                if (Utils.checkDay(expDate) >= 0)
                                                    check(expDate, regexDate, 4);
                                            }
                                        }
                                    }
                                }
                            })
                            .addOnFailureListener(
                                    e -> {
                                            // Task failed with an exception
                                            // ...
                                        })
                            .addOnCompleteListener(new OnCompleteListener<Text>() {
                                @Override
                                public void onComplete(@NonNull Task<Text> task) {
                                    image.close();
                                }
                            });

                    }

            });
        }
    }

    @OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    public void detectFace(Bitmap bitmap){
        if (countDownTimer != null){
            countDownTimer.cancel();
            countDownTimer = null;
        }
        nbrFramesForFace++;
        @SuppressLint("UnsafeExperimentalUsageError")
        InputImage faceImage = InputImage.fromMediaImage(Objects.requireNonNull(imageProxy.getImage()), imageProxy.getImageInfo().getRotationDegrees());
        faceDetector.process(faceImage)
                .addOnSuccessListener(firebaseVisionFaces -> {
                    if (firebaseVisionFaces.size() >= 1) {
                        for (Face face : firebaseVisionFaces){
                            int height = face.getBoundingBox().bottom - face.getBoundingBox().top;
                            int width = face.getBoundingBox().right - face.getBoundingBox().left;
                            double diag = Math.pow(height,2) + Math.pow(width,2);
                            diag = Math.sqrt(diag);
                            Log.i("+++++*+", diag + "---" + (faceImage.getHeight()/4));
                            if (diag >= (faceImage.getHeight()*10/38)) {
                                Log.i("+++++---", "detectFace: ");
                                pauseAnalyse = true;
                                rectoScanned =false;
                                rectoScanned(bitmap);
                            }else if (nbrFramesForFace == 20){
                                faceNotDetected();
                            }
                        }
                    }else{
                        imageProxy.close();
                        if (nbrFramesForFace == 20)
                            faceNotDetected();
                    }
                }).addOnCompleteListener(runnable -> imageProxy.close());
    }

    public void faceNotDetected(){
        detectingFace = false;
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(ScanOldCinActivity.this);
            LayoutInflater layoutInflaterAndroid = LayoutInflater.from(ScanOldCinActivity.this);
            View view2 = layoutInflaterAndroid.inflate(R.layout.cam_dialog, null);
            builder.setView(view2);
            builder.setCancelable(true);
            final AlertDialog alertDialog = builder.create();
            alertDialog.setCancelable(false);

            if (alertDialog.getWindow() != null)
                alertDialog.getWindow().getDecorView().setBackgroundColor(Color.TRANSPARENT);
            if (!alertDialog.isShowing())
                alertDialog.show();

            ImageView imageView = view2.findViewById(R.id.iv_emo);
            imageView.setImageResource(R.drawable.sademoji);
            TextView textView1 = view2.findViewById(R.id.tv1);
            view2.findViewById(R.id.tv2).setVisibility(View.GONE);
            textView1.setText(langResources.getString(R.string.visage_non_detectee));

            ((TextView) view2.findViewById(R.id.iv_continuer)).setText(langResources.getString(R.string.reessayer));
            ((TextView) view2.findViewById(R.id.iv_annuler)).setText(langResources.getString(R.string.annuler));

            view2.findViewById(R.id.iv_annuler).setOnClickListener(v1 -> {
                alertDialog.dismiss();
                reset();
                Intent returnIntentCanceled = new Intent();
                setResult(Activity.RESULT_CANCELED, returnIntentCanceled);
                finish();
            });
            view2.findViewById(R.id.iv_continuer).setOnClickListener(v12 -> {
                nbrFramesForFace = 0;
                detectingFace = true;
                pauseAnalyse = false;
                rebindPreview();
                alertDialog.dismiss();
            });
        });
    }

    public void rectoScanned(Bitmap mImage){
        if (countDownTimer != null) {
            countDownTimer.cancel();

        }
        Bitmap bitmapRectoCIN = mImage.copy(mImage.getConfig(), mImage.isMutable());

        Bitmap resizedImage = Utils.resizeBitmap(bitmapRectoCIN, 720);
        File file = new File(getBaseContext().getCacheDir().getAbsolutePath() + "/image_cin.jpg");

        file.delete();
        try (FileOutputStream out = new FileOutputStream(file)) {
            resizedImage.compress(Bitmap.CompressFormat.JPEG, 80, out);
        } catch (IOException e) {
            e.printStackTrace();
        }

        bitmaps[0] = resizedImage;

        returnIntent.putExtra("pic", file.getAbsolutePath());
        Log.i("***********Anas", file.getAbsolutePath());

        returnRectoResult(res,file);
        /*this.runOnUiThread(() -> {
            ((TextView) findViewById(R.id.tv_scan_info)).setText(langResources.getString(R.string.detect_cin));

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater layoutInflaterAndroid = LayoutInflater.from(this);
            View view2 = layoutInflaterAndroid.inflate(R.layout.cam_dialog, null);
            builder.setView(view2);
            builder.setCancelable(true);
            final AlertDialog alertDialog = builder.create();
            alertDialog.setCancelable(false);

                if (alertDialog.getWindow() != null)
                    alertDialog.getWindow().getDecorView().setBackgroundColor(Color.TRANSPARENT);
                if (!alertDialog.isShowing())
                    alertDialog.show();
            TextView textView = view2.findViewById(R.id.tv1);
            textView.setText(langResources.getString(R.string.recto_scanned));
            TextView textView2 = view2.findViewById(R.id.tv2);
            textView2.setText(langResources.getString(R.string.validate_cin));

            ((TextView) view2.findViewById(R.id.iv_annuler)).setText(langResources.getString(R.string.annuler));
            ((TextView) view2.findViewById(R.id.iv_continuer)).setText(langResources.getString(R.string.continuer));

                view2.findViewById(R.id.iv_annuler).setOnClickListener(v1 -> {
                    alertDialog.dismiss();
                    reset();
                    Intent returnIntentCanceled = new Intent();
                    //returnIntentCanceled.putExtra("is_a_cin",is_a_card);
                    setResult(Activity.RESULT_CANCELED, returnIntentCanceled);
                    finish();
                });
                view2.findViewById(R.id.iv_continuer).setOnClickListener(v12 -> {
                    startDetectingCIN = true;
                    camera.getCameraControl().enableTorch(true);
                    this.image.close();
                    alertDialog.dismiss();
                });
            });*/
    }

//    public void detectCINFailed(){
//        camera.getCameraControl().enableTorch(false);
//        this.runOnUiThread(() -> {
//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            LayoutInflater layoutInflaterAndroid = LayoutInflater.from(this);
//            View view2 = layoutInflaterAndroid.inflate(R.layout.cam_dialog, null);
//            builder.setView(view2);
//            builder.setCancelable(true);
//            final AlertDialog alertDialog = builder.create();
//            alertDialog.setCancelable(false);
//
//            if (alertDialog.getWindow() != null)
//                alertDialog.getWindow().getDecorView().setBackgroundColor(Color.TRANSPARENT);
//            if (!alertDialog.isShowing())
//                alertDialog.show();
//
//            ImageView imageView = view2.findViewById(R.id.iv_emo);
//            imageView.setImageDrawable(langResources.getDrawable(R.drawable.sademoji));
//            TextView textView = view2.findViewById(R.id.tv1);
//            textView.setText(langResources.getString(R.string.not_detected_cin_msg));
//            TextView textView2 = view2.findViewById(R.id.tv2);
//            textView2.setText(langResources.getString(R.string.use_real_card));
//
//            TextView button = view2.findViewById(R.id.iv_continuer);
//            button.setText(langResources.getString(R.string.reessayer));
//            ((TextView) view2.findViewById(R.id.iv_annuler)).setText(langResources.getString(R.string.annuler));
//
//            if (attemptsNbr == 2){
//                button.setVisibility(View.GONE);
//            }
//
//            view2.findViewById(R.id.iv_annuler).setOnClickListener(v1 -> {
//                alertDialog.dismiss();
//                reset();
//                Intent returnIntentCanceled = new Intent();
//                //returnIntentCanceled.putExtra("is_a_cin",is_a_card);
//                setResult(Activity.RESULT_CANCELED, returnIntentCanceled);
//                finish();
//            });
//            view2.findViewById(R.id.iv_continuer).setOnClickListener(v12 -> {
//                camera.getCameraControl().enableTorch(true);
//                resetCINDetection();
//                attemptsNbr++;
//                alertDialog.dismiss();
//            });
//        });
//    }

    @SuppressLint("UnsafeExperimentalUsageError")
    @Override
    @OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    protected void processVersoFrames(ImageProxy image) {
        bitmap = ImageToBitmap.toBitmap(image);
        if (doubleChecked(versoRes) && !pauseAnalyse) {
            pauseAnalyse = true;
            showFinalResult(versoRes, bitmap);
        } else if (!pauseAnalyse) {
            runInBackground(() -> {
                Image mediaImage = image.getImage();
                if (mediaImage != null) {
                    InputImage inputImage=
                            InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());
                    recognizer.process(inputImage)
                            .addOnSuccessListener(new OnSuccessListener<Text>() {
                                @Override
                                public void onSuccess(Text mlText) {
                                    if (!mlText.getText().equals("")) {
                                        //List<MLText.Block> blocks = mlText.getBlocks();
                                        Log.i("++++", mlText.getText());
                                        String[] results = mlText.getText().split("\\n");
                                        int i = 0;
                                        for (String result : results) {
                                            matcherVersoCIN = patternVersoCIN.matcher(result.trim());

                                            if (matcherVersoCIN.find()) {
                                                versoCheck(matcherVersoCIN.group(), 0);
                                            }

                                            matcherFisDe = patternFisDe.matcher(result.trim());
                                            matcherFilleDe = patternFilleDe.matcher(result.trim());
                                            //if (result.trim().startsWith("Fils de") || result.trim().startsWith("Flls"))
                                            if (matcherFisDe.find()) {
                                                //set sexe
                                                if (versoRes.get(4).equals(""))
                                                    versoRes.set(4, "M");
                                                //Father's name
                                                matcherFather = patternFather.matcher(result.trim());
                                                if (matcherFather.find())
                                                    versoCheck(matcherFather.group(), 1);
                                                else {
                                                    if (results.length > i + 1) {
                                                        matcherFather = patternFather.matcher(results[i + 1]);
                                                        if (matcherFather.find())
                                                            versoCheck(matcherFather.group(), 1);
                                                    }
                                                }
                                            } else if (matcherFilleDe.find()) {//result.trim().startsWith("Fille de") || result.trim().startsWith("Flle")
                                                //set sexe
                                                if (versoRes.get(4).equals(""))
                                                    versoRes.set(4, "F");
                                                //Father's name
                                                matcherFather = patternFather.matcher(result.trim());
                                                if (matcherFather.find())
                                                    versoCheck(matcherFather.group(), 1);
                                                /*else {
                                                    if (results.length > i + 2) {
                                                        matcherFather = patternFather.matcher(results[i + 1]);
                                                        if (matcherFather.find())
                                                            versoCheck(matcherFather.group(), 1);
                                                    }
                                                }*/
                                            } else if (result.startsWith("et de")) {
                                                //Mother's name
                                                matcherMother = patternMother.matcher(result.trim());
                                                if (matcherMother.find())
                                                    versoCheck(matcherMother.group(), 2);
                                                /*else if (results.length > i + 2) {
                                                    matcherMother = patternMother.matcher(results[i + 1]);
                                                    if (matcherMother.find())
                                                        versoCheck(matcherMother.group(), 2);
                                                }*/
                                            }
                                            if (result.contains(" ") && result.length() > 8){
                                                if (Utils.similarity(result.trim().split(" ")[0], "Adresse")
                                                        || Utils.similarity(result.trim().split(" ")[0], "drese")) {

                                                    int wordSize = result.trim().split(" ")[0].length();

                                                    String adresse = "";
                                                    if (result.length() > wordSize)
                                                        adresse = result.substring(wordSize);
                                                    if (results.length >= i + 1){
                                                        if (results[i + 1].matches(regexName))
                                                            adresse = adresse + " " + results[i + 1];
                                                    }

                                                    versoCheck(adresse.toUpperCase(), 3);

                                                }
                                            }
                                            i++;
                                        }
                                    }
                                }
                            })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Task failed with an exception
                                            // ...
                                        }
                                    })
                    .addOnCompleteListener(new OnCompleteListener<Text>() {
                        @Override
                        public void onComplete(@NonNull Task<Text> task) {
                            image.close();
                        }
                    });
                }
            });
        }
    }

    public void check(String txt, String regex, int valD) {
        //if the list already contains this value and the result is empty
        if (!txt.matches(regex))
            return;
        if (rectoOccurences.get(valD).getOccurence() == 1 && res.get(valD).equals("")) {
            if (valD == 3) {
                if (Utils.similarity(rectoOccurences.get(valD).getValue(), txt)) {
                    res.set(valD, txt.toUpperCase());
                } else {
                    rectoOccurences.get(valD).setValue(txt);
                }
            } else {
                if (rectoOccurences.get(valD).getValue().equals(txt)) {
                    res.set(valD, txt.toUpperCase());
                } else {
                    rectoOccurences.get(valD).setValue(txt);
                }
            }

        } else {
            DetectedValues detectedValues = new DetectedValues(txt, 1);
            rectoOccurences.set(valD, detectedValues);
        }
        Log.i("***********Younes", res.toString());
    }

    public void versoCheck(String txt, int valD) {
        if (valD == 3){
            //if the list already contains this value and the result is empty
            if (versoOccurences.get(valD).getOccurence() == 1 && versoRes.get(valD).equals("")) {
                if (Utils.similarity(versoOccurences.get(valD).getValue(), txt))
                    versoRes.set(valD, txt);
                else {
                    versoOccurences.get(valD).setValue(txt);
                }
            }
            //the first time we get this value
            else {
                DetectedValues detectedValues = new DetectedValues(txt, 1);
                versoOccurences.set(valD, detectedValues);
            }
        }else {
            //if the list already contains this value and the result is empty
            if (versoOccurences.get(valD).getOccurence() == 1 && versoRes.get(valD).equals("")) {
                if (versoOccurences.get(valD).getValue().equals(txt))
                    versoRes.set(valD, txt);
                else {
                    versoOccurences.get(valD).setValue(txt);
                }
            }
            //the first time we get this value
            else {
                DetectedValues detectedValues = new DetectedValues(txt, 1);
                versoOccurences.set(valD, detectedValues);
            }
        }
        Log.i("***********YounesVerso", versoRes.toString());
    }

    public Boolean doubleChecked(List<String> resultList) {
        for (String s : resultList) {
            if (s.equals(""))
                return false;
        }
        return true;
    }



    private void returnRectoResult(List<String> resToShow, File file) {
        /*if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }*/
        if (detectFaceCountDownTimer != null) {
            detectFaceCountDownTimer.cancel();
            detectFaceCountDownTimer = null;
        }

        ArrayList<String> rectoResult = new ArrayList<>(resToShow);
        returnIntent = new Intent();
        returnIntent.putStringArrayListExtra("result", rectoResult);

        returnIntent.putExtra("pic", file.getAbsolutePath());

        this.runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater layoutInflaterAndroid = LayoutInflater.from(this);
            View view2 = layoutInflaterAndroid.inflate(R.layout.cam_dialog, null);
            builder.setView(view2);
            builder.setCancelable(true);
            final AlertDialog alertDialog = builder.create();
            alertDialog.setCancelable(false);

            if (alertDialog.getWindow() != null)
                alertDialog.getWindow().getDecorView().setBackgroundColor(Color.TRANSPARENT);
            if (!alertDialog.isShowing())
                alertDialog.show();

            view2.findViewById(R.id.iv_annuler).setOnClickListener(v1 -> {
                alertDialog.dismiss();
                reset();
                Intent returnIntentCanceled = new Intent();
                //returnIntentCanceled.putExtra("is_a_cin",is_a_card);
                setResult(Activity.RESULT_CANCELED, returnIntentCanceled);
                finish();
            });
            view2.findViewById(R.id.iv_continuer).setOnClickListener(v12 -> {
                isVerso = true;
                stopAnalyzing = false;
                pauseAnalyse = false;
                initCountDown();
                reset();
                ((TextView) findViewById(R.id.tv_scan_info)).setText(getString(R.string.scan_verso_cin));
                this.rebindPreview();
                alertDialog.dismiss();
            });
        });
    }
    @OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    public void checkFaceImage(ImageProxy image){
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        if (!countDownStarted){
            countDownStarted = true;
            this.runOnUiThread(this::initDetectFaceCountDown);
        }

        Log.i("******", "checkFaceImage: ");
        @SuppressLint("UnsafeExperimentalUsageError")
        InputImage faceImage = InputImage.fromMediaImage(Objects.requireNonNull(image.getImage()), image.getImageInfo().getRotationDegrees());

            faceDetector.process(faceImage)
                    .addOnSuccessListener(firebaseVisionFaces -> {
                        if (firebaseVisionFaces.size() >= 1) {
                            for (Face face : firebaseVisionFaces){
                                int height = face.getBoundingBox().bottom - face.getBoundingBox().top;
                                int width = face.getBoundingBox().right - face.getBoundingBox().left;
                                if (height*width >= 10000){
                                    Bitmap rectoCin = ImageToBitmap.toBitmap(image);
                                    assert rectoCin != null;
                                    countDownStarted = false;
                                    startDetecteingFace = false;
                                    stopAnalyzing = true;

                                    Bitmap imageBitmap = ImageToBitmap.toBitmap(image);
                                    Bitmap bitmapRectoCIN = imageBitmap.copy(imageBitmap.getConfig(), imageBitmap.isMutable());

                                    Bitmap resizedImage = Utils.resizeBitmap(bitmapRectoCIN, 720);
                                    File file = new File(getBaseContext().getCacheDir().getAbsolutePath() + "/image_cin.jpg");

                                    file.delete();
                                    try (FileOutputStream out = new FileOutputStream(file)) {
                                        resizedImage.compress(Bitmap.CompressFormat.JPEG, 80, out);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    detectFaceCountDownTimer.cancel();
                                    returnRectoResult(res, file);
                                }
                            }
                        }else
                            image.close();
                    }).addOnFailureListener(e -> {
                        image.close();
                    });
    }

    private void showFinalResult(List<String> resToShow, Bitmap mImage) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        Bitmap resizedImage = Utils.resizeBitmap(mImage, 720);
        File file = new File(getBaseContext().getCacheDir().getAbsolutePath() + "/image_cin_verso.jpg");

        file.delete(); //delete if we have an old file
        try (FileOutputStream out = new FileOutputStream(file)) {
            resizedImage.compress(Bitmap.CompressFormat.JPEG, 80, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater layoutInflaterAndroid = LayoutInflater.from(this);
            View view2 = layoutInflaterAndroid.inflate(R.layout.cam_dialog, null);
            builder.setView(view2);
            builder.setCancelable(false);
            final AlertDialog alertDialog = builder.create();
            if (alertDialog.getWindow() != null)
                alertDialog.getWindow().getDecorView().setBackgroundColor(Color.TRANSPARENT);
            if (!alertDialog.isShowing())
                alertDialog.show();

            ArrayList<String> versoResult = new ArrayList<>(resToShow);
            TextView textView = view2.findViewById(R.id.tv1);
            TextView textView2 = view2.findViewById(R.id.tv2);

            textView.setText(getResources().getString(R.string.scan_msg_success));
            textView2.setVisibility(View.GONE);

            view2.findViewById(R.id.iv_annuler).setOnClickListener(v1 -> {
                reset();
                isVerso = true;
                alertDialog.dismiss();
                finish();
            });
            view2.findViewById(R.id.iv_continuer).setOnClickListener(v12 -> {
                alertDialog.dismiss();
                returnIntent.putExtra("picVerso", file.getAbsolutePath());
                Log.i("***********Anas3", file.getAbsolutePath());
                returnIntent.putStringArrayListExtra("resultVerso", versoResult);
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            });
        });
    }

}
