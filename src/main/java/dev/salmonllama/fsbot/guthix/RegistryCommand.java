package dev.salmonllama.fsbot.guthix;

class RegistryCommand {
    private String command;
    private String[] args;


    RegistryCommand(String command, String[] args) {
        this.command = command;
        this.args = args;
    }

    String getCommand() {
        return command;
    }

    String[] getArgs() {
        return args;
    }
}
