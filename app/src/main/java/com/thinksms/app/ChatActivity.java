package com.thinksms.app;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.util.HashMap;

import ai.wit.sdk.IWitListener;
import ai.wit.sdk.Wit;



public class ChatActivity extends ActionBarActivity implements IWitListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
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

}
