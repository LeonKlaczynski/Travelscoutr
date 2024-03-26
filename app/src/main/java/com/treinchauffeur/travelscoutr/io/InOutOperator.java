package com.treinchauffeur.travelscoutr.io;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.Toast;

import com.treinchauffeur.travelscoutr.*;
import com.treinchauffeur.travelscoutr.obj.Spot;
import com.treinchauffeur.travelscoutr.ui.UserInterfaceHandler;

import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class InOutOperator {

    static String TAG = "InOutOperations";
    public ArrayList<Spot> spots = new ArrayList<Spot>();

    private MapsActivity activity;
    UserInterfaceHandler uiHandler;

    public InOutOperator(MapsActivity activity) {
        this.activity = activity;
        uiHandler = new UserInterfaceHandler(activity);
    }

    /**
     * Downloads the original json data from locationscout.net & converts it all to Spot objects.
     * @throws JSONException
     */
    public void startConversion() throws JSONException {
        uiHandler.setIsLoading(true);
        InputStream in;
        try {
            Logger.log(TAG, "Downloading JSON from: "+ Constants.LOCATIONSCOUT_DATA_URL);
            in = new URL(Constants.LOCATIONSCOUT_DATA_URL).openStream();
            DataInputStream dis = new DataInputStream(in);
            byte[] buffer = new byte[1024];
            int length;
            String filename = "mapdata.json";
            FileOutputStream fos = activity.openFileOutput(filename, Context.MODE_PRIVATE);
            while ((length = dis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        } catch (Exception e) {
            e.printStackTrace();
            uiHandler.sendToast("Failed to load new mapdata, using offline (old) version if available.");
        }

        JSONArray array = null;
        try {
            array = new JSONArray(FileReader.jsonData("mapdata.json"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        assert array != null;
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
        uiHandler.setIsLoading(false);
        Logger.log(TAG, "Successfully loaded " + spots.size() + " objects!");
        //convert(); We don't care about geoJSON for now
    }

    /**
     * Converts locationscout map objects to geoJson
     * @throws JSONException
     */
    private void convert() throws JSONException {
        if (spots.size() == 0) {
            Logger.log(TAG, "ERROR, array size is 0?");
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
