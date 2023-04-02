package com.klaczynski.travelscoutr.io;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.klaczynski.travelscoutr.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;

public class Searcher {

    RequestQueue queue;
    public static final String TAG = "Searcher";

    public Searcher(Context context) {
        queue = Volley.newRequestQueue(context);
    }

    public LatLng goToPlace(String query, GoogleMap map) {
        final double[] lat = new double[1];
        final double[] lng = new double[1];
        try {
            String requestURL = "https://api.tomtom.com/search/2/search/" +
                    URLEncoder.encode(query) +
                    ".json?key=" + Constants.TOMTOMKEY;

            StringRequest stringRequest = new StringRequest(Request.Method.GET, requestURL,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                JSONObject resultJson = new JSONObject(response);
                                JSONArray searchResultList = resultJson.getJSONArray("results");
                                JSONObject position = searchResultList.getJSONObject(0).getJSONObject("position");
                                lat[0] = position.getDouble("lat");
                                lng[0] = position.getDouble("lon");
                                Log.d(TAG, "onResponse: latlong: "+lat[0]+" "+lng[0]);

                                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(lat[0], lng[0]), (float) 10.0);
                                map.animateCamera(cameraUpdate);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "onErrorResponse: " + error.toString());
                }
            });
            queue.add(stringRequest);

        } catch (Exception e) {
            Log.e(TAG, "firstResultFromQuery: ", e);
        }
            return new LatLng(lat[0], lng[0]);
    }
}
