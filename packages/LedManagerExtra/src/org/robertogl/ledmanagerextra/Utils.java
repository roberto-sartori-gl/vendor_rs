package org.robertogl.ledmanagerextra;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

public final class Utils {
    protected static void setProp(String property, String value) {
        Process sh = null;
        String[] cmd = {"setprop", property, value};
        try {
            sh = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            sh.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected static String getProp(String property) {
        Process sh = null;
        BufferedReader reader = null;
        String[] cmd = {"getprop", property};
        try {
            sh = Runtime.getRuntime().exec(cmd);
            reader = new BufferedReader(new InputStreamReader(sh.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            return reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> int[] indexOfMultiple(List<T> list, T object) {
        return IntStream.range(0, list.size())
                .filter(i -> Objects.equals(object, list.get(i)))
                .toArray();
    }


    protected static String getStringFromFile (String filePath) throws IOException {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();
        return ret;
    }

    private static String convertStreamToString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        Boolean firstLine = true;
        while ((line = reader.readLine()) != null) {
            if(firstLine){
                sb.append(line);
                firstLine = false;
            } else {
                sb.append("\n").append(line);
            }
        }
        reader.close();
        return sb.toString();
    }

    protected static String removeLastChars(String str, int chars) {
        return str.substring(0, str.length() - chars);
    }
}
