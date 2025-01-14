package com.example.parthmusicplayer;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL;

public class AlbumDetailsAdapter extends RecyclerView.Adapter<AlbumDetailsAdapter.MyHolder> {

    private Context mContext;
    static ArrayList<MusicFiles> albumFiles;
    View view;

    public AlbumDetailsAdapter(Context mContext, ArrayList<MusicFiles> albumFiles) {
        this.mContext = mContext;
        this.albumFiles = albumFiles;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        view = LayoutInflater.from(mContext).inflate(R.layout.music_items, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumDetailsAdapter.MyHolder holder, int position) {

        holder.album_name.setText(albumFiles.get(position).getTitle());
        try {
            byte[] image = getAlbumArt(albumFiles.get(position).getPath());

            if(image != null){
                Glide.with(mContext).asBitmap()
                        .load(image)
                        .into(holder.album_image);
            } else {

                Glide.with(mContext).asBitmap()
                        .load(R.drawable.defaultmusicimage)
                        .into(holder.album_image);
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Intent intent = new Intent(mContext, PlayerActivity.class);
                    intent.putExtra("sender", "albumDetails");
                    intent.putExtra("position", position);
                    mContext.startActivity(intent);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public int getItemCount() {
        return albumFiles.size();
    }

    public class MyHolder extends RecyclerView.ViewHolder{
        ImageView album_image;
        TextView album_name;
        public MyHolder(@NonNull View itemView) {
            super(itemView);

            album_image = itemView.findViewById(R.id.musicImg);
            album_name = itemView.findViewById(R.id.musicFileName);
        }
    }

    private byte[] getAlbumArt(String uri) throws IOException {

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri);

        byte[] art = retriever.getEmbeddedPicture();
        retriever.release();
        return art;
    }
}
