package com.treinchauffeur.travelscoutr.net;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.treinchauffeur.travelscoutr.Credentials;
import com.treinchauffeur.travelscoutr.MapsActivity;
import com.treinchauffeur.travelscoutr.ui.UserInterfaceHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;

public class PlacesSearcher {

    RequestQueue queue;
    public static final String TAG = "Searcher";
    MapsActivity activity;
    UserInterfaceHandler uiHandler;



    public PlacesSearcher(Context context, MapsActivity activity) {
        queue = Volley.newRequestQueue(context);
        this.activity = activity;
        uiHandler = new UserInterfaceHandler(activity);
    }

    public void goToPlace(String query, GoogleMap map) {
        uiHandler.setIsLoading(true);
        final double[] lat = new double[1];
        final double[] lng = new double[1];
        try {
            String requestURL = "https://api.tomtom.com/search/2/search/" +
                    URLEncoder.encode(query) +
                    ".json?key=" + Credentials.TOMTOMKEY;

            StringRequest stringRequest = new StringRequest(Request.Method.GET, requestURL,
                    response -> {
                        try {
                            JSONObject resultJson = new JSONObject(response);
                            JSONArray searchResultList = resultJson.getJSONArray("results");
                            JSONObject position = searchResultList.getJSONObject(0).getJSONObject("position");
                            lat[0] = position.getDouble("lat");
                            lng[0] = position.getDouble("lon");
                            Log.d(TAG, "onResponse: latlong: "+lat[0]+" "+lng[0]);

                            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(lat[0], lng[0]), (float) 14.0);
                            map.animateCamera(cameraUpdate);
                            uiHandler.setIsLoading(false);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            uiHandler.setIsLoading(false);
                        }
                    }, error -> Log.e(TAG, "onErrorResponse: " + error.toString()));
            queue.add(stringRequest);
        } catch (Exception e) {
            Log.e(TAG, "firstResultFromQuery: ", e);
            uiHandler.setIsLoading(false);
        }
    }
}
