package jburg.debugger;

import java.awt.*;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.GroupLayout.*;

public class Console extends JPanel
{
    final JTextField                commandLine = new JTextField();
    final DefaultListModel<String>  output = new DefaultListModel<String>();
    final List<String>              history = new ArrayList<String>();
    int                             historyPos = -1;
    final JLabel                    statusLine = new JLabel();

    final AbstractExecutive         executive;

    static final String downArrow = "downArrow";
    static final String upArrow = "upArrow";

    Console(AbstractExecutive executive)
    {
        this.executive = executive;

        commandLine.setMaximumSize(new Dimension(Integer.MAX_VALUE, commandLine.getPreferredSize().height));
        commandLine.getInputMap().put(KeyStroke.getKeyStroke("DOWN"), downArrow);
        commandLine.getInputMap().put(KeyStroke.getKeyStroke("UP"), upArrow);

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

        JList<String> outputList = new JList<String>(output);
        final JScrollPane outputPane = new JScrollPane(outputList);

        commandLine.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                output.addElement(commandLine.getText());
                updateStatus("");
                executive.executeCommand(commandLine.getText());
                history.add(0, commandLine.getText());
                historyPos = -1;
                commandLine.setText("");

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        outputPane.getVerticalScrollBar().setValue(outputPane.getVerticalScrollBar().getMaximum());
                    }
                });
            }
        });

        GroupLayout layout = new GroupLayout(this);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setHorizontalGroup(
            layout.createParallelGroup()
                .addComponent(outputPane)
                .addComponent(commandLine)
                .addComponent(statusLine)
        );
        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(outputPane)
                .addComponent(commandLine)
                .addComponent(statusLine)
        );

        setLayout(layout);
    }

    void extractHistory(Properties properties)
    {
        int i = 0;
        String key = historyKey(i++);

        while (properties.containsKey(key)) {
            if (history.size() < i) {
                history.add(properties.getProperty(key));
            } else {
                history.set(i, properties.getProperty(key));
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
        for (int i = 0; i < history.size(); i++) {
            properties.setProperty(historyKey(i), history.get(i));
        }
    }

    void updateStatus(String format, Object... args)
    {
        statusLine.setText(String.format(format, args));
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

    AbstractConsole getAbstractConsole()
    {
        return new GuiConsole();
    }

    class GuiConsole extends AbstractConsole
    {
        StringBuilder pendingOutput = null;

        @Override
        public synchronized void print(String s)
        {
            if (this.pendingOutput == null) {
                this.pendingOutput = new StringBuilder();
            }
            this.pendingOutput.append(s);

            if (s.endsWith("\n")) {
                println("");
            }
        }

        @Override
        public synchronized void println(String s)
        {
            if (this.pendingOutput == null) {
                output.addElement(s);
            } else {
                output.addElement(this.pendingOutput.toString() + s);
                this.pendingOutput = null;
            }
        }

        @Override
        public void exception(String operation, Exception ex) {

            if (ex.getMessage() != null) {
                status(ex.getMessage());
            } else {
                status(ex.toString());
            }
        }

        @Override
        public void status(String format, Object... args) {
            String formattedMessage = String.format(format, args);
            println(formattedMessage);
            updateStatus(formattedMessage);
        }
    }

    public interface AbstractExecutive
    {
        public boolean executeCommand(String command);
    }

    public void display(String title)
    {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(this);
        frame.pack();
        setIcon(frame);
        frame.setVisible(true);
    }

    int framesGenerated = 0;

    void prepareFrame(JFrame frame)
    {
        // Position the new frame in a tiled arrangement.
        framesGenerated = (framesGenerated+1) % 20;
        Point frameLoc = getLocation(null);
        int horizOffset = (int)getMinimumSize().getWidth() + framesGenerated * 10;
        int vertOffset = (int)getMinimumSize().getHeight() + framesGenerated * 10;
        frameLoc.translate(horizOffset,vertOffset);
        frame.setLocation(frameLoc);

        // TODO: Add an ESC listener to close the frame.
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setIcon(frame);
    }

    void setIcon(JFrame frame)
    {
        try {
            java.net.URL url = ClassLoader.getSystemResource("resources/jbd.png");
            java.awt.Toolkit kit = java.awt.Toolkit.getDefaultToolkit();
            frame.setIconImage(kit.createImage(url));
        } catch (Exception cannotSetIcon) {
            updateStatus(String.format("Problem loading icon: %s", cannotSetIcon));
        }
    }

    void clear()
    {
        output.clear();
    }
}
