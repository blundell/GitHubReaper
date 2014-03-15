package com.blundell.github.reaper;

import java.io.IOException;

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
        InputValidator.Input input = validateInput(args);
        Credentials credentials = input.getCredentials();
        Repository repository = input.getRepository();
        logNumberOfApiCallsAllowedRemaining(credentials, repository);
        reapImages(credentials, repository);
        logFinished();
    }

    private static InputValidator.Input validateInput(String[] args) {
        return new InputValidator().validate(args);
    }

    private static void logNumberOfApiCallsAllowedRemaining(Credentials credentials, Repository repository) throws IOException {
        new GitHubRateChecker(credentials, repository).checkRates();
    }

    private static void reapImages(Credentials credentials, Repository repository) throws IOException {
        new Reaper().reapImages(credentials, repository);
    }

    private static void logFinished() {
        System.out.println("Finished");
    }

}
