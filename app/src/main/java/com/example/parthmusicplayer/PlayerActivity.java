package com.example.parthmusicplayer;

import static com.example.parthmusicplayer.AlbumDetailsAdapter.albumFiles;
import static com.example.parthmusicplayer.ApplicationClass.ACTION_NEXT;
import static com.example.parthmusicplayer.ApplicationClass.ACTION_PLAY;
import static com.example.parthmusicplayer.ApplicationClass.ACTION_PREVIOUS;
import static com.example.parthmusicplayer.ApplicationClass.CHANNEL_ID_2;
import static com.example.parthmusicplayer.MainActivity.musicFiles;
import static com.example.parthmusicplayer.MainActivity.repeatBoolean;
import static com.example.parthmusicplayer.MainActivity.shuffleBoolean;
import static com.example.parthmusicplayer.MusicAdapter.mFiles;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class PlayerActivity extends AppCompatActivity
        implements ActionPlaying, ServiceConnection {

    TextView song_name, artist_name, duration_played, duration_total;
    ImageView cover_art, nextBtn, prevBtn,backBtn, shuffleBtn, repeatBtn;
    FloatingActionButton playPauseBtn;
    SeekBar seekBar;
    static int position = -1;

    private Handler handler = new Handler();

    static ArrayList<MusicFiles> listSongs = new ArrayList<>();

    static Uri uri;

//    static MediaPlayer mediaPlayer;

    private Thread playThread, prevThread, nextThread;

    MusicService musicService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setFulScreen();
        setContentView(R.layout.activity_player);
        getSupportActionBar().hide();

        initViews();

        getIntentMethod();


        //when we tap on seekbar this method call
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(musicService != null && fromUser){
                    musicService.seekTo(progress * 1000); //when we touch on seekbar then progress get that second position and seekTo method play that song from particular time
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        PlayerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(musicService != null){

                    int mCurrentPosition = musicService.getCurrentPosition() /1000; //for in seconds
                    seekBar.setProgress(mCurrentPosition);
                    duration_played.setText(formattedTime(mCurrentPosition));

                }
                handler.postDelayed(this,1000);
            }
        });

        //For shuffle Button
        shuffleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(shuffleBoolean){

                    shuffleBoolean = false;
                    shuffleBtn.setImageResource(R.drawable.baseline_shuffle_off);

                }else {

                    shuffleBoolean = true;
                    shuffleBtn.setImageResource(R.drawable.baseline_shuffle_on);
                }
            }
        });

        //For Repeat Button
        repeatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(repeatBoolean){

                    repeatBoolean = false;
                    repeatBtn.setImageResource(R.drawable.baseline_repeat_off);
                }else {

                    repeatBoolean = true;
                    repeatBtn.setImageResource(R.drawable.baseline_repeat_on);
                }
            }
        });
    }

    private void setFulScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    protected void onResume() {
        /* onResume() is called whenever you navigate back to the activity from a call or something else.
            Just as an spoiler, onCreate() gets called first, then if you paused the activity by either going to home screen
            or by launching another activity, onPause() gets called. If the OS destroys the activity in the meantime, onDestroy() gets called.
            If you resume the app and the app already got destroyed, onCreate() will get called, or else onResume() will get called.*/

        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, this, BIND_AUTO_CREATE);

        playThreadBtn();
        nextThreadBtn();
        prevThreadBtn();

        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(this);
    }

    private void prevThreadBtn() {
        prevThread = new Thread(){
            @Override
            public void run() {
                super.run();
                prevBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        prevBtnClicked();
                    }
                });
            }
        };
        prevThread.start();
    }

    public void prevBtnClicked() {
        if(musicService.isPlaying()){

            musicService.stop();
            musicService.release();

            if(shuffleBoolean && !repeatBoolean){
                position = getRandom(listSongs.size() - 1);
            }
            else if(!shuffleBoolean && !repeatBoolean){
                position = (position - 1) < 0 ? (listSongs.size() - 1) : (position - 1);
            }
            else {
                //now repeat button is on so we can't change position
            }

            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());

            //Now Set seekbar
            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(musicService != null){
                        int mCurrentPosition = musicService.getCurrentPosition()/1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
            musicService.OnCompleted();

            musicService.showNotification(R.drawable.baseline_pause_24);

            playPauseBtn.setBackgroundResource(R.drawable.baseline_pause_24);
            musicService.start();
        }
        else {
            musicService.stop();
            musicService.release();

            if(shuffleBoolean && !repeatBoolean){
                position = getRandom(listSongs.size() - 1);
            }
            else if(!shuffleBoolean && !repeatBoolean){
                position = (position - 1) < 0 ? (listSongs.size() - 1) : (position - 1);
            }
            else {
                //now repeat button is on so we can't change position
            }

            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());

            //Now set seekbar

            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if( musicService != null){
                        int mCurrentPosition = musicService.getCurrentPosition()/1000;

                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
            musicService.OnCompleted();

            musicService.showNotification(R.drawable.baseline_pause_24);

            playPauseBtn.setImageResource(R.drawable.baseline_pause_24);
            musicService.start();
        }
    }

    private void nextThreadBtn() {
        nextThread = new Thread(){

            @Override
            public void run() {
                super.run();

                nextBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        nextBtnClicked();
                    }
                });
            }
        };
        nextThread.start();
    }

    public void nextBtnClicked() {
        if(musicService.isPlaying()){

            musicService.stop();
            musicService.release();

            if(shuffleBoolean && !repeatBoolean){
                position = getRandom(listSongs.size() - 1);
            }
            else if(!shuffleBoolean && !repeatBoolean){
//            position = ((position + 1) % listSongs.size());
                position = (position + 1) < listSongs.size() ? position + 1 : 0;
            }
            else {
                //now repeat button is on so we can't change position
            }

            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());

            //Now set seekbar

            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if( musicService != null){
                        int mCurrentPosition = musicService.getCurrentPosition()/1000;

                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
            musicService.OnCompleted();

            musicService.showNotification(R.drawable.baseline_pause_24);
            playPauseBtn.setBackgroundResource(R.drawable.baseline_pause_24);
            musicService.start();
        }
        else {

            musicService.stop();
            musicService.release();

            if(shuffleBoolean && !repeatBoolean){
                position = getRandom(listSongs.size() - 1);
            }
            else if(!shuffleBoolean && !repeatBoolean){
//            position = ((position + 1) % listSongs.size());
                position = (position + 1) < listSongs.size() ? position + 1 : 0;
            }
            else {
                //now repeat button is on so we can't change position
            }

            uri = Uri.parse(listSongs.get(position).getPath());
            musicService.createMediaPlayer(position);
            metaData(uri);
            song_name.setText(listSongs.get(position).getTitle());
            artist_name.setText(listSongs.get(position).getArtist());

            //Now set seekbar

            seekBar.setMax(musicService.getDuration() / 1000);
            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if( musicService != null){
                        int mCurrentPosition = musicService.getCurrentPosition()/1000;

                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
            musicService.OnCompleted();

            musicService.showNotification(R.drawable.baseline_pause_24);

            playPauseBtn.setImageResource(R.drawable.baseline_pause_24);
            musicService.start();
        }
    }

    private int getRandom(int i) {

        Random random = new Random();
        return random.nextInt(i+1);
    }

    private void playThreadBtn() {
        playThread = new Thread(){
            @Override
            public void run() {
                super.run();

                playPauseBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playPauseBtnClicked();
                    }
                });

            }
        };
        playThread.start();
    }

    public void playPauseBtnClicked() {

        if(musicService.isPlaying()){

            playPauseBtn.setImageResource(R.drawable.baseline_play_arrow_24);
            musicService.pause();

            musicService.showNotification(R.drawable.baseline_play_arrow_24);

            seekBar.setMax(musicService.getDuration() / 1000);

            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(musicService != null){
                        int mCurrentPosition = musicService.getCurrentPosition() /1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
        }
        else {
            musicService.showNotification(R.drawable.baseline_pause_24);

            playPauseBtn.setImageResource(R.drawable.baseline_pause_24);
            musicService.start();

            seekBar.setMax(musicService.getDuration() / 1000);

            PlayerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(musicService != null){
                        int mCurrentPosition = musicService.getCurrentPosition() /1000;
                        seekBar.setProgress(mCurrentPosition);
                    }
                    handler.postDelayed(this, 1000);
                }
            });
        }
    }

    private String formattedTime(int mCurrentPosition) {

        String totalOut = "";
        String totalNew = "";
        String seconds = String.valueOf(mCurrentPosition % 60);  //if 01:10 then seconds= 70%60= 10 and minutes = 70/60 = 1
        String minutes = String.valueOf(mCurrentPosition / 60);
        totalOut = minutes + ":" + seconds;
        totalNew = minutes + ":" + "0" + seconds;

        if (seconds.length() == 1){
            return totalNew;
        }
        else {
            return totalOut;
        }
    }

    private void getIntentMethod() {

        //for access the position of song which is pass by intent
        position = getIntent().getIntExtra("position", -1);
        String sender = getIntent().getStringExtra("sender");


        if(sender != null && sender.equals("albumDetails")){

            listSongs = albumFiles;
        } else {

            listSongs = mFiles;
        }

        if(listSongs != null){

            playPauseBtn.setImageResource(R.drawable.baseline_pause_24);
            uri = Uri.parse(listSongs.get(position).getPath());

        }


//
//        if(musicService != null){
//
//            musicService.stop();
//            musicService.release();
//
//            musicService.createMediaPlayer(position);
//            musicService.start();
//        }
//        else {
//            musicService.createMediaPlayer(position);
//            musicService.start();
//        }

        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra("servicePosition", position);
        startService(intent);


    }

    private void initViews() {
        song_name = findViewById(R.id.song_name);
        artist_name = findViewById(R.id.song_artist);
        duration_played = findViewById(R.id.durationPlayed);
        duration_total = findViewById(R.id.durationTotal);
        cover_art = findViewById(R.id.cover_art);
        nextBtn = findViewById(R.id.id_next);
        prevBtn = findViewById(R.id.id_prev);
        backBtn = findViewById(R.id.back_btn);
        shuffleBtn = findViewById(R.id.id_shuffle);
        repeatBtn = findViewById(R.id.id_repeat);
        playPauseBtn = findViewById(R.id.play_pause);
        seekBar = findViewById(R.id.seekBar);
    }

    private void metaData(Uri uri){

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri.toString());

        int durationTotal = Integer.parseInt(listSongs.get(position).getDuration()) / 1000;
        duration_total.setText(formattedTime(durationTotal));

        byte[] art = retriever.getEmbeddedPicture();

        Bitmap bitmap;

        if(art != null){

            bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);

            imageAnimation(this, cover_art, bitmap);

            Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(@Nullable Palette palette) {
                    Palette.Swatch swatch = palette.getDominantSwatch();

                    if (swatch != null){
                        ImageView gradient = findViewById(R.id.imageViewGradient);
                        RelativeLayout mContainer = findViewById(R.id.mContainer);

                        gradient.setBackgroundResource(R.drawable.gradient_bg);
                        mContainer.setBackgroundResource(R.drawable.main_bg);

                        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                                new int[]{swatch.getRgb(), 0x00000000});
                        gradient.setBackground(gradientDrawable);

                        GradientDrawable gradientDrawableBg = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                                new int[]{swatch.getRgb(), swatch.getRgb()});
                        mContainer.setBackground(gradientDrawableBg);

                        song_name.setTextColor(swatch.getTitleTextColor());
                        artist_name.setTextColor(swatch.getBodyTextColor());
                    }
                    else {
                        ImageView gradient = findViewById(R.id.imageViewGradient);
                        RelativeLayout mContainer = findViewById(R.id.mContainer);

                        gradient.setBackgroundResource(R.drawable.gradient_bg);
                        mContainer.setBackgroundResource(R.drawable.main_bg);

                        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                                new int[]{0xff000000, 0x00000000});
                        gradient.setBackground(gradientDrawable);

                        GradientDrawable gradientDrawableBg = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                                new int[]{0xff000000, 0xff000000});
                        mContainer.setBackground(gradientDrawableBg);

                        song_name.setTextColor(Color.WHITE);
                        artist_name.setTextColor(Color.DKGRAY);
                    }
                }
            });
        } else {
            Glide.with(this)
                    .asBitmap()
                    .load(R.drawable.defaultmusicimage)
                    .into(cover_art);

            ImageView gradient = findViewById(R.id.imageViewGradient);
            RelativeLayout mContainer = findViewById(R.id.mContainer);

            gradient.setBackgroundResource(R.drawable.gradient_bg);
            mContainer.setBackgroundResource(R.drawable.main_bg);

            song_name.setTextColor(Color.WHITE);
            artist_name.setTextColor(Color.DKGRAY);
        }
    }

    public void imageAnimation(final Context context,final ImageView imageView,final Bitmap bitmap){

        Animation animOut = AnimationUtils.loadAnimation(context, android.R.anim.fade_out);
        final Animation animIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);

        animOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

                Glide.with(context).load(bitmap).into(imageView);
                animIn.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                imageView.startAnimation(animIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        imageView.startAnimation(animOut);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicService.MyBinder myBinder = (MusicService.MyBinder) service;
        musicService = myBinder.getService();

        musicService.setCallBack(this);

        Toast.makeText(this, "Connected" + musicService, Toast.LENGTH_SHORT).show();

        seekBar.setMax(musicService.getDuration() / 1000);  // this method gave total duration of particular media in media player object
        metaData(uri);

        song_name.setText(listSongs.get(position).getTitle());
        artist_name.setText(listSongs.get(position).getArtist());

        musicService.OnCompleted();
        musicService.showNotification(R.drawable.baseline_pause_24);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        musicService = null;
    }

}
