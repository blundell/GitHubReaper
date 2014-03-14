package com.blundell.github.reaper;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.squareup.okhttp.OkHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.squareup.okhttp.internal.Util.UTF_8;

/**
 * http://developer.github.com/v3/pulls/#list-pull-requests
 * http://developer.github.com/v3/#pagination
 * http://developer.github.com/v3/#rate-limiting
 * http://developer.github.com/v3/pulls/#get-a-single-pull-request
 */
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
        URL url = getNextPullRequestIdsPageUrl(args);
        List<GsonPullRequestId> ids = new ArrayList<GsonPullRequestId>();
        getPullRequestIds(args, url, ids);

        for (GsonPullRequestId requestId : ids) {
            HttpURLConnection connection = connectForPullRequest(args, requestId.number);
            GsonPullRequest request = parsePullRequest(connection);

            String body = request.body;
            if (body.contains("![")) {
                List<String> urls = pullGitHubUploadedImageLinks(body);
                System.out.println(request.number + ":" + urls);
                if (urls.isEmpty()) {
                    System.err.println(body);
                }
            }
        }
    }

    private static List<String> pullGitHubUploadedImageLinks(String text) {
        List<String> links = new ArrayList<String>();

        String regex = "\\(?\\b(https://|www[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(text);
        while (m.find()) {
            String urlStr = m.group();
            if (urlStr.startsWith("(") && urlStr.endsWith(")")) {
                urlStr = urlStr.substring(1, urlStr.length() - 1);
            }
            links.add(urlStr);
        }
        return links;
    }

    private static URL getNextPullRequestIdsPageUrl(String[] args) throws IOException {
        HttpURLConnection connection = connectForPullRequestIds(args, null);
        return getNextPageUrl(connection);
    }

    private static HttpURLConnection connectForPullRequestIds(String[] args, URL newUrl) throws IOException {
        return connectFor(args, newUrl, "/pulls?state=all");
    }

    private static HttpURLConnection connectForPullRequest(String[] args, final int pullRequestNumber) throws IOException {
        return connectFor(args, null, "/pulls/" + pullRequestNumber);
    }

    private static HttpURLConnection connectFor(String[] args, URL newUrl, String segment) throws IOException {
        URL url = newUrl;
        if (url == null) {
            url = new URL("https://api.github.com/repos/" + args[2] + "/" + args[3] + segment);
        }
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

    private static void getPullRequestIds(String[] args, URL url, List<GsonPullRequestId> pullRequests) throws IOException {
        HttpURLConnection connection = connectForPullRequestIds(args, url);
        pullRequests.addAll(parsePullRequestIds(connection));
        URL nextPageUrl;
        try {
            nextPageUrl = getNextPageUrl(connection);
        } catch (MalformedURLException e) {
            return;
        }
        getPullRequestIds(args, nextPageUrl, pullRequests);
    }

    private static List<GsonPullRequestId> parsePullRequestIds(HttpURLConnection connection) throws IOException {
        InputStream inputStream = connection.getInputStream();
        String output = new Scanner(inputStream).useDelimiter("\\A").next();

        return Arrays.asList(new Gson().fromJson(output, GsonPullRequestId[].class));
    }

    static class GsonPullRequestId {
        @SerializedName("number")
        private int number;
    }

    private static GsonPullRequest parsePullRequest(HttpURLConnection connection) throws IOException {
        InputStream inputStream = connection.getInputStream();
        String output = new Scanner(inputStream).useDelimiter("\\A").next();

        return new Gson().fromJson(output, GsonPullRequest.class);
    }

    static class GsonPullRequest {
        @SerializedName("number")
        private int number;
        @SerializedName("body")
        private String body;

    }
}
