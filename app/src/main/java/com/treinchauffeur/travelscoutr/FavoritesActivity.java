package com.treinchauffeur.travelscoutr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.NonHierarchicalViewBasedAlgorithm;
import com.treinchauffeur.travelscoutr.obj.ClusterMarker;
import com.treinchauffeur.travelscoutr.ui.CustomFavClusterRenderer;
import com.treinchauffeur.travelscoutr.ui.CustomInfoWindowAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private ClusterManager<ClusterMarker> clusterManager;
    private final DisplayMetrics metrics = new DisplayMetrics();
    public static final String TAG = "MapsActivity";
    @SuppressLint("StaticFieldLeak")
    public static Context context;
    MaterialToolbar toolbar;
    LocationManager lm;
    ArrayList<ClusterMarker> favorites = new ArrayList<>();

    NavigationBarView navBar;
    Menu navMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.activity_favorites);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.favoritesMap);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        //Setting up the googleMap ui options & map style (day/night, if applicable)
        setupMapUiAndStyle();

        //Location fab & enable my location if possible
        setupLocationAndPermissions();

        //Setting up the clusterManager
        setUpClusterer();

        //Load all favorites from sharedPrefs
        loadFavorites();

        setMenuOptions();

        goToCenterOfFavorites();
    }

    private void goToCenterOfFavorites() {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        if(favorites.size() > 0) {
            for (ClusterMarker marker : favorites) {
                builder.include(marker.getPosition());
            }

            LatLngBounds bounds = builder.build();
            int padding = 150;

            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            map.animateCamera(cu);
        } else if(map.isMyLocationEnabled()) {
            float zoomLevel = (map.getCameraPosition().zoom > 14) ? map.getCameraPosition().zoom : 14;
            if(map.getMyLocation() != null) {
                LatLng latLng = new LatLng(map.getMyLocation().getLatitude(), map.getMyLocation().getLongitude());
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel));
            }
        }
    }

    private void setMenuOptions() {
        toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        navBar.setSelectedItemId(R.id.favoritesNavItem);
        navMenu.findItem(R.id.homeNavItem).setOnMenuItemClickListener(item -> {
            onBackPressed();
            return false;
        });
    }

    @SuppressLint("MissingPermission")
    private void setupLocationAndPermissions() {
        FloatingActionButton locationFab = findViewById(R.id.locationFab);
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceVariant, typedValue, true);
        int colorOff = ContextCompat.getColor(this, typedValue.resourceId);
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimaryContainer, typedValue, true);
        int colorOn = ContextCompat.getColor(this, typedValue.resourceId);
        toolbar = findViewById(R.id.materialToolbar);
        navBar = findViewById(R.id.navigationRailView);
        navMenu = navBar.getMenu();


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
            locationFab.setBackgroundTintList(ColorStateList.valueOf(colorOn));
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            locationFab.setBackgroundTintList(ColorStateList.valueOf(colorOff));
            locationFab.setVisibility(View.GONE);

        }

        //If location disabled, color fab gray
        if (!map.isMyLocationEnabled())
            locationFab.setBackgroundTintList(ColorStateList.valueOf(com.google.android.material.R.attr.colorOutline));

        //We want to move the camera to my location when fab is clicked. If disabled, enable when initially pressed
        locationFab.setOnClickListener(view -> {
            if (map.isMyLocationEnabled()) {
                LatLng latLng = new LatLng(lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).getLatitude(),
                        lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).getLongitude());
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
                map.animateCamera(cameraUpdate);
            } else if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                map.setMyLocationEnabled(true);
                locationFab.setBackgroundTintList(ColorStateList.valueOf(colorOn));
            } else {
                Toast.makeText(FavoritesActivity.this, "Not able to get location. Are permissions granted?", Toast.LENGTH_SHORT).show();
            }
        });

        locationFab.setOnLongClickListener(view -> {
            if (!map.isMyLocationEnabled() && (ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                map.setMyLocationEnabled(true);
                locationFab.setBackgroundTintList(ColorStateList.valueOf(colorOn));
            } else {
                map.setMyLocationEnabled(false);
                locationFab.setBackgroundTintList(ColorStateList.valueOf(colorOff));
            }
            return true;
        });
    }

    private void setupMapUiAndStyle() {
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setRotateGesturesEnabled(false);
        try {
            int resId = R.raw.night;
            int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_NO || nightModeFlags == Configuration.UI_MODE_NIGHT_UNDEFINED)
                resId = R.raw.day;
            boolean success = map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, resId));
            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }

        FloatingActionButton satelliteFab = findViewById(R.id.satelliteFab);
        satelliteFab.setOnClickListener(view -> {
            if (map.getMapType() == GoogleMap.MAP_TYPE_NORMAL) {
                map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            } else {
                map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            }
        });
    }

    private void setUpClusterer() {
        clusterManager = new ClusterManager<>(this, map);
        clusterManager.setAnimation(true);
        CustomFavClusterRenderer renderer = new CustomFavClusterRenderer(this, map, clusterManager);
        clusterManager.setRenderer(renderer);
        clusterManager.setAlgorithm(new NonHierarchicalViewBasedAlgorithm<>(
                metrics.widthPixels, metrics.heightPixels));
        clusterManager.getMarkerCollection().setInfoWindowAdapter(new CustomInfoWindowAdapter(LayoutInflater.from(this)));
        handleInteractions(clusterManager, map, renderer);
    }

    private void handleInteractions(ClusterManager<ClusterMarker> clusterManager, GoogleMap map, CustomFavClusterRenderer renderer) {
        map.setOnCameraIdleListener(clusterManager);
        clusterManager.setOnClusterItemInfoWindowClickListener(item -> {
            assert item.getSnippet() != null;
            String url = item.getSnippet().split("!!!")[0];

            Dialog dialog = new Dialog(FavoritesActivity.this);
            dialog.setContentView(R.layout.actions_dialog);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();

            MaterialButton openBtn = dialog.findViewById(R.id.buttonShow);
            if(url.contains("flickr.com")) openBtn.setText(R.string.show_on_flickr);
            openBtn.setOnClickListener(v -> {
                dialog.dismiss();
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            });

            MaterialButton directions = dialog.findViewById(R.id.buttonDirections);
            directions.setOnClickListener(v -> {
                String uri = "google.navigation:q=" + item.getPosition().latitude + "," + item.getPosition().longitude;
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");
                startActivity(intent);
                dialog.dismiss();
            });

            MaterialButton favoriteBtn = dialog.findViewById(R.id.buttonAddFavorite);
            favoriteBtn.setText(R.string.remove_from_favourites);
            favoriteBtn.setOnClickListener(v -> {
                dialog.dismiss();
                if (item.getSnippet().contains(Constants.FAVE_STRING) || favorites.contains(item)) {
                    removeFromFavorites(item);
                    Toast.makeText(context, "Removed from favorites!", Toast.LENGTH_SHORT).show();
                    refreshFavorites();
                    return;
                }
                refreshFavorites();
                saveFavorites();
            });

            MaterialButton dismiss = dialog.findViewById(R.id.buttonCancel);
            dismiss.setOnClickListener(v -> dialog.dismiss());
        });
        clusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<ClusterMarker>() {
            Marker currentShown;

            @Override
            public boolean onClusterItemClick(ClusterMarker item) {
                Marker marker = renderer.getMarker(item);

                Log.d(TAG, "Marker clicked: " + marker.getTitle());
                if (marker.equals(currentShown)) {
                    marker.hideInfoWindow();
                    currentShown = null;
                } else {
                    marker.showInfoWindow();
                    currentShown = marker;
                }
                return true;
            }
        });
        clusterManager.setOnClusterClickListener(cluster -> {
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(cluster.getPosition(), map.getCameraPosition().zoom + 2);
            map.animateCamera(cameraUpdate);
            return true;
        });
        map.setOnMapLongClickListener(latLng -> {
            Intent i = new Intent(FavoritesActivity.this, StreetViewActivity.class);
            i.putExtra("lat", (float) latLng.latitude);
            i.putExtra("lng", (float) latLng.longitude);
            startActivity(i);
        });
        //favoriting stuff
        clusterManager.setOnClusterItemInfoWindowLongClickListener(item -> {
            assert item.getSnippet() != null;
            String url = item.getSnippet().split("!!!")[0];

            Dialog dialog = new Dialog(FavoritesActivity.this);
            dialog.setContentView(R.layout.actions_dialog);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();

            MaterialButton openBtn = dialog.findViewById(R.id.buttonShow);
            if(url.contains("flickr.com")) openBtn.setText(R.string.show_on_flickr);
            openBtn.setOnClickListener(v -> {
                dialog.dismiss();
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            });

            MaterialButton directions = dialog.findViewById(R.id.buttonDirections);
            directions.setOnClickListener(v -> {
                String uri = "google.navigation:q=" + item.getPosition().latitude + "," + item.getPosition().longitude;
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");
                startActivity(intent);
                dialog.dismiss();
            });

            MaterialButton favoriteBtn = dialog.findViewById(R.id.buttonAddFavorite);
            favoriteBtn.setText(R.string.remove_from_favourites);
            favoriteBtn.setOnClickListener(v -> {
                dialog.dismiss();
                if (item.getSnippet().contains(Constants.FAVE_STRING) || favorites.contains(item)) {
                    removeFromFavorites(item);
                    Toast.makeText(context, "Removed from favorites!", Toast.LENGTH_SHORT).show();
                    refreshFavorites();
                    return;
                }
                refreshFavorites();
                saveFavorites();
            });

            MaterialButton dismiss = dialog.findViewById(R.id.buttonCancel);
            dismiss.setOnClickListener(v -> dialog.dismiss());
        });
    }

    void refreshFavorites() {
        clusterManager.cluster();
        toolbar.setSubtitle("Amount of favourites loaded: "+favorites.size());
    }

    private void loadFavorites() {
        favorites = new ArrayList<>();
        SharedPreferences preferences = getSharedPreferences(Constants.PREFS, MODE_PRIVATE);
        String faves_json = "";
        if (preferences.contains(Constants.FAVS_PREF))
            faves_json = preferences.getString(Constants.FAVS_PREF, "");
        Gson gson = new Gson();
        Type type = new TypeToken<List<ClusterMarker>>() {
        }.getType();

        Log.d(TAG, "loadFavorites: Loaded json: "+faves_json);

        if (!faves_json.equals("")) favorites = gson.fromJson(faves_json, type);


        if (!favorites.isEmpty()) {
            for (ClusterMarker faveMarker : favorites) {
                clusterManager.addItem(faveMarker);
            }
            clusterManager.onCameraIdle();
        }
        saveFavorites();
        refreshFavorites();

        if(favorites.size() == 0) {
            Log.e(TAG, "loadFavorites: No favourites found, reverting to raw file");
            try {
                loadFavoritesFromRaw();
            } catch (IOException e) {

            }
        }
    }

    private void loadFavoritesFromRaw() throws IOException {
        Resources res = getResources();
        InputStream in_s = res.openRawResource(R.raw.testfavourites);

        byte[] b = new byte[in_s.available()];
        in_s.read(b);

        String faves_json = new String(b);
        favorites = new ArrayList<>();

        Gson gson = new Gson();
        Type type = new TypeToken<List<ClusterMarker>>() {
        }.getType();

        Log.d(TAG, "loadFavoritesRaw: Loaded json: " + faves_json);

        if (!faves_json.equals("")) favorites = gson.fromJson(faves_json, type);


        if (!favorites.isEmpty()) {
            for (ClusterMarker faveMarker : favorites) {
                clusterManager.addItem(faveMarker);
            }
            clusterManager.onCameraIdle();
        }
        saveFavorites();
        refreshFavorites();
    }

    void saveFavorites() {
        SharedPreferences preferences = getSharedPreferences(Constants.PREFS, MODE_PRIVATE);
        Gson gson = new Gson();

        String json = gson.toJson(favorites);

        Log.d(TAG, "Saving json: "+json);

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Constants.FAVS_PREF, json);
        editor.apply();
    }

    void removeFromFavorites(ClusterMarker toRemove) {
        ArrayList<ClusterMarker> tempFavorites = favorites;
        for(int i = 0; i < favorites.size(); i++) {
            ClusterMarker toCompare = favorites.get(i);
            assert toCompare.getSnippet() != null;
            if(toCompare.getSnippet().equals(toRemove.getSnippet())) {
                tempFavorites.remove(toRemove);
                clusterManager.removeItem(toRemove);
            }
        }
        favorites = tempFavorites;
        saveFavorites();
        refreshFavorites();
    }
}