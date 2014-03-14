package com.blundell.github.reaper;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.squareup.okhttp.OkHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static com.squareup.okhttp.internal.Util.UTF_8;

public class Main {

    /**
     * @param args username password 'github user/org' 'github repo'
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        validate(args);
        checkRates(args);
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

    private static void checkRates(String[] args) throws IOException {
        URL url = new URL("https://api.github.com/users/" + args[2]);
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
        System.out.println("Date:" + connection.getHeaderField("Date"));
        System.out.println("X-RateLimit-Limit:" + connection.getHeaderField("X-RateLimit-Limit"));
        System.out.println("X-RateLimit-Remaining:" + connection.getHeaderField("X-RateLimit-Remaining"));
        System.out.println("X-RateLimit-Reset:" + connection.getHeaderField("X-RateLimit-Reset"));
    }

    private static void doYourThing(String[] args) throws IOException {
        URL url = getNextPullRequestPageUrl(args);
        List<GsonPullRequest> pullRequests = getPullRequests(args, url);

        for (GsonPullRequest request : pullRequests) {
            System.out.println("PR: " + request.getNumber());
        }
    }

    private static URL getNextPullRequestPageUrl(String[] args) throws IOException {
        HttpURLConnection connection = getPullRequests(args);
        return getNextPageUrl(connection);
    }

    private static HttpURLConnection getPullRequests(String[] args) throws IOException {
        URL url = new URL("https://api.github.com/repos/" + args[2] + "/" + args[3] + "/pulls?state=all");
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
        return connection;
    }

    private static URL getNextPageUrl(HttpURLConnection connection) throws MalformedURLException {
        String linkHeader = connection.getHeaderField("Link");
        PageLinks pageLinks = new PageLinks(linkHeader);
        return new URL(pageLinks.getNext());
    }

    private static List<GsonPullRequest> getPullRequests(String[] args, URL url) throws IOException {
        HttpURLConnection connection = getPullRequests(args);
        return parsePullRequests(connection);
    }

    private static List<GsonPullRequest> parsePullRequests(HttpURLConnection connection) throws IOException {
        InputStream inputStream = connection.getInputStream();
        String output = new Scanner(inputStream).useDelimiter("\\A").next();

        return Arrays.asList(new Gson().fromJson(output, GsonPullRequest[].class));
    }

    static class GsonPullRequest {
        @SerializedName("number")
        private int number;

        public int getNumber() {
            return number;
        }
    }
}
