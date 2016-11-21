package jburg.debugger;

enum CommandType
{
    Analyze("Analyze an ad-hoc tree"),
    Echo("Echo a property"),
    Error,
    Execute("Execute dump-generating program"),
    Exit("Exit the debugger"),
    Help("Display help -- help <command name> for details"),
    PrintStackTrace("Print stack trace of most recent exception"),
    PrintState("PrintState <state number> -- display information about a state"),
    Reload("Reload the BURS tables"),
    Set("Set a property")
    ;

    final String shortHelpText;

    CommandType()
    {
        shortHelpText = null;
    }

    CommandType(String shortHelpText)
    {
        this.shortHelpText = shortHelpText;
    }

    static CommandType getCommandType(String token)
    {
        for (CommandType ctype: values()) {
            if (ctype.toString().equalsIgnoreCase(token)) {
                return ctype;
            }
        }

        return Error;
    }

    static void help(AbstractConsole console, String helpTopic)
    {
        if (helpTopic != null) {
            CommandType ctype = getCommandType(helpTopic);

            if (ctype != Error) {
                console.println(String.format("%s - %s\n", ctype, ctype.shortHelpText));
            }
        } else {
            for (CommandType ctype: values()) {

                if (ctype.shortHelpText != null) {
                    console.println(ctype.toString());
                }
            }
        }
    }
}
