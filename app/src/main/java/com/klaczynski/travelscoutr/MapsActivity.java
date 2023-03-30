package com.klaczynski.travelscoutr;

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
import android.os.Environment;
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.flickr4java.flickr.FlickrException;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarMenu;
import com.google.android.material.navigation.NavigationBarView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.NonHierarchicalViewBasedAlgorithm;
import com.klaczynski.travelscoutr.databinding.ActivityMapsBinding;
import com.klaczynski.travelscoutr.io.InOutOperations;
import com.klaczynski.travelscoutr.net.FlickrSearcher;
import com.klaczynski.travelscoutr.obj.ClusterMarker;
import com.klaczynski.travelscoutr.obj.Spot;
import com.klaczynski.travelscoutr.ui.CustomClusterRenderer;
import com.klaczynski.travelscoutr.ui.CustomInfoWindowAdapter;

import org.json.JSONException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private ClusterManager<ClusterMarker> clusterManager;
    private DisplayMetrics metrics = new DisplayMetrics();

    private ActivityMapsBinding binding;
    public static String filesDir;
    public static final String TAG = "MapsActivity";
    public static Context context;
    MaterialToolbar toolbar;
    LocationManager lm;
    int itemSize = 0;
    ArrayList<ClusterMarker> favorites = new ArrayList<>();
    boolean favoritesOnly = true;

    NavigationBarView navBar;
    Menu navMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        filesDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        Logger.log(TAG, filesDir);
        context = this;
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        //Clear Glide cache to confirm images will be up-to-date & avoid massive cache build-up.
        new Thread(new Runnable() {
            @Override
            public void run() {
                Glide.get(context).clearDiskCache();
            }
        }).start();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        //Setting up the googleMap ui options & map style (day/night, if applicable)
        setupMapUiAndStyle();

        //Location fab & enable my location if possible
        setupLocationAndPermissions();

        //Handle preset view buttons
        handlePresets();

        //Sets actions to do with Flickr (initializing the search tool, assigns button action)
        handleFlickr();

        //Yes, doing this on the main thread. Loving that. Maybe a TODO..
        try {
            InOutOperations.startConversion();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (InOutOperations.spots.size() == 0) {
            Toast.makeText(context, "No spots were loaded. Network issues?", Toast.LENGTH_SHORT).show();
        } else {
            setUpClusterer();
            toolbar.setSubtitle("Amount of spots loaded: " + InOutOperations.spots.size());
        }

        //
        loadFavorites();

        setDisplayMode();

        setMenuOptions();
    }

    private void setMenuOptions() {
        navMenu.findItem(R.id.favoritesNavItem).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(MapsActivity.this, FavoritesActivity.class);
                startActivity(i);
                return false;
            }
        });
    }

    private void setDisplayMode() {
            for(Marker m : clusterManager.getMarkerCollection().getMarkers()) {
                if(!m.getSnippet().contains(Constants.FAVE_STRING)) {
                    m.setVisible(!favoritesOnly);
                }
            }
        clusterManager.onCameraIdle();
    }

    private void handleFlickr() {
        FlickrSearcher flickr = new FlickrSearcher(this);
        ExtendedFloatingActionButton flickrBtn = findViewById(R.id.flickrFab);
        flickrBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MapsActivity.this, "Searching flickr..", Toast.LENGTH_SHORT).show();
                try {
                    flickr.performSearch(map, clusterManager);
                } catch (FlickrException e) {
                    Log.e(TAG, "handleFlickr: " + e, e);
                }
            }
        });
    }

    private void handlePresets() {
        MaterialButton lofotenFab = findViewById(R.id.lofotenFab);
        lofotenFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LatLng latLng = new LatLng(68.36315324768857, 14.898834563791752);
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, (float) 6.5);
                map.animateCamera(cameraUpdate);
            }
        });

        MaterialButton scotlandFab = findViewById(R.id.scotlandFab);
        scotlandFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LatLng latLng = new LatLng(57.322641848476735, -4.313399456441402);
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, (float) 6.9);
                map.animateCamera(cameraUpdate);
            }
        });

        MaterialButton icelandFab = findViewById(R.id.icelandFab);
        icelandFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LatLng latLng = new LatLng(65.2742675631778, -19.45199064910412);
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, (float) 5.57);
                map.animateCamera(cameraUpdate);
            }
        });

        MaterialButton miscFab = findViewById(R.id.miscFab);
        miscFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LatLng toSave = map.getCameraPosition().target;
                float zoom = map.getCameraPosition().zoom;
                Log.d(TAG, "LatLng + Zoom: " + toSave + zoom);
            }
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

        }

        //If location disabled, color fab gray
        if (!map.isMyLocationEnabled())
            locationFab.setBackgroundTintList(ColorStateList.valueOf(com.google.android.material.R.attr.colorOutline));
        else if (lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) != null) {
            LatLng latLng = new LatLng(lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).getLatitude(),
                    lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).getLongitude());
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
            map.animateCamera(cameraUpdate);
        }

        //We want to move the camera to my location when fab is clicked. If disabled, enable when initially pressed
        locationFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                    Toast.makeText(MapsActivity.this, "Not able to get location. Are permissions granted?", Toast.LENGTH_SHORT).show();
                }
            }
        });

        locationFab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (!map.isMyLocationEnabled() && (ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                    map.setMyLocationEnabled(true);
                    locationFab.setBackgroundTintList(ColorStateList.valueOf(colorOn));
                } else {
                    map.setMyLocationEnabled(false);
                    locationFab.setBackgroundTintList(ColorStateList.valueOf(colorOff));
                }
                return true;
            }
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
        satelliteFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (map.getMapType() == GoogleMap.MAP_TYPE_NORMAL) {
                    map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                } else {
                    map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                }
            }
        });
        /*FloatingActionButton favoritesFab = findViewById(R.id.favoritesFab);
        favoritesFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MapsActivity.this, FavoritesActivity.class);
                startActivity(i);
            }
        });*/
    }

    private void setUpClusterer() {
        clusterManager = new ClusterManager<>(this, map);
        clusterManager.setAnimation(true);
        CustomClusterRenderer renderer = new CustomClusterRenderer(this, map, clusterManager);
        clusterManager.setRenderer(renderer);
        clusterManager.setAlgorithm(new NonHierarchicalViewBasedAlgorithm<ClusterMarker>(
                metrics.widthPixels, metrics.heightPixels));
        clusterManager.getMarkerCollection().setInfoWindowAdapter(new CustomInfoWindowAdapter(LayoutInflater.from(this)));
        addItemsToClusterer();
        handleInteractions(clusterManager, map, renderer);

    }

    private void handleInteractions(ClusterManager<ClusterMarker> clusterManager, GoogleMap map, CustomClusterRenderer renderer) {
        map.setOnCameraIdleListener(clusterManager);
        clusterManager.setOnClusterItemInfoWindowClickListener(new ClusterManager.OnClusterItemInfoWindowClickListener<ClusterMarker>() {
            @Override
            public void onClusterItemInfoWindowClick(ClusterMarker item) {
                String url = item.getSnippet().split("!!!")[0];

                Dialog dialog = new Dialog(MapsActivity.this);
                dialog.setContentView(R.layout.actions_dialog);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();

                MaterialButton openBtn = dialog.findViewById(R.id.buttonShow);
                if(url.contains("flickr.com")) openBtn.setText("Show on flickr.com");
                openBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(url));
                        startActivity(i);
                    }
                });

                MaterialButton directions = dialog.findViewById(R.id.buttonDirections);
                directions.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String uri = "google.navigation:q=" + item.getPosition().latitude + "," + item.getPosition().longitude;
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        intent.setPackage("com.google.android.apps.maps");
                        startActivity(intent);
                        dialog.dismiss();
                    }
                });

                MaterialButton favoriteBtn = dialog.findViewById(R.id.buttonAddFavorite);
                favoriteBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        if (favoritesContains(item)) {
                            Toast.makeText(context, "Already added to favourites!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        item.setSnippet(item.getSnippet() + Constants.FAVE_STRING);
                        clusterManager.addItem(item);
                        favorites.add(item);
                        Toast.makeText(MapsActivity.this, "Added to favorites!", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "onClusterItemInfoWindowLongClick: adding "+item.getSnippet());
                        refreshFavorites();
                        saveFavorites();
                    }
                });

                MaterialButton dismiss = dialog.findViewById(R.id.buttonCancel);
                dismiss.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
            }
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
        clusterManager.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<ClusterMarker>() {
            @Override
            public boolean onClusterClick(Cluster<ClusterMarker> cluster) {
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(cluster.getPosition(), map.getCameraPosition().zoom + 2);
                map.animateCamera(cameraUpdate);
                return true;
            }
        });
        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull LatLng latLng) {
                Intent i = new Intent(MapsActivity.this, StreetViewActivity.class);
                i.putExtra("lat", (float) latLng.latitude);
                i.putExtra("lng", (float) latLng.longitude);
                startActivity(i);
            }
        });
        //favoriting stuff
        clusterManager.setOnClusterItemInfoWindowLongClickListener(new ClusterManager.OnClusterItemInfoWindowLongClickListener<ClusterMarker>() {
            @Override
            public void onClusterItemInfoWindowLongClick(ClusterMarker item) {
                String url = item.getSnippet().split("!!!")[0];

                Dialog dialog = new Dialog(MapsActivity.this);
                dialog.setContentView(R.layout.actions_dialog);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();

                MaterialButton openBtn = dialog.findViewById(R.id.buttonShow);
                if(url.contains("flickr.com")) openBtn.setText("Show on flickr.com");
                openBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(url));
                        startActivity(i);
                    }
                });

                MaterialButton directions = dialog.findViewById(R.id.buttonDirections);
                directions.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String uri = "google.navigation:q=" + item.getPosition().latitude + "," + item.getPosition().longitude;
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        intent.setPackage("com.google.android.apps.maps");
                        startActivity(intent);
                        dialog.dismiss();
                    }
                });

                MaterialButton favoriteBtn = dialog.findViewById(R.id.buttonAddFavorite);
                favoriteBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        if (favoritesContains(item)) {
                            Toast.makeText(context, "Already added to favourites!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        item.setSnippet(item.getSnippet() + Constants.FAVE_STRING);
                        clusterManager.addItem(item);
                        favorites.add(item);
                        Toast.makeText(MapsActivity.this, "Added to favorites!", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "onClusterItemInfoWindowLongClick: adding "+item.getSnippet());
                        refreshFavorites();
                        saveFavorites();
                    }
                });

                MaterialButton dismiss = dialog.findViewById(R.id.buttonCancel);
                dismiss.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
            }
        });
    }

    private boolean favoritesContains(ClusterMarker item) {
        for(ClusterMarker toCompare : favorites) {
            if (toCompare.getPosition().equals(item.getPosition())) return true;
        }
        return false;
    }

    void refreshFavorites() {
        clusterManager.cluster();
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
            clusterManager.onCameraIdle(); //idk y, but this updates the map and therefor the items on it.
        }
        saveFavorites();
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
            if(toCompare.getSnippet().equals(toRemove.getSnippet()))
                tempFavorites.remove(toRemove);
        }
        favorites = tempFavorites;
        saveFavorites();
    }

    private void addItemsToClusterer() {
        ArrayList<ClusterMarker> markers = new ArrayList<>();
        for (Spot spot : InOutOperations.spots) {
            ClusterMarker marker = new ClusterMarker(new LatLng(spot.getLat(), spot.getLng()), spot);
            markers.add(marker);
        }
        clusterManager.addItems(markers);
        itemSize = markers.size();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(clusterManager != null) loadFavorites();
        if(navBar != null) navBar.setSelectedItemId(R.id.homeNavItem);
    }
}