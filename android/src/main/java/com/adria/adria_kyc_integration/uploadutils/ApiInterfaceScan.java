package com.adria.adria_kyc_integration.uploadutils;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface ApiInterfaceScan {

    @Multipart
    @POST("api/verify")
    Call<ResponseModel> uploadImage(@Part MultipartBody.Part image, @Part MultipartBody.Part video);

    @Multipart
    @POST("api/verify")
    Call<ResponseModelP> uploadImage2(@Part MultipartBody.Part zip);

    @Multipart
    @POST("api/id_verification")
    Call<ResponseModelP> uploadImage3(@Part MultipartBody.Part zip);

    @Multipart
    @POST("verifyOCR/{id}")
    Call<ResponseModelP> uploadImageBE(@Part MultipartBody.Part zip, @Path("id") String id);

}
