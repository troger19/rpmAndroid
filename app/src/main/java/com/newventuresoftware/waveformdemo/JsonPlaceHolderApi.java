package com.newventuresoftware.waveformdemo;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface JsonPlaceHolderApi {

    @GET("allPerson")
    Call<List<PersonDto>> getAllPersons();

    @POST("create")
    Call<ResponseBody> createTraining(@Body TrainingDto trainingDto);

    @GET("person/{name}")
    Call<PersonDto> getPerson(@Path("name") String name);

    @GET("all")
    Call<List<TrainingDto>> getAllTrainings();

}