package com.newventuresoftware.waveformdemo;

import android.util.Base64;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthenticationInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();

        String credentials = "admin" + ":" + "password";
        final String basic = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
        Request.Builder builder = original.newBuilder()
                .header("Authorization", basic);

        Request request = builder.build();
        return chain.proceed(request);
    }
}