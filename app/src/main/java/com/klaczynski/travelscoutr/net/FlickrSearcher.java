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
import java.util.Objects;

public class FlickrSearcher {

    static Flickr flickr;
    private Context context;
    public static final String TAG = "FlickrSearcher";
    private Toast toast;
    ArrayList<Toast> activeToasts = new ArrayList<>();

    ArrayList<ClusterMarker> flickrMarkers = new ArrayList<>();

    public FlickrSearcher(Context context) {
        this.context = context;
        toast = new Toast(context);
        flickr = new Flickr(context.getString(R.string.flickrApiKey), context.getString(R.string.flickrApiSecret), new REST());
        Log.d(TAG, "FlickrSearcher: " + context.getString(R.string.flickrApiKey));
    }

    /**
     * Searches flickr images that are currently located on the viewable map
     *
     * @param clusterManager The cluster manager used on the map, used to add search results to the cluster and therefore the map.
     * @param map            The displayed map, used to retrieve the current viewport (we're searching flickr using a bounding box).
     */
    public void performSearch(GoogleMap map, ClusterManager clusterManager) throws FlickrException {
        double maxLat, minLat, maxLong, minLong;
        for (Toast toast : activeToasts) toast.cancel();
        maxLat = map.getProjection().getVisibleRegion().latLngBounds.northeast.latitude;
        maxLong = map.getProjection().getVisibleRegion().latLngBounds.northeast.longitude;
        minLat = map.getProjection().getVisibleRegion().latLngBounds.southwest.latitude;
        minLong = map.getProjection().getVisibleRegion().latLngBounds.southwest.longitude;

        PhotosInterface photosInterface = flickr.getPhotosInterface();
        SearchParameters parameters = new SearchParameters();
        parameters.setExtras(Collections.singleton("geo"));
        parameters.setBBox(String.valueOf(minLong), String.valueOf(minLat), String.valueOf(maxLong), String.valueOf(maxLat));
        PhotoList<Photo> photoList = photosInterface.search(parameters, 250, 1);

        ArrayList<ClusterMarker> markersToAdd = new ArrayList<>();
        boolean hasDuplicates = false;
        for (Photo photo : photoList) {
            if (photo.hasGeoData()) {
                Spot s = new Spot();
                s.setName(photo.getTitle());
                s.setUrl("http://www.flickr.com/" + photo.getOwner().getId() + "/" + photo.getId());
                s.setImgurl(photo.getSmall320Url());
                s.setLat(photo.getGeoData().getLatitude());
                s.setLng(photo.getGeoData().getLongitude());

                ClusterMarker marker = new ClusterMarker(new LatLng(s.getLat(), s.getLng()), s);
                marker.setSnippet(marker.getSnippet() + Constants.FLICKR_STRING);

                //Checking for dupes
                for (ClusterMarker toCompare : flickrMarkers) {
                    hasDuplicates = Objects.equals(toCompare.getSnippet(), marker.getSnippet());
                    if(hasDuplicates) break;
                }
                if(!hasDuplicates) markersToAdd.add(marker);

            }
        }
        if(hasDuplicates && markersToAdd.size() == 0) toast = Toast.makeText(context, "No new search results were found!", Toast.LENGTH_LONG);
        if(hasDuplicates) toast = Toast.makeText(context, "Updating map with " + markersToAdd.size() + " added search results!", Toast.LENGTH_LONG);
        if (!hasDuplicates && markersToAdd.size() < 250) toast = Toast.makeText(context, "Adding " + markersToAdd.size() + " flickr search results!", Toast.LENGTH_LONG);
        else toast = Toast.makeText(context, "Displaying " + markersToAdd.size() + " photos of total " + photoList.getTotal() + " flickr search results!", Toast.LENGTH_LONG);
        activeToasts.add(toast);
        toast.show();

        clusterManager.addItems(markersToAdd);
        flickrMarkers.addAll(markersToAdd);
        clusterManager.onCameraIdle();

        flickrMarkers.addAll(markersToAdd);
    }
}
