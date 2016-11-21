package jburg.debugger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jburg.util.PrintState;

public class Debugger implements Console.AbstractExecutive
{
    final Console       console;
    final Properties    properties;
    String              burmDumpFilename;
    Document            burmDump;
    final static String propertiesFileName = "jburgDebugger.properties";

    public static void main(String[] args)
    {
        new Debugger(args);
    }

    Debugger(String[] args)
    {
        properties = new Properties();

        try {
            properties.load(new FileInputStream(propertiesFileName));
        } catch (Exception cannotLoad) {
            // Ignore.
        }

        console = new Console(this);
        console.display("Debugger");

        try {
            burmDumpFilename = args[0];
            load();
        } catch (Exception loadError) {
            console.getAbstractConsole().status(String.format("Problem loading %s: %s", burmDumpFilename, loadError));
        }
    }

    private void load()
    throws Exception
    {
        this.burmDump = parseXML(new FileInputStream(burmDumpFilename));
    }

    private Document parseXML(InputStream is)
    throws Exception
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        dbf.setIgnoringComments(true);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setCoalescing(true);
        dbf.setExpandEntityReferences(false);

        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(is);
    }

    public boolean executeCommand(String command)
    {
        try {
            String[] tokens = tokenize(command);

            if (tokens.length > 0) {
                CommandType ctype = CommandType.getCommandType(tokens[0]);

                if (ctype != CommandType.Error) {
                    switch (ctype) {

                        case Analyze:
                            analyze(command.substring(tokens[0].length()));
                            break;

                        case Echo: {
                                String propertyName = tokens[1];
                                String propertyValue = properties.getProperty(propertyName);
                                println("%s = %s", propertyName, propertyValue);
                            }
                            break;

                        case Exit:
                            // TODO: Clean up, exit more gracefully
                            System.exit(0);

                        case Execute:
                            execute(command.substring(tokens[0].length()));
                            break;

                        case Help:

                            if (tokens.length == 2) {
                                CommandType.help(console.getAbstractConsole(), tokens[1]);
                            } else {
                                CommandType.help(console.getAbstractConsole(), null);
                            }
                            break;

                        case PrintStackTrace: {

                                if (mostRecentException != null) {
                                    println(mostRecentException.toString());
                                    for (StackTraceElement element: mostRecentException.getStackTrace()) {
                                        println(element.toString());
                                    }
                                }
                            }
                            break;

                        case PrintState: {
                                for (PrintState.Finding finding: PrintState.analyzeState(burmDump, tokens[1])) {
                                    println(finding.toString());
                                }
                            }
                            break;

                        case Reload:
                            load();
                            break;
                        case Set: {
                                String propertyName = tokens[1];
                                set(propertyName, allTextAfter(command, propertyName));
                            }
                            break;

                        default:
                            status("Unimplmemented command: %s",ctype);
                            break;
                    }
                } else {
                    status("Unknown command %s -- try help",ctype);
                }
            }
        } catch (Exception commandProblem) {
            exception(commandProblem, "Problem executing %s", command);
        }

        return true;
    }

    private String[] tokenize(String tokenSource)
    {
        return tokenSource.split("\\s+");
    }

    private void set(String propertyName, String propertyValue)
    throws Exception
    {
        // Strip optional =
        propertyValue = propertyValue.replaceAll("^\\s+=\\s*", "");
        properties.setProperty(propertyName, propertyValue);
        properties.store(new FileOutputStream(propertiesFileName), "JBurg3 debugger properties");
    }

    private String allTextAfter(String command, String lastTokenUsed)
    {
        return command.substring(command.indexOf(lastTokenUsed) + lastTokenUsed.length());
    }

    private Exception mostRecentException = null;

    private void analyze(String xml)
    throws Exception
    {
        @SuppressWarnings("deprecation")
        Document parsedXML = parseXML(new java.io.StringBufferInputStream(xml));
        new DumpAnalyzer(parsedXML.getFirstChild());
    }

    private void execute(String execCommand)
    throws Exception
    {
        // If there's a stored execCommand, substitute into it.
        String storedCommand = properties.getProperty("execCommand");

        if (storedCommand != null) {
            execCommand = storedCommand.replaceAll("\\$\\*", execCommand);
            println("Executing %s", execCommand);
        }

        Process proc = Runtime.getRuntime().exec(execCommand);

        // Capture stderr and stdout.
        String errorOutput = readAll(proc.getErrorStream());
        String stdout = readAll(proc.getInputStream());
        int exitVal = proc.waitFor();

        println("result = %s", stdout);
        analyze(stdout);
    }

    String readAll(InputStream stream)
    throws Exception
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        StringBuilder result = new StringBuilder();
        String line = null;
        while ( (line = br.readLine()) != null)
            result.append(line);

        return result.toString();
    }

    private void status(String format, Object... args)
    {
        console.getAbstractConsole().status(String.format(format,args));
    }

    private void println(String format, Object... args)
    {
        console.getAbstractConsole().println(String.format(format,args));
    }

    private void exception(Exception exception, String formatString, Object... args)
    {
        console.getAbstractConsole().exception(String.format(formatString, args), exception);
        mostRecentException = exception;
    }
}
