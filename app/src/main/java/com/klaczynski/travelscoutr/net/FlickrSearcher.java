package com.klaczynski.travelscoutr.net;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.PhotoList;
import com.flickr4java.flickr.photos.PhotosInterface;
import com.flickr4java.flickr.photos.SearchParameters;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterManager;
import com.klaczynski.travelscoutr.Constants;
import com.klaczynski.travelscoutr.R;
import com.klaczynski.travelscoutr.obj.ClusterMarker;
import com.klaczynski.travelscoutr.obj.Spot;

import java.util.ArrayList;
import java.util.Collections;

public class FlickrSearcher {

    static Flickr flickr;
    private Context context;
    public static final String TAG = "FlickrSearcher";

    ArrayList<ClusterMarker> flickrMarkers = new ArrayList<>();

    public FlickrSearcher(Context context) {
        this.context = context;
        flickr = new Flickr(context.getString(R.string.flickrApiKey), context.getString(R.string.flickrApiSecret), new REST());
        Log.d(TAG, "FlickrSearcher: "+context.getString(R.string.flickrApiKey));
    }

    /**
     * Searches flickr images that are currently located on the viewable map
     * @param clusterManager The cluster manager used on the map, used to add search results to the cluster and therefore the map.
     * @param map The displayed map, used to retrieve the current viewport (we're searching flickr using a bounding box).
     */
    public void performSearch(GoogleMap map, ClusterManager clusterManager) throws FlickrException {
        double maxLat, minLat, maxLong, minLong;
        maxLat = map.getProjection().getVisibleRegion().latLngBounds.northeast.latitude;
        maxLong = map.getProjection().getVisibleRegion().latLngBounds.northeast.longitude;
        minLat = map.getProjection().getVisibleRegion().latLngBounds.southwest.latitude;
        minLong = map.getProjection().getVisibleRegion().latLngBounds.southwest.longitude;

        PhotosInterface photosInterface = flickr.getPhotosInterface();
        SearchParameters parameters = new SearchParameters();
        parameters.setExtras(Collections.singleton("geo"));
        parameters.setBBox(String.valueOf(minLong), String.valueOf(minLat), String.valueOf(maxLong), String.valueOf(maxLat));
        PhotoList photoList = photosInterface.search(parameters, 250, 1);

        ArrayList<ClusterMarker> markersToAdd = new ArrayList<>();
        for(Photo photo : (ArrayList<Photo>) photoList) {
            if(photo.hasGeoData()) {
                Spot s = new Spot();
                s.setName(photo.getTitle());
                s.setUrl("http://www.flickr.com/" + photo.getOwner().getId() + "/" + photo.getId());
                s.setImgurl(photo.getSmall320Url());
                s.setLat(photo.getGeoData().getLatitude());
                s.setLng(photo.getGeoData().getLongitude());

                ClusterMarker marker = new ClusterMarker(new LatLng(s.getLat(), s.getLng()), s);
                marker.setSnippet(marker.getSnippet() + Constants.FLICKR_STRING);
                markersToAdd.add(marker);
            }
        }
        Toast.makeText(context, "Displaying " + markersToAdd.size() + " photos of total " + photoList.getTotal() + " flickr search results!", Toast.LENGTH_LONG).show();
        clusterManager.addItems(markersToAdd);
        flickrMarkers.addAll(markersToAdd);
        clusterManager.onCameraIdle();

        flickrMarkers.addAll(markersToAdd);
    }
}
