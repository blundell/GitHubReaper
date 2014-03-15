package com.blundell.github.reaper;

import com.squareup.okhttp.OkHttpClient;

import java.io.IOException;
import java.net.HttpURLConnection;

public class GitHubRateChecker {

    private final Credentials credentials;
    private final Repository repository;

    public GitHubRateChecker(Credentials credentials, Repository repository) {
        this.credentials = credentials;
        this.repository = repository;
    }

    public void checkRates() throws IOException {
        OkHttpClient client = new OkHttpClient();
        HttpURLConnection connection = client.open(repository.asUsers());
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/vnd.github.beta+json");
        String hash = credentials.getHashed();
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

}
