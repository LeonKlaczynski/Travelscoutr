package com.treinchauffeur.travelscoutr.ui;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.treinchauffeur.travelscoutr.Constants;
import com.treinchauffeur.travelscoutr.FavoritesActivity;
import com.treinchauffeur.travelscoutr.Logger;
import com.treinchauffeur.travelscoutr.MapsActivity;
import com.treinchauffeur.travelscoutr.R;
import com.treinchauffeur.travelscoutr.obj.ClusterMarker;

import java.util.Objects;

public class SpotDialog extends Dialog {

    public static final String TAG = "SpotDialog";
    protected Context context;
    protected ClusterMarker item;
    protected Activity activity;
    protected String url;
    protected boolean isFavorite;

    public SpotDialog(@NonNull Context context, Activity activity, ClusterMarker item) {
        super(context);
        assert item.getSnippet() != null;
        this.context = context;
        this.activity = activity;
        this.item = item;
        url = item.getSnippet().split("!!!")[0];
        isFavorite = item.getSnippet().contains(Constants.FAVE_STRING);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actions_dialog);
        Objects.requireNonNull(getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setButtonActions();
    }

    private void setButtonActions() {
        MaterialButton openBtn = findViewById(R.id.buttonShow);
        if (url.contains("flickr.com")) openBtn.setText(R.string.show_on_flickr_com);
        openBtn.setOnClickListener(v -> {
            dismiss();
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            context.startActivity(i);
        });

        MaterialButton directions = findViewById(R.id.buttonDirections);
        directions.setOnClickListener(v -> {
            String uri = "google.navigation:q=" + item.getPosition().latitude + "," + item.getPosition().longitude;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            activity.startActivity(intent);
            dismiss();
        });

        MaterialButton favoriteBtn = findViewById(R.id.buttonAddFavorite);
        if (activity instanceof FavoritesActivity) {
            FavoritesActivity favoritesActivity = (FavoritesActivity) activity;
            favoriteBtn.setText(R.string.remove_from_favourites);
            favoriteBtn.setOnClickListener(v -> {
                dismiss();
                assert item.getSnippet() != null;
                if (item.getSnippet().contains(Constants.FAVE_STRING) || favoritesActivity.favorites.contains(item)) {
                    favoritesActivity.removeFromFavorites(item);
                    Toast.makeText(context, "Removed from favourites!", Toast.LENGTH_SHORT).show();
                    favoritesActivity.refreshFavorites();
                    return;
                }
                favoritesActivity.refreshFavorites();
                favoritesActivity.saveFavorites();
            });
        } else if (activity instanceof MapsActivity) {
            if (isFavorite) {
                favoriteBtn.setEnabled(false);
                favoriteBtn.setText(R.string.already_added_to_favourites);
            }
            MapsActivity mapsActivity = (MapsActivity) activity;
            favoriteBtn.setOnClickListener(v -> {
                dismiss();
                if (mapsActivity.favoritesContains(item)) {
                    Toast.makeText(context, "Already added to favourites!", Toast.LENGTH_SHORT).show();
                    return;
                }

                item.setSnippet(item.getSnippet() + Constants.FAVE_STRING);
                mapsActivity.clusterManager.addItem(item);
                mapsActivity.favorites.add(item);
                Toast.makeText(context, "Added to favorites!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onClusterItemInfoWindowLongClick: adding " + item.getSnippet());
                mapsActivity.refreshFavorites();
                mapsActivity.saveFavorites();
            });
        }

        MaterialButton coordinates = findViewById(R.id.buttonCoordinates);
        coordinates.setOnClickListener(v -> {
            String coordinatesString = item.getPosition().latitude + ", " + item.getPosition().longitude;
            Logger.debug(TAG, "copied: "+coordinatesString);
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Spot coordinates", coordinatesString);
            clipboard.setPrimaryClip(clip);
            dismiss();
            Toast.makeText(context, "Coordinates copied to clipboard!", Toast.LENGTH_SHORT).show();
        });

        MaterialButton dismiss = findViewById(R.id.buttonCancel);
        dismiss.setOnClickListener(v -> dismiss());
    }
}
