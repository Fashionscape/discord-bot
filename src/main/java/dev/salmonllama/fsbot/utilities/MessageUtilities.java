package dev.salmonllama.fsbot.utilities;

import org.javacord.api.entity.message.Message;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MessageUtilities {
    public static Consumer<Message> deleteAfter(long delay, TimeUnit timeUnit) {
        return msg -> msg.getApi().getThreadPool().getScheduler().schedule((Runnable) msg::delete, delay, timeUnit);
    }
}
