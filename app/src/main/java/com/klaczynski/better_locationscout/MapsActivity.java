package com.klaczynski.better_locationscout;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.NonHierarchicalViewBasedAlgorithm;
import com.klaczynski.better_locationscout.databinding.ActivityMapsBinding;
import com.klaczynski.better_locationscout.io.InOutOperations;
import com.klaczynski.better_locationscout.obj.ClusterMarker;
import com.klaczynski.better_locationscout.obj.Spot;
import com.klaczynski.better_locationscout.ui.CustomClusterRenderer;
import com.klaczynski.better_locationscout.ui.CustomInfoWindowAdapter;

import org.json.JSONException;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private ClusterManager<ClusterMarker> clusterManager;
    private DisplayMetrics metrics = new DisplayMetrics();
    boolean locationPermissionGranted;

    private ActivityMapsBinding binding;
    public static String filesDir;
    public static final String TAG = "MapsActivity";
    public static Context context;
    MaterialToolbar toolbar;
    LocationManager lm;
    int itemSize = 0;

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

        //Clear Glide cache to confirm images are up-to-date & avoid massive cache build-up.
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

        //Handle favorite buttons
        handleFavorites();

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
    }

    private void handleFavorites() {
        MaterialButton lofotenFab = findViewById(R.id.lofotenFab);
        lofotenFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LatLng latLng = new LatLng(68.36315324768857, 14.898834563791752);
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, (float)6.5);
                map.animateCamera(cameraUpdate);
            }
        });

        MaterialButton scotlandFab = findViewById(R.id.scotlandFab);
        scotlandFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LatLng latLng = new LatLng(57.322641848476735,-4.313399456441402);
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, (float)6.9);
                map.animateCamera(cameraUpdate);
            }
        });

        MaterialButton icelandFab = findViewById(R.id.icelandFab);
        icelandFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LatLng latLng = new LatLng(65.2742675631778,-19.45199064910412);
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, (float)5.57);
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
    }


    private void setUpClusterer() {
        clusterManager = new ClusterManager<>(this, map);
        clusterManager.setAnimation(true);
        CustomClusterRenderer renderer = new CustomClusterRenderer(this, map, clusterManager);
        clusterManager.setRenderer(renderer);
        clusterManager.setAlgorithm(new NonHierarchicalViewBasedAlgorithm<ClusterMarker>(
                metrics.widthPixels, metrics.heightPixels));
        clusterManager.getMarkerCollection().setInfoWindowAdapter(new CustomInfoWindowAdapter(LayoutInflater.from(this)));
        addItems();
        handleInteractions(clusterManager, map, renderer);

    }

    private void handleInteractions(ClusterManager<ClusterMarker> clusterManager, GoogleMap map, CustomClusterRenderer renderer) {
        map.setOnCameraIdleListener(clusterManager);
        clusterManager.setOnClusterItemInfoWindowClickListener(new ClusterManager.OnClusterItemInfoWindowClickListener<ClusterMarker>() {
            @Override
            public void onClusterItemInfoWindowClick(ClusterMarker item) {
                String url = item.getSnippet().split("!!!")[0];
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
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
    }

    private void addItems() {
        ArrayList<ClusterMarker> markers = new ArrayList<>();
        for (Spot spot : InOutOperations.spots) {
            ClusterMarker marker = new ClusterMarker(new LatLng(spot.getLat(), spot.getLng()), spot);
            markers.add(marker);
        }
        clusterManager.addItems(markers);
        itemSize = markers.size();
    }
}