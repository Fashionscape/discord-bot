package dev.salmonllama.fsbot.services;

import java.util.UUID;

public class IdService {

    public static String generate() {
        return UUID.randomUUID().toString();
    }
}
