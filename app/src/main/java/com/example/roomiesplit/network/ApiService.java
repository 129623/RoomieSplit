package com.example.roomiesplit.network;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import okhttp3.MultipartBody;
import retrofit2.http.Multipart;
import retrofit2.http.Part;

public interface ApiService {

        // Login
        @POST("api/v1/auth/login")
        Call<JsonObject> login(@Body JsonObject loginRequest);

        // Register
        @POST("api/v1/auth/register")
        Call<JsonObject> register(@Body JsonObject registerRequest);

        // Get My Ledgers
        @GET("api/v1/ledgers")
        Call<JsonObject> getMyLedgers(@Header("X-User-Id") Long userId);

        // Create Ledger
        @POST("api/v1/ledgers")
        Call<JsonObject> createLedger(@Header("X-User-Id") Long userId, @Body JsonObject createLedgerRequest);

        // Get Ledger Detail
        @GET("api/v1/ledgers/{id}")
        Call<JsonObject> getLedgerDetail(@Header("X-User-Id") Long userId, @Path("id") Long ledgerId);

        // Create Transaction
        @POST("api/v1/ledgers/{id}/transactions")
        Call<JsonObject> createTransaction(@Header("X-User-Id") Long userId, @Path("id") Long ledgerId,
                        @Body JsonObject transactionRequest);

        @POST("api/v1/ledgers/{id}/members/status")
        Call<JsonObject> updateMemberStatus(@Header("X-User-Id") Long userId, @Path("id") Long ledgerId,
                        @Body JsonObject statusRequest);

        // Get Transactions
        @GET("api/v1/ledgers/{id}/transactions")
        Call<JsonObject> getTransactions(@Path("id") Long ledgerId);

        // --- Notification ---
        @GET("api/v1/notifications")
        Call<JsonObject> getMyNotifications(@Header("X-User-Id") Long userId);

        @POST("api/v1/notifications/{id}/read")
        Call<JsonObject> markNotificationRead(@Header("X-User-Id") Long userId, @Path("id") Long notificationId);

        @retrofit2.http.DELETE("api/v1/notifications/{id}")
        Call<JsonObject> deleteNotification(@Header("X-User-Id") Long userId, @Path("id") Long id);

        // --- Poll ---
        @GET("api/v1/ledgers/{id}/polls")
        Call<JsonObject> getPolls(@Path("id") Long ledgerId);

        @POST("api/v1/ledgers/{id}/polls")
        Call<JsonObject> createPoll(@Header("X-User-Id") Long userId, @Path("id") Long ledgerId,
                        @Body JsonObject pollRequest);

        @POST("api/v1/polls/{id}/vote")
        Call<JsonObject> vote(@Header("X-User-Id") Long userId, @Path("id") Long pollId, @Body JsonObject voteRequest);

        @GET("api/v1/polls/{id}/result")
        Call<JsonObject> getPollResult(@Path("id") Long pollId);

        // --- Karma ---
        @GET("api/v1/ledgers/{id}/karma")
        Call<JsonObject> getKarmaStats(@Path("id") Long ledgerId);

        @POST("api/v1/ledgers/{id}/karma/work")
        Call<JsonObject> recordKarmaWork(@Header("X-User-Id") Long userId, @Path("id") Long ledgerId,
                        @Body JsonObject workRequest);

        @POST("api/v1/ledgers/{id}/karma/draw")
        Call<JsonObject> karmaDraw(@Header("X-User-Id") Long userId, @Path("id") Long ledgerId,
                        @Body JsonObject drawRequest);

        // --- Invitation ---
        @POST("api/v1/ledgers/{id}/invite")
        Call<JsonObject> inviteMember(@Header("X-User-Id") Long userId, @Path("id") Long ledgerId,
                        @Body JsonObject inviteRequest);

        @POST("api/v1/invitations/{token}/accept")
        Call<JsonObject> acceptInvitation(@Header("X-User-Id") Long userId, @Path("token") String token);

        @POST("api/v1/invitations/{token}/reject")
        Call<JsonObject> rejectInvitation(@Header("X-User-Id") Long userId, @Path("token") String token);

        // --- Report ---
        @GET("api/v1/ledgers/{id}/debt-graph")
        Call<JsonObject> getDebtGraph(@Path("id") Long ledgerId);

        @GET("api/v1/ledgers/{id}/reports/data")
        Call<JsonObject> getReportData(@Path("id") Long ledgerId, @Query("startDate") String startDate,
                        @Query("endDate") String endDate);

        // --- User Profile ---
        @GET("api/v1/users/{id}/profile")
        Call<JsonObject> getUserProfile(@Path("id") Long userId);

        @POST("api/v1/users/{id}/profile")
        Call<JsonObject> updateUserProfile(@Path("id") Long userId, @Body JsonObject profileRequest);

        @Multipart
        @POST("api/v1/upload")
        Call<JsonObject> uploadImage(@Part MultipartBody.Part file, @Query("skipOCR") boolean skipOCR);

        @POST("api/v1/settlements/smart")
        Call<JsonObject> smartSettle(@Body com.google.gson.JsonObject body);

        @POST("api/v1/settlements/confirm")
        Call<JsonObject> confirmSettlement(@Query("id") Long id);

        @POST("api/v1/settlements/remind")
        Call<JsonObject> remindPayment(@Body com.google.gson.JsonObject body);
}
