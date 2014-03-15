package com.blundell.github.reaper;

public class YouFuckedUpError extends RuntimeException {
    public YouFuckedUpError(int errorCode) {
        super("Wtf did you do now, only code " + errorCode + " knows");
    }

    public YouFuckedUpError(String error) {
        super("Wtf did you do now. " + error);
    }

    public YouFuckedUpError(Exception e) {
        super("Wtf did you do now. ", e);
    }
}
