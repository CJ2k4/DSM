package com.DSM.Platform.federation;

/** Thrown when a peer server can't be reached or returns an invalid response. */
public class FederationClientException extends RuntimeException {

    public FederationClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public FederationClientException(String message) {
        super(message);
    }
}
