package com.blundell.github.reaper;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.squareup.okhttp.OkHttpClient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.squareup.okhttp.internal.Util.UTF_8;

public class Reaper {

    void reapImages(String username, String password, String owner, String repo) throws IOException {
        URL url = getNextPullRequestIdsPageUrl(username, password, owner, repo);
        List<GsonPullRequestId> ids = new ArrayList<GsonPullRequestId>();
        getPullRequestIds(username, password, owner, repo, url, ids);

        getGitHubLoadedImagesFrom(username, password, owner, repo, ids);
    }

    private static URL getNextPullRequestIdsPageUrl(String username, String password, String owner, String repo) throws IOException {
        HttpURLConnection connection = connectForPullRequestIds(username, password, owner, repo, null);
        return getNextPageUrl(connection);
    }

    private static HttpURLConnection connectForPullRequestIds(String username, String password, String owner, String repo, URL newUrl) throws IOException {
        return connectFor(username, password, owner, repo, newUrl, "/pulls?state=all");
    }

    private static HttpURLConnection connectForPullRequest(String username, String password, String owner, String repo, final int pullRequestNumber) throws IOException {
        return connectFor(username, password, owner, repo, null, "/pulls/" + pullRequestNumber);
    }

    private static HttpURLConnection connectFor(String username, String password, String owner, String repo, URL newUrl, String segment) throws IOException {
        URL url = newUrl;
        if (url == null) {
            url = new URL("https://api.github.com/repos/" + owner + "/" + repo + segment);
        }
        OkHttpClient client = new OkHttpClient();
        HttpURLConnection connection = client.open(url);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/vnd.github.beta+json");
        String hash = new String(Base64Coder.encode((username + ":" + password).getBytes(UTF_8)));
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

    private static void getPullRequestIds(String username, String password, String owner, String repo, URL url, List<GsonPullRequestId> pullRequests) throws IOException {
        HttpURLConnection connection = connectForPullRequestIds(username, password, owner, repo, url);
        pullRequests.addAll(parsePullRequestIds(connection));
        URL nextPageUrl;
        try {
            nextPageUrl = getNextPageUrl(connection);
        } catch (MalformedURLException e) {
            return;
        }
        getPullRequestIds(username, password, owner, repo, nextPageUrl, pullRequests);
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

    private static void getGitHubLoadedImagesFrom(String username, String password, String owner, String repo, List<GsonPullRequestId> ids) throws IOException {
        int dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        File output = new File("output/" + dayOfMonth);
        if (!output.exists()) {
            boolean mkdir = output.mkdir();
            if (!mkdir) {
                throw new YouFuckedUpError("couldn't make output folder");
            }
        }
        for (GsonPullRequestId requestId : ids) {
            HttpURLConnection connection = connectForPullRequest(username, password, owner, repo, requestId.number);
            GsonPullRequest request = parsePullRequest(connection);

            String body = request.body;
            if (body.contains("![")) {
                List<URL> urls = pullGitHubUploadedImageLinks(body);

                if (urls.isEmpty()) {
                    System.err.println("Failed to regex the images for " + body);
                }

                for (URL url : urls) {
                    if (!url.toString().contains(".png")) {
                        System.err.println("Url is not a png: " + url);
                        continue;
                    }
                    BufferedImage image = ImageIO.read(url);
                    if (image == null) {
                        System.err.println("Failed to load image for " + url);
                    }
                    File outputFile = new File("output/" + dayOfMonth + url.getPath().substring(url.getPath().lastIndexOf("/")));
                    ImageIO.write(image, "png", outputFile);
                }

                System.out.println(request.number + ":" + urls);
            }
        }
    }

    private static List<URL> pullGitHubUploadedImageLinks(String text) throws MalformedURLException {
        List<URL> links = new ArrayList<URL>();

        String regex = "\\(?\\b(https://)[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(text);
        while (m.find()) {
            String urlStr = m.group();
            if (urlStr.startsWith("(") && urlStr.endsWith(")")) {
                urlStr = urlStr.substring(1, urlStr.length() - 1);
            }
            links.add(new URL(urlStr));
        }
        return links;
    }
}
