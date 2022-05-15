package com.example.appdialog;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.dialogflow.v2.DetectIntentRequest;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.SessionsSettings;
import com.google.cloud.dialogflow.v2.TextInput;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener{

    // res -> resources
    // res/layouts/activity_main.xml -> R.layouts.activity_main
    //btConfirmar -> R.id.btConfirmar


    Button btConfirmar;
    Button btHablar;
    EditText etFrase;
    TextView tvResultado;
    TextView tvBot;

    /* text to speech */
    boolean ttsReady;
    TextToSpeech tts;

    /* speech to text */
    private final int REQ_CODE = 100;

    /* bot */
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int USER = 10001;
    private static final int BOT = 10002;

    private String uuid = UUID.randomUUID().toString();

    // Java V2
    private SessionsClient sessionsClient;
    private SessionName session;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialize();
        initV2Chatbot();

    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public void callbackV2(DetectIntentResponse response) {
        if (response != null) {
            // process aiResponse here
            String botReply = response.getQueryResult().getFulfillmentText();
            Map<Descriptors.FieldDescriptor, Object> params = response.getAllFields();
            String intent_displayName = response.getQueryResult().getIntent().getDisplayName();
            boolean all_required_params_present = response.getQueryResult().getAllRequiredParamsPresent();
            String fulfillment_text = response.getQueryResult().getFulfillmentText();

            /*
            Log.d(TAG, "V2 Bot Reply: " + botReply);
            Log.d(TAG, "Params: " + params);
             */

            /*
            Log.d(TAG, "params: " + params);
             */

            if (intent_displayName.equalsIgnoreCase("Concertar una cita")){
                if (all_required_params_present){

                    Map<String, Value> collection = response.getQueryResult().getParameters().getFieldsMap();
                    Struct collection2 = response.getQueryResult().getParameters();
                    Log.d(TAG, "colletion1:" + collection);
                    Log.d(TAG, "colletion2:" + collection2);
                    Log.d(TAG, "Key and values:" + collection.keySet() + collection.values());

                    String time = "";
                    String date = "";
                    String address = "";
                    String titulo = "";
                    for (Map.Entry<String, Value> entry : collection.entrySet()) {
                        if (entry.getKey().equalsIgnoreCase("time")){
                            time = entry.getValue().toString();
                        }
                        if (entry.getKey().equalsIgnoreCase("date")){
                            date = entry.getValue().toString();
                        }
                        if (entry.getKey().equalsIgnoreCase("address")){
                            address = entry.getValue().toString();
                        }
                        if (entry.getKey().equalsIgnoreCase("titulo")){
                            titulo = entry.getValue().toString();
                        }

                    }
                    /*
                    Log.d(TAG, "time_F:" + time.substring(25,time.length()-2));
                    Log.d(TAG, "date_F:" + date.substring(15,25));
                    Log.d(TAG, "location_F:" + address.substring(15,address.length()-2));
                    Log.d(TAG, "title_F:" + titulo.substring(15,titulo.length()-2));
                     */

                    String date_time = date.substring(15,25) + time.substring(25,time.length()-2);
                    address = address.substring(15,address.length()-2);
                    titulo = titulo.substring(15,titulo.length()-2);


                    saveInCalendar(date_time,titulo,address);


                }


            }

            etFrase.setText("");
            tvBot.setText("Respuesta Bot: "+botReply);
            showTextView(botReply, BOT);

        } else {
            Log.d(TAG, "Bot Reply: Null");
            /*
            Log.i(TAG, "Bot Reply: Null");
             */
            showTextView("There was some communication issue. Please Try again!", BOT);

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void saveInCalendar(String date, String title, String location) {

        /*
        String datee = "2022-05-06T12:00:00+02:00";
         */

        DateTimeFormatter isoDateFormatter = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime ldate = LocalDateTime.parse(date, isoDateFormatter);
        Date rDate = Date.from(ldate.atZone(ZoneId.of("UTC+0")).toInstant());
        long begin = rDate.getTime();

        try {

            Intent intent = new Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE, title)
                    .putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
        }
        catch(Exception e) {
            Log.i(TAG, "Fallo"+e);
        }

    }

    private void showTextView(String message, int type) {
        /*
        Log.d(TAG, "message" + message);
         */
        /*
        Log.i(TAG, "message" + message);
         */
    }



    @Override
    protected void onDestroy() {
        tts.shutdown();
        super.onDestroy();
    }

    @Override
    public void onInit(int i) {
        ttsReady = true;
        tts.setLanguage(new Locale("spa", "ES"));
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initialize() {
        tts = new TextToSpeech(this, this);

        btConfirmar = findViewById(R.id.btConfirmar);
        etFrase = findViewById(R.id.etFrase);
        tvResultado = findViewById(R.id.tvResultado);
        tvBot = findViewById(R.id.tvBot);

        //asignar un evento al boton
        btConfirmar.setOnClickListener(view -> onClickBtConfirmar());
        btConfirmar.setOnClickListener(view -> sendMessage());

        btHablar = findViewById(R.id.btHablar);
        btHablar.setOnClickListener(view -> onClickBtHablar());


    }

    private void initV2Chatbot() {
        try {
            InputStream stream = getResources().openRawResource(R.raw.credential);
            GoogleCredentials credentials = GoogleCredentials.fromStream(stream);
            String projectId = ((ServiceAccountCredentials)credentials).getProjectId();

            SessionsSettings.Builder settingsBuilder = SessionsSettings.newBuilder();
            SessionsSettings sessionsSettings = settingsBuilder.setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();
            sessionsClient = SessionsClient.create(sessionsSettings);
            session = SessionName.of(projectId, uuid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void onClickBtConfirmar() {
        String frase = etFrase.getText().toString();
        etFrase.setText("");
        tvResultado.setText(frase);

        if(ttsReady && !tvResultado.getText().toString().isEmpty()) {
            tts.speak(tvResultado.getText().toString(), TextToSpeech.QUEUE_ADD, null, null);
        }

    }

    private void onClickBtHablar(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Need to speak");
        try {
            startActivityForResult(intent, REQ_CODE);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    "Sorry your device not supported",
                    Toast.LENGTH_SHORT).show();
        }

    }

    private void sendMessage() {
        String msg = etFrase.getText().toString();
        if (msg.trim().isEmpty()) {
            Toast.makeText(MainActivity.this, "Please enter your query!", Toast.LENGTH_LONG).show();
        } else {
            /*Log.d(TAG, "enviando mensaje: ");

             */
            Log.i(TAG, "enviando mensaje: " + msg);
            showTextView(msg, USER);
            /*
            etFrase.setText("");
             */

            // Java V2
            /*
            etFrase.setText(msg);
             */
            QueryInput queryInput = QueryInput.newBuilder().setText(TextInput.newBuilder().setText(msg).setLanguageCode("en-US")).build();
            new RequestJavaV2Task(MainActivity.this, session, sessionsClient, queryInput).execute();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE) {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> result = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                etFrase.setText(result.get(0));
            }
        }
    }



}

    class RequestJavaV2Task extends AsyncTask<Void, Void, DetectIntentResponse> {

        Activity activity;
        private SessionName session;
        private SessionsClient sessionsClient;
        private QueryInput queryInput;

        RequestJavaV2Task(Activity activity, SessionName session, SessionsClient sessionsClient, QueryInput queryInput) {
            this.activity = activity;
            this.session = session;
            this.sessionsClient = sessionsClient;
            this.queryInput = queryInput;
        }

        @Override
        protected DetectIntentResponse doInBackground(Void... voids) {
            try{
                DetectIntentRequest detectIntentRequest =
                        DetectIntentRequest.newBuilder()
                                .setSession(session.toString())
                                .setQueryInput(queryInput)
                                .build();
                return sessionsClient.detectIntent(detectIntentRequest);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        protected void onPostExecute(DetectIntentResponse response) {
            ((MainActivity) activity).callbackV2(response);
        }
    }