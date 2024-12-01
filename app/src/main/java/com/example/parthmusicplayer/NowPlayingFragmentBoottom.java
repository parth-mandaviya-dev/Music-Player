package com.example.parthmusicplayer;

import static android.content.Context.MODE_PRIVATE;
import static com.example.parthmusicplayer.MainActivity.ARTIST_TO_FRAG;
import static com.example.parthmusicplayer.MainActivity.PATH_TO_FRAG;
import static com.example.parthmusicplayer.MainActivity.SHOW_MINI_PLAYER;
import static com.example.parthmusicplayer.MainActivity.SONG_NAME_TO_FRAG;
import static com.example.parthmusicplayer.MainActivity.musicFiles;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;

public class NowPlayingFragmentBoottom extends Fragment implements ServiceConnection {

    ImageView nextBtn, albumArt;
    TextView artist, songName;
    FloatingActionButton playPauseBtn;
    View view;
    MusicService musicService;


    public static final String MUSIC_LAST_PLAYED = "LAST_PLAYED";
    public static final String MUSIC_FILE = "STORED_MUSIC";
    public static final String ARTIST_NAME = "ARTIST NAME";
    public static final String SONG_NAME = "SONG NAME";

    public NowPlayingFragmentBoottom() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_now_playing_boottom, container, false);

        artist = view.findViewById(R.id.song_artist_miniplayer);
        songName = view.findViewById(R.id.song_name_miniplayer);
        albumArt = view.findViewById(R.id.bottom_album_art);
        nextBtn = view.findViewById(R.id.skip_next_bottom);
        playPauseBtn = view.findViewById(R.id.play_pause_miniplayer);

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(musicService != null){
                    musicService.nextBtnClicked();

                    if(getActivity() != null) {
                        SharedPreferences.Editor editor = getActivity().getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE).edit();
                        editor.putString(MUSIC_FILE, musicService.musicFiles.get(musicService.position).getPath());
                        editor.putString(ARTIST_NAME, musicService.musicFiles.get(musicService.position).getArtist());
                        editor.putString(SONG_NAME, musicService.musicFiles.get(musicService.position).getTitle());
                        editor.apply();

                        SharedPreferences preferencesc = getActivity().getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE);

                        String path = preferencesc.getString(MUSIC_FILE, null);
                        String artistName = preferencesc.getString(ARTIST_NAME, null);
                        String song_name = preferencesc.getString(SONG_NAME, null);

                        if(path != null){
                            SHOW_MINI_PLAYER = true;
                            PATH_TO_FRAG = path;
                            ARTIST_TO_FRAG = artistName;
                            SONG_NAME_TO_FRAG = song_name;
                        } else {
                            SHOW_MINI_PLAYER = false;
                            PATH_TO_FRAG = null;
                            ARTIST_TO_FRAG = null;
                            SONG_NAME_TO_FRAG = null;
                        }


                        if(SHOW_MINI_PLAYER){
                            if(PATH_TO_FRAG != null) {
                                byte[] art;
                                try {

                                    art = getAlbumArt(PATH_TO_FRAG);

                                    if(art != null) {
                                        Glide.with(getContext()).load(art)
                                                .into(albumArt);
                                    } else {
                                        Glide.with(getContext()).load(R.drawable.defaultmusicimage)
                                                .into(albumArt);
                                    }

                                    songName.setText(SONG_NAME_TO_FRAG);
                                    artist.setText(ARTIST_TO_FRAG);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                }
            }
        });

        playPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(musicService != null){
                    musicService.playPauseBtnClicked();

                    if(musicService.isPlaying()){
                        playPauseBtn.setImageResource(R.drawable.baseline_pause_24);
                    } else {
                        playPauseBtn.setImageResource(R.drawable.baseline_play_arrow_24);
                    }

                }
            }
        });

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(requireContext(), "MiniPlayer", Toast.LENGTH_LONG).show();
            }
        });



        return view;
    }



    @Override
    public void onResume() {
        super.onResume();
        if(SHOW_MINI_PLAYER){
            if(PATH_TO_FRAG != null) {
                byte[] art;
                try {

                    art = getAlbumArt(PATH_TO_FRAG);

                    if(art != null) {
                        Glide.with(getContext()).load(art)
                                .into(albumArt);
                    } else {
                        Glide.with(getContext()).load(R.drawable.defaultmusicimage)
                                .into(albumArt);
                    }

                    songName.setText(SONG_NAME_TO_FRAG);
                    artist.setText(ARTIST_TO_FRAG);

                    Intent intent = new Intent(getContext(), MusicService.class);

                    if(getContext() != null){
                        getContext().bindService(intent, this, Context.BIND_AUTO_CREATE);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

//        if(getContext() != null){
//            getContext().unbindService(this);
//        }
    }

    private byte[] getAlbumArt(String uri) throws IOException {

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri);

        byte[] art = retriever.getEmbeddedPicture();
        retriever.release();
        return art;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {

        MusicService.MyBinder binder = (MusicService.MyBinder) service;
        musicService = binder.getService();

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        musicService = null;
    }
}