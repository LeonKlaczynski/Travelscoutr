package com.klaczynski.better_locationscout.obj;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class ClusterMarker implements ClusterItem {

    private LatLng latLng;
    private String title, snippet;
    private Spot spot;

    public ClusterMarker(LatLng latLng, Spot s){
        this.spot = s;
        this.latLng = latLng;
        this.title = spot.getName();
        this.snippet = spot.getUrl() + "!!!" + spot.getImgurl();
    }

    @Override
    public LatLng getPosition() {
        return latLng;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getSnippet() {
        return snippet;
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public void setSpot(Spot spot) {
        this.spot = spot;
    }
}