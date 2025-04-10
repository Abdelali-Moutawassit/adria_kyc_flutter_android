package com.adria.adria_kyc_integration;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;

import com.adria.adria_kyc_integration.adriascancinlibrary.ScanOldCinActivity;
import com.adria.adria_kyc_integration.adriascancinlibrary.ScanNewCinActivity;
import com.adria.adria_kyc_integration.adriascancinlibrary.facedetection.FaceActivity;

/** AdriaKycIntegrationPlugin */
public class AdriaKycIntegrationPlugin implements FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {

  private MethodChannel channel;
  private Activity activity;
  private MethodChannel.Result pendingResult;
  private static final int SCAN_OLD_CIN = 1, SCAN_NEW_CIN = 2, FACE_RECOGNITION = 3;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "com.adria.adriascansin/kyc_sdk");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
    pendingResult = result;
    switch (call.method) {
      case "scanCIN":
        scanCin();
        break;
      case "scanNewCIN":
        scanNewCin();
        break;
      case "faceRecognition":
        faceRecognition();
        break;
      default:
        result.notImplemented();
        break;
    }
  }

  private void scanCin() {
    Intent intent = new Intent(activity, ScanOldCinActivity.class);
    activity.startActivityForResult(intent, SCAN_OLD_CIN);
  }

  private void scanNewCin() {
    Intent intent = new Intent(activity, ScanNewCinActivity.class);
    activity.startActivityForResult(intent, SCAN_NEW_CIN);
  }

  private void faceRecognition() {
    Intent intent = new Intent(activity, FaceActivity.class);
    activity.startActivityForResult(intent, FACE_RECOGNITION);
  }

  @Override
  public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
    if (pendingResult == null) return false;

    if (resultCode == Activity.RESULT_OK && data != null) {
      ArrayList<String> resultList;
      String imgPath;
      switch (requestCode) {
        case SCAN_OLD_CIN:
        case SCAN_NEW_CIN:
          resultList = data.getStringArrayListExtra("result");
          imgPath = data.getStringExtra("pic");
          if (resultList != null) {
            String response = "{"
                    + "\"prenom\": \"" + resultList.get(0) + "\","
                    + "\"nom\": \"" + resultList.get(1) + "\","
                    + "\"date_naissance\": \"" + resultList.get(2) + "\","
                    + "\"ville\": \"" + resultList.get(3) + "\","
                    + "\"date_exp\": \"" + resultList.get(4) + "\","
                    + "\"num_cin\": \"" + resultList.get(5) + "\","
                    + "\"img_path\": \"" + imgPath + "\""
                    + "}";
            pendingResult.success(response);
          } else {
            pendingResult.error("DATA_ERROR", "Aucune donnée reçue", null);
          }
          break;
        case FACE_RECOGNITION:
          String zipPath = data.getStringExtra("zipFolder");
          if (zipPath != null) {
            pendingResult.success(zipPath);
          } else {
            pendingResult.error("FACE_ERROR", "Échec de la reconnaissance faciale", null);
          }
          break;
        default:
          pendingResult.error("UNKNOWN_REQUEST", "Requête inconnue", null);
          break;
      }
    } else {
      pendingResult.error("SCAN_CANCELED", "L'utilisateur a annulé le scan", null);
    }
    pendingResult = null;
    return true;
  }

  // Implémentation de ActivityAware
  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    activity = binding.getActivity();
    binding.addActivityResultListener(this);
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    activity = null;
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    activity = binding.getActivity();
    binding.addActivityResultListener(this);
  }

  @Override
  public void onDetachedFromActivity() {
    activity = null;
  }
}
