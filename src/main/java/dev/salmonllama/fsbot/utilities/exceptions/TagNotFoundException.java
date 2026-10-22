package dev.salmonllama.fsbot.utilities.exceptions;

public class TagNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    @Override
    public String getMessage() {
        return "That tag was not found, check your spelling and try again.";
    }
}
