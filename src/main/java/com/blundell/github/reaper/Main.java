package com.blundell.github.reaper;

import com.squareup.okhttp.OkHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import static com.squareup.okhttp.internal.Util.UTF_8;

public class Main {

    /**
     * @param args username password 'github user/org' 'github repo'
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        validate(args);
        doYourThing(args);

    }

    private static void validate(String[] args) {
        if (args.length == 0) {
            throw new YouFuckedUpError("Argument 1 needs to be your username");
        }
        if (args.length == 1) {
            throw new YouFuckedUpError("Argument 2 needs to be your password");
        }
        if (args.length == 2) {
            throw new YouFuckedUpError("Argument 3 needs to be a github user/org name");
        }
        if (args.length == 3) {
            throw new YouFuckedUpError("Argument 4 needs to be a github repo name");
        }
    }

    private static void doYourThing(String[] args) throws IOException {
        URL url = new URL("https://api.github.com/repos/" + args[2] + "/" + args[3] + "/pulls?state=closed");
        OkHttpClient client = new OkHttpClient();
        HttpURLConnection connection = client.open(url);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/vnd.github.beta+json");
        String hash = new String(Base64Coder.encode((args[0] + ":" + args[1]).getBytes(UTF_8)));
        connection.setRequestProperty("Authorization", "Basic " + hash);
        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new YouFuckedUpError(responseCode);
        }
        InputStream inputStream = connection.getInputStream();
        String output = new Scanner(inputStream).useDelimiter("\\A").next();
        System.out.println(output);
    }
}
