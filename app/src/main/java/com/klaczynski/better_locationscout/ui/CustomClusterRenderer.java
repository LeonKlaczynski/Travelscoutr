package com.klaczynski.better_locationscout.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;
import com.klaczynski.better_locationscout.R;
import com.klaczynski.better_locationscout.obj.ClusterMarker;

import java.util.ArrayList;


public class CustomClusterRenderer extends DefaultClusterRenderer<ClusterMarker> implements GoogleMap.OnCameraIdleListener {
    Context context;
    IconGenerator iconGen;
    private boolean shouldCluster = true;
    private static final int MIN_CLUSTER_SIZE = 5;
    GoogleMap map;
    ClusterManager manager;


    public CustomClusterRenderer(Context context, GoogleMap map, ClusterManager<ClusterMarker> clusterManager) {
        super(context, map, clusterManager);
        this.context = context;
        this.manager = clusterManager;
        this.iconGen = new IconGenerator(context.getApplicationContext());
        this.map = map;
    }

    @Override
    public int getMinClusterSize() {
        return MIN_CLUSTER_SIZE;
    }

    @Override
    public void setOnClusterClickListener(ClusterManager.OnClusterClickListener<ClusterMarker> listener) {
        super.setOnClusterClickListener(listener);
    }

    @Override
    protected void onBeforeClusterItemRendered(@NonNull ClusterMarker item, @NonNull MarkerOptions markerOptions) {
        super.onBeforeClusterItemRendered(item, markerOptions);
        iconGen.setBackground(ContextCompat.getDrawable(context, R.drawable.ic_marker));
        final Bitmap icon = iconGen.makeIcon();
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
        markerOptions.alpha((float)0.9);
    }

    @Override
    protected void onBeforeClusterRendered(@NonNull Cluster<ClusterMarker> cluster, @NonNull MarkerOptions markerOptions) {
        super.onBeforeClusterRendered(cluster, markerOptions);
        markerOptions.alpha((float)0.4);

    }

    @Override
    protected int getColor(int clusterSize) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(com.google.android.material.R.attr.colorTertiaryContainer, typedValue, true);
        return typedValue.data;
    }

    @Override
    protected boolean shouldRenderAsCluster(@NonNull Cluster<ClusterMarker> cluster) {
        if (shouldCluster) {
            return cluster.getSize() > MIN_CLUSTER_SIZE;
        } else {
            return shouldCluster;
        }
    }

    @Override
    public void onCameraIdle() {
        /*ArrayList<Marker> visibleMarkers = new ArrayList<>();
        for(Marker m : manager.getClusterMarkerCollection().getMarkers()) {
            if(map.getProjection().getVisibleRegion().latLngBounds.contains(m.getPosition()))
                visibleMarkers.add(m);
        }
        shouldCluster = visibleMarkers.size() <= 20;

         */
    }
}