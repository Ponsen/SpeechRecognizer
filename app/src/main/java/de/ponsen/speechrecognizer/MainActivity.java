package de.ponsen.speechrecognizer;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.gson.GsonBuilder;

import java.util.ArrayList;

import de.ponsen.speechrecognizer.RestClient.IRequestCallback;
import de.ponsen.speechrecognizer.RestClient.RequestHandler;
import okhttp3.HttpUrl;

public class MainActivity extends AppCompatActivity implements RecognitionListener{

    private final String TAG = this.getClass().getSimpleName();

    ImageButton startListeningButton;
    Animation breath;
    TextView speech_error_txt, speech_text, rest_result_txt;

    private SpeechRecognizer speech = null;
    private Intent recognizerIntent;

    //settings
    boolean settingRestartSTT;
    boolean settingEnableHTTP;
    String settingServerURL;
    int settingServerPort;
    String settingServerEndpoint;
    int settingWaitInMilis;

    private RequestHandler requestHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        speech_error_txt = (TextView) findViewById(R.id.speech_error_txt);
        speech_text = (TextView) findViewById(R.id.txtSpeechInput);

        rest_result_txt = (TextView) findViewById(R.id.rest_result_txt);

        speech = SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(this);

        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        //recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, settingWaitInMilis);


        breath = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.breath_anim);

        startListeningButton = (ImageButton) findViewById(R.id.btnSpeak);

        startListeningButton.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if(event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    if(!v.isPressed()){
                        v.startAnimation(breath);
                        Log.d(TAG, "startListening");
                        speech.startListening(recognizerIntent);
                    }else{
                        v.clearAnimation();
                        Log.d(TAG, "stopListening");
                        speech.stopListening();
                    }
                    v.setPressed(!v.isPressed());
                }
                return true;//Return true, so there will be no onClick-event
            }
        });
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        //applying settings here to we get the changes
        settingRestartSTT = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(getResources().getString(R.string.pref_continous_key), false);

        settingEnableHTTP = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(getResources().getString(R.string.pref_switch_sendResult_key), false);
        if(settingEnableHTTP){
            settingServerURL = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(getResources().getString(R.string.pref_serverurl_key), "");
            String port = (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(getResources().getString(R.string.pref_port_key), "80"));
            //if user decided to enter empty string...
            if(port.equals(""))
                port = "80";
            settingServerPort = Integer.valueOf(port);

            requestHandler = new RequestHandler(HttpUrl.parse(settingServerURL + ":" + settingServerPort));

            settingServerEndpoint = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(getResources().getString(R.string.pref_endpoint_key), "");
        }

        String milis = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(getResources().getString(R.string.pref_wait_milis_key), "1000");
        if(milis.equals(""))
            milis = "1000";
        settingWaitInMilis = Integer.valueOf(milis);

        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (speech != null) {
            speech.destroy();
            Log.i(TAG, "onPause");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void restartSpeech(){
        speech.stopListening();
        speech.startListening(recognizerIntent);
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.d(TAG, "onReadyForSpeech");
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d(TAG, "onBeginningOfSpeech");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        Log.d(TAG, "onRmsChanged: " + rmsdB);
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.d(TAG, "onBufferReceived: " + buffer.toString());
    }

    @Override
    public void onEndOfSpeech() {
        Log.d(TAG, "onEndOfSpeech");
    }

    @Override
    public void onError(int error) {
        String errorMessage = getErrorText(error);
        speech_error_txt.setText(errorMessage);
        startListeningButton.clearAnimation();
        startListeningButton.setPressed(false);
    }

    @Override
    public void onResults(Bundle results) {
        Log.d(TAG, "onResults");
        ArrayList<String> textResults = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        float[] confidences = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
        Long timestamp = System.currentTimeMillis()/1000;

        ResultModel resultModel = new ResultModel();
        resultModel.text = textResults;
        resultModel.confidence = confidences;
        resultModel.timestamp = timestamp.toString();

        Log.i(TAG, new GsonBuilder().create().toJson(resultModel, ResultModel.class));
        String text_temp = "";
        for (String result : textResults)
            text_temp += result + "\n";

        speech_text.setText(text_temp);
        startListeningButton.clearAnimation();
        startListeningButton.setPressed(false);
        speech_error_txt.setText("");

        if(settingEnableHTTP){
            requestHandler.postSpeechResult(settingServerEndpoint, resultModel, new IRequestCallback() {
                @Override
                public void requestCallback(String data) {
                    Log.i(TAG, data);
                    rest_result_txt.setText(data);
                }
            });
        }

        //restart after result has been handeled
        if(settingRestartSTT){
            restartSpeech();
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        Log.d(TAG, "onPartialResults");
        /*
        ArrayList<String> textResults = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        float[] confidences = partialResults.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
        Long timestamp = System.currentTimeMillis()/1000;

        ResultModel resultModel = new ResultModel();
        resultModel.text = textResults;
        resultModel.confidence = confidences;
        resultModel.timestamp = timestamp.toString();

        Log.i(TAG, new GsonBuilder().create().toJson(resultModel, ResultModel.class));
        String text_temp = "";
        for (String result : textResults)
            text_temp += result + "\n";

        speech_text.setText(text_temp);
        startListeningButton.clearAnimation();
        startListeningButton.setPressed(false);
        speech_error_txt.setText("");

        if(settingEnableHTTP){
            requestHandler.postSpeechResult(settingServerEndpoint, resultModel, new IRequestCallback() {
                @Override
                public void requestCallback(String data) {
                    Log.i(TAG, data);
                    rest_result_txt.setText(data);
                }
            });
        }
        */
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        Log.d(TAG, "onEvent");
    }

    public static String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }


}
