package com.akondi.homemarket.remote;

import com.akondi.homemarket.model.MyResponse;
import com.akondi.homemarket.model.Sender;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {
    @Headers(
            {
                 "Content-Type:application/json",
                 "Authorization:key=AAAA-SYzOnI:APA91bFWYy9I8f_JPYK1RZKpy5d_aY2FDe1Jd7hEVOCs6TIH3Lu6mMzQfwlLSHbYzN-UCi7plg6YBxdWhdmc-8udmRFZ7lhPNCX8OhnHoAIlDBAM78ubY6iADRqeSxgWdFdaH2HlNsMN"
            }
    )
    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body Sender body);
}
