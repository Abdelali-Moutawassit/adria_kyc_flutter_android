package com.adria.adria_kyc_integration.uploadutils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.widget.Toast;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Utils {

    public void uploadZip(Context context, String resultSelfie, Activity activity){

        ProgressDialog dialog = new ProgressDialog(activity);
        dialog.show();

        ApiInterfaceScan anInterface = BuilderScan.build().create(ApiInterfaceScan.class);
        RequestBody body2 = RequestBody.create(MediaType.parse("application/zip"), new File(resultSelfie));
        MultipartBody.Part part2 = MultipartBody.Part.createFormData("file", "zipFolder.zip", body2);

        Call<ResponseModelP> call = anInterface.uploadImage3(part2);
        call.enqueue(new Callback<ResponseModelP>() {

            @Override
            public void onResponse(Call<ResponseModelP> call, Response<ResponseModelP> response) {

                dialog.dismiss();

                ResponseModelP responseModel = response.body();
                if (responseModel.getStatus().toString().equals("Ok")) {
                    if (responseModel.getInfo_code() == 8 ) {
                        Toast.makeText(context, "Essayez de prendre une meilleure vidéo", Toast.LENGTH_LONG).show();
                    } else {

                        if (responseModel.isMatch()) {
                            //SuccessDialog sd = new SuccessDialog(getApplicationContext(), "La probabilité de correspondance entre la photo sur la CIN et le selfie est de : " + responseModel.getProbability() + "%");
                            Toast.makeText(context, responseModel.getProbability_msg(), Toast.LENGTH_LONG).show();

                        } else {
                            Toast.makeText(context,responseModel.getProbability_msg(), Toast.LENGTH_LONG).show();
                        }
                    }
                } else if ((responseModel.getStatus()).equals("Fail")) {


                    Toast.makeText(context, "Fail : " + responseModel.getInfo(),
                            Toast.LENGTH_LONG).show();

                } else {
                    Toast.makeText(context, "Erreur réponse serveur" + responseModel.getInfo(),
                            Toast.LENGTH_LONG).show();
                }

                //new DialogConfirmation(AdriaApp.getInstance().getCurrentActivity(),"Response Model",response.body().getStatus()+" "+response.body()).show();

            }

            @Override
            public void onFailure(Call<ResponseModelP> call, Throwable t) {

                dialog.dismiss();

                Toast.makeText(context, t.getMessage(), Toast.LENGTH_LONG).show();
                t.printStackTrace();
            }
        });
    }
}
