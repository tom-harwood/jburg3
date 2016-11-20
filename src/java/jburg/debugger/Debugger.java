package jburg.debugger;

import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import java.io.FileInputStream;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jburg.util.PrintState;

public class Debugger implements Console.AbstractExecutive
{
    final Console   console;
    String          burmDumpFilename;
    Document        burmDump;

    public static void main(String[] args)
    {
        new Debugger(args);
    }

    Debugger(String[] args)
    {
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
            String[] tokens = command.split("\\s+");

            if (tokens.length > 0) {
                CommandType ctype = CommandType.getCommandType(tokens[0]);

                if (ctype != CommandType.Error) {
                    switch (ctype) {

                        case Analyze:
                            analyze(command.substring(tokens[0].length()));
                            break;

                        case Exit:
                            // TODO: Clean up, exit more gracefully
                            System.exit(0);

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

    private Exception mostRecentException = null;

    private void analyze(String xml)
    throws Exception
    {
        @SuppressWarnings("deprecation")
        Document parsedXML = parseXML(new java.io.StringBufferInputStream(xml));
        new DumpAnalyzer(parsedXML.getFirstChild());
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
