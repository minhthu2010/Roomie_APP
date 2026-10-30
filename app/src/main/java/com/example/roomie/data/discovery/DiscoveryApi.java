package com.example.roomie.data.discovery;

import com.example.roomie.data.remote.ApiResponse;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
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

    // ---------- Chi tiết phòng, ảnh, đã lưu, hồ sơ người đăng ----------

    @GET("room-posts/{postId}")
    Call<ApiResponse<RoomModels.RoomDetail>> getRoomDetail(@Path("postId") String postId);

    @GET("room-posts/{postId}/images")
    Call<ApiResponse<RoomModels.RoomImages>> getRoomImages(@Path("postId") String postId);

    @POST("room-posts/{postId}/contact-clicks")
    Call<ApiResponse<RoomModels.ContactClick>> logContactClick(@Path("postId") String postId,
                                                              @Body RoomModels.ContactClick body);

    @GET("saved-posts")
    Call<ApiResponse<List<RoomModels.SavedRoom>>> getSavedRooms(@Query("page") int page, @Query("limit") int limit);

    @PUT("saved-posts/{postId}")
    Call<ApiResponse<RoomModels.SaveResult>> savePost(@Path("postId") String postId);

    @DELETE("saved-posts/{postId}")
    Call<ApiResponse<RoomModels.SaveResult>> unsavePost(@Path("postId") String postId);

    @GET("users/{userId}/public-profile")
    Call<ApiResponse<RoomModels.PublicProfile>> getPublicProfile(@Path("userId") String userId);

    // ---------- Bản đồ ----------

    /** Tham số: SearchCriteria.toQuery() + minLat, maxLat, minLng, maxLng, minMatch */
    @GET("room-posts/map")
    Call<ApiResponse<MapModels.MapRooms>> getMapRooms(@QueryMap Map<String, String> query);

    @GET("room-posts/{postId}/nearby")
    Call<ApiResponse<MapModels.Nearby>> getNearby(@Path("postId") String postId);

    // ---------- Phường/xã thật + tra địa chỉ ----------

    @GET("places/suggest")
    Call<ApiResponse<PlaceModels.Suggest>> suggestPlaces(@Query("q") String q,
                                                         @Query("provinceCode") String provinceCode);

    @GET("admin-units/provinces")
    Call<ApiResponse<List<PlaceModels.Province>>> getProvinces();

    @GET("admin-units/wards")
    Call<ApiResponse<List<PlaceModels.Ward>>> getWards(@Query("provinceCode") String provinceCode,
                                                       @Query("q") String q,
                                                       @Query("limit") Integer limit);

    @GET("admin-units/wards/{wardCode}/center")
    Call<ApiResponse<PlaceModels.WardCenter>> getWardCenter(@Path("wardCode") String wardCode);

    @GET("landmarks/suggest")
    Call<ApiResponse<List<SearchModels.Landmark>>> suggestLandmarks(@Query("q") String q,
                                                                    @Query("type") String type,
                                                                    @Query("limit") Integer limit,
                                                                    @Query("provinceCode") String provinceCode);
}
