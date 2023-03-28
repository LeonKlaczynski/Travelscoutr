package com.klaczynski.travelscoutr.io;

import com.klaczynski.travelscoutr.MapsActivity;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Reader {

    public static String jsonData(String inputFile) throws FileNotFoundException {
        FileInputStream fis = MapsActivity.context.openFileInput("mapdata.json");
        InputStreamReader inputStreamReader =
                new InputStreamReader(fis, StandardCharsets.UTF_8);
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
            String line = reader.readLine();
            while (line != null) {
                stringBuilder.append(line).append('\n');
                line = reader.readLine();
            }
        } catch (IOException e) {
            // Error occurred when opening raw file for reading.
        } finally {
            return stringBuilder.toString();
        }
    }

}
