package jburg.semantics;

import java.util.HashMap;
import java.util.Map;

/**
 * DebuggerSemantics uses ad-hoc semantics
 * to enable the debugger to relabel a tree.
 */
public class DebuggerSemantics extends CppSemantics<Object,String>
{
    public DebuggerSemantics()
    {
        super("Object","String");
    }

    /**
     * Get a node type given its "name," i.e., a nominal description.
     * @param   typeName  the "name" of the node type.
     * @return  the canonical node type object corresponding to the name.
     */
    @SuppressWarnings("unchecked")
    public String getNodeType(Object typeName)
    {
        return typeName.toString();
    }
}
