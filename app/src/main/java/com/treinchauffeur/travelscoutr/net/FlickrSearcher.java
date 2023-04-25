package com.treinchauffeur.travelscoutr.net;

import android.content.Context;
import android.util.Log;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.PhotoList;
import com.flickr4java.flickr.photos.PhotosInterface;
import com.flickr4java.flickr.photos.SearchParameters;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.clustering.ClusterManager;
import com.treinchauffeur.travelscoutr.Constants;
import com.treinchauffeur.travelscoutr.MapsActivity;
import com.treinchauffeur.travelscoutr.R;
import com.treinchauffeur.travelscoutr.obj.ClusterMarker;
import com.treinchauffeur.travelscoutr.obj.Spot;
import com.treinchauffeur.travelscoutr.ui.UserInterfaceHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class FlickrSearcher {

    static Flickr flickr;
    public static final String TAG = "FlickrSearcher";

    MapsActivity activity;
    UserInterfaceHandler uiHandler;

    ArrayList<ClusterMarker> flickrMarkers = new ArrayList<>();

    double maxLat;
    double minLat;
    double maxLong;
    double minLong;

    public FlickrSearcher(Context context, MapsActivity activity) {
        this.activity = activity;
        uiHandler = new UserInterfaceHandler(activity);
        flickr = new Flickr(context.getString(R.string.flickrApiKey), context.getString(R.string.flickrApiSecret), new REST());
        Log.d(TAG, "FlickrSearcher: " + context.getString(R.string.flickrApiKey));
    }

    /**
     * Searches flickr images that are currently located on the viewable map
     *
     * @param clusterManager The cluster manager used on the map, used to add search results to the cluster and therefore the map.
     * @param latLngBounds The currently viewed bounding box as latlongs.
     */
    public void performSearch(ClusterManager clusterManager, LatLngBounds latLngBounds) {
        Thread thread = new Thread(() -> {
            try {
                uiHandler.setIsLoading(true);
                uiHandler.sendToast("Searching Flickr..");

                maxLat = latLngBounds.northeast.latitude;
                maxLong = latLngBounds.northeast.longitude;
                minLat = latLngBounds.southwest.latitude;
                minLong = latLngBounds.southwest.longitude;

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
                            if (hasDuplicates) break;
                        }
                        if (!hasDuplicates) markersToAdd.add(marker);

                    }
                }
                if (hasDuplicates && markersToAdd.size() == 0)
                    uiHandler.sendToast("No new search results were found!");
                if (hasDuplicates)
                    uiHandler.sendToast("Updating map with " + markersToAdd.size() + " added search results!");
                if (!hasDuplicates && markersToAdd.size() < 250)
                    uiHandler.sendToast("Adding " + markersToAdd.size() + " flickr search results!");
                else
                    uiHandler.sendToast("Displaying " + markersToAdd.size() + " photos of total " + photoList.getTotal() + " flickr search results!");

                activity.runOnUiThread(() -> {
                    boolean b = clusterManager.addItems(markersToAdd);
                    clusterManager.cluster();
                    flickrMarkers.addAll(markersToAdd);
                    clusterManager.onCameraIdle();
                    uiHandler.setIsLoading(false);
                });
            } catch (Exception e) {
                Log.e(TAG, "performSearch: "+e, e);
                uiHandler.setIsLoading(false);
                if(e.toString().contains("IP address"))
                    uiHandler.sendToast("An error has occurred! Please check internet connectivity.");
                else uiHandler.sendToast("An error has occurred!");
            }
        });
        thread.start();
    }


}
