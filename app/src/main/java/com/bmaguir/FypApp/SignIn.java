package com.bmaguir.FypApp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ActionViewTarget;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.multiplayer.Multiplayer;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Brian on 19/02/2015.
 */
public class SignIn extends Activity  implements
            GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "debugSignInActivity";

    private static final String KEY_IN_RESOLUTION = "is_in_resolution";

    /**
     * Request code for auto Google Play Services error resolution.
     */
    protected static final int REQUEST_CODE_RESOLUTION = 1;
    private static final int START_ACTIVITY_REQUEST = 22;

    /**
     * Google API client.
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     * Determines if the client is in a resolution state, and
     * waiting for resolution intent to return.
     */
    private boolean mIsInResolution;

    private boolean mSignInAutomatically;

    /**
     * Called when the activity is starting. Restores the activity state.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_in);
        if (savedInstanceState != null) {
            mIsInResolution = savedInstanceState.getBoolean(KEY_IN_RESOLUTION, false);
        }
        Log.d(TAG, "app created");
        getSignInAutomatically();
        if(isFirstTime()){
            onFirstTime();
        }

    }

    private void getSignInAutomatically(){
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        mSignInAutomatically = preferences.getBoolean("signInAutomatically", false);
        if (!mSignInAutomatically) {
            // first time
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("signInAutomatically", false);
            editor.commit();
        }
    }

    private void setSignInAutomatically(Boolean b){
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("signInAutomatically", b);
        editor.commit();
    }

    private void onFirstTime(){

        ViewTarget target = new ViewTarget(R.id.signIn, this);
        new ShowcaseView.Builder(this)
                .setTarget(target)
                .setContentTitle("Sign In")
                .setContentText("Sign in to Google+ to allow for multilayer games")
                .hideOnTouchOutside()
                .build();

    }

    private boolean isFirstTime()
    {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        boolean ranBefore = preferences.getBoolean("isFirstTime", false);
        if (!ranBefore) {
            // first time
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isFirstTime", true);
            editor.commit();
        }
        return !ranBefore;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    public void signIn(View v) {
        Log.d(TAG, "signing in");
        if (mGoogleApiClient != null && !mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
        setSignInAutomatically(true);
    }

    public void signOut(View v) {
        if (mGoogleApiClient.isConnected()) {
            Toast.makeText(this, "Signing out", Toast.LENGTH_LONG).show();
            Games.signOut(mGoogleApiClient);
        }
        Button b = (Button) findViewById(R.id.signOut);
        b.setVisibility(View.GONE);
        b = (Button) findViewById(R.id.play);
        b.setVisibility(View.GONE);
        b = (Button) findViewById(R.id.signIn);
        b.setVisibility(View.VISIBLE);
        setSignInAutomatically(false);

    }

    public void play(View v){
        //new ServletPostAsyncTask().execute(new Pair<Context, String>(this, "Manfred"));
        //commented out for debugging backend
        ///*
        if(mGoogleApiClient.isConnected()) {
            Intent intent = new Intent(this, StartActivity.class);
            startActivity(intent);
        }
        //*/
    }

    /**
     * Called when the Activity is made visible.
     * A connection to Play Services need to be initiated as
     * soon as the activity is visible. Registers {@code ConnectionCallbacks}
     * and {@code OnConnectionFailedListener} on the
     * activities itself.
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Games.API)
                    .addScope(Games.SCOPE_GAMES)
                            // Optionally, add additional APIs and scopes if required.
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        if(mSignInAutomatically) {
            mGoogleApiClient.connect();
        }
    }

    /**
     * Called when activity gets invisible. Connection to Play Services needs to
     * be disconnected as soon as an activity is invisible.
     */
    @Override
    protected void onStop() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    /**
     * Saves the resolution state.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IN_RESOLUTION, mIsInResolution);
    }

    /**
     * Handles Google Play Services resolution callbacks.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "on activity result");
        switch (requestCode) {
            case (REQUEST_CODE_RESOLUTION) :
            {
                Log.d(TAG, "on activity result retry connection");
                retryConnecting();
                break;
            }
            case (START_ACTIVITY_REQUEST):
            {
                switch (resultCode){
                    case(0):
                        finish();
                        break;
                    case(1):
                        if(mGoogleApiClient.isConnected()) {
                            mGoogleApiClient.disconnect();
                        }
                        setSignInAutomatically(false);
                        Button b = (Button) findViewById(R.id.signIn);
                        b.setVisibility(View.VISIBLE);
                        b = (Button) findViewById(R.id.play);
                        b.setVisibility(View.GONE);
                        b = (Button) findViewById(R.id.signOut);
                        b.setVisibility(View.GONE);
                        break;
                    case(2):
                        if(!mGoogleApiClient.isConnected())
                            mGoogleApiClient.connect();
                        break;
                    case(3):
                        Log.d(TAG, "connection lost");
                        break;
                }
            }
        }
    }

    private void retryConnecting() {
        mIsInResolution = false;
        if (!mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
    }

    /**
     * Called when {@code mGoogleApiClient} is connected.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "GoogleApiClient connected");
        Button b = (Button) findViewById(R.id.signIn);
        b.setVisibility(View.GONE);
        Button p = (Button) findViewById(R.id.play);
        p.setVisibility(View.VISIBLE);
        p = (Button) findViewById(R.id.signOut);
        p.setVisibility(View.VISIBLE);

        //start game activity
        if(mSignInAutomatically) {
            //commented out for debugging backend!
            ///*
            Intent intent = new Intent(this, StartActivity.class);
            startActivityForResult(intent, START_ACTIVITY_REQUEST);
            //*/
        }
    }

    /**
     * Called when {@code mGoogleApiClient} connection is suspended.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
        retryConnecting();
    }

    /**
     * Called when {@code mGoogleApiClient} is trying to connect but failed.
     * Handle {@code result.getResolution()} if there is a resolution
     * available.
     */

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // Show a localized error dialog.
            Toast.makeText(this, "Problem connecting!", Toast.LENGTH_LONG).show();
            return;
        }
        // If there is an existing resolution error being displayed or a resolution
        // activity has started before, do nothing and wait for resolution
        // progress to be completed.
        if (mIsInResolution) {
            return;
        }
        mIsInResolution = true;
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
            retryConnecting();
        }
    }

    class ServletPostAsyncTask extends AsyncTask<Pair<Context, String>, Void, String> {
        private Context context;

        @Override
        protected String doInBackground(Pair<Context, String>... params) {
            context = params[0].first;
            String name = params[0].second;

            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("http://10.0.2.2:8080/hello"); // 10.0.2.2 is localhost's IP address in Android emulator
            try {
                // Add name data to request
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
                nameValuePairs.add(new BasicNameValuePair("name", name));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                HttpResponse response = httpClient.execute(httpPost);
                if (response.getStatusLine().getStatusCode() == 200) {
                    return EntityUtils.toString(response.getEntity());
                }
                return "Error: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();

            } catch (ClientProtocolException e) {
                return e.getMessage();
            } catch (IOException e) {
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(context, result, Toast.LENGTH_LONG).show();
        }
    }
}