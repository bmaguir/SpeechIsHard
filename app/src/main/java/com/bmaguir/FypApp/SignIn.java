package com.bmaguir.FypApp;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.multiplayer.Multiplayer;

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

    private boolean mSignInAutomatically = true;

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
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public void signIn(View v) {
        Log.d(TAG, "signing in");
        if (mGoogleApiClient != null && !mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    public void signOut(View v) {
        if (mGoogleApiClient.isConnected()) {
            Toast.makeText(this, "Signing out", Toast.LENGTH_LONG).show();
            Games.signOut(mGoogleApiClient);
        }

    }

    public void play(View v){
        if(mGoogleApiClient.isConnected()) {
            Intent intent = new Intent(this, StartActivity.class);
            startActivity(intent);
        }
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
                        mSignInAutomatically = false;
                        Button b = (Button) findViewById(R.id.signIn);
                        b.setVisibility(View.VISIBLE);
                        b = (Button) findViewById(R.id.play);
                        b.setVisibility(View.GONE);
                        break;
                    case(2):
                        if(!mGoogleApiClient.isConnected())
                            mGoogleApiClient.connect();
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

        //start game activity
        if(mSignInAutomatically) {
            Intent intent = new Intent(this, StartActivity.class);
            startActivityForResult(intent, START_ACTIVITY_REQUEST);
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
}