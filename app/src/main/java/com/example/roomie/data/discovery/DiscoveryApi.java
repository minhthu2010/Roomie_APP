package com.example.roomie.data.discovery;

import com.example.roomie.data.remote.ApiResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** API mảng Khám phá (người C): Roomie_backend/src/modules/discovery */
public interface DiscoveryApi {

    String MODE_PERSONAL = "personal";
    String MODE_ROOM = "room";

    @GET("questionnaire/current")
    Call<ApiResponse<QuizModels.Questionnaire>> getQuestionnaire(@Query("mode") String mode);

    @GET("me/lifestyle-test")
    Call<ApiResponse<QuizModels.LifestyleTest>> getMyLifestyleTest();

    @PUT("me/lifestyle-answers")
    Call<ApiResponse<QuizModels.LifestyleTest>> saveMyAnswers(@Body QuizModels.SaveAnswersRequest body);

    @GET("room-posts/{postId}/lifestyle-answers")
    Call<ApiResponse<QuizModels.LifestyleTest>> getPostAnswers(@Path("postId") String postId);

    @PUT("room-posts/{postId}/lifestyle-answers")
    Call<ApiResponse<QuizModels.LifestyleTest>> savePostAnswers(@Path("postId") String postId,
                                                               @Body QuizModels.SaveAnswersRequest body);
}
