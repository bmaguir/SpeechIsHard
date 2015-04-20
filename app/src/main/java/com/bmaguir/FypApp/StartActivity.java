package com.bmaguir.FypApp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.unity3d.player.UnityPlayer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class StartActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        RoomUpdateListener,
        RealTimeMessageReceivedListener,
        RoomStatusUpdateListener
{
    private static final String TAG = "debugStartActivity";
    private UnityPlayer m_UnityPlayer;
    SpeechRecognizer mSpeechRecognizer;
    Intent mSpeechRecognizerIntent;
    boolean mIsListening;
    public static Context context;
    private GoogleApiClient mGoogleApiClient;
    private String mRoomId;
    private static final int FINISH_GAME_ACTIVITY = 1278;
    private static final int REQUEST_LEADERBOARD = 1846;
    private boolean replay = false;


    private String mPlayerType;

    private int[] MapInfo;

    //room identifier
    final static int RC_WAITING_ROOM = 10002;

    int MAX_PLAYERS = 1;

    private Room mRoom;
    private String mMyId;
    private boolean startGameBool;
    private PopupWindow mPopupWindow;

    int xCo, zCo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        getWindow().setFormat(PixelFormat.RGB_565);
        setContentView(R.layout.start_activity);

        unityInit();

        //Speech recognition code
        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());
        SpeechRecognitionListener listener = new SpeechRecognitionListener();
        mSpeechRecognizer.setRecognitionListener(listener);

        mPlayerType = "player2";
        startGameBool = false;
        replayGame = 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //m_UnityPlayer.resume();
    }

    void unityInit(){
        if(m_UnityPlayer != null){
            m_UnityPlayer.quit();
        }
        m_UnityPlayer = new UnityPlayer(this);
        int glesMode = m_UnityPlayer.getSettings().getInt("gles_mode", 1);
        boolean trueColor8888 = false;
        m_UnityPlayer.init(glesMode, trueColor8888);


        // Add the Unity view
        FrameLayout layout = (FrameLayout) findViewById(R.id.frameLayout);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        layout.addView(m_UnityPlayer, 0, lp);
    }

    // Quit Unity
    @Override protected void onDestroy ()
    {
        if(mGoogleApiClient.isConnected()){
            Games.RealTimeMultiplayer.leave(mGoogleApiClient, this, mRoomId);
        }
        m_UnityPlayer.quit();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menuSignIn:
                MediaPlayer mp;
                mp = MediaPlayer.create(context, R.raw.blop);
                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.reset();
                        mp.release();
                    }

                });
                mp.start();
                return true;
            case R.id.menuSignOut:
                if(mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.disconnect();
                }
                setResult(1);
                finish();
                return true;
            case R.id.menuPlay:
                setMapInfo();
                mPlayerType = "player1";
                m_UnityPlayer.resume();
                return true;
            case R.id.debug:
                //startFinishGameActivity();
                replayGame = 1;
                return true;
            default:
                return false;
        }
    }

    //function called by Unity gamemanager
    public String startFunc(){
        //Log.i("test","-----------------test");
        return mPlayerType;
    }

    //sets random values for the map, textures and key loc etc...
    private void setMapInfo(){
        int size = 5;
        int materialsLength = 4;
        int statuesLength = 4;
        Random rand = new Random();
        int[] mapIndex = new int [size*size*2+2];
        for(int i=0;i<size*size;i++){
            mapIndex[i] =  rand.nextInt(materialsLength);
        }
        for(int i=size*size;i<size*size*2;i++){
            mapIndex[i] =  rand.nextInt(statuesLength);
        }
        mapIndex[size*size*2] = rand.nextInt(size);
        mapIndex[size*size*2+1] = rand.nextInt(size);

/*
        //debug
        for(int i=0;i<mapIndex.length; i++){
            mapIndex[i] = 3;
        }
*/
        ByteBuffer byteBuffer = ByteBuffer.allocate(mapIndex.length * 4);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(mapIndex);
        byte[] message = byteBuffer.array();
        byte[] header = Arrays.copyOf(message, message.length + 1);
        header[message.length] = 1;


        if(mRoom != null) { //if connected to a room

            for (Participant p : mRoom.getParticipants()) {
                if (!p.getParticipantId().equals(mMyId)) {
                    Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, header,
                            mRoomId, p.getParticipantId());
                    Log.d(TAG, "sent mapInfo");
                }
            }
        }

        MapInfo = mapIndex;
    }


    //Unity called Functions

    int replayGame;
    public int getReplayGame(){
        //Log.d(TAG, "getReplayGame");
        return replayGame;
}

    public boolean getStartGame(){
        if(startGameBool){
            startGameBool = false;
            return true;
        }
        else{
            return false;
        }
    }

    //transfers mapinfo to unity code
    public int[] getMapInfo(){


        return MapInfo;
    }

    public void setPlayerCo(int x, int z){

        byte[] x_bytes =  ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(x).array();
        byte[] z_bytes =  ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(z).array();

        byte[] message = new byte[9];

        for(int i=0;i<4;i++){
            message[i] = x_bytes[i];
        }
        for(int i=4;i<8;i++){
            message[i] = z_bytes[i-4];
        }
        message[8] = 2;

        if(mRoom != null) { //if connected to a room
            Games.RealTimeMultiplayer.sendUnreliableMessageToOthers(mGoogleApiClient, message, mRoomId);
        }
    }

    public int[] getPlayerCo(){
        int [] cor = {xCo, zCo};
        return cor;
    }

    int Score;

    public void finishGame(int score) {
        Score = score;
        this.runOnUiThread(new Runnable()
        {
            public void run()
            {
                byte[] message = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(Score).array();
                byte[] header = Arrays.copyOf(message, message.length + 1);
                header[message.length] = 4;

                if (mRoom != null) { //if connected to a room
                    for (Participant p : mRoom.getParticipants()) {
                        if (!p.getParticipantId().equals(mMyId)) {
                            Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, header,
                                    mRoomId, p.getParticipantId());
                            Log.d(TAG, "sent end game");
                        }
                    }
                }
                Log.d(TAG, "debug 1");
                Games.Leaderboards.submitScore(mGoogleApiClient, getString(R.string.LEADERBOARD_ID), Score);
                Toast.makeText(context, "Level Completed in " + Integer.toString(Score) + " seconds", Toast.LENGTH_LONG).show();
                Log.d(TAG, "debug 2");
                m_UnityPlayer.pause();
                Log.d(TAG, "debug 3");
                startFinishGameActivity();
            }
        });
    }


    void startFinishGameActivity(){
        Log.d(TAG, "debug 4");
        m_UnityPlayer.pause();
        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPopupWindow = new PopupWindow(
                inflater.inflate(R.layout.finish_game_layout, null, false),
                300,
                300,
                true);
        Log.d(TAG, "debug 5");
        mPopupWindow.showAtLocation(this.findViewById(R.id.frameLayout), Gravity.CENTER, 0, 0);
    }

    public void playAgain(View v){
        Log.d(TAG, "replaying match");
        mPopupWindow.dismiss();
        if(mPlayerType.compareTo("player2") == 0){
            mPlayerType = "player1";
            setMapInfo();
            startGameNow();
        }
        else{
            mPlayerType = "player2";
        }
    }

    public void viewLeaderBoard(View v){
        if(mPlayerType.compareTo("player1") == 0) {
            startActivityForResult(Games.Leaderboards.getLeaderboardIntent(mGoogleApiClient,
                    getString(R.string.LEADERBOARD_ID)), REQUEST_LEADERBOARD);
        }
        else{
            startActivityForResult(Games.Leaderboards.getLeaderboardIntent(mGoogleApiClient,
                    getString(R.string.LEADERBOARD_GUIDE_ID)), REQUEST_LEADERBOARD);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        m_UnityPlayer.windowFocusChanged(hasFocus);
    }


    public void speakClick(View v){
        if (!mIsListening)
        {
            Button b = (Button) findViewById(R.id.speak);
            b.setText("One Moment");
            mIsListening = true;
            mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
        }
        else{
            Button b = (Button) findViewById(R.id.speak);
            b.setText("Press to Speak");
            mSpeechRecognizer.stopListening();
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
        mGoogleApiClient.connect();
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

    public void playClick(View v){

        Log.d(TAG, "starting game");


        // auto-match criteria to invite one random automatch opponent.
        // You can also specify more opponents (up to 3).
        Bundle am = RoomConfig.createAutoMatchCriteria(1, 1, 0);

        // build the room config:
        RoomConfig.Builder roomConfigBuilder = makeBasicRoomConfigBuilder();
        roomConfigBuilder.setAutoMatchCriteria(am);
        RoomConfig roomConfig = roomConfigBuilder.build();

        // create room:
        Games.RealTimeMultiplayer.create(mGoogleApiClient, roomConfig);

        // prevent screen from sleeping during handshake
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //remove play button
        Button b = (Button) findViewById(R.id.play);
        b.setVisibility(View.GONE);
        // go to game screen
    }

    private RoomConfig.Builder makeBasicRoomConfigBuilder() {
        RoomConfig.Builder builder = RoomConfig.builder(this);
        builder.setMessageReceivedListener(this);
        builder.setRoomStatusUpdateListener(this);
        builder.setMessageReceivedListener(this);

        return builder;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Button b = (Button) findViewById(R.id.play);
        b.setVisibility(View.VISIBLE);
    }


    @Override
    public void onConnectionSuspended(int i) {
        if (!mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "Connection Failure",Toast.LENGTH_SHORT).show();
        setResult(2);
        finish();
    }

    @Override
    public void onBackPressed(){
        new AlertDialog.Builder(this)
                .setTitle("Really Exit?")
                .setMessage("Are you sure you want to exit?")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface arg0, int arg1) {
                        setResult(0);
                        finish();
                    }
                }).create().show();
    }

    @Override
    public void onRealTimeMessageReceived(RealTimeMessage realTimeMessage) {

        byte[] array = realTimeMessage.getMessageData();
        byte [] message = Arrays.copyOf(array, array.length -1);
        byte header = array[array.length-1];

        switch(header){
            case(1): //map info
                    IntBuffer intBuf =
                            ByteBuffer.wrap(message)
                                    .order(ByteOrder.BIG_ENDIAN)
                                    .asIntBuffer();
                    int[] temp = new int[intBuf.remaining()];
                    intBuf.get(temp);

                    for (int i = 0; i < temp.length; i++) {
                        // temp[i] = array[i];
                        //Log.d(TAG, Integer.toString(temp[i]));
                    }
                    MapInfo = temp;
                    //start game;
                    Log.d(TAG, "recieved message, starting game");
                    startGameNow();
                break;
            case(2):    //player position
                intBuf =
                        ByteBuffer.wrap(message)
                                .order(ByteOrder.BIG_ENDIAN)
                                .asIntBuffer();
                temp = new int[intBuf.remaining()];
                intBuf.get(temp);

                xCo = temp[0];
                zCo = temp[1];
                break;
            case(3):    //speech message
                TextView textView = (TextView)findViewById(R.id.textView);
                String cur = (String)textView.getText();
                textView.setText("Them: " + new String(message) +"\n" +cur);

                MediaPlayer mp;
                mp = MediaPlayer.create(context, R.raw.blop);
                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                    @Override
                    public void onCompletion(MediaPlayer mp) {

                        mp.reset();
                        mp.release();
                        mp=null;
                    }

                });
                mp.start();

                break;
            case(4):    //finished game
                intBuf = ByteBuffer.wrap(message)
                                .order(ByteOrder.BIG_ENDIAN)
                                .asIntBuffer();
                temp = new int[intBuf.remaining()];
                intBuf.get(temp);

                int score = temp[0];
                //m_UnityPlayer.pause();
                Toast.makeText(this, "Level Completed in " + Integer.toString(score) + " seconds", Toast.LENGTH_LONG).show();
                Games.Leaderboards.submitScore(mGoogleApiClient, getString(R.string.LEADERBOARD_GUIDE_ID), Score);
                //pushes the player into the end section
                //xCo = xCo + 100;
                zCo = zCo + 100;
                startFinishGameActivity();
                break;
        }
    }

    @Override
    public void onRoomConnecting(Room room) {

    }

    @Override
    public void onRoomAutoMatching(Room room) {

    }

    @Override
    public void onPeerInvitedToRoom(Room room, List<String> strings) {

    }

    @Override
    public void onPeerDeclined(Room room, List<String> strings) {

    }

    @Override
    public void onPeerJoined(Room room, List<String> strings) {

    }

    @Override
    public void onPeerLeft(Room room, List<String> strings) {
        Log.d(TAG, "Partner's left the room");
        m_UnityPlayer.pause();

        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPopupWindow = new PopupWindow(
                inflater.inflate(R.layout.lost_connection_layout, null, false),
                300,
                300,
                true);
        mPopupWindow.showAtLocation(this.findViewById(R.id.frameLayout), Gravity.CENTER, 0, 0);
    }

    public void returnToMenu(View v){
        setResult(3);
        finish();
    }

    public void keepPlaying(View v){
        mPopupWindow.dismiss();
        m_UnityPlayer.resume();
    }

    @Override
    public void onConnectedToRoom(Room room) {
        Log.d(TAG, "room disconnected");
    }

    @Override
    public void onDisconnectedFromRoom(Room room) {

    }

    @Override
    public void onPeersConnected(Room room, List<String> strings) {

    }

    @Override
    public void onPeersDisconnected(Room room, List<String> strings) {

    }

    @Override
    public void onP2PConnected(String s) {

    }

    @Override
    public void onP2PDisconnected(String s) {
        Log.d(TAG, "peer disconnected from room");
    }

    @Override
    public void onRoomCreated(int statusCode, Room room) {
        Log.d(TAG, "room created");
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            // display error
            Log.d(TAG, "status code " +statusCode);
            return;
        }

        mRoomId = room.getRoomId();

        // get waiting room intent
        Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(mGoogleApiClient, room, MAX_PLAYERS);
        startActivityForResult(i, RC_WAITING_ROOM);
    }

    @Override
    public void onJoinedRoom(int statusCode, Room room) {
        if (statusCode != GamesStatusCodes.STATUS_OK) {
            // display error
            return;
        }

        mRoomId = room.getRoomId();

        mRoom = room;

        // get waiting room intent
        Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(mGoogleApiClient, room, MAX_PLAYERS);
        startActivityForResult(i, RC_WAITING_ROOM);
    }

    @Override
    public void onLeftRoom(int i, String s) {
        Log.d(TAG, "left Room");
    }

    @Override
    public void onRoomConnected(int statusCode, Room room) {

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
            default:
                    Log.d(TAG, "default activity");
                break;
            }
        }

    private void startGameNow(){
        startGameBool = true;
        m_UnityPlayer.resume();
    }

    private void startGame() {

        //finds the id with the highest alpha numerical value, sets them as player 1
        ArrayList<String> p = mRoom.getParticipantIds();
        for(int i=0; i<p.size(); i++){
            if( p.get(i).compareTo(mMyId) < 0)
            {
                mPlayerType = "player1";
                Log.d(TAG, "player 1 chosen, myId = " + mMyId + " otherId = " + p.get(i));
                Toast.makeText(this, "player1", Toast.LENGTH_LONG).show();
                setMapInfo();
                startGameNow();
            }
            else if(p.get(i).compareTo(mMyId) != 0)
            {
                mPlayerType = "player2";
                Log.d(TAG, "player 2 chosen, myId = " + mMyId + " otherId = " + p.get(i));
                Toast.makeText(this, "player2", Toast.LENGTH_LONG).show();
            }
        }
    }

    protected class SpeechRecognitionListener implements RecognitionListener
    {
        @Override
        public void onBeginningOfSpeech()
        {
            Log.d(TAG, "onBeginingOfSpeech");
        }

        @Override
        public void onBufferReceived(byte[] buffer)
        {

        }

        @Override
        public void onEndOfSpeech()
        {
            Log.d(TAG, "onEndOfSpeech");
        }

        @Override
        public void onError(int error)
        {
            // mSpeechRecognizer.startListening(mSpeechRecognizerIntent);

            Log.d(TAG, "error = " + error);
        }

        @Override
        public void onEvent(int eventType, Bundle params)
        {

        }

        @Override
        public void onPartialResults(Bundle partialResults)
        {

        }

        @Override
        public void onReadyForSpeech(Bundle params)
        {
            Log.d("User_Tag", "onReadyForSpeech"); //$NON-NLS-1$
            Toast.makeText(getApplicationContext(), "SPEAK", Toast.LENGTH_LONG).show();
            Button b = (Button) findViewById(R.id.speak);
            b.setText("Send");
        }

        @Override
        public void onResults(Bundle results)
        {
            mIsListening = false;

            Button b = (Button) findViewById(R.id.speak);
            b.setText("Press to Speak");

            Log.d(TAG, "onResults"); //$NON-NLS-1$
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            String best = matches.get(0);
            //Toast.makeText(getApplicationContext(),"Results = " + best, Toast.LENGTH_LONG).show();

            TextView textView = (TextView)findViewById(R.id.textView);
            String cur = (String)textView.getText();
            textView.setText("You: " + best +"\n" +cur);

            byte[] message = best.getBytes();
            byte[] header = Arrays.copyOf(message, message.length + 1);
            header[message.length] = 3;


            if(mRoom != null) { //if connected to a room

                for (Participant p : mRoom.getParticipants()) {
                    if (!p.getParticipantId().equals(mMyId)) {
                        Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, header,
                                mRoomId, p.getParticipantId());
                        Log.d(TAG, "sent message");
                    }
                }
            }
        }

        @Override
        public void onRmsChanged(float rmsdB)
        {

        }

    }



}