package com.treinchauffeur.travelscoutr;

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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.NonHierarchicalViewBasedAlgorithm;
import com.treinchauffeur.travelscoutr.databinding.ActivityMapsBinding;
import com.treinchauffeur.travelscoutr.io.InOutOperator;
import com.treinchauffeur.travelscoutr.net.FlickrSearcher;
import com.treinchauffeur.travelscoutr.net.PlacesSearcher;
import com.treinchauffeur.travelscoutr.obj.ClusterMarker;
import com.treinchauffeur.travelscoutr.obj.Spot;
import com.treinchauffeur.travelscoutr.ui.CustomClusterRenderer;
import com.treinchauffeur.travelscoutr.ui.CustomInfoWindowAdapter;
import com.treinchauffeur.travelscoutr.ui.UserInterfaceHandler;

import org.json.JSONException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private ClusterManager<ClusterMarker> clusterManager;
    private final DisplayMetrics metrics = new DisplayMetrics();
    public static String filesDir;
    public static final String TAG = "MapsActivity";

    @SuppressLint("StaticFieldLeak")
    public static Context context;
    protected LocationManager lm;
    protected int itemSize = 0;
    private ArrayList<ClusterMarker> favorites = new ArrayList<>();

    public DrawerLayout drawerLayout;
    private NavigationBarView navBar;
    private NavigationView drawer;
    protected Menu navMenu;
    private MaterialCardView cardView;

    protected ExtendedFloatingActionButton flickrBtn;
    private FloatingActionButton locationFab;
    private boolean isFollowingLocation = false;
    private boolean keepScreenAwake = false;


    private PlacesSearcher placesSearcher;
    private InOutOperator inOutOperator;
    private UserInterfaceHandler uiHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        com.treinchauffeur.travelscoutr.databinding.ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        drawerLayout = findViewById(R.id.rootView);
        uiHandler = new UserInterfaceHandler(this);
        inOutOperator = new InOutOperator(this);
        filesDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        Logger.log(TAG, filesDir);
        context = this;
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        placesSearcher = new PlacesSearcher(MapsActivity.this, this);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        //Clear Glide cache to confirm images will be up-to-date & avoid massive cache build-up.
        new Thread(() -> Glide.get(context).clearDiskCache()).start();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        //Setting up the googleMap ui options & map style (day/night, if applicable)
        setupMapUiAndStyle();

        //Sets actions to do with Flickr (initializing the search tool, assigns button action)
        setupFlickr();

        @SuppressLint("SetTextI18n") Thread thread = new Thread(() -> {
            try {
                inOutOperator.startConversion();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (inOutOperator.spots.size() == 0) {
                Toast.makeText(context, "No spots were loaded. Network issues?", Toast.LENGTH_SHORT).show();
            } else {
                setUpClusterer();
            }
            //Loading favorites from sharedpreferences
            loadFavorites();

            runOnUiThread(() -> {
                TextView drawerSubtitle = drawer.getHeaderView(0).findViewById(R.id.drawer_subtitle);
                drawerSubtitle.setText(getString(R.string.amount_of_spots_loaded) + inOutOperator.spots.size());
            });
        });
        thread.start();

        setupNavigationAndUX();

        //Loads preferences (currently whether or not to keep the screen awake & my location settings and accompanying permissions)
        loadPreferences();
    }


    @SuppressLint("MissingPermission")
    private void loadPreferences() {
        locationFab = findViewById(R.id.locationFab);

        SharedPreferences preferences = getSharedPreferences(Constants.PREFS, MODE_PRIVATE);

        keepScreenAwake = preferences.getBoolean(Constants.WAKELOCK_PREF, false);
        drawerLayout.setKeepScreenOn(keepScreenAwake);
        ((MaterialSwitch) drawer.getMenu().findItem(R.id.keepAwake).getActionView()).setChecked(keepScreenAwake);
        ((MaterialSwitch) drawer.getMenu().findItem(R.id.keepAwake).getActionView()).setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(Constants.WAKELOCK_PREF, isChecked);
            editor.apply();

            keepScreenAwake = isChecked;
            drawerLayout.setKeepScreenOn(keepScreenAwake);
        });

        //Check the preferences to enable/disable my location

        if (preferences.getBoolean(Constants.LOCATION_ENABLED_PREF, false)) {
            //if yes, check permissions
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1312);
            } else {
                //Permissions are granted, enable my location
                map.setMyLocationEnabled(true);
            }
        } else map.setMyLocationEnabled(false);

        ((MaterialSwitch) drawer.getMenu().findItem(R.id.drawer_myLocation).getActionView()).setChecked(map.isMyLocationEnabled());
        ((MaterialSwitch) drawer.getMenu().findItem(R.id.drawer_myLocation).getActionView()).setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(Constants.LOCATION_ENABLED_PREF, isChecked);
            editor.apply();

            if (isChecked) {
                if (ContextCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MapsActivity.this, "Location permissions are not granted!", Toast.LENGTH_SHORT).show();
                    ActivityCompat.requestPermissions(MapsActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                            Constants.LOCATION_PERMS_CODE);
                    buttonView.setChecked(false);
                } else {
                    map.setMyLocationEnabled(true);
                    locationFab.setVisibility(View.VISIBLE);
                }
            } else {
                map.setMyLocationEnabled(false);
                locationFab.setVisibility(View.INVISIBLE);
            }
        });

        //Upon startup, go to location if enabled. If not, get rid of fab.
        if (!map.isMyLocationEnabled()) locationFab.setVisibility(View.INVISIBLE);
        else {
            goToMyLocation();
            locationFab.setVisibility(View.VISIBLE);
        }

        locationFab.setOnClickListener(v -> setFollowMyLocation(!isFollowingLocation));


    }

    @SuppressLint("SetTextI18n")
    private void setupNavigationAndUX() {
        navBar = findViewById(R.id.navigationRailView);
        navMenu = navBar.getMenu();
        drawer = findViewById(R.id.navigationView);
        ImageView cardSearchBtn = findViewById(R.id.cardSearchIcon);
        ImageView cardNavIcon = findViewById(R.id.cardNavIcon);
        TextView cardAppLabel = findViewById(R.id.cardAppLabel);
        EditText cardAppSearchText = findViewById(R.id.cardAppSearchText);
        MenuItem drawerAllItems = drawer.getMenu().findItem(R.id.drawer_allItems);
        MenuItem drawerFavorites = drawer.getMenu().findItem(R.id.drawer_favorites);
        MenuItem drawerMyLocation = drawer.getMenu().findItem(R.id.drawer_myLocation);
        MenuItem drawerSatellite = drawer.getMenu().findItem(R.id.drawer_satelliteToggle);
        MenuItem lofotenNavItem = drawer.getMenu().findItem(R.id.LofotenNavItem);
        MenuItem icelandNavItem = drawer.getMenu().findItem(R.id.IcelandNavItem);
        MenuItem scotlandNavItem = drawer.getMenu().findItem(R.id.ScotlandNavItem);
        MenuItem currentBBoxNavItem = drawer.getMenu().findItem(R.id.currentBBox);
        cardView = findViewById(R.id.topCardView);
        drawerAllItems.setChecked(true);
        cardAppSearchText.setShowSoftInputOnFocus(true);

        cardAppLabel.setOnClickListener(v -> {
            cardAppLabel.setVisibility(View.GONE);
            cardAppSearchText.setVisibility(View.VISIBLE);
            cardAppSearchText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(cardAppSearchText, InputMethodManager.SHOW_IMPLICIT);
        });

        cardSearchBtn.setOnClickListener(v -> {
            if (cardAppSearchText.getVisibility() == View.VISIBLE) {
                if (cardAppSearchText.getText().length() > 2) {
                    placesSearcher.goToPlace(cardAppSearchText.getText().toString(), map);
                    cardView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(cardView.getWindowToken(), 0);
                } else {
                    cardAppSearchText.setError("No query specified!");
                }
            } else {
                cardAppLabel.setVisibility(View.GONE);
                cardAppSearchText.setVisibility(View.VISIBLE);
                cardAppSearchText.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(cardAppSearchText, InputMethodManager.SHOW_IMPLICIT);

            }
        });

        cardAppSearchText.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                if (!cardAppSearchText.getText().toString().equals("")) {
                    placesSearcher.goToPlace(cardAppSearchText.getText().toString(), map);
                    cardView.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(cardView.getWindowToken(), 0);
                } else {
                    cardAppSearchText.setError("No query specified!");
                }
                return false;
            }
            return false;
        });

        cardNavIcon.setOnClickListener(v -> drawerLayout.open());

        map.setOnMapClickListener(latLng -> {
            setFollowMyLocation(false);
            uiHandler.onMapClick();
        });

        navMenu.findItem(R.id.favoritesNavItem).setOnMenuItemClickListener(item -> {
            Intent i = new Intent(MapsActivity.this, FavoritesActivity.class);
            startActivity(i);
            return false;
        });

        drawerFavorites.setOnMenuItemClickListener(item -> {
            drawerLayout.close();
            Intent i = new Intent(MapsActivity.this, FavoritesActivity.class);
            startActivity(i);
            return false;
        });

        drawerMyLocation.setChecked(map.isMyLocationEnabled());

        MaterialSwitch satelliteSwitch = (MaterialSwitch) drawerSatellite.getActionView();
        satelliteSwitch.setChecked(map.getMapType() == GoogleMap.MAP_TYPE_HYBRID);

        satelliteSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (map.getMapType() == GoogleMap.MAP_TYPE_NORMAL) {
                map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                satelliteSwitch.setChecked(true);
            } else {
                map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                satelliteSwitch.setChecked(false);
            }
            drawerLayout.close();
        });

        lofotenNavItem.setOnMenuItemClickListener(item -> {
            LatLng latLng = new LatLng(68.36315324768857, 14.898834563791752);
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, (float) 6.5);
            map.animateCamera(cameraUpdate);
            drawerLayout.close();
            return false;
        });
        scotlandNavItem.setOnMenuItemClickListener(item -> {
            LatLng latLng = new LatLng(57.322641848476735, -4.313399456441402);
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, (float) 6.9);
            map.animateCamera(cameraUpdate);
            drawerLayout.close();
            return false;
        });
        icelandNavItem.setOnMenuItemClickListener(item -> {
            LatLng latLng = new LatLng(65.2742675631778, -19.45199064910412);
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, (float) 5.57);
            map.animateCamera(cameraUpdate);
            drawerLayout.close();
            return false;
        });
        currentBBoxNavItem.setOnMenuItemClickListener(item -> {
            LatLng toSave = map.getCameraPosition().target;
            float zoom = map.getCameraPosition().zoom;
            Log.d(TAG, "LatLng + Zoom: " + toSave + zoom);
            drawerLayout.close();
            return false;
        });
    }


    private void setupFlickr() {
        FlickrSearcher flickr = new FlickrSearcher(MapsActivity.this, this);
        flickrBtn = findViewById(R.id.flickrFab);
        flickrBtn.setOnClickListener(view -> runOnUiThread(() -> flickr.performSearch(clusterManager, map.getProjection().getVisibleRegion().latLngBounds)));
    }

    private void goToMyLocation() {
        map.setOnMyLocationChangeListener(location -> {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
            setFollowMyLocation(false);
        });
    }

    public void setFollowMyLocation(boolean shouldFollow) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceVariant, typedValue, true);
        int colorOff = ContextCompat.getColor(this, typedValue.resourceId);
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimaryContainer, typedValue, true);
        int colorOn = ContextCompat.getColor(this, typedValue.resourceId);
        float zoomLevel = (map.getCameraPosition().zoom > 14) ? map.getCameraPosition().zoom : 14;

        if (shouldFollow) {
            isFollowingLocation = true;
            if (map.getMyLocation() != null) {
                LatLng latLng = new LatLng(map.getMyLocation().getLatitude(), map.getMyLocation().getLongitude());
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel));
            }
            locationFab.setBackgroundTintList(ColorStateList.valueOf(colorOn));
            map.setOnMyLocationChangeListener(location -> {
                if (!uiHandler.mapIsMoving) {
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    //if speed higher that 4m/s, zoom in a little less. We assume the user is moving in a car.
                    if (location.hasSpeed())
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, (location.getSpeed() > 4) ? 12 : zoomLevel));
                    else
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
                }
            });
        } else {
            isFollowingLocation = false;
            locationFab.setBackgroundTintList(ColorStateList.valueOf(colorOff));
            map.setOnMyLocationChangeListener(null);
        }
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
    }

    private void setUpClusterer() {
        runOnUiThread(() -> {
        clusterManager = new ClusterManager<>(this, map);
        clusterManager.setAnimation(true);
        CustomClusterRenderer renderer = new CustomClusterRenderer(this, map, clusterManager);
        clusterManager.setRenderer(renderer);
        clusterManager.setAlgorithm(new NonHierarchicalViewBasedAlgorithm<>(metrics.widthPixels, metrics.heightPixels));
        clusterManager.getMarkerCollection().setInfoWindowAdapter(new CustomInfoWindowAdapter(LayoutInflater.from(this)));
        addItemsToClusterer();
        handleMapInteractions(clusterManager, map, renderer);
        });
    }

    private void handleMapInteractions(ClusterManager<ClusterMarker> clusterManager, GoogleMap map, CustomClusterRenderer renderer) {
        map.setOnCameraIdleListener(() -> {
            clusterManager.onCameraIdle();
            uiHandler.onCameraIdle();
        });

        map.setOnCameraMoveStartedListener(reason -> uiHandler.onCameraMoveStart(reason));

        clusterManager.setOnClusterItemInfoWindowClickListener(item -> {
            onSpotWindowClick(item);
            setFollowMyLocation(false);
        });

        clusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<ClusterMarker>() {
            Marker currentShown;
            @Override
            public boolean onClusterItemClick(ClusterMarker item) {
                setFollowMyLocation(false);
                Marker marker = renderer.getMarker(item);
                Log.d(TAG, "Marker clicked: " + marker.getTitle());
                if (marker.equals(currentShown)) {
                    marker.hideInfoWindow();
                    currentShown = null;
                    uiHandler.isInfoWindowShown = true;
                } else {
                    marker.showInfoWindow();
                    currentShown = marker;
                }
                return true;
            }
        });
        map.setOnInfoWindowCloseListener(marker -> {
            setFollowMyLocation(false);
            uiHandler.isInfoWindowShown = false;
        });
        clusterManager.setOnClusterClickListener(cluster -> {
            setFollowMyLocation(false);
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(cluster.getPosition(), map.getCameraPosition().zoom + 2);
            map.animateCamera(cameraUpdate);
            return true;
        });
        map.setOnMapLongClickListener(latLng -> {
            setFollowMyLocation(false);
            Intent i = new Intent(MapsActivity.this, StreetViewActivity.class);
            i.putExtra("lat", (float) latLng.latitude);
            i.putExtra("lng", (float) latLng.longitude);
            startActivity(i);
        });

        //favoriting stuff
        clusterManager.setOnClusterItemInfoWindowLongClickListener(item -> {
            assert item.getSnippet() != null;
            String url = item.getSnippet().split("!!!")[0];

            Dialog dialog = new Dialog(MapsActivity.this);
            dialog.setContentView(R.layout.actions_dialog);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();

            MaterialButton openBtn = dialog.findViewById(R.id.buttonShow);
            if (url.contains("flickr.com")) openBtn.setText(R.string.show_on_flickr_com);
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
            favoriteBtn.setOnClickListener(v -> {
                dialog.dismiss();
                if (favoritesContains(item)) {
                    Toast.makeText(context, "Already added to favourites!", Toast.LENGTH_SHORT).show();
                    return;
                }

                item.setSnippet(item.getSnippet() + Constants.FAVE_STRING);
                clusterManager.addItem(item);
                favorites.add(item);
                Toast.makeText(MapsActivity.this, "Added to favorites!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onClusterItemInfoWindowLongClick: adding " + item.getSnippet());
                refreshFavorites();
                saveFavorites();
            });

            MaterialButton dismiss = dialog.findViewById(R.id.buttonCancel);
            dismiss.setOnClickListener(v -> dialog.dismiss());
        });
    }

    private void onSpotWindowClick(ClusterMarker item) {
        assert item.getSnippet() != null;
        String url = item.getSnippet().split("!!!")[0];
        boolean isFavorite = item.getSnippet().contains(Constants.FAVE_STRING);

        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.actions_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();

        MaterialButton openBtn = dialog.findViewById(R.id.buttonShow);
        if (url.contains("flickr.com")) openBtn.setText(R.string.show_on_flickr_com);
        openBtn.setOnClickListener(v -> {
            dialog.dismiss();
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            context.startActivity(i);
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
        if (isFavorite) {
            favoriteBtn.setEnabled(false);
            favoriteBtn.setText(R.string.already_added_to_favourites);
        }
        favoriteBtn.setOnClickListener(v -> {
            dialog.dismiss();
            if (favoritesContains(item)) {
                Toast.makeText(context, "Already added to favourites!", Toast.LENGTH_SHORT).show();
                return;
            }

            item.setSnippet(item.getSnippet() + Constants.FAVE_STRING);
            clusterManager.addItem(item);
            favorites.add(item);
            Toast.makeText(context, "Added to favorites!", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onClusterItemInfoWindowLongClick: adding " + item.getSnippet());
            refreshFavorites();
            saveFavorites();
        });

        MaterialButton dismiss = dialog.findViewById(R.id.buttonCancel);
        dismiss.setOnClickListener(v -> dialog.dismiss());
    }

    private boolean favoritesContains(ClusterMarker item) {
        for (ClusterMarker toCompare : favorites) {
            if (toCompare.getPosition().equals(item.getPosition())) return true;
        }
        return false;
    }

    public void refreshFavorites() {
        clusterManager.cluster();
    }

    private void loadFavorites() {
        runOnUiThread(() -> {
            favorites = new ArrayList<>();
            SharedPreferences preferences = getSharedPreferences(Constants.PREFS, MODE_PRIVATE);
            String faves_json = "";
            if (preferences.contains(Constants.FAVS_PREF))
                faves_json = preferences.getString(Constants.FAVS_PREF, "");
            Gson gson = new Gson();
            Type type = new TypeToken<List<ClusterMarker>>() {
            }.getType();

            Log.d(TAG, "loadFavorites: Loaded json: " + faves_json);

            if (!faves_json.equals("")) favorites = gson.fromJson(faves_json, type);

            if (!favorites.isEmpty()) {
                for (ClusterMarker faveMarker : favorites) {
                    clusterManager.addItem(faveMarker);
                }
                clusterManager.onCameraIdle();
            }
            saveFavorites();
        });
    }

    void saveFavorites() {
        SharedPreferences preferences = getSharedPreferences(Constants.PREFS, MODE_PRIVATE);
        Gson gson = new Gson();

        String json = gson.toJson(favorites);

        Log.d(TAG, "Saving json: " + json);

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Constants.FAVS_PREF, json);
        editor.apply();
    }

    private void addItemsToClusterer() {
        ArrayList<ClusterMarker> markers = new ArrayList<>();
        for (Spot spot : inOutOperator.spots) {
            ClusterMarker marker = new ClusterMarker(new LatLng(spot.getLat(), spot.getLng()), spot);
            markers.add(marker);
        }
        clusterManager.addItems(markers);
        itemSize = markers.size();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (clusterManager != null) loadFavorites();
        if (navBar != null) navBar.setSelectedItemId(R.id.homeNavItem);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constants.LOCATION_PERMS_CODE) {
            SharedPreferences preferences = getSharedPreferences(Constants.PREFS, MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationFab.setVisibility(View.VISIBLE);
                map.setMyLocationEnabled(true);

                editor.putBoolean(Constants.LOCATION_ENABLED_PREF, true);
                editor.apply();
            } else {
                Toast.makeText(context, "Permission was denied!", Toast.LENGTH_SHORT).show();
                locationFab.setVisibility(View.INVISIBLE);
                map.setMyLocationEnabled(false);

                editor.putBoolean(Constants.LOCATION_ENABLED_PREF, false);
                editor.apply();
            }
            ((MaterialSwitch) drawer.getMenu().findItem(R.id.drawer_myLocation).getActionView()).setChecked(map.isMyLocationEnabled());
        }
    }
}