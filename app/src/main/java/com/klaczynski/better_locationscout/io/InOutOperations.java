package com.klaczynski.better_locationscout.io;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import com.klaczynski.better_locationscout.*;
import com.klaczynski.better_locationscout.obj.Spot;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class InOutOperations {

    /**
     * @author Leonk Attempts to download locationscout.net's JSON file & converts it to GeoJSON.
     */
    static String TAG = "InOutOperations";
    public static ArrayList<Spot> spots = new ArrayList<Spot>();

    /**
     *
     *
     */
    public static void startConversion() throws JSONException {

        InputStream in;
        try {
            Logger.log(TAG, "Downloading JSON from: "+Settings.URL);
            in = new URL(Settings.URL).openStream();
            DataInputStream dis = new DataInputStream(in);
            byte[] buffer = new byte[1024];
            int length;
            String filename = "mapdata.json";
            FileOutputStream fos = MapsActivity.context.openFileOutput(filename, Context.MODE_PRIVATE);
            while ((length = dis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(MapsActivity.context, "Failed to load new mapdata, using offline (old) version if available.", Toast.LENGTH_SHORT).show();
        }

        JSONArray array = null;
        try {
            array = new JSONArray(Reader.jsonData("mapdata.json"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Logger.log(TAG, "JSON Array size: " + array.length());
        for (int i = 0; i < array.length(); i++) {
            Spot spot = new Spot("INVALID_NAME", 0.00000, 0.00000, "INVALID_URL", "INVALID_URL");
            JSONObject jsonobject = array.getJSONObject(i);
            String name = jsonobject.getString("name");
            double lat = jsonobject.getDouble("lat");
            double lng = jsonobject.getDouble("lng");
            String imgurl = "https://images.locationscout.net/" + jsonobject.getString("image_m").split("\\?")[0];
            String url = "https://www.locationscout.net" + jsonobject.getString("url");

            spot.setName(name);
            spot.setLat(lat);
            spot.setLng(lng);
            spot.setImgurl(imgurl);
            spot.setUrl(url);

            if (!spot.getName().equals("INVALID_NAME"))
                spots.add(spot);

        }
        Logger.log(TAG, "Successfully loaded " + spots.size() + " objects!");
        //convert(); We don't care about geoJSON for now
    }

    private static void convert() throws JSONException {
        if (spots.size() == 0) {
            Logger.log(TAG, "ERROR, array size is 0");
            return;
        }
        JSONObject geoJSON = new JSONObject();
        geoJSON.put("type", "FeatureCollection");
        JSONArray convertedArray = new JSONArray();
        for (Spot s : spots) {
            JSONObject geoObj = new JSONObject();
            JSONObject geometryObj = new JSONObject();
            JSONArray latlong = new JSONArray();
            latlong.put(s.getLng());
            latlong.put(s.getLat());
            geometryObj.put("coordinates", latlong);
            geometryObj.put("type", "Point");

            JSONObject propsObj = new JSONObject();
            propsObj.put("name", s.getName());
            propsObj.put("URL", s.getUrl());
            propsObj.put("imgURL", s.getImgurl());

            geoObj.put("geometry", geometryObj);
            geoObj.put("type", "Feature");
            geoObj.put("properties", propsObj);

            convertedArray.put(geoObj);
            geoJSON.put("features", convertedArray);
        }

        try {
            String filename = "geoJSON.json";
            try (FileOutputStream fos = MapsActivity.context.openFileOutput(filename, Context.MODE_PRIVATE)) {
                fos.write(geoJSON.toString().getBytes(StandardCharsets.UTF_8));
            }

        } catch (IOException e) {
            Logger.log(TAG, e.toString());
        }

    }

}
