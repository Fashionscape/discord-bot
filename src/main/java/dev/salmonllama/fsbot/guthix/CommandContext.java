package dev.salmonllama.fsbot.guthix;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.beans.factory.annotation.Autowired;

import dev.salmonllama.fsbot.config.BotProperties;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class CommandContext {
    private MessageCreateEvent event;
    private DiscordApi api;
    private Message message;
    private MessageAuthor author;
    private TextChannel channel;
    private Optional<Server> server;
    private Command usedCommand;
    private String usedAlias;
    private String[] args;

    @Autowired 
    BotProperties properties;

    private CommandContext(CommandContextBuilder builder) {
        this.event = builder.event;
        this.api = builder.api;
        this.message = builder.message;
        this.author = builder.author;
        this.channel = builder.channel;
        this.server = builder.server;
        this.usedCommand = builder.usedCommand;
        this.usedAlias = builder.usedAlias;
        this.args = builder.args;
    }

    public DiscordApi getApi() {
        return api;
    }

    public Message getMessage() {
        return message;
    }

    public MessageAuthor getAuthor() {
        return author;
    }

    public TextChannel getChannel() {
        return channel;
    }

    public Optional<Server> getServer() {
        return server;
    }

    public Command getUsedCommand() {
        return usedCommand;
    }

    public String getUsedAlias() {
        return usedAlias;
    }

    public String[] getArgs() {
        return args;
    }

    public User getUser() {
        if (author.asUser().isPresent()) {
            return author.asUser().get();
        } else {
            // Log something to discord, I dunno
        }
        return null;
    }

    public Optional<Collection<Role>> getUserRoles() {
        User user = getUser();
        if (getServer().isPresent()) {
            return Optional.of(user.getRoles(getServer().get()));
        }
        return Optional.empty();
    }

    public boolean isUserOwner() {
        return getUser().getId() == (properties.discord().ownerId());
    }

    public boolean isPrivateMessage() {
        return event.isPrivateMessage();
    }

    public CompletableFuture<Message> reply(String msg) {
        return channel.sendMessage(msg);
    }

    public CompletableFuture<Message> reply(EmbedBuilder embed) {
        return channel.sendMessage(embed);
    }

    public CompletableFuture<Message> reply(EmbedBuilder... embeds) {
        return channel.sendMessage(embeds);
    }

    public CompletableFuture<Message> reply(String content, EmbedBuilder... embeds) {
        return channel.sendMessage(content, embeds);
    }

    public static class CommandContextBuilder {
        private MessageCreateEvent event;
        private DiscordApi api;
        private Message message;
        private MessageAuthor author;
        private TextChannel channel;
        private Optional<Server> server;
        private Command usedCommand;
        private String usedAlias;
        private String[] args;

        public CommandContextBuilder(
                MessageCreateEvent event,
                Command usedCommand,
                String usedAlias,
                String[] args
        ) {
            this.event = event;
            this.api = event.getApi();
            this.message = event.getMessage();
            this.author = event.getMessageAuthor();
            this.channel = event.getChannel();
            this.server = event.getServer();
            this.usedCommand = usedCommand;
            this.usedAlias = usedAlias;
            this.args = args;
        }

        public CommandContext build() {
            return new CommandContext(this);
        }
    }
}
