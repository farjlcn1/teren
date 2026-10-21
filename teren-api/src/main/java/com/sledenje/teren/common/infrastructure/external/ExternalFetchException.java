package com.sledenje.teren.common.infrastructure.external;

/**
 * Zunanji sistem ni vrnil uporabnega odgovora. Konteksti jo pretvorijo v svojo domensko napako.
 */
public class ExternalFetchException extends RuntimeException {

    private final String reason;

    /**
     * @param reason kratek razlog brez internih podrobnosti (npr. "HTTP 500"), primeren za prikaz administratorju
     */
    public ExternalFetchException(String reason) {
        super(reason);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

}
