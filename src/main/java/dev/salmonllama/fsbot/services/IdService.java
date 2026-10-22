package dev.salmonllama.fsbot.services;

import java.util.UUID;

public class IdService {

    public static String generateShort() {
        return UUID.randomUUID().toString().split("-")[0];
    }

    public static String generate() {
        return UUID.randomUUID().toString();
    }
}
