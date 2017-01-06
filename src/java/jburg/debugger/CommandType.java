package jburg.debugger;

enum CommandType
{
    Analyze("Analyze an ad-hoc tree"),
    Clear("Clear the console log"),
    Echo("Echo a property"),
    Error,
    Exit("Exit the debugger"),
    Help("Display help -- help <command name> for details"),
    HGrep("Grep command history"),
    History("Display command history"),
    Load("Load a new BURS table"),
    PrintStackTrace("Print stack trace of most recent exception"),
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
            console.println("help <command name> for more information.");
            for (CommandType ctype: values()) {

                if (ctype.shortHelpText != null) {
                    console.println(ctype.toString());
                }
            }
        }
    }
}
