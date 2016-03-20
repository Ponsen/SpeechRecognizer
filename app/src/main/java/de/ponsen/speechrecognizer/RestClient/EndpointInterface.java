package de.ponsen.speechrecognizer.RestClient;

import de.ponsen.speechrecognizer.ResultModel;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Created by olda_ on 20.03.2016.
 */
public interface EndpointInterface {
    @POST("/{path}")
    Call<ResponseBody> postSpeechResult(@Path("path") String path, @Body ResultModel resultModel);
}
