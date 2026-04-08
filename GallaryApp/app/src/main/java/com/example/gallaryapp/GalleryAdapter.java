package com.example.gallaryapp;

import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder> {
    private static final String TAG = "GalleryAdapter";

    public interface OnImageClickListener {
        void onImageClick(Uri uri);
    }

    private final List<Uri> imageUris;
    private final OnImageClickListener onImageClickListener;

    public GalleryAdapter(List<Uri> imageUris, OnImageClickListener onImageClickListener) {
        this.imageUris = imageUris;
        this.onImageClickListener = onImageClickListener;
    }

    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gallery_placeholder, parent, false);
        return new GalleryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
        Uri uri = imageUris.get(position);
        Log.d(TAG, "Binding thumbnail URI: " + uri);
        Glide.with(holder.thumbnailImage.getContext())
                .load(uri)
                .centerCrop()
                .thumbnail(0.25f)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_dialog_alert)
                .into(holder.thumbnailImage);

        holder.itemView.setOnClickListener(v -> onImageClickListener.onImageClick(uri));
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    static class GalleryViewHolder extends RecyclerView.ViewHolder {
        final ImageView thumbnailImage;

        GalleryViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnailImage = itemView.findViewById(R.id.imgThumbnail);
        }
    }
}
