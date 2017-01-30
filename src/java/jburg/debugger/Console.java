package jburg.debugger;

import java.awt.*;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.GroupLayout.*;

public class Console extends JPanel
{
    /** An editable command line. */
    final JTextField commandLine = new JTextField();

    /** The contents of the history pane; they extend the history with a font. */
    final DefaultListModel<ConsoleHistoryElement>  output = new DefaultListModel<ConsoleHistoryElement>();

    /** The list displaying the contents of the history pane. */
    final JList<ConsoleHistoryElement> outputList;

    /** The history pane itself. */
    final JScrollPane outputPane;

    /** The default font used for debugger output. */
    final Font normalFont;
    /** The italic version of the default font. */
    final Font italicFont;
    /** The bold version of the default font. */
    final Font boldFont;

    /** The contents of command history; saved and restored via properties. */
    final List<String> history = new ArrayList<String>();

    /** Current position scrolling through the history list. */
    int historyPos = -1;

    /** Set true when an application shutdown is in progress;
     *  avoids recursive attempts to shutdown the application
     *  as this Console's window closes.
     */
    boolean applicationShutdownCommanded = false;

    final Debugger debugger;

    static final String downArrow = "downArrow";
    static final String upArrow = "upArrow";

    Console(Debugger debugger)
    {
        this.debugger = debugger;

        commandLine.setMaximumSize(new Dimension(Integer.MAX_VALUE, commandLine.getPreferredSize().height));
        commandLine.getInputMap().put(KeyStroke.getKeyStroke("DOWN"), downArrow);
        commandLine.getInputMap().put(KeyStroke.getKeyStroke("UP"), upArrow);
        commandLine.setBorder(BorderFactory.createLoweredBevelBorder());

        commandLine.getActionMap().put(downArrow, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                injectHistory(--historyPos);
            }
        });

        commandLine.getActionMap().put(upArrow, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                injectHistory(++historyPos);
            }
        });

        this.outputList = new JList<ConsoleHistoryElement>(output);
        outputList.setCellRenderer(new ConsoleHistoryRenderer());
        this.normalFont = outputList.getFont().deriveFont(Font.PLAIN);
        this.italicFont = normalFont.deriveFont(Font.ITALIC);
        this.boldFont = normalFont.deriveFont(Font.BOLD);

        this.outputPane = new JScrollPane(outputList);
        outputPane.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

        commandLine.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final String command = commandLine.getText();
                addOutput(command, boldFont);
                new CommandExecutor(command).start();
                history.add(0, command);
                historyPos = -1;
                commandLine.setText("");
            }
        });

        GroupLayout layout = new GroupLayout(this);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setHorizontalGroup(
            layout.createParallelGroup()
                .addComponent(outputPane)
                .addComponent(commandLine)
        );
        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(outputPane)
                .addComponent(commandLine)
        );

        setLayout(layout);
    }

    synchronized void addOutput(String content, Font font)
    {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                output.addElement(new ConsoleHistoryElement(content, font));
                outputList.ensureIndexIsVisible(output.getSize()-1);
            }
        });
    }

    void extractHistory(Properties properties)
    {
        Set<String> uniqueHistoryEntries = new HashSet<String>();

        int i = 0;
        String key = historyKey(i++);

        while (properties.containsKey(key)) {
            String value = properties.getProperty(key);

            if (uniqueHistoryEntries.add(value))  {
                history.add(value);
            }
            key = historyKey(i++);
        }
    }

    String historyKey(int i)
    {
        return "Console.history." + String.valueOf(i);
    }

    void saveHistory(Properties properties)
    {
        Set<String> uniqueHistoryEntries = new HashSet<String>();

        for (int i = 0; i < history.size(); i++) {
            if (uniqueHistoryEntries.add(history.get(i))) {
                properties.setProperty(historyKey(i), history.get(i));
            }
        }
    }

    void injectHistory(int newPos)
    {
        if (newPos < 0) {
            historyPos = 0;
        } else if (newPos >= history.size()) {
            historyPos = Math.max(history.size() - 1, 0);
        }

        if (history.size() > historyPos) {
            commandLine.setText(history.get(historyPos));
        }
    }

    public void println(String s)
    {
        addOutput(s, normalFont);
    }

    public void exception(String operation, Exception ex) {

        String diagnostic = ex.getMessage() != null?  ex.getMessage(): ex.toString();
        addOutput(diagnostic, italicFont);
    }

    public void status(String format, Object... args)
    {
        addOutput(String.format(format, args), italicFont);
    }

    public void display(String title)
    {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(this);
        frame.pack();
        setIcon(frame);

        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(fileMenu);

        JMenuItem loadItem = new JMenuItem("Open BURS grammar...", KeyEvent.VK_O);
        fileMenu.add(loadItem);
        loadItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    debugger.executeCommand("Load");
                }
            }
        );

        fileMenu.addSeparator();

        JMenuItem closeItem = new JMenuItem("Exit", KeyEvent.VK_X);
        fileMenu.add(closeItem);
        closeItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    applicationShutdownCommanded = true;
                    debugger.executeCommand("Exit");
                }
            }
        );

        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        menuBar.add(editMenu);

        JMenuItem clearItem = new JMenuItem("Clear console output", KeyEvent.VK_O);
        editMenu.add(clearItem);
        clearItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    output.clear();
                }
            }
        );



        // Exit the application if the console window closes.
        // Only do this if the shutdown sequence is not already
        // active, since this window will get a window close
        // notification when the debugger core shuts down all windows.
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {

                if (!applicationShutdownCommanded) {
                    applicationShutdownCommanded = true;
                    debugger.executeCommand("Exit");
                }
            }
        });
        frame.setVisible(true);
    }

    int framesGenerated = 0;

    /**
     * Prepare a subsidary frame with icon and place it relative to the console frame.
     */
    void prepareFrame(JFrame frame)
    {
        // Position the new frame in a tiled arrangement.
        framesGenerated = (framesGenerated+1) % 20;
        Point frameLoc = frame.getLocation(null);
        int horizOffset = (int)getMinimumSize().getWidth() + framesGenerated * 10;
        int vertOffset = (int)getMinimumSize().getHeight() + framesGenerated * 10;
        frameLoc.translate(horizOffset,vertOffset);
        frame.setLocation(frameLoc);

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setIcon(frame);
    }

    void setIcon(JFrame frame)
    {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                java.net.URL url = ClassLoader.getSystemResource("resources/jbd.png");
                java.awt.Toolkit kit = java.awt.Toolkit.getDefaultToolkit();
                frame.setIconImage(kit.createImage(url));
            }
        });
    }

    void clear()
    {
        output.clear();
    }

    void hgrep(String pattern)
    {
        pattern = ".*" + pattern.trim() + ".*";

        String lastHistory = null;
        List<String> foundHistory = new ArrayList<String>();

        for (String s: history) {

            if (s.matches(pattern)) {

                if (!s.equals(lastHistory)) {
                    foundHistory.add(s);
                    lastHistory = s;
                }
            }
        }

        Collections.reverse(foundHistory);
        for (String s: foundHistory) {
            addOutput(s, normalFont);
        }
    }

    void history()
    {
        hgrep(".*");
    }

    class ConsoleHistoryElement
    {
        String  content;
        Font    font;
        ConsoleHistoryElement(String content, Font font)
        {
            this.content = content;
            this.font = font;
        }

        @Override
        public String toString()
        {
            return content;
        }
    }

    class ConsoleHistoryRenderer extends JLabel implements ListCellRenderer<ConsoleHistoryElement>
    {
        public Component getListCellRendererComponent( JList<? extends ConsoleHistoryElement> list, ConsoleHistoryElement element, int index, boolean isSelected, boolean cellHasFocus)
        {
            setEnabled(list.isEnabled());
            setText(element.content);
            setFont(element.font);
            setOpaque(isSelected);
            return this;
        }
    }

    class CommandExecutor extends Thread
    {
        CommandExecutor(final String command)
        {
            super(new Runnable() {
                public void run() {
                    debugger.executeCommand(command);
                }
            });
        }
    }
}
