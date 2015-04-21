package com.bmaguir.FypApp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.google.android.gms.games.Games;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;

import java.util.ArrayList;


public class InvitePlayerActivity extends StartActivity implements OnInvitationReceivedListener {

    private String mIncomingInvitationId;
    private static final String TAG = "debugInvitePlayer";
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_invite_player, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.checkInvitations:
                Intent intent = Games.Invitations.getInvitationInboxIntent(mGoogleApiClient);
                startActivityForResult(intent, RC_INVITATION_INBOX);
                return true;
            case R.id.menuSignOut:
                if(mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.disconnect();
                }
                setResult(1);
                finish();
                return true;
            case R.id.debug:
                Intent i = Games.RealTimeMultiplayer.getSelectOpponentsIntent(mGoogleApiClient, 1, 2, true);
                startActivityForResult(i, RC_SELECT_PLAYERS);
                return true;
            case R.id.exitGame:
                setResult(2);
                finish();
            default:
                return false;
        }
    }

    @Override
    public void onInvitationReceived(Invitation invitation) {
        // show in-game popup to let user know of pending invitation
        Log.d(TAG, "recieved invitation ");
        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPopupWindow = new PopupWindow(
                inflater.inflate(R.layout.invitation_recieved_layout, null, false),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);
        mPopupWindow.showAtLocation(this.findViewById(R.id.frameLayout), Gravity.CENTER, 0, 0);
        // store invitation for use when player accepts this invitation
        mIncomingInvitationId = invitation.getInvitationId();
    }

    public void acceptInvitation(View v){
        mPopupWindow.dismiss();
        RoomConfig.Builder roomConfigBuilder = makeBasicRoomConfigBuilder();
        roomConfigBuilder.setInvitationIdToAccept(mIncomingInvitationId);
        Games.RealTimeMultiplayer.join(mGoogleApiClient, roomConfigBuilder.build());
        // prevent screen from sleeping during handshake
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void declineInvitation(View v){
        mPopupWindow.dismiss();
    }

    @Override
    public void onInvitationRemoved(String s) {

    }

    @Override
    public void onConnected(Bundle connectionHint) {
        super.onConnected(connectionHint);
        Games.Invitations.registerInvitationListener(mGoogleApiClient, this);
        if (connectionHint != null) {
            Invitation inv =
                    connectionHint.getParcelable(Multiplayer.EXTRA_INVITATION);

            if (inv != null) {
                // accept invitation
                RoomConfig.Builder roomConfigBuilder = makeBasicRoomConfigBuilder();
                roomConfigBuilder.setInvitationIdToAccept(inv.getInvitationId());
                Games.RealTimeMultiplayer.join(mGoogleApiClient, roomConfigBuilder.build());

                // prevent screen from sleeping during handshake
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                // go to game screen
            }
        }
    }

    public void playClick(View v){
        Intent i = Games.RealTimeMultiplayer.getSelectOpponentsIntent(mGoogleApiClient, 1, 2, true);
        startActivityForResult(i, RC_SELECT_PLAYERS);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_WAITING_ROOM :

                if (resultCode == Activity.RESULT_OK) {
                    mRoom = data.getExtras().getParcelable(Multiplayer.EXTRA_ROOM);
                    mMyId = mRoom.getParticipantId(Games.Players.getCurrentPlayerId(mGoogleApiClient));

                    startGame();
                } else {

                    Games.RealTimeMultiplayer.leave(mGoogleApiClient, this, mRoomId);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    Button b = (Button) findViewById(R.id.play);
                    b.setVisibility(View.VISIBLE);
                }
                break;
            case RC_SELECT_PLAYERS:
                if (resultCode != Activity.RESULT_OK) {
                    // user canceled
                    return;
                }

                // get the invitee list
                Bundle extras = data.getExtras();
                final ArrayList<String> invitees =
                        data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);

                // get auto-match criteria
                Bundle autoMatchCriteria = null;
                int minAutoMatchPlayers =
                        data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
                int maxAutoMatchPlayers =
                        data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);

                if (minAutoMatchPlayers > 0) {
                    autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
                            minAutoMatchPlayers, maxAutoMatchPlayers, 0);
                } else {
                    autoMatchCriteria = null;
                }

                // create the room and specify a variant if appropriate
                RoomConfig.Builder roomConfigBuilder = makeBasicRoomConfigBuilder();
                roomConfigBuilder.addPlayersToInvite(invitees);
                if (autoMatchCriteria != null) {
                    roomConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
                }
                RoomConfig roomConfig = roomConfigBuilder.build();
                Games.RealTimeMultiplayer.create(mGoogleApiClient, roomConfig);

                // prevent screen from sleeping during handshake
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                break;
            case RC_INVITATION_INBOX:
                if (resultCode != Activity.RESULT_OK) {
                    // canceled
                    return;
                }

                // get the selected invitation
                extras = data.getExtras();
                Invitation invitation =
                        extras.getParcelable(Multiplayer.EXTRA_INVITATION);

                // accept it!
                roomConfig = makeBasicRoomConfigBuilder()
                        .setInvitationIdToAccept(invitation.getInvitationId())
                        .build();
                Games.RealTimeMultiplayer.join(mGoogleApiClient, roomConfig);

                // prevent screen from sleeping during handshake
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                break;
            default:
                Log.d(TAG, "default activity");
                break;
        }
    }
}
