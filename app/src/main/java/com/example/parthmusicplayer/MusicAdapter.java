package com.example.parthmusicplayer;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MyViewHolder> {

    private Context mContext;
    static ArrayList<MusicFiles> mFiles;

    MusicAdapter(Context mContext, ArrayList<MusicFiles> mFiles){

        this.mFiles = mFiles;
        this.mContext = mContext;

    }


    @NonNull
    @Override
    public MusicAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(mContext).inflate(R.layout.music_items, parent, false);

        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicAdapter.MyViewHolder holder, @SuppressLint("RecyclerView") final int position) {

        holder.file_name.setText(mFiles.get(position).getTitle());
        try {
            byte[] image = getAlbumArt(mFiles.get(position).getPath());
            if(image != null){

                Glide.with(mContext).asBitmap()
                        .load(image)
                        .into(holder.album_art);

            }
            else {
                Glide.with(mContext)
                        .load(R.drawable.defaultmusicimage)
                        .into(holder.album_art);
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Intent intent = new Intent(mContext, PlayerActivity.class);
                    intent.putExtra("position", position);  // when we tap on any song then we must pass the uri of that song to player activity. so when we pass intent then we also send postion of that song.
                    mContext.startActivity(intent);

                }
            });

            holder.menuMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    PopupMenu popupMenu = new PopupMenu(mContext, v);

                    popupMenu.getMenuInflater().inflate(R.menu.popup, popupMenu.getMenu());
                    popupMenu.show();

                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @SuppressLint("NonConstantResourceId")
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {

                            if (item.getItemId() == R.id.delete) {
//                                Toast.makeText(mContext, "Delete Clicked!!", Toast.LENGTH_SHORT).show();
                                deleteFile(position, v);
                            }
                            return true;
                        }
                    });

                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void deleteFile(final int position, View v) {
        //use to delete file from mediastore -  content://

        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                Long.parseLong(mFiles.get(position).getId()));


        File file = new File(mFiles.get(position).getPath());
        boolean deleted = file.delete();

        if(deleted){
            mContext.getContentResolver().delete(contentUri, null, null);
            mFiles.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, mFiles.size());
            Snackbar.make(v, "File Deleted : ",Snackbar.LENGTH_SHORT).show();
        }else {
            Snackbar.make(v, "Can't be Deleted : ",Snackbar.LENGTH_SHORT).show();

        }
    }

    @Override
    public int getItemCount() {
        return mFiles.size();
    }
    public static class MyViewHolder extends RecyclerView.ViewHolder{

        TextView file_name;
        ImageView album_art, menuMore;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            file_name = itemView.findViewById(R.id.musicFileName);
            album_art = itemView.findViewById(R.id.musicImg);
            menuMore = itemView.findViewById(R.id.menuMore);

        }
    }

    private byte[] getAlbumArt(String uri) throws IOException {

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri);
        byte[] art = retriever.getEmbeddedPicture();
        retriever.release();

        return art;
    }

    @SuppressLint("NotifyDataSetChanged")
    void updateList(ArrayList<MusicFiles> musicFilesArrayList){

        mFiles = new ArrayList<>();
        mFiles.addAll(musicFilesArrayList);
        notifyDataSetChanged();
    }
}
