package jburg.semantics;

import java.util.Map;

/**
 * BURMSemantics' primary responsibility is finding
 * callback routines for rules; it may also validate
 * these callback routines' return types to ensure
 * that they are compatible with the BURM's declared
 * nonterminal-to-host type mappings.
 */
public interface BURMSemantics<Nonterminal, NodeType>
{
    /**
     * Create a nonterminal-host class mapping.
     * @param nt        the nonterminal.
     * @param hostType  the host language type.
     */
    public void setNonterminalClass(Object nt, Object hostType);

    public void setDefaultNonterminalClass(Object defaultHostType);

    /**
     * Get a pre-order callback method, by name.
     * @param methodName    the callback method's name.
     */
    public HostRoutine getPreCallback(String methodName)
    throws Exception;

    /**
     * Get a predicate method, by name.
     * @param methodName    the predicate method's name.
     */
    public HostRoutine getPredicate(String methodName)
    throws Exception;

    /**
     * Get a post-order callback method, by name.
     * @param methodName    the callback method's name.
     * @param isVariadic    true if the production is variadic.
     * @param producesNt    the nonterminal callback's production produces.
     * @param nonterminals  the nonterminals of the callback's production's children.
     */
    @SuppressWarnings("unchecked")
    public HostRoutine getPostCallback(String methodName, boolean isVariadic, Object producesNt, Object ... nonterminals)
    throws Exception;

    /**
     * Get a fixed-arity post-order callback method, by name.
     * @param methodName    the callback method's name.
     * @param producesNt    the nonterminal callback's production produces.
     * @param nonterminals  the nonterminals of the callback's production's children.
     */
    @SuppressWarnings("unchecked")
    public HostRoutine getFixedArityPostCallback(String methodName, Object producesNt, Object ... nonterminals)
    throws Exception;

    /**
     * Get a variadic post-order callback method, by name.
     * @param methodName    the callback method's name.
     * @param producesNt    the nonterminal callback's production produces.
     * @param nonterminals  the nonterminals of the callback's production's children.
     */
    @SuppressWarnings("unchecked")
    public HostRoutine getVariadicPostCallback(String methodName, Object producesNt, Object ... nonterminals)
    throws Exception;

    /**
     * Get a nonterminal given its "name," i.e., a nominal description.
     * @param   ntName  the "name" of the nonterminal.
     * @return  the canonical nonterminal object corresponding to the name.
     */
    public Nonterminal getNonterminal(Object ntName);

    /**
     * Get a node type given its "name," i.e., a nominal description.
     * @param   typeName  the "name" of the node type.
     * @return  the canonical node type object corresponding to the name.
     */
    public NodeType getNodeType(Object typeName);
}
