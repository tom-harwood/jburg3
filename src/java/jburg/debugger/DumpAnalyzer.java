package jburg.debugger;

import org.w3c.dom.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.tree.*;
import javax.swing.event.*;

import jburg.BurgInput;
import jburg.Reducer;

class DumpAnalyzer extends JPanel
{
    final AdapterNode   root;
    final Debugger      debugger;
    final JTree         tree;
    final JFrame        frame;

    static final String closeCommand = "Close";
    static final String expandAllCommand = "Expand fully";
    static final String relabelCommand = "Rebuild BURS annotations";
    static final String setTitleCommand = "Set title...";

    DumpAnalyzer(Debugger debugger, String title, Node root)
    {
        this.debugger = debugger;

        this.root = new AdapterNode(root);

        this.tree = new JTree(new DomToTreeModelAdapter());
        tree.setCellRenderer(new NodeTreeCellRenderer());
        tree.addMouseListener(new TreePopupListener(tree));

        this.setLayout(new BorderLayout());
        this.add("Center", new JScrollPane(tree));
        this.frame = new JFrame("Tree Analyzer: " + title);
        frame.getContentPane().add("Center", this);

        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(fileMenu);

        JMenuItem closeItem = new JMenuItem(closeCommand, KeyEvent.VK_C);
        fileMenu.add(closeItem);
        closeItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                    frame.dispose();
                }
            }
        );

        JMenu treeMenu = new JMenu("Tree");
        treeMenu.setMnemonic(KeyEvent.VK_T);
        menuBar.add(treeMenu);

        JMenuItem expandItem = new JMenuItem(expandAllCommand, KeyEvent.VK_E);
        treeMenu.add(expandItem);
        expandItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    expandAll();
                    frame.pack();
                }
            }
        );

        JMenuItem relabelItem = new JMenuItem(relabelCommand, KeyEvent.VK_L);
        treeMenu.add(relabelItem);
        relabelItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    relabel();
                }
            }
        );

        JMenuItem setTitleItem = new JMenuItem(setTitleCommand, KeyEvent.VK_T);
        treeMenu.add(setTitleItem);
        setTitleItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String title = JOptionPane.showInputDialog("Title:");
                    if (title != null) {
                        frame.setTitle(title);
                    }
                }
            }
        );

        expandNonViable();

        frame.pack();
        debugger.console.prepareFrame(frame);
        frame.setVisible(true);
    }

    private void expandNonViable()
    {
        java.util.List<AdapterNode> path = new ArrayList<AdapterNode>();
        path.add(root);

        for (AdapterNode child: root.childNodes) {
            expandNonViable(path, child);
        }
    }

    private void expandNonViable(java.util.List<AdapterNode> path, AdapterNode next)
    {
        if (next.stateNumber == 0) {
            expandPath(path);
        }

        path.add(next);

        for (AdapterNode child: next.childNodes) {
            expandNonViable(path, child);
        }

        path.remove(path.size()-1);
    }

    private void expandPath(java.util.List<AdapterNode> path)
    {
        tree.expandPath(new TreePath(path.toArray(new Object[0])));
    }

    private void expandAll()
    {
        java.util.List<AdapterNode> path = new ArrayList<AdapterNode>();
        path.add(root);

        for (AdapterNode child: root.childNodes) {
            expandAll(path, child);
        }
    }

    private void expandAll(java.util.List<AdapterNode> path, AdapterNode next)
    {
        if (next.getChildCount() == 0) {
            expandPath(path);
        } else {
            path.add(next);

            for (AdapterNode child: next.childNodes) {
                expandAll(path, child);
            }

            path.remove(path.size()-1);
        }
    }

    private void relabel()
    {
        if (debugger.productionTable != null) {
            Reducer<Object,String> reducer = new Reducer<Object,String>(null, debugger.productionTable);

            try {
                reducer.label(root);
            } catch (Exception cannotLabel) {
                debugger.exception(cannotLabel, "Problem labeling");
            }

            expandNonViable();
            frame.repaint();

        } else {
            debugger.exception(new IllegalStateException("No production table loaded"), "Problem labeling");
        }
    }

    /**
     * AdapterNode wraps a DOM node, which allows it
     * to be displayed in a JTree and also to be re-labled
     * with new versions of the BURS tables.
     */
    class AdapterNode implements BurgInput<Object,String>
    {
        private final Node              domNode;
        private final StringBuilder     attributeText = new StringBuilder();
        private final java.util.List<AdapterNode> childNodes;
        private Integer                 stateNumber;
        private Object                  transitionTableLeaf = null;


        AdapterNode(Node domNode)
        {
            if (domNode == null) {
                throw new IllegalStateException("null domNode?");
            } else if (domNode.getNodeName() == null) {
                throw new IllegalStateException("null node name?");
            }

            this.domNode = domNode;

            switch(domNode.getChildNodes().getLength()) {
                case 0:
                    this.childNodes = Collections.emptyList();
                    break;
                default:
                    this.childNodes = new ArrayList<AdapterNode>();
                    for (Node n = domNode.getFirstChild(); n != null; n = n.getNextSibling()) {

                        if (n.getNodeName() != null && !"#text".equals(n.getNodeName())) {
                            childNodes.add(new AdapterNode(n));
                        }
                    }
            }

            // Populate the attribute display string.
            NamedNodeMap attrs = domNode.getAttributes();

            for (int i = 0; attrs != null && i < attrs.getLength(); i++) {
                Attr attr = (Attr)attrs.item(i);

                if (attr.getName().equals("state")) {
                    continue;
                }
                attributeText.append(" ");
                attributeText.append(attr.getName());
                attributeText.append("=\"");
                attributeText.append(attr.getValue());
                attributeText.append("\"");
            }

            if (attrs != null && attrs.getNamedItem("state") != null) {
                this.stateNumber = Integer.parseInt(((Attr)attrs.getNamedItem("state")).getValue());
            } else {
                this.stateNumber = 0;
            }
        }

        public int getChildCount()
        {
            return childNodes.size();
        }

        public AdapterNode getChild(int index)
        {
            return childNodes.get(index);
        }

        public int indexOf(AdapterNode child)
        {
            return childNodes.indexOf(child);
        }

        public String toString()
        {
            return String.format("%s state=%s %s", domNode.getNodeName(), stateNumber, attributeText);
        }

        public String getNodeType()
        {
            return domNode.getNodeName() != null? domNode.getNodeName(): "null";
        }

        public int getSubtreeCount()
        {
            return getChildCount();
        }

        public BurgInput<Object, String>  getSubtree(int idx)
        {
            return getChild(idx);
        }

        public void setStateNumber(int stateNumber)
        {
            this.stateNumber = stateNumber;
        }

        public int getStateNumber()
        {
            return this.stateNumber;
        }

        public Object getTransitionTableLeaf()
        {
            return this.transitionTableLeaf;
        }

        public void setTransitionTableLeaf(Object ttleaf)
        {
            this.transitionTableLeaf = ttleaf;
        }
    }

    public class DomToTreeModelAdapter implements javax.swing.tree.TreeModel
    {
        public Object getRoot()
        {
            return DumpAnalyzer.this.root;
        }

        public boolean isLeaf(Object aNode)
        {
            return asAdapterNode(aNode).getChildCount() == 0;
        }

        public int getChildCount(Object o)
        {
            return asAdapterNode(o).getChildCount();
        }

        public int getIndexOfChild(Object parent, Object child)
        {
            return asAdapterNode(parent).indexOf(asAdapterNode(child));
        }

        public Object getChild(Object parent, int index)
        {
            return asAdapterNode(parent).getChild(index);
        }

        public void addTreeModelListener(TreeModelListener listener)
        {
        }

        public void removeTreeModelListener(TreeModelListener listener)
        {
        }

        public void valueForPathChanged(TreePath path, Object newValue)
        {
        }

        AdapterNode asAdapterNode(Object o)
        {
            return (AdapterNode)o;
        }
    }

    public class NodeTreeCellRenderer extends DefaultTreeCellRenderer
    {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean sel, boolean exp, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, exp, leaf, row, hasFocus);

            AdapterNode node = (AdapterNode)value;

            if (node.stateNumber == 0 && !"#text".equals(node.attributeText.toString())) {
                setForeground(Color.red.darker());
            }

            return this;
        }
    }

    class TreePopupListener implements MouseListener
    {
        final JTree tree;
        final JPopupMenu treePopup;
        final static String PRINT_STATE = "Display state";

        TreePopupListener(JTree tree)
        {
            this.tree = tree;
            treePopup = new JPopupMenu();
            JMenuItem item = new JMenuItem(PRINT_STATE);
            treePopup.add(item);
            item.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent event) {
                        String command = event.getActionCommand();

                        if (command.equals(PRINT_STATE)) {
                            AdapterNode node = (AdapterNode)tree.getLeadSelectionPath().getLastPathComponent();
                            new PrintStateThread(node).start();
                        }
                    }
                }
            );
        }

        @Override
        public void mouseClicked(MouseEvent e) {

            if (SwingUtilities.isRightMouseButton(e)) {

                int row = tree.getClosestRowForLocation(e.getX(), e.getY());
                tree.setSelectionRow(row);
                treePopup.show(e.getComponent(), e.getX(), e.getY());
            }
        }

        public void mouseEntered(MouseEvent e) { /* Ignore. */  }
        public void mouseExited(MouseEvent e) { /* Ignore. */  }
        public void mousePressed(MouseEvent e) { /* Ignore. */  }
        public void mouseReleased(MouseEvent e) { /* Ignore. */  }
    }

    class PrintStateThread extends Thread
    {
        final JTextArea area = new JTextArea();
        final JFrame popupFrame;

        Runnable runner;

        PrintStateThread(final AdapterNode node)
        {
            final String stateNumber = String.valueOf(node.stateNumber);
            this.popupFrame = new JFrame("State " + stateNumber);

            this.runner = new Runnable() {
                public void run() {
                    debugger.console.prepareFrame(popupFrame);
                    area.setEditable(false);
                    area.setBorder(
                        BorderFactory.createCompoundBorder(
                            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
                            BorderFactory.createEmptyBorder(0,2,0,4)
                        )
                    );
                    area.setText("working...");
                    popupFrame.add(area);
                    popupFrame.pack();
                    popupFrame.setVisible(true);

                    String stateInfo;

                    try {
                        SwingUtilities.invokeLater(new AreaUpdater(debugger.getStateInformation(stateNumber)));
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(new AreaUpdater(String.format("Problem generating state description: %s", ex)));
                    }
                }
            };
        }

        public void run()
        {
            runner.run();
        }

        class AreaUpdater implements Runnable
        {
            final String text;

            AreaUpdater(final String text)
            {
                this.text = text;
            }

            public void run()
            {
                area.setText(text);
                popupFrame.pack();
            }
        }
    }
}
