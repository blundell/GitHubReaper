package com.blundell.github.reaper;

import com.squareup.okhttp.OkHttpClient;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

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
        String username = args[0];
        String password = args[1];
        String owner = args[2];
        String repo = args[3];
        new Reaper().reapImages(username, password, owner, repo);
        System.out.println("Complete");
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

}
