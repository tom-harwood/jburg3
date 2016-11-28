package jburg.semantics;

import java.util.HashMap;
import java.util.Map;

/**
 * CppSemantics relies on string-based type names
 * to loosely emulate a C++-style type system.
 */
public class CppSemantics implements BURMSemantics
{
    private HashMap<Object,String> nonterminalMappings = new HashMap<Object,String>();
    private String defaultMapping = null;
    private final String nodeClassName;
    private final String visitorClassName;
    private final static String preCallbackType = "void";
    private final static String predicateType = "bool";
    private final static boolean FixedArity = false;
    private final static boolean VariadicArity = true;

    public CppSemantics(String nodeClassName, String visitorClassName)
    {
        this.nodeClassName = nodeClassName;
        this.visitorClassName = visitorClassName;
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
     * Get a pre-order callback method, by name.
     * @param methodName    the callback method's name.
     */
    public HostRoutine getPreCallback(String methodName)
    {
        return new CppHostRoutine(methodName, FixedArity, preCallbackType);
    }

    /**
     * Get a predicate method, by name.
     * @param methodName    the predicate method's name.
     */
    public HostRoutine getPredicate(String methodName)
    {
        return new CppHostRoutine(methodName, FixedArity, predicateType);
    }

    /**
     * Get a post-order callback method, by name.
     * @param methodName    the callback method's name.
     * @param isVariadic    true if the production is variadic.
     * @param producesNt    the nonterminal callback's production produces.
     * @param nonterminals  the nonterminals of the callback's production's children.
     */
    @SuppressWarnings("unchecked")
    public HostRoutine getPostCallback(String methodName, boolean isVariadic, Object producesNt, Object ... nonterminals)
    {
        return new CppHostRoutine(methodName, isVariadic, producesNt, nonterminals);
    }

    /**
     * Get a fixed-arity post-order callback method, by name.
     * @param methodName    the callback method's name.
     * @param producesNt    the nonterminal callback's production produces.
     * @param nonterminals  the nonterminals of the callback's production's children.
     */
    @SuppressWarnings("unchecked")
    public HostRoutine getFixedArityPostCallback(String methodName, Object producesNt, Object ... nonterminals)
    {
        return new CppHostRoutine(methodName, FixedArity, producesNt, nonterminals);
    }

    /**
     * Get a variadic post-order callback method, by name.
     * @param methodName    the callback method's name.
     * @param producesNt    the nonterminal callback's production produces.
     * @param nonterminals  the nonterminals of the callback's production's children.
     */
    @SuppressWarnings("unchecked")
    public HostRoutine getVariadicPostCallback(String methodName, Object producesNt, Object ... nonterminals)
    {
        return new CppHostRoutine(methodName, VariadicArity, producesNt, nonterminals);
    }

    String getNonterminalMapping(Object nt)
    {
        if (this.nonterminalMappings.containsKey(nt)) {
            return this.nonterminalMappings.get(nt);
        } else if (this.defaultMapping != null) {
            return this.defaultMapping;
        } else {
            throw new IllegalArgumentException(String.format("Nonterminal %s has no type mapping",nt));
        }
    }

    class CppHostRoutine extends HostRoutine<String>
    {
        final Object methodName;
        final boolean isVariadic;
        final Object producesNt;
        final Object[] nonterminals;

        CppHostRoutine(Object methodName, boolean isVariadic, Object producesNt, Object ... nonterminals)
        {
            this.methodName = methodName;
            this.isVariadic = isVariadic;
            this.producesNt = producesNt;
            this.nonterminals = nonterminals;
        }
        public String getName()
        {
            return methodName.toString();
        }

        public Object getDeclaringClass()
        {
            return visitorClassName;
        }

        public int getParameterCount()
        {
            // Add one for the Node type.
            return nonterminals.length + 1;
        }

        public String[] getParameterTypes()
        {
            String[] result = new String[getParameterCount()];
            result[0] = nodeClassName;

            for (int i = 0; i < nonterminals.length; i++) {
                result[i+1] = getNonterminalMapping(nonterminals[i]);
            }

            return result;
        }

        public String getParameterType(int index)
        {
            assert index > 0;
            return getNonterminalMapping(nonterminals[index-1]);
        }

        public boolean isVarArgs()
        {
            return isVariadic;
        }

        public String getVariadicComponentType()
        {
            return getParameterType(getVariadicOffset());
        }

        public int getVariadicOffset()
        {
            return nonterminals.length-1;
        }

        public Object invoke(Object receiver, Object... args)
        {
            throw new UnsupportedOperationException();
        }
    }
}
