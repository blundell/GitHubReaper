package com.blundell.github.reaper;

public class InputValidator {

    public void validate(String[] args) {
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

    public Input retrieve(String[] args) {
        return new Input(getCredentials(args), getRepository(args));
    }

    private static Credentials getCredentials(String[] args) {
        String username = args[0];
        String password = args[1];
        return Credentials.generate(username, password);
    }

    private static Repository getRepository(String[] args) {
        String owner = args[2];
        String repoName = args[3];
        return new Repository(owner, repoName);
    }

    public static class Input {
        Credentials credentials;
        Repository repository;

        public Input(Credentials credentials, Repository repository) {
            this.credentials = credentials;
            this.repository = repository;
        }

        public Credentials getCredentials() {
            return credentials;
        }

        public Repository getRepository() {
            return repository;
        }
    }

}
