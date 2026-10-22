package dev.salmonllama.fsbot.exceptions;

import java.sql.SQLException;

public class UnknownParameterException extends SQLException {
    private static final long serialVersionUID = 1L;
    private String message;

    public UnknownParameterException(Object param) {
        message = "Unknown parameter type: " + param;
    }

    public UnknownParameterException(Object param, int index) {
        message = String.format("Unknown parameter type %s at %d", param, index);
    }

    @Override
    public String toString() {
        return message;
    }
}
