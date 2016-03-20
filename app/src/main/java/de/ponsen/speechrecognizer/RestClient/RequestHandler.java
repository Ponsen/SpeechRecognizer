package de.ponsen.speechrecognizer.RestClient;

import android.util.Log;

import de.ponsen.speechrecognizer.ResultModel;
import okhttp3.HttpUrl;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by olda_ on 20.03.2016.
 */
public class RequestHandler {
    protected final String TAG = getClass().getSimpleName();

    private EndpointInterface endpointInterface;

    public RequestHandler(HttpUrl baseURL) {
        endpointInterface = RestClient.createService(EndpointInterface.class, baseURL, null, null);
    }

    public RequestHandler(HttpUrl baseURL, String username, String password) {
        endpointInterface = RestClient.createService(EndpointInterface.class, baseURL, username, password);
    }

    public void postSpeechResult(String path, ResultModel resultModel, final IRequestCallback iRequestCallback) {
        try {
            final Call<ResponseBody> call = endpointInterface.postSpeechResult(path, resultModel);
            call.enqueue(new Callback<ResponseBody>() {

                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        Log.i(TAG, String.valueOf(response.code()));
                        if (response.body() != null) {
                            iRequestCallback.requestCallback(response.body().string());
                        } else if (response.errorBody() != null) {
                            iRequestCallback.requestCallback(response.errorBody().string());
                        } else {
                            iRequestCallback.requestCallback(String.valueOf(response.code()));
                        }
                    } catch (Exception e) {
                        iRequestCallback.requestCallback(e.toString());
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.i(TAG, "postRequest", t);
                    iRequestCallback.requestCallback(t.toString());
                }
            });
        } catch (Exception e) {
            iRequestCallback.requestCallback(e.toString());
        }
    }
}