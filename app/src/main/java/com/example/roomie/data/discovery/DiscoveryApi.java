package com.example.roomie.data.discovery;

import com.example.roomie.data.remote.ApiResponse;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

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

    @GET("room-posts/{postId}/compatibility")
    Call<ApiResponse<MatchingModels.Compatibility>> getCompatibility(@Path("postId") String postId);

    /** Tham số: xem SearchCriteria.toQuery() (q, area, landmarkId, minRent, maxRent, roomTypes, gender, amenities, sort, page, limit) */
    @GET("room-posts/search")
    Call<ApiResponse<List<SearchModels.RoomCard>>> searchRooms(@QueryMap Map<String, String> query);

    @GET("landmarks/suggest")
    Call<ApiResponse<List<SearchModels.Landmark>>> suggestLandmarks(@Query("q") String q,
                                                                    @Query("type") String type,
                                                                    @Query("limit") Integer limit);
}
