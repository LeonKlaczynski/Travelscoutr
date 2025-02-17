package com.treinchauffeur.travelscoutr.obj;

import com.google.android.gms.maps.model.Marker;
import com.treinchauffeur.travelscoutr.Constants;

public class Spot {

    double lat, lng;
    String name, imgurl, url;

    public Spot(String name, double lat, double lng, String imgurl, String url) {
        super();
        this.lat = lat;
        this.lng = lng;
        this.name = name;
        this.imgurl = imgurl;
        this.url = url;
    }
    public Spot(){}

    /**
     * @return the lat
     */
    public double getLat() {
        return lat;
    }

    /**
     * @param lat2 the lat to set
     */
    public void setLat(double lat2) {
        this.lat = lat2;
    }

    /**
     * @return the lng
     */
    public double getLng() {
        return lng;
    }

    /**
     * @param lng2 the lng to set
     */
    public void setLng(double lng2) {
        this.lng = lng2;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the imgurl
     */
    public String getImgurl() {
        return imgurl;
    }

    /**
     * @param imgurl the imgurl to set
     */
    public void setImgurl(String imgurl) {
        this.imgurl = imgurl;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    public static Spot fromMarker(Marker m) {
        Spot s = new Spot();
        s.setName(m.getTitle());
        s.setUrl(m.getSnippet().split(Constants.SplitRegex)[0]);
        s.setImgurl(m.getSnippet().split(Constants.SplitRegex)[1]);
        s.setLat(m.getPosition().latitude);
        s.setLng(m.getPosition().longitude);
        return s;
    }

}
