package com.example.global_insights.Network;

import com.example.global_insights.model.NewsResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NewsApiService {

    @GET("everything")
    Call<NewsResponse> getEverything(
            @Query("q") String query,
            @Query("apiKey") String apiKey,
            @Query("language") String language
    );
}
