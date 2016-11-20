package jburg.debugger;

import org.w3c.dom.*;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.*;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;

class DumpAnalyzer extends JPanel
{
    AdapterNode root;

    DumpAnalyzer(Node root)
    {
        this.root = new AdapterNode(root);
        JTree tree = new JTree(new DomToTreeModelAdapter());

        tree.setCellRenderer(new NodeTreeCellRenderer());

        this.setLayout(new BorderLayout());
        this.add("Center", new JScrollPane(tree));
        JFrame frame = new JFrame("Dump Analyzer");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().add("Center", this);
        frame.pack();
        frame.setVisible(true);
    }

    class AdapterNode 
    {
        private final Node              domNode;
        private final String            text;
        private final Integer           stateNumber;
        private final java.util.List<AdapterNode> childNodes;

        AdapterNode(Node domNode)
        {
            if (domNode == null) {
                throw new IllegalStateException("null domNode?");
            }

            this.domNode = domNode;

            switch(domNode.getChildNodes().getLength()) {
                case 0:
                    this.childNodes = Collections.emptyList();
                    break;
                default:
                    this.childNodes = new ArrayList<AdapterNode>();
                    for (Node n = domNode.getFirstChild(); n != null; n = n.getNextSibling()) {

                        if (n.getNodeName() != null) {
                            childNodes.add(new AdapterNode(n));
                        }
                    }
            }

            // Populate the display string.
            StringBuilder builder = new StringBuilder();

            if (domNode.getNodeName() != null) {
                builder.append(domNode.getNodeName());
            }
            NamedNodeMap attrs = domNode.getAttributes();

            for (int i = 0; attrs != null && i < attrs.getLength(); i++) {
                Attr attr = (Attr)attrs.item(i);
                builder.append(" ");
                builder.append(attr.getName());
                builder.append("=\"");
                builder.append(attr.getValue());
                builder.append("\"");
            }

            if (attrs != null && attrs.getNamedItem("state") != null) {
                this.stateNumber = Integer.parseInt(((Attr)attrs.getNamedItem("state")).getValue());
            } else {
                this.stateNumber = 0;
            }

            this.text = builder.toString();
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
            return text;
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

            if (node.stateNumber == 0) {
                setForeground(Color.red.darker());
            }

            return this;
        }
    }
}
