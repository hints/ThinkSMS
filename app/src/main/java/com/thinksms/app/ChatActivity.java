package com.thinksms.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Stack;

import ai.wit.sdk.IWitListener;
import ai.wit.sdk.Wit;

import com.google.gson.JsonObject;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.Constants;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.ListCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.NotificationTextCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.SimpleTextCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManager;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManagerListener;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCards;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCardsException;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteResourceStore;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteToqNotification;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.resource.DeckOfCardsLauncherIcon;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.util.ParcelableUtil;

import org.json.JSONObject;

import com.philips.lighting.hue.listener.PHLightListener;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.utilities.PHUtilities;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;



public class ChatActivity extends ActionBarActivity implements IWitListener {
    private static final String TAG = "ChatActivity";

    public static Wit WIT_FRAG = null;

    private final static String DEMO_PREFS_FILE= "demo_prefs_file";
    private final static String DECK_OF_CARDS_KEY= "deck_of_cards_key";
    private final static String DECK_OF_CARDS_VERSION_KEY= "deck_of_cards_version_key";

    private DeckOfCardsManager deckOfCardsManager;

    private DeckOfCardsManagerListener deckOfCardsManagerListener;
    private DeckOfCardsEventListener deckOfCardsEventListener;

    private RemoteResourceStore resourceStore;

    private RemoteDeckOfCards deckOfCards;

    private Stack<EmotionState> emotionStates = new Stack<EmotionState>();

    private static ChatActivity instance;
    private PHHueSDK phHueSDK;

    public static ChatActivity getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        this.instance = this;

        phHueSDK = PHHueSDK.create();


        PlaceholderFragment holder = new PlaceholderFragment();
        emotionStates.push(new EmotionState());
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, holder)
                    .commit();
        }

        // Get the reference to the deck of cards manager
        deckOfCardsManager= DeckOfCardsManager.getInstance(getApplicationContext());
//        Logger.setLoggingEnabled(false); // Disable api logging
//        Logger.setTag("MyApp"); // Set custom api logging tag

        // Create listeners
        deckOfCardsManagerListener= new DeckOfCardsManagerListenerImpl();
        deckOfCardsEventListener= new DeckOfCardsEventListenerImpl(holder.wit_fragment);
        initDeckOfCards();
    }


    /**
     * @see android.app.Activity#onStart()
     */
    protected void onStart(){
        super.onStart();
        Log.d(TAG, "ToqApiDemo.onStart");
        // Add the listeners
        deckOfCardsManager.addDeckOfCardsManagerListener(deckOfCardsManagerListener);
        deckOfCardsManager.addDeckOfCardsEventListener(deckOfCardsEventListener);

        // If not connected, try to connect
        if (!deckOfCardsManager.isConnected()){

            Log.d(TAG, "ToqApiDemo.onStart - not connected, connecting...");

            try{
                deckOfCardsManager.connect();
            }
            catch (RemoteDeckOfCardsException e){
                Toast.makeText(this, "Error connecting to service", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "ToqApiDemo.onStart - error connecting to Toq app service", e);
            }

        }
        else{
            Log.d(TAG, "ToqApiDemo.onStart - already connected");
        }
    }


    /**
     * @see android.app.Activity#onStop()
     */
    public void onStop(){
        super.onStop();
        Log.d(TAG, "ToqApiDemo.onStop");
        // Remove listeners
        deckOfCardsManager.removeDeckOfCardsManagerListener(deckOfCardsManagerListener);
        deckOfCardsManager.removeDeckOfCardsEventListener(deckOfCardsEventListener);
    }


    /**
     * @see android.app.Activity#onDestroy()
     */
    public void onDestroy(){
        super.onDestroy();
        Log.d(Constants.TAG, "ToqApiDemo.onDestroy");
        PHBridge bridge = phHueSDK.getSelectedBridge();
        if (bridge != null) {

            if (phHueSDK.isHeartbeatEnabled(bridge)) {
                phHueSDK.disableHeartbeat(bridge);
            }

            phHueSDK.disconnect(bridge);
        }
        deckOfCardsManager.disconnect();
        this.instance = null;
    }


    public void setEmotionState(EmotionState state) {
        if (state.endDate != null) {
            // push state and schedule pop
            emotionStates.push(state);
            // Schedule pop
            long delay = (state.endDate.getTime() - new Date().getTime()) / 1000;
            Log.d(TAG, "Delaying notifications for " + delay + " seconds.");
            MessageQueue.getInstance().queuePopState((state.endDate.getTime() - new Date().getTime()) / 1000);
        }
        else {
           // replace top state
            emotionStates.pop();
            emotionStates.push(state);
        }
        ((TextView)findViewById(R.id.emoticonText)).setText(state.emoticon);

        if (!this.doNotDisturb()) {
            MessageQueue.getInstance().deliverPendingMessagesOnUIThread();
        }
    }


    public void popEmotionState() {
        Log.d(TAG, "Pop pending emotion state, if any");
        if (emotionStates.size() > 1) {
            emotionStates.pop();
            if (!this.doNotDisturb()) {
                MessageQueue.getInstance().deliverPendingMessagesOnUIThread();
            }
        }
       else {
            Log.d(TAG, "Did not pop state, only one state in the stack");
        }
    }


    public EmotionState getEmotionState() {
        return emotionStates.peek();
    }


    public Boolean quietMode() {
        Boolean quietMode = false;
        EmotionState state = getEmotionState();
        if (state != null) {
            if ("Tired".equals(state.intent)) {
                quietMode = true;
            }
        }
        return quietMode;
    }


    public Boolean doNotDisturb() {
        Boolean doNotDisturb = false;
        EmotionState state = getEmotionState();
        if (state != null) {
            if ("Sleeping".equals(state.intent)) {
                doNotDisturb = true;
            }
            else if ("Busy".equals(state.intent)) {
                doNotDisturb = true;
            }
        }
        return doNotDisturb;
    }


    public void showMessageReceived(String text, String emoticon) {
        TextView messageView = (TextView) findViewById(R.id.jsonView);
        CharSequence oldText = messageView.getText();
        CharSequence newText = Html.fromHtml("<span><b>" + emoticon +
                "<b></span>&nbsp;&nbsp;&nbsp;&nbsp;<span>" + text + "</span><br/>");
        messageView.setText(TextUtils.concat(oldText, newText));
    }

    // Initialise deck of cards
    private void initDeckOfCards(){

        // Try to retrieve a stored deck of cards
        try{

            // If there is no stored deck of cards or it is unusable, then create new and store
            if ((deckOfCards= getStoredDeckOfCards()) == null){
                deckOfCards= createDeckOfCards();
                storeDeckOfCards();
            }

        }
        catch (Throwable th){
            Log.w(Constants.TAG, "ToqApiDemo.initDeckOfCards - error occurred retrieving the stored deck of cards: " + th.getMessage());
            deckOfCards= null; // Reset to force recreate
        }

        // Make sure in usable state
        if (deckOfCards == null){
            deckOfCards= createDeckOfCards();
        }

        // Get the icons
        resourceStore= new RemoteResourceStore();

        try{

            DeckOfCardsLauncherIcon whiteIcon= new DeckOfCardsLauncherIcon("white", getIcon("white.png"), DeckOfCardsLauncherIcon.WHITE);
            DeckOfCardsLauncherIcon colorIcon= new DeckOfCardsLauncherIcon("color", getIcon("color.png"), DeckOfCardsLauncherIcon.COLOR);

            // Re-add the icons
            deckOfCards.setLauncherIcons(resourceStore, new DeckOfCardsLauncherIcon[]{whiteIcon, colorIcon});

            /*
            ListCard listCard= deckOfCards.getListCard();

            // Notification
            ((EditText)findViewById(R.id.notification_title_text)).setText("Emoji");
            ((EditText)findViewById(R.id.notification_message_text)).setText(concatStrings(new String[]{"Hello World"}));
            ((EditText)findViewById(R.id.notification_info_text)).setText("99");
            ((CheckBox)findViewById(R.id.notification_events_checkbox)).setChecked(true);
            ((EditText)findViewById(R.id.notification_menu_options_text)).setText(concatStrings(new String[]{"Reply"}));
            ((CheckBox)findViewById(R.id.notification_divider_checkbox)).setChecked(true);
            ((CheckBox)findViewById(R.id.notification_vibe_checkbox)).setChecked(true);

            // Status
            statusTextView= (TextView)findViewById(R.id.status_text);
            statusTextView.setText("Initialised");
            */

        }
        catch (Exception e){
            Toast.makeText(this, "Error initializing deck of cards", Toast.LENGTH_SHORT).show();
            Log.e(Constants.TAG, "ToqApiDemo.initDeckOfCards - error occurred parsing the icons", e);
        }

    }


    // Install deck of cards applet
    private void installDeckOfCards() {

        Log.d(TAG, "ToqApiDemo.installDeckOfCards");

//        updateDeckOfCardsFromUI();

        try{
            deckOfCardsManager.installDeckOfCards(deckOfCards, resourceStore);
        }
        catch (RemoteDeckOfCardsException e){
            Toast.makeText(this, "Error installing deck of cards.", Toast.LENGTH_SHORT).show();
            Log.e(Constants.TAG, "ToqApiDemo.installDeckOfCards - error installing deck of cards applet", e);
        }

        try{
            storeDeckOfCards();
        }
        catch (Exception e){
            Log.e(Constants.TAG, "ToqApiDemo.installDeckOfCards - error storing deck of cards applet", e);
        }

    }

    // Get stored deck of cards if one exists
    private RemoteDeckOfCards getStoredDeckOfCards() throws Exception{

        if (!isValidDeckOfCards()){
            Log.w(Constants.TAG, "ToqApiDemo.getStoredDeckOfCards - stored deck of cards not valid for this version of the demo, recreating...");
            return null;
        }

        SharedPreferences prefs= getSharedPreferences(DEMO_PREFS_FILE, Context.MODE_PRIVATE);
        String deckOfCardsStr= prefs.getString(DECK_OF_CARDS_KEY, null);

        if (deckOfCardsStr == null){
            return null;
        }
        else{
            return ParcelableUtil.unmarshall(deckOfCardsStr, RemoteDeckOfCards.CREATOR);
        }

    }

    // Store deck of cards
    private void storeDeckOfCards() throws Exception{
        SharedPreferences prefs= getSharedPreferences(DEMO_PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor= prefs.edit();
        editor.putString(DECK_OF_CARDS_KEY, ParcelableUtil.marshall(deckOfCards));
        editor.putInt(DECK_OF_CARDS_VERSION_KEY, Constants.VERSION_CODE);
        editor.commit();
    }


    // Check if the stored deck of cards is valid for this version of the demo
    private boolean isValidDeckOfCards(){

        SharedPreferences prefs= getSharedPreferences(DEMO_PREFS_FILE, Context.MODE_PRIVATE);
        int deckOfCardsVersion= prefs.getInt(DECK_OF_CARDS_VERSION_KEY, 0);

        if (deckOfCardsVersion < Constants.VERSION_CODE){
            return false;
        }

        return true;
    }


    // Create some cards with example content
    private RemoteDeckOfCards createDeckOfCards(){

        ListCard listCard= new ListCard();

        // Card 1
        SimpleTextCard simpleTextCard= new SimpleTextCard("card1", "Think SMS",
                System.currentTimeMillis(),
                "Welcome",
                new String[]{"SMS"});
        simpleTextCard.setInfoText("10");
        simpleTextCard.setReceivingEvents(true);
        simpleTextCard.setMenuOptions(new String[]{"Ping back"});
        simpleTextCard.setShowDivider(false);
        listCard.add(simpleTextCard);

        return new RemoteDeckOfCards(this, listCard);
    }


    // Read an icon from assets and return as a bitmap
    private Bitmap getIcon(String fileName) throws Exception{

        try{
            InputStream is= getAssets().open(fileName);
            return BitmapFactory.decodeStream(is);
        }
        catch (Exception e){
            throw new Exception("An error occurred getting the icon: " + fileName, e);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public void colorLights(int r,int g,int b)
    {
        PHBridge bridge = phHueSDK.getSelectedBridge();

        if (bridge == null) {
            Log.d(TAG, "No hue bridge found, early out");
            return;
        }

        List<PHLight> allLights = bridge.getResourceCache().getAllLights();

        for (PHLight light : allLights)
        {

            float xy[] = PHUtilities.calculateXYFromRGB(r,g,b, light.getModelNumber());


            PHLightState lightState = new PHLightState();
            lightState.setX(xy[0]);
            lightState.setY(xy[1]);
            // To validate your lightstate is valid (before sending to the bridge) you can use:
            // String validState = lightState.validateState();
            bridge.updateLightState(light, lightState, lightListener);
            //  bridge.updateLightState(light, lightState);   // If no bridge response is required then use this simpler form.
        }
    }


    // If you want to handle the response from the bridge, create a PHLightListener object.
    PHLightListener lightListener = new PHLightListener() {

        @Override
        public void onSuccess() {
            Log.w(TAG, "Light onSuccess");
        }

        @Override
        public void onStateUpdate(Hashtable<String, String> arg0, List<PHHueError> arg1) {
            //   Log.w(TAG, "Light has updated");
        }

        @Override
        public void onError(int arg0, String arg1) {
            Log.w(TAG, "Light onError");
        }
    };

    public void setRgbColorForEmoticon(String emoticon)
    {
        String [] colors = EmoticonMap.rgbForEmoticon(emoticon);
        int r = Integer.parseInt(colors[0]);
        int g = Integer.parseInt(colors[1]);
        int b = Integer.parseInt(colors[2]);
        colorLights(r,g,b);

    }


    @Override
    public void witDidGraspIntent(String intent, HashMap<String, JsonElement> entities, String body, double confidence, Error error) {
        ((TextView) findViewById(R.id.txtText)).setText(body);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(entities);
//        ((TextView) findViewById(R.id.jsonView)).setText(Html.fromHtml("<span><b>Intent: " + intent +
//                "<b></span><br/>") + jsonOutput +
//                Html.fromHtml("<br/><span><b>Confidence: " + confidence + "<b></span>"));

        List<String> messageEntities = new ArrayList<String>();
        Message message = new Message(body, intent, messageEntities);

        String [] colors = EmoticonMap.rgbForIntent(intent);
        int r = Integer.parseInt(colors[0]);
        int g = Integer.parseInt(colors[1]);
        int b = Integer.parseInt(colors[2]);
 //       colorLights(r,g,b);

        EmotionState emotionState = new EmotionState();
        emotionState.intent = message.intent;
        emotionState.emoticon = message.getEmoticon();

        // Parse datetime, if we have one
        JsonElement datetimeElement = entities.get("datetime");
        if (datetimeElement != null) {
            JsonObject datetime = datetimeElement.getAsJsonObject();
            JsonObject value = datetime.get("value").getAsJsonObject();
            String startDate = value.get("from").getAsString();
            String endDate = value.get("to").getAsString();
            SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            try {
                emotionState.startDate = dateParser.parse(startDate);
                emotionState.endDate = dateParser.parse(endDate);
                Log.d(TAG, "Date: " + emotionState.endDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        // Parse duration, if we have one
        JsonElement durationElement = entities.get("duration");
        if (durationElement != null) {
            JsonObject duration = durationElement.getAsJsonObject();
            int seconds = duration.get("value").getAsInt();
            emotionState.startDate = new Date();
            emotionState.endDate = new Date(emotionState.startDate.getTime() + (seconds * 1000));
            Log.d(TAG, "Duration: " + seconds + " seconds");
        }

        this.setEmotionState(emotionState);

        message.send();  // Send the message as a push notification
    }


    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public Wit wit_fragment = null;

        public PlaceholderFragment() {
            // Wit wit_fragment = (Wit) getFragmentManager().findFragmentByTag("wit_fragment");
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            // Initialize Fragment
            Wit wit_fragment = (Wit) getFragmentManager().findFragmentByTag("wit_fragment");
            ChatActivity.WIT_FRAG = wit_fragment;

            if (wit_fragment != null) {
                wit_fragment.setAccessToken("RNYDQYNPJ3XZUIG5HLAVG7YBP6XHRQ5I");
            }

            return rootView;
        }
    }


    // Handle service connection lifecycle and installation events
    private class DeckOfCardsManagerListenerImpl implements DeckOfCardsManagerListener {
        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManagerListener#onConnected()
         */
        public void onConnected() {
            runOnUiThread(new Runnable(){
                public void run(){
                    Log.d(TAG, "Connected!!!");
                     // installDeckOfCards();
//                    setStatus(getString(R.string.status_connected));
//                    refreshUI();
                }
            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManagerListener#onDisconnected()
         */
        public void onDisconnected() {
//            runOnUiThread(new Runnable(){
//                public void run(){
//                    setStatus(getString(R.string.status_disconnected));
//                    disableUI();
//                }
//            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManagerListener#onInstallationSuccessful()
         */
        public void onInstallationSuccessful() {
//            runOnUiThread(new Runnable(){
//                public void run(){
//                    setStatus(getString(R.string.status_installation_successful));
//                    updateUIInstalled();
//                }
//            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManagerListener#onInstallationDenied()
         */
        public void onInstallationDenied() {
//            runOnUiThread(new Runnable(){
//                public void run(){
//                    setStatus(getString(R.string.status_installation_denied));
//                    updateUINotInstalled();
//                }
//            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManagerListener#onUninstalled()
         */
        public void onUninstalled() {
//            runOnUiThread(new Runnable(){
//                public void run(){
//                    setStatus(getString(R.string.status_uninstalled));
//                    updateUINotInstalled();
//                }
//            });
//        }
        }
    }


    // Handle card events triggered by the user interacting with a card in the installed deck of cards
    private class DeckOfCardsEventListenerImpl implements DeckOfCardsEventListener {

        public Wit wit_fragment = null;
        public DeckOfCardsEventListenerImpl(Wit wf) {
            this.wit_fragment = wf;
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener#onCardOpen(java.lang.String)
         */
        public void onCardOpen(final String cardId){
//            runOnUiThread(new Runnable(){
//                public void run(){
//                    Toast.makeText(ToqApiDemo.this, getString(R.string.event_card_open) + cardId, Toast.LENGTH_SHORT).show();
//                }
//            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener#onCardVisible(java.lang.String)
         */
        public void onCardVisible(final String cardId){
//            runOnUiThread(new Runnable(){
//                public void run(){
//                    Toast.makeText(ToqApiDemo.this, getString(R.string.event_card_visible) + cardId, Toast.LENGTH_SHORT).show();
//                }
//            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener#onCardInvisible(java.lang.String)
         */
        public void onCardInvisible(final String cardId){
//            runOnUiThread(new Runnable(){
//                public void run(){
//                    Toast.makeText(ToqApiDemo.this, getString(R.string.event_card_invisible) + cardId, Toast.LENGTH_SHORT).show();
//                }
//            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener#onCardClosed(java.lang.String)
         */
        public void onCardClosed(final String cardId){
//            runOnUiThread(new Runnable(){
//                public void run(){
//                    Toast.makeText(ToqApiDemo.this, getString(R.string.event_card_closed) + cardId, Toast.LENGTH_SHORT).show();
//                }
//            });
        }

        /**
         * @see com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener#onMenuOptionSelected(java.lang.String, java.lang.String)
         */
        public void onMenuOptionSelected(final String cardId, final String menuOption){
            // final Wit wf = this.wit_fragment;


            runOnUiThread(new Runnable(){
                public void run(){
                    Toast.makeText(ChatActivity.this, "Menu option selected" + cardId + " [" + menuOption +"]", Toast.LENGTH_SHORT).show();
                    // event_menu_option_selected
                    // NOTE(ricky): Call back from Toq -> Android.
                    Log.d(TAG, "Selected notification, start listening with mic");

                    ChatActivity.WIT_FRAG.triggerRec(false);

                    // ((Button) findViewById(R.id.btnSpeak)).performClick();
                }
            });
        }

    }

}


