package jburg.debugger;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JFrame;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import java.awt.Frame;
import java.awt.event.WindowEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import jburg.util.PrintState;

public class Debugger implements Console.AbstractExecutive
{
    final Console       console;
    final Properties    properties;
    String              burmDumpFilename;
    Document            burmDump;
    final static String propertiesFileName = "jburgDebugger.properties";
    final static String debuggerConsoleName = "Debugger";

    public static void main(String[] args)
    {
        new Debugger(args);
    }

    Debugger(String[] args)
    {
        console = new Console(this);
        properties = new Properties();

        try {
            properties.load(new FileInputStream(propertiesFileName));
        } catch (Exception cannotLoad) {
            console.getAbstractConsole().exception("loading properties ", cannotLoad);
        }

        console.extractHistory(properties);

        StringBuilder aPrioriCommand = null;

        for (int i = 0; i < args.length; i++) {

            if (args[i].equalsIgnoreCase("-burm") && i+1 < args.length) {
                burmDumpFilename = args[++i];
            } else if ((args[i].equalsIgnoreCase("-command") || args[i].equalsIgnoreCase("-cmd") || args[i].equals("-c")) && i+1 < args.length) {
                aPrioriCommand = new StringBuilder();

                for (i = i+1;i < args.length; i++) {
                    aPrioriCommand.append(" ");
                    aPrioriCommand.append(args[i]);
                }

            } else {
                println("Unrecognized command " + args[i]);
            }
        }

        if (burmDumpFilename != null) {

            try {
                load();
            } catch (Exception loadError) {
                console.getAbstractConsole().exception(String.format("loading %s", burmDumpFilename), loadError);
            }
        }

        console.display(debuggerConsoleName);

        if (aPrioriCommand != null) {
            executeCommand(aPrioriCommand.toString());
        }
    }

    private void load()
    throws Exception
    {
        this.burmDump = parseXML(new FileInputStream(burmDumpFilename));
        cachedStateInfo.clear();
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

        // Get diagnostics via thrown exceptions.
        // TODO: Configure this better so the more
        // informative parser diagnostics show up.
        db.setErrorHandler(new NilErrorHandler());
        return db.parse(is);
    }

    public boolean executeCommand(String command)
    {
        if (command.length() == 0 || command.startsWith("#")) {
            return false;
        }

        try {
            String[] tokens = tokenize(command);

            if (tokens.length > 0) {
                CommandType ctype = CommandType.getCommandType(tokens[0]);

                if (ctype != CommandType.Error) {

                    switch (ctype) {

                        case Analyze:
                            analyze(allTextAfter(command, tokens[0]));
                            break;

                        case Clear:
                            console.clear();
                            break;

                        case Echo: {
                                String propertyName = tokens[1];
                                String propertyValue = properties.getProperty(propertyName);
                                println("%s = %s", propertyName, propertyValue);
                            }
                            break;

                        case Exit: {
                                if (burmDumpFilename != null) {
                                    properties.setProperty("lastDumpFile", burmDumpFilename);
                                }
                                console.saveHistory(properties);

                                try {
                                    properties.store(new FileOutputStream(propertiesFileName), "JBurg3 debugger properties");
                                } catch (Exception cannotStore) {
                                    // Bummer.
                                }

                                for (Frame frame: Frame.getFrames()) {
                                    frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                                    frame.dispose();
                                }

                                notifyAll();
                            }
                            break;

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

                        case HGrep:
                            console.hgrep(command.substring(tokens[0].length()));
                            break;

                        case History:
                            console.history();
                            break;

                        case Load: {
                                JFileChooser chooser = new JFileChooser();
                                FileNameExtensionFilter filter = new FileNameExtensionFilter("XML files", "xml");
                                chooser.setFileFilter(filter);

                                if (properties.getProperty("lastDumpfile") != null) {
                                    chooser.setCurrentDirectory(new File(properties.getProperty("lastDumpfile")).getParentFile());
                                }

                                int returnVal = chooser.showOpenDialog(new JFrame());

                                if (returnVal == JFileChooser.APPROVE_OPTION) {
                                    burmDumpFilename = chooser.getSelectedFile().getCanonicalPath();
                                    load();
                                }

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
                    status("Unknown command %s -- try help", command);
                }
            }
        } catch (Exception commandProblem) {
            exception(commandProblem, "Problem executing %s", command);
        }

        return true;
    }

    private String[] tokenize(String tokenSource)
    {
        return tokenSource.trim().split("\\s+");
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
        return command.substring(command.indexOf(lastTokenUsed) + lastTokenUsed.length()).trim();
    }

    private Exception mostRecentException = null;

    private void analyze(String xml)
    throws Exception
    {
        if (xml.startsWith("<")) {
            analyze(xml,xml);
        } else {
            execute(xml);
        }
    }

    private void analyze(String title, String xml)
    throws Exception
    {
        @SuppressWarnings("deprecation")
        Document parsedXML = parseXML(new java.io.StringBufferInputStream(xml));
        new DumpAnalyzer(this, title, parsedXML.getFirstChild());
    }

    private void execute(String execCommand)
    throws Exception
    {
        Process proc = Runtime.getRuntime().exec(execCommand);

        // Capture stderr and stdout.
        String errorOutput = readAll(proc.getErrorStream());
        String stdout = readAll(proc.getInputStream());
        int exitVal = proc.waitFor();

        if (stdout.length() > 0) {
            analyze(execCommand, stdout);
        } else if (errorOutput.length() > 0) {
            for (String line: errorOutput.split("\\n")) {
                println(line);
            }
        } else {
            println("Command %s produced no output.", execCommand);
        }
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

    Map<String,String> cachedStateInfo = new HashMap<String,String>();

    String getStateInformation(String stateNumber)
    throws Exception
    {
        if (!cachedStateInfo.containsKey(stateNumber)) {
            StringBuilder result = new StringBuilder();

            boolean isFirstTime = true;
            for (PrintState.Finding finding: PrintState.analyzeState(burmDump, stateNumber)) {

                if (isFirstTime) {
                    isFirstTime = false;
                } else {
                    result.append("\n");
                }
                result.append(finding.toString());
            }
            cachedStateInfo.put(stateNumber, result.toString());
        }

        return cachedStateInfo.get(stateNumber);
    }

    private void status(String format, Object... args)
    {
        console.getAbstractConsole().status(String.format(format,args));
    }

    private void println(String format, Object... args)
    {
        console.getAbstractConsole().println(String.format(format,args));
    }

    void exception(Exception exception, String formatString, Object... args)
    {
        String diagnostic;
        try {
            diagnostic = String.format(formatString, args);
        } catch (Exception cannotFormat) {
            diagnostic = "Funky diagnostic: " + formatString + String.format(" %s",args);
        }
        console.getAbstractConsole().exception(diagnostic, exception);
        mostRecentException = exception;
    }

    /**
     * These exceptions are reported as necessary via the more
     * general throws/catch wrapper around executeCommand().
     */
    private class NilErrorHandler implements ErrorHandler
    {
        public void	error(SAXParseException exception) {}
        public void	fatalError(SAXParseException exception) {}
        public void	warning(SAXParseException exception) {}
    }

}
