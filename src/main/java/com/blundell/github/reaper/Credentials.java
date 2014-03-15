package com.blundell.github.reaper;

import static com.squareup.okhttp.internal.Util.UTF_8;

public class Credentials {
    private final String username;
    private final String hashedCredentials;

    public static Credentials generate(String username, String password) {
        String hashedCredentials = new String(Base64Coder.encode((username + ":" + password).getBytes(UTF_8)));
        return new Credentials(username, hashedCredentials);
    }

    private Credentials(String username, String hashedCredentials) {
        this.username = username;
        this.hashedCredentials = hashedCredentials;
    }

    public String getUsername() {
        return username;
    }

    public String getHashed() {
        return hashedCredentials;
    }
}
