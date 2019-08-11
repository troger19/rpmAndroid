package com.newventuresoftware.waveformdemo;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface JsonPlaceHolderApi {

    @POST("training")
    Call<ResponseBody> createTraining(@Body TrainingDto trainingDto);

    @GET("person/{name}")
    Call<PersonDto> getPerson(@Path("name") String name);

    @GET("health")
    Call<ResponseBody> healthCheck();

    @GET("training/{name}")
    Call<List<TrainingDto>> getTrainingsByName(@Path("name") String name);

}