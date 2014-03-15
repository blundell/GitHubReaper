package com.blundell.github.reaper;

import java.net.MalformedURLException;
import java.net.URL;

public class Repository {
    private final String owner;
    private final String repo;

    public Repository(String owner, String repo) {
        this.owner = owner;
        this.repo = repo;
    }

    private String asPath() {
        return owner + "/" + repo;
    }

    public URL asReposWithAppended(String segment) {
        try {
            return new URL("https://api.github.com/repos/" + asPath() + segment);
        } catch (MalformedURLException e) {
            throw new YouFuckedUpError(e);
        }
    }

    public URL asUsers() {
        try {
            return new URL("https://api.github.com/users/" + owner);
        } catch (MalformedURLException e) {
            throw new YouFuckedUpError(e);
        }
    }
}
