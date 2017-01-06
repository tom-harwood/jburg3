package jburg.semantics;

import java.util.HashMap;
import java.util.Map;

/**
 * DebuggerSemantics uses ad-hoc semantics
 * to enable the debugger to relabel a tree.
 */
public class DebuggerSemantics implements BURMSemantics<Object,String>
{
    private HashMap<Object,String> nonterminalMappings = new HashMap<Object,String>();
    private String defaultMapping = null;

    public DebuggerSemantics()
    {
    }
    
    /**
     * Create a nonterminal-host class mapping.
     * @param nt        the nonterminal.
     * @param hostType  the host language type.
     */
    public void setNonterminalClass(Object nt, Object hostType)
    {
        if (!nonterminalMappings.containsKey(nt)) {
            nonterminalMappings.put(nt,hostType.toString());
        } else if (!nonterminalMappings.get(nt).equals(hostType)) {
            throw new IllegalArgumentException(String.format("Nonterminal %s already mapped to %s", nt, hostType));
        }
    }

    public void setDefaultNonterminalClass(Object defaultHostType)
    {
        if (defaultMapping == null) {
            defaultMapping = defaultHostType.toString();
        } else if (!defaultMapping.equals(defaultHostType)) {
            throw new IllegalStateException(String.format("Default nonterminal class already set to %s", defaultMapping));
        }
    }

    /**
     * Stub a pre-order callback method.
     * @param methodName    the callback method's name.
     * @return null
     */
    public HostRoutine getPreCallback(String methodName)
    {
        return null;
    }

    /**
     * Stub a predicate method.
     * @param methodName    the predicate method's name.
     * @return null
     */
    public HostRoutine getPredicate(String methodName)
    {
        return null;
    }

    /**
     * Stub a post-order callback method.
     * @param methodName    the callback method's name.
     * @param isVariadic    true if the production is variadic.
     * @param producesNt    the nonterminal callback's production produces.
     * @param nonterminals  the nonterminals of the callback's production's children.
     * @return null
     */
    @SuppressWarnings("unchecked")
    public HostRoutine getPostCallback(String methodName, boolean isVariadic, Object producesNt, Object ... nonterminals)
    {
        return null;
    }

    /**
     * Stub a fixed-arity post-order callback method.
     * @param methodName    the callback method's name.
     * @param producesNt    the nonterminal callback's production produces.
     * @param nonterminals  the nonterminals of the callback's production's children.
     * @return null
     */
    @SuppressWarnings("unchecked")
    public HostRoutine getFixedArityPostCallback(String methodName, Object producesNt, Object ... nonterminals)
    {
        return null;
    }

    /**
     * Stub a variadic post-order callback method.
     * @param methodName    the callback method's name.
     * @param producesNt    the nonterminal callback's production produces.
     * @param nonterminals  the nonterminals of the callback's production's children.
     * @return null
     */
    @SuppressWarnings("unchecked")
    public HostRoutine getVariadicPostCallback(String methodName, Object producesNt, Object ... nonterminals)
    {
        return null;
    }

    /**
     * Get a nonterminal given its "name," i.e., a nominal description.
     * @param   ntName  the "name" of the nonterminal.
     * @return  the canonical nonterminal object corresponding to the name.
     */
    @SuppressWarnings("unchecked")
    public String getNonterminal(Object ntName)
    {
        if (this.nonterminalMappings.containsKey(ntName.toString())) {
            return ntName.toString();
        } else {
            throw new IllegalArgumentException(String.format("Nonterminal %s has no type mapping", ntName));
        }
    }

    /**
     * Get the host class a nonterminal maps to.
     * @param ntName the name of the nonterminal.
     * @return the mapped host class for that nonterminal.
     */
    @SuppressWarnings("unchecked")
    private Object getNonterminalMapping(Object ntName)
    {
        if (this.nonterminalMappings.containsKey(ntName.toString())) {
            return this.nonterminalMappings.get(ntName.toString());
        } else if (this.defaultMapping != null) {
            return this.defaultMapping;
        } else {
            throw new IllegalArgumentException(String.format("Nonterminal %s has no type mapping",ntName));

        }
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
