package com.adria.adria_kyc_integration.adriascancinlibrary;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
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

public class ScanNewCinActivity extends CameraXActivity {

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

    private FaceDetector detector;
    private ImageProxy imageProxy;
    private CountDownTimer detectFaceCountDownTimer;
    private Boolean countDownStarted = false;
    private TextView sdkVersionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .enableTracking()
                        .build();
        detector = FaceDetection.getClient(options);

        patternDates = Pattern.compile(regexDateNaissance);
        patternAdr = Pattern.compile(regexName);

        patternVersoCIN = Pattern.compile(regexCIN);
        patternFilleDe = Pattern.compile(regexFilleDe);
        patternFisDe = Pattern.compile(regexFisDe);
        patternFather = Pattern.compile(regexFather);
        patternMother = Pattern.compile(regexMother);

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
                        Toast.makeText(ScanNewCinActivity.this, R.string.demande_de_recadrage, Toast.LENGTH_SHORT).show();
                        break;
                    case 15:
                        Toast.makeText(ScanNewCinActivity.this, R.string.scan_msg_1, Toast.LENGTH_SHORT).show();
                        break;
                    case 20:
                        Toast.makeText(ScanNewCinActivity.this, R.string.scan_msg_2, Toast.LENGTH_SHORT).show();
                        break;
                }
                i[0]++;
            }

            @Override
            public void onFinish() {
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
                    AlertDialog.Builder builder = new AlertDialog.Builder(ScanNewCinActivity.this);
                    LayoutInflater layoutInflaterAndroid = LayoutInflater.from(ScanNewCinActivity.this);
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
        if (doubleChecked(res)) {
            pauseAnalyse = true;
            startDetecteingFace = true;
            image.close();
        } else if (!pauseAnalyse) {
            upperCaseValues = new ArrayList<>();
            dates = new ArrayList<>();
            runInBackground(() -> {
                @androidx.camera.core.ExperimentalGetImage
                Image mediaImage = image.getImage();
                if (mediaImage != null) {
                    InputImage inputImage =
                            InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());
                    recognizer.process(inputImage)
                            .addOnSuccessListener(new OnSuccessListener<Text>() {
                                @Override
                                public void onSuccess(Text mlText) {
                                    if (Utils.checkIfCIN(mlText.getText()))
                                        countIsCIN++;
                                    String[] results = mlText.getText().split("\\n");
                                    int j = 0;
                                    for (String result : results) {
                                        Log.i("-----", result);

                                        matcher = patternAdr.matcher(result);
                                        if (matcher.find()) {
                                            //Log.i("----", matcher.group());
                                            upperCaseValues.add(result);
                                        }
                                        if (res.get(3).equals("")) {
                                            if (result.startsWith("a ") || result.startsWith("à ") || result.startsWith("i "))
                                                if (result.length() > 6) {
                                                    adrNaissance = result.substring(2).toUpperCase();
                                                    if (results[j + 1].matches(regexName) && !upperCaseValues.contains(results[j + 1]))
                                                        adrNaissance = adrNaissance + " " + results[j + 1];
                                                    check(adrNaissance, regexName, 3);
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
                                                }
                                                break;
                                            }
                                        }
                                    }
                                    int indexDates = 0;
                                    if (res.get(2).equals("") || res.get(4).equals("")) {
                                        matcher = patternDates.matcher(mlText.getText());
                                        while (matcher.find()) {
                                            if (res.get(2).equals("") || res.get(4).equals("")) {
                                                if (indexDates == 0) {
                                                    if (matcher.group().length()==4){
                                                        check(matcher.group(), "[0-9]{4}", 2);
                                                    }else {
                                                        String birthDate = matcher.group().replace(" ", ".");
                                                        if (Utils.checkDay(birthDate) < 0)
                                                            check(birthDate, regexDate, 2);
                                                    }
                                                    indexDates++;
                                                } else {
                                                    String expDate = matcher.group().replace(" ", ".");
                                                    if (expDate.length()>4){
                                                        if (Utils.checkDay(expDate) >= 0)
                                                            check(expDate, regexDate, 4);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            })
                            .addOnCompleteListener(task -> image.close());
                }
            });
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    @Override
    @OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    protected void processVersoFrames(ImageProxy image) {
        bitmap = ImageToBitmap.toBitmap(image);
        runInBackground(() -> {
            if (doubleChecked(versoRes)) {
                pauseAnalyse = true;
                showFinalResult(versoRes, bitmap);
            } else if (!pauseAnalyse) {
                @androidx.camera.core.ExperimentalGetImage
                Image mediaImage = image.getImage();
                if (mediaImage != null) {
                    InputImage inputImage =
                            InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());
                    recognizer.process(inputImage)
                            .addOnSuccessListener(new OnSuccessListener<Text>() {
                                @Override
                                public void onSuccess(Text mlText) {
                                    if (!mlText.getText().equals("")) {
                                        //List<MLText.Block> blocks = mlText.getBlocks();
                                        String[] results = mlText.getText().split("\\n");
                                        int i = 0;
                                        for (String result : results) {
                                            matcherVersoCIN = patternVersoCIN.matcher(result.trim());
                                            matcherFisDe = patternFisDe.matcher(result.trim());
                                            matcherFilleDe = patternFilleDe.matcher(result.trim());

                                            if (matcherVersoCIN.find() && versoRes.get(0).equals("")) {
                                                versoCheck(matcherVersoCIN.group(), 0);
                                            } else if (matcherFisDe.find()) {
                                                //set sexe
                                                if (versoRes.get(4).equals(""))
                                                    versoRes.set(4, "M");

                                                //Father's name
                                                matcherFather = patternFather.matcher(result.trim());
                                                if (matcherFather.find() && versoRes.get(1).equals(""))
                                                    versoCheck(matcherFather.group(), 1);

                                                //Mother's name
                                                if (results.length > i + 1) {
                                                    matcherMother = patternMother.matcher(results[i + 1]);
                                                    if (matcherMother.find() && versoRes.get(2).equals(""))
                                                        versoCheck(matcherMother.group(), 2);
                                                }

                                            } else if (matcherFilleDe.find()) {
                                                //set sexe
                                                if (versoRes.get(4).equals(""))
                                                    versoRes.set(4, "F");

                                                //Father's name
                                                matcherFather = patternFather.matcher(result.trim());
                                                if (matcherFather.find() && versoRes.get(1).equals(""))
                                                    versoCheck(matcherFather.group(), 1);

                                                //Mother's name
                                                if (results.length > i + 1) {
                                                    matcherMother = patternMother.matcher(results[i + 1]);
                                                    if (matcherMother.find() && versoRes.get(2).equals(""))
                                                        versoCheck(matcherMother.group(), 2);
                                                }
                                            } else if (results.length > i+2) {
                                                if (results[i+1].contains("<<")){
                                                    int wordSize = result.trim().split(" ")[0].length();
                                /*matcher = patternAdr.matcher(result.trim());
                                if (matcher.find())
                                    versoCheck(matcher.group(),3);*/
                                                    if (result.length() > wordSize && versoRes.get(3).equals(""))
                                                        versoCheck(result.substring(wordSize).toUpperCase(), 3);
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
            }
        });
    }

    @Override
    @OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    protected void checkFaceImage(ImageProxy image) {
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
        detector.process(faceImage)
                .addOnSuccessListener(firebaseVisionFaces -> {
                    if (firebaseVisionFaces.size() >= 1) {
                        for (Face face : firebaseVisionFaces){
                            int height = face.getBoundingBox().bottom - face.getBoundingBox().top;
                            int width = face.getBoundingBox().right - face.getBoundingBox().left;
                            double diag = Math.pow(height,2) + Math.pow(width,2);
                            diag = Math.sqrt(diag);
                            Log.i("+++++*+ hanana", diag + "---" + (faceImage.getHeight()*10/38));
                            if (diag >= (faceImage.getHeight()*10/38)){
//                            if (height*width >= 10000){
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

    public void check(String txt, String regex, int valD) {
        //if the list already contains this value and the result is empty
        if (!txt.matches(regex))
            return;
        if (rectoOccurences.get(valD).getOccurence() == 1 && res.get(valD).equals("")) {
            if (valD == 3) {
                if (Utils.similarity(rectoOccurences.get(valD).getValue(), txt)) {
                    res.set(valD, txt);
                } else {
                    rectoOccurences.get(valD).setValue(txt);
                }
            } else {
                if (rectoOccurences.get(valD).getValue().equals(txt)) {
                    res.set(valD, txt);
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
                if (Utils.similarity(versoOccurences.get(valD).getValue(),txt))
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
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        String adr = res.get(3);
        if (adr.endsWith(res.get(1)))
            res.set(3, adr.substring(adr.length() - res.get(1).length()).trim());
        else if (adr.endsWith(res.get(0)))
            res.set(3, adr.substring(adr.length() - res.get(0).length()).trim());

        /*Bitmap bitmapRectoCIN = mImage.copy(mImage.getConfig(), mImage.isMutable());

        Bitmap resizedImage = Utils.resizeBitmap(bitmapRectoCIN, 720);
        File file = new File(getBaseContext().getCacheDir().getAbsolutePath() + "/image_cin.jpg");

        file.delete();
        try (FileOutputStream out = new FileOutputStream(file)) {
            resizedImage.compress(Bitmap.CompressFormat.JPEG, 80, out);
        } catch (IOException e) {
            e.printStackTrace();
        }*/
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

    private void showFinalResult(List<String> resToShow, Bitmap mImage) {
        countDownTimer.cancel();

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
                returnIntent.putStringArrayListExtra("resultVerso", versoResult);
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            });
        });
    }

}
