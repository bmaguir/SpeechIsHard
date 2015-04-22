package com.bmaguir.FypApp;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;


public class PracticePlayActivity extends StartActivity {

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //Toast.makeText(this, "Connection Failure", Toast.LENGTH_SHORT).show();
        //setResult(2);
       // finish();
    }

    public void playClick(View v){
        setMapInfo();
        mPlayerType = "player1";
        m_UnityPlayer.resume();
    }

    public void playAgain(View v){
        mPopupWindow.dismiss();
        setMapInfo();
        startGameNow();
    }

    @Override
    public void onBackPressed(){
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_practice_play, menu);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
