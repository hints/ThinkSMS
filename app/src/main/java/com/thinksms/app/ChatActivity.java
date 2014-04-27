package com.thinksms.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ai.wit.sdk.IWitListener;
import ai.wit.sdk.Wit;
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


public class ChatActivity extends ActionBarActivity implements IWitListener {
    private static final String TAG = "ChatActivity";

    private final static String DEMO_PREFS_FILE= "demo_prefs_file";
    private final static String DECK_OF_CARDS_KEY= "deck_of_cards_key";
    private final static String DECK_OF_CARDS_VERSION_KEY= "deck_of_cards_version_key";

    private DeckOfCardsManager deckOfCardsManager;

    private DeckOfCardsManagerListener deckOfCardsManagerListener;
    private DeckOfCardsEventListener deckOfCardsEventListener;

    private RemoteResourceStore resourceStore;

    private RemoteDeckOfCards deckOfCards;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        // Get the reference to the deck of cards manager
        deckOfCardsManager= DeckOfCardsManager.getInstance(getApplicationContext());
//        Logger.setLoggingEnabled(false); // Disable api logging
//        Logger.setTag("MyApp"); // Set custom api logging tag

        // Create listeners
        deckOfCardsManagerListener= new DeckOfCardsManagerListenerImpl();
        deckOfCardsEventListener= new DeckOfCardsEventListenerImpl();
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
        deckOfCardsManager.disconnect();
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

    @Override
    public void witDidGraspIntent(String intent, HashMap<String, JsonElement> entities, String body, double confidence, Error error) {
        ((TextView) findViewById(R.id.txtText)).setText(body);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(entities);
        ((TextView) findViewById(R.id.jsonView)).setText(Html.fromHtml("<span><b>Intent: " + intent +
                "<b></span><br/>") + jsonOutput +
                Html.fromHtml("<br/><span><b>Confidence: " + confidence + "<b></span>"));

        List<String> messageTraits = new ArrayList<String>();
        Message message = new Message(body, messageTraits);
        message.send();  // Send the message as a push notification
    }


    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            // Initialize Fragment
            Wit wit_fragment = (Wit) getFragmentManager().findFragmentByTag("wit_fragment");
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
                    //installDeckOfCards();
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
            runOnUiThread(new Runnable(){
                public void run(){
                    Toast.makeText(ChatActivity.this, "Menu option selected" + cardId + " [" + menuOption +"]", Toast.LENGTH_SHORT).show();
                    // event_menu_option_selected
                    // NOTE(ricky): Call back from Toq -> Android.
                    Log.d(TAG, "Selected notification, start listening with mic");
                    ((Button) findViewById(R.id.btnSpeak)).performClick();
                }
            });
        }

    }

}


