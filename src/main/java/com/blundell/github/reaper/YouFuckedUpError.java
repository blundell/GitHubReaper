package com.blundell.github.reaper;

public class YouFuckedUpError extends RuntimeException {
    public YouFuckedUpError(int errorCode) {
        super("Wtf did you do know, only code " + errorCode + " knows");
    }

    public YouFuckedUpError(String error) {
        super("Wtf did you do know. " + error);
    }
}
