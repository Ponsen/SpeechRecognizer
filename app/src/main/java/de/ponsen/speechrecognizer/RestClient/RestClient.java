package de.ponsen.speechrecognizer.RestClient;

import android.util.Base64;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by olda_ on 20.03.2016.
 */
public class RestClient {

    private static OkHttpClient httpClient = new OkHttpClient();
    private static Retrofit.Builder builder;

    public static <S> S createService(Class<S> serviceClass, HttpUrl baseURL) {
        builder = new Retrofit.Builder()
                .baseUrl(baseURL)
                .addConverterFactory(GsonConverterFactory.create());
        return createService(serviceClass,baseURL , null, null);
    }

    public static <S> S createService(Class<S> serviceClass, HttpUrl baseURL, String username, String password) {
        builder = new Retrofit.Builder()
                .baseUrl(baseURL)
                .addConverterFactory(GsonConverterFactory.create());
        if (username != null && password != null) {
            String credentials = username + ":" + password;
            final String basic = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

            httpClient = new OkHttpClient.Builder()
                    .addInterceptor(
                            new Interceptor() {
                                @Override
                                public Response intercept(Interceptor.Chain chain) throws IOException {
                                    Request original = chain.request();

                                    // RequestHandler customization: add request headers
                                    Request.Builder requestBuilder = original.newBuilder()
                                            .header("Authorization", basic)
                                            .method(original.method(), original.body());

                                    Request request = requestBuilder.build();
                                    return chain.proceed(request);
                                }
                            })
                    .build();
        }

        Retrofit retrofit = builder.client(httpClient).build();
        return retrofit.create(serviceClass);
    }
}