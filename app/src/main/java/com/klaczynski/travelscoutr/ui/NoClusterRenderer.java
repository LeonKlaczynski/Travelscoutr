package com.klaczynski.travelscoutr.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;
import com.klaczynski.travelscoutr.Constants;
import com.klaczynski.travelscoutr.R;
import com.klaczynski.travelscoutr.obj.ClusterMarker;


public class NoClusterRenderer extends DefaultClusterRenderer<ClusterMarker> implements GoogleMap.OnCameraIdleListener {
    Context context;
    IconGenerator iconGen;
    private static final int MIN_CLUSTER_SIZE = 5;
    GoogleMap map;
    ClusterManager manager;


    public NoClusterRenderer(Context context, GoogleMap map, ClusterManager<ClusterMarker> clusterManager) {
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

        if(item.getSnippet().contains(Constants.FLICKR_STRING)) {
            iconGen.setBackground(ContextCompat.getDrawable(context, R.drawable.map_marker_flickr));
            final Bitmap icon = iconGen.makeIcon();
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
            markerOptions.alpha((float)0.8);
        } else {
            iconGen.setBackground(ContextCompat.getDrawable(context, R.drawable.ic_marker));
            final Bitmap icon = iconGen.makeIcon();
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
            markerOptions.alpha((float)0.8);
        }
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
        return false;
    }

    @Override
    public void onCameraIdle() {
    }
}