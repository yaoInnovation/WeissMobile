package com.weiss.mobile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.weiss.bubblechat.R;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.Locale;


public class MainActivity extends Activity {
    private static final String TAG = "Weiss Main";
    private static final int SPEECH = 1;
    private static final String INIT_OPT = "init";
    private static final String INQUIRY_OPT = "inquiry";
    private int FID = -1;

    //Utils
    private TextToSpeech tts = null;
    private SpeechRecognizer mSpeechRecognizer; //speach recognizer for callbacks
    Intent mSpeechIntent; //intent for speech recogniztion
    DialogUtil dialogUtil = null;

    //Widgets
    private LinearLayout inputView = null;
    private ChatArrayAdapter chatArrayAdapter;
    private ListView listView;
    private EditText queryText;
    private Button speechButton;
    private Button askButton;
    private ImageButton modeBtn = null;

    //Flags
    private boolean isTalking = false; // indicate current input mode
    private int ttsResult = -1; // To store TTS result
    private boolean  isListenning = false;
    private boolean side = true;

    @Override
    protected void onDestroy() {
        //Close the Text to Speech Library
        if(tts != null) {

            tts.stop();
            tts.shutdown();
            Log.d(TAG, "TTS Destroyed");
        }
        super.onDestroy();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        setContentView(R.layout.activity_main);

        //Get elements
        askButton = (Button) findViewById(R.id.askButton);
        listView = (ListView) findViewById(R.id.listView);
        modeBtn = (ImageButton) findViewById(R.id.modeBtn);
        askButton = (Button) findViewById(R.id.askButton);
        queryText = (EditText) findViewById(R.id.query);
        inputView = (LinearLayout) findViewById(R.id.inputView);

        //setTTS
        setUpTTS();

        //set Voice Recognizer
        setRecognizer();

        //Set Button Listener
        createButton();
        modeBtn.setOnClickListener(changeModeListener);

        //Set layout
        modeBtn.setBackground(getResources().getDrawable(R.drawable.speech));

        //Set queryText listener
        queryText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    return askWeiss();
                }
                return false;
            }
        });

        //Set Ask Weiss Button Listener
        askButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                askWeiss();
            }
        });

        //Set List View for Dialog System
        chatArrayAdapter = new ChatArrayAdapter(getApplicationContext(), R.layout.activity_chat_singlemessage);
        listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listView.setAdapter(chatArrayAdapter);

        //to scroll the list view to bottom on data change
        chatArrayAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(chatArrayAdapter.getCount() - 1);
            }
        });


        //Set dialog Util
        dialogUtil = new DialogUtil();
        String []init = new String[1];
        init[0] = INIT_OPT;
        dialogUtil.execute(init);

    }

    private String getResponse(String query) {
        if(query.equalsIgnoreCase("What's your name")) {
            return "My name is Ming";
        } else if (query.equalsIgnoreCase("How old are you")) {
            return "I'm 23 years old";
        } else if (query.equalsIgnoreCase("How are you")) {
            return "I'm fine thank you, and you? (Classic Chinese Student Response)";
        } else if (query.equalsIgnoreCase("exit")){
            finish();
            return query;
        } else {
            return query;
        }
    }

    private boolean askWeiss(String query){

        //Invalid query
        if(query.replaceAll("\\s","").equals(""))
            return false;

        chatArrayAdapter.add(new ChatMessage(side,query));
        side = !side;

        String res = getResponse(query);
        dialogUtil = new DialogUtil();

        String []queryParams = new String[3];
        queryParams[0] = INQUIRY_OPT;
        queryParams[1] = Integer.toString(this.FID);
        queryParams[2] = query;
        dialogUtil.execute(queryParams);

        queryText.setText("");
        return true;
    }

    private boolean askWeiss(){

        String query =  queryText.getText().toString();
        //Invalid query
        if(query.replaceAll("\\s","").equals(""))
            return false;

        //Add inquiry to Adapter
        chatArrayAdapter.add(new ChatMessage(side,query));
        side = !side;

        dialogUtil = new DialogUtil();
        String []queryParams = new String[3];
        queryParams[0] = INQUIRY_OPT;
        queryParams[1] = Integer.toString(this.FID);
        queryParams[2] = query;
        dialogUtil.execute(queryParams);

        queryText.setText("");
        return true;
    }

    private View.OnClickListener changeModeListener = new View.OnClickListener() {
      public void onClick(View v) {
          Log.d(TAG, "Image button clicked!");
          ImageButton changeMode = (ImageButton)findViewById(R.id.modeBtn);

          if(isTalking == true) {
              changeMode.setBackground(getResources().getDrawable(R.drawable.speech));
              changeLayout(isTalking);
              queryText.requestFocus();
              InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
              imm.showSoftInput(queryText, InputMethodManager.SHOW_IMPLICIT);
              isTalking = !isTalking;

          } else {
              changeMode.setBackground(getResources().getDrawable(R.drawable.type));
              queryText.clearFocus();
              InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
              imm.hideSoftInputFromWindow(queryText.getWindowToken(), 0);
              changeLayout(isTalking);
              isTalking = !isTalking;
          }
      }
    };

    private void setUpTTS() {
        this.tts = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS) {
                    ttsResult = tts.setLanguage(Locale.US);
                } else {
                    Toast.makeText(getApplicationContext(), "Feature not support for this device!"
                            , Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });

    }

    private void setRecognizer() {
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(MainActivity.this);
        SpeechListener mRecognitionListener = new SpeechListener();
        mSpeechRecognizer.setRecognitionListener(mRecognitionListener);
        mSpeechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        // Given an hint to the recognizer about what the user is going to say
        mSpeechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        mSpeechIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,getApplication().getPackageName());

        //Wait at least for 60 seconds to complete input
        mSpeechIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,60000);

        // Specify how many results you want to receive. The results will be sorted
        // where the first result is the one with higher confidence.
        mSpeechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        mSpeechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);

    }

    /** Layout Functions */
    private static ViewGroup getParent(View view) {
        return (ViewGroup)view.getParent();
    }

    private static void removeView(View view) {
        ViewGroup parent = getParent(view);
        if(parent != null) {
            parent.removeView(view);
        }
    }

    private static void replaceView(View currentView, View newView) {
        ViewGroup parent = getParent(currentView);
        if(parent == null) {
            return;
        }
        final int index = parent.indexOfChild(currentView);
        removeView(currentView);
        parent.addView(newView, index);
    }

    private Button createButton() {
        this.speechButton = new Button(this);
        this.speechButton.setId(R.id.speechButton);
        this.speechButton.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        this.speechButton.setText("Hold to Talk");
        this.speechButton.setBackgroundResource(android.R.drawable.button_onoff_indicator_on);
        this.speechButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (isListenning == false) {
                        if(tts.isSpeaking())
                            tts.stop(); // stop current playing response
                        mSpeechRecognizer.startListening(mSpeechIntent);
                        speechButton.setBackgroundResource(android.R.drawable.button_onoff_indicator_on);
                        speechButton.setText("Release to Send");
                        isListenning = true;
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    Log.d("Released", "Button released");
                    mSpeechRecognizer.stopListening();
                    speechButton.setBackgroundResource(android.R.drawable.button_onoff_indicator_off);
                    speechButton.setText("Hold to Talk");
                    isListenning = false;
                }
                return true;
            }
        });
        return this.speechButton;
    }

    private void changeLayout(boolean isTalking) {
        if(isTalking == true) {
            this.replaceView(this.speechButton, this.queryText);
            //this.speechButton = null;
        } else {
            this.replaceView(this.queryText, this.speechButton);
        }
    }



    class SpeechListener implements RecognitionListener {
        public void onBufferReceived(byte[] buffer) {
            Log.d(TAG, "buffer recieved ");
        }
        public void onError(int error) {
            //if critical error then exit
            if(error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS){
                Log.d(TAG, "client error");
            }

            if(error == SpeechRecognizer.ERROR_NO_MATCH) {
                Log.d(TAG, "No Match");
                ArrayList<String> noMatch = new ArrayList<String>();
                noMatch.add("");
            }

            //else ask to repeats
            else{
                Log.d(TAG, "other error:" + error);
            }
        }

        public void onEvent(int eventType, Bundle params) {
            Log.d(TAG, "onEvent");
        }
        public void onPartialResults(Bundle partialResults) {
            Log.d(TAG, "partial results");
        }
        public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "on ready for speech");
        }
        public void onResults(Bundle results) {
            Log.d(TAG, "on results");
            ArrayList<String> matches = null;
            if(results != null){
                matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if(matches != null){
                    Log.d(TAG, "results are " + matches.toString());
                    final ArrayList<String> matchesStrings = matches;
                    askWeiss(matchesStrings.get(0));
                }
            }

        }
        public void onRmsChanged(float rmsdB) {
            //			Log.d(TAG, "rms changed");
        }
        public void onBeginningOfSpeech() {
            Log.d(TAG, "speach begining");
        }
        public void onEndOfSpeech() {
            //Log.d(TAG, "speach done");
        }

    }

    class DialogUtil extends AsyncTask<String, Void, Void> {
        private static final String USER_AGENT = "Mozilla/5.0";
        //For local test, run server on 0.0.0.0:8000 and use wifi ipv4 ip to access
        private static final String hostname = "http://192.168.0.101";
        private static final int port = 8000;
        private int fid = -1;
        private static final String TAG = "DialogUtil";

        private static final String initURL = "/api/init";
        private static final String inqueryURL = "/api/inquire";
        private static final String closeURL = "/api/close";


        private ArrayList<String> response = null;
        private String option = "";

        public DialogUtil() {

        }

        @Override
        protected Void doInBackground(String... params) {
            option = params[0];
            Log.d(TAG, "Do it background, opt:"+option);

            if(option.equals("init")) {
                init();
            } else if (option.equals("inquiry")) {
                fid = Integer.parseInt(params[1]);
                String query = params[2];
                inquiry(this.fid, query);
            } else {
                close();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Log.d(TAG, "OnPostExecute background:"+response.get(0));
            if(response == null) {
                return;
            }

            if(option.equals("init")) {
                FID = Integer.parseInt(response.get(0));
                String greetings = response.get(1);
                chatArrayAdapter.add(new ChatMessage(side, greetings));
                side = !side;


            } else if (option.equals("inquiry")) {
                String inqueryResponse = response.get(0);
                chatArrayAdapter.add(new ChatMessage(side, inqueryResponse));
                side = !side;

                if(ttsResult == TextToSpeech.LANG_NOT_SUPPORTED ||
                        ttsResult == TextToSpeech.LANG_MISSING_DATA) {
                    Toast.makeText(getApplicationContext(), "Your Device Doesn't support " +
                            "Speech To Text", Toast.LENGTH_LONG).show();
                } else {
                    tts.speak(inqueryResponse, TextToSpeech.QUEUE_FLUSH, null);
                }

            } else {
                close();
            }
            return;
        }


        public ArrayList<String> init() {
            response = new ArrayList<String>();

            String url = hostname+":"+port+initURL;

            String responseString = getRequest(url);
            if(responseString == null) {
                return null;
            }

            JsonParser jsonParser = new JsonParser();

            //Get fid
            JsonElement fidObj = jsonParser.parse(responseString).getAsJsonObject().get("fid");
            response.add(fidObj.toString());
            this.fid = Integer.parseInt(fidObj.toString());

            //Get greetings
            JsonElement greetingsObj = jsonParser.parse(responseString).getAsJsonObject().get("response");
            response.add(greetingsObj.toString());
            Log.d(TAG, "Init Background, got response:" + response.get(1));

            return response;
        }

        public ArrayList<String> inquiry(int fid, String query) {

            String url = hostname+":"+port+inqueryURL;
            String responseString = queryPostRequest(url, query);
            if(responseString == null) {
                return null;
            }

            JsonParser jsonParser = new JsonParser();
            response = new ArrayList<String>();

            //Get greetings
            JsonElement responseObj = jsonParser.parse(responseString).getAsJsonObject().get("response");
            response.add(responseObj.toString());

            return response;
        }

        public void close() {

            String url = hostname+":"+port+closeURL;
            closePostRequest(url);

        }

        /**
         * @brief getRequest template (currently used by init)
         * @param url
         * @return
         */
        private String getRequest(String url) {
            try {
                Log.d(TAG, "Get request Background:"+url);
                HttpClient httpClient = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(url);
                HttpResponse response = httpClient.execute(httpGet);

                int responseCode = response.getStatusLine().getStatusCode();
                if(responseCode != 200) {
                    Log.d(TAG, "Get request responseCode:"+responseCode);
                    return null;
                }
                else {
                    HttpEntity entity = response.getEntity();
                    String responseString = null;
                    if(entity != null) {
                        Log.d(TAG, "Get request Background:"+responseString);
                        responseString = EntityUtils.toString(entity);
                    }
                    Log.d(TAG, "Get request Background:"+responseString);
                    return responseString;
                }


            }catch(Exception e) {
                e.printStackTrace();
                Log.d(TAG, e.getClass().getSimpleName());
                Log.d(TAG, "URL:"+url);
                return null;
            }
        }

        /**
         * @brief send Query and get response
         * @param URL
         * @param query
         * @return
         */
        public String queryPostRequest(String URL, String query) {
            Gson gson= new Gson();
            HttpPost post = new HttpPost(URL);
            Query q = new Query(this.fid, query);
            HttpClient httpClient = new DefaultHttpClient();

            try {
                StringEntity postingString = new StringEntity(gson.toJson(q));//convert query to json
                post.setEntity(postingString);
                post.setHeader("Content-type", "application/json");
                HttpResponse response = httpClient.execute(post);

                int responseCode = response.getStatusLine().getStatusCode();
                if(responseCode != 200)
                    return null;
                else {
                    HttpEntity entity = response.getEntity();
                    String responseString = null;
                    if(entity != null) {
                        responseString = EntityUtils.toString(entity);
                    }
                    Log.d(TAG, "queryPostRequest:"+responseString);
                    return responseString;
                }
            } catch(Exception e) {
                Log.d(TAG, e.getClass().getSimpleName());
                return null;
            }finally {
                httpClient.getConnectionManager().shutdown();
            }
        }

        /**
         * @brief Close the session
         * @param URL
         */
        public void closePostRequest(String URL) {
            Gson gson= new Gson();
            HttpPost post = new HttpPost(URL);

            HttpClient httpClient = new DefaultHttpClient();

            try {
                StringEntity postingString = new StringEntity(gson.toJson(this.fid));//convert query to json
                post.setEntity(postingString);
                post.setHeader("Content-type", "application/json");

            } catch(Exception e) {
                Log.d(TAG, e.getClass().getSimpleName());
            }finally {
                httpClient.getConnectionManager().shutdown();
            }
        }

        private class Query {
            int fid = 0;
            String query;

            Query(int fid, String query) {
                this.fid = fid;
                this.query = query;
            }

        }
    }

}
