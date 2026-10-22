package dev.salmonllama.fsbot.utilities.exceptions;

public class OutfitNotFoundException extends Exception {
    private static final long serialVersionUID = 1L;

    @Override
    public String getMessage() {
        return "An outfit with that ID was not found";
    }
}
