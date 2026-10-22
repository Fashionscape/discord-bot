package dev.salmonllama.fsbot.guthix;


import java.util.*;
import java.util.function.Predicate;

class Registry {
    private Collection<Command> commands;
    private HashMap<String, Command> commandsMap;

    Registry() {
        commands = new ArrayList<>();
        commandsMap = new HashMap<>();
    }

    Predicate<String> valueMatch(String input) {
        return str -> str.equals(input);
    }

    Command addCommand(Command cmd) {
        commands.add(cmd);
        commandsMap.put(cmd.name(), cmd);
        return cmd;
    }

    Collection<Command> listCommands() {
        return commands;
    }

    HashMap<String, Command> mapCommands() {
        return commandsMap;
    }

    boolean startsWithPrefix(String input) {
        return input.startsWith("~");
    }

    Collection<String> getCommandAliases() {
        Collection<String> aliases = new ArrayList<>();

        commands.forEach(cmd -> {
            aliases.addAll(cmd.aliases());
        });

        return aliases;
    }

    Optional<Command> findCommand(String alias) {
        for (Command cmd: commands) {
            if (cmd.aliases().contains(alias)) {
                return Optional.of(cmd);
            }
        }
        return Optional.empty();
    }

    boolean isCommandAlias(String input) {
        return getCommandAliases().stream().anyMatch(valueMatch(input));
    }

    String removePrefix(String input) {
        int startPoint = "~".length();
        return input.substring(startPoint);
    }

    List<String> splitArgs(String input) {
        if (input.contains(" ")) {
            input = removePrefix(input);
            String[] splits = input.split(" ");
            return cleanSpaces(splits);
        } else {
            input = removePrefix(input);
            return new ArrayList<>(Collections.singletonList(input));
        }
    }

    List<String> cleanSpaces(String[] input) {
        List<String> list = new ArrayList<>(Arrays.asList(input));
        list.removeIf(""::equals);
        return list;
    }

    RegistryCommand getCommandInfo(String input) {
        List<String> args = splitArgs(input);
        String command = args.get(0);
        args.remove(0);

        return new RegistryCommand(command, args.toArray(new String[0]));
    }
}
