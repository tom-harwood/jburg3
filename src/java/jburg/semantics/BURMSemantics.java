package jburg.semantics;

import java.util.Map;

/**
 * BURMSemantics' primary responsibility is finding
 * callback routines for rules; it may also validate
 * these callback routines' return types to ensure
 * that they are compatible with the BURM's declared
 * nonterminal-to-host type mappings.
 */
public interface BURMSemantics
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
}
