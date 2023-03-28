package com.klaczynski.travelscoutr.ui;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.HttpException;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.klaczynski.travelscoutr.Constants;
import com.klaczynski.travelscoutr.R;

public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private static final String TAG = "InfoWindowAdapter";
    private final LayoutInflater mInflater;

    public CustomInfoWindowAdapter(LayoutInflater inflater) {
        this.mInflater = inflater;
    }

    @Override public View getInfoWindow(Marker marker) {
        final View popup = mInflater.inflate(R.layout.info_window_layout, null);

        ((TextView) popup.findViewById(R.id.title)).setText(marker.getTitle());

        if(marker.getSnippet().contains(Constants.FLICKR_STRING))
            ((TextView) popup.findViewById(R.id.clickToOpen)).setText("Click to open on flickr.com");

        ImageView pictureView = popup.findViewById(R.id.pictureView);
        String url = marker.getSnippet().split("!!!")[1] + "?h=300";
        Glide.with(popup).load(url).transform(new CenterCrop(),new RoundedCorners(25))
                .listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                // log exception
                Log.e("TAG", "Error loading image", e);
                if (e.getCauses().get(0) instanceof HttpException) {
                    Toast.makeText(popup.getContext(), "Error loading image, http error code: " + ((HttpException) e.getCause()).getStatusCode(), Toast.LENGTH_SHORT).show();
                    return false;
                } else {
                    Toast.makeText(popup.getContext(), "Error loading image, try re-opening marker.", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                if(!dataSource.equals(DataSource.MEMORY_CACHE)) {
                    marker.showInfoWindow();
                }
                return false;
            }
        }).into(pictureView);
        return popup;
    }

    @Override public View getInfoContents(Marker marker) {
        final View popup = mInflater.inflate(R.layout.info_window_layout, null);

        ((TextView) popup.findViewById(R.id.title)).setText(marker.getTitle());

        if(marker.getSnippet().contains(Constants.FLICKR_STRING))
            ((TextView) popup.findViewById(R.id.clickToOpen)).setText("Click to open on flickr.com");

        ImageView pictureView = popup.findViewById(R.id.pictureView);
        String url = marker.getSnippet().split("!!!")[1];
        Glide.with(popup).load(url).transform(new CenterCrop(),new RoundedCorners(25))
                .listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                // log exception
                Log.e("TAG", "Error loading image", e);
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                //if(!dataSource.equals(DataSource.MEMORY_CACHE)) marker.showInfoWindow(); //Can't because of infinite loop
                return false;
            }
        }).into(pictureView);
        return popup;
    }
}