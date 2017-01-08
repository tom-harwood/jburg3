package jburg.semantics;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JavaSemantics is a BURMSemantics
 * implementation based on Java reflection.
 */
public class JavaSemantics<Nonterminal,NodeType> implements BURMSemantics<Nonterminal, NodeType>
{
    /**
     * The class of the visitor that hosts the callback routines.
     */
    private final Class<?> visitorClass;

    /**
     * The class of the AST node, which is passed to callbacks.
     */
    private final Class<?> nodeClass;

    /**
     * The class of the nonterminal enumeration.
     */
    private final Class<?> nonterminalClass;

    /**
     * The class of the node tpye enumeration.
     */
    private final Class<?> nodeTypeClass;

    /**
     * Nonterminals whose classes are known.
     */
    private final Map<Object,Class<?>> nonterminalClasses = new HashMap<Object,Class<?>>();

    /**
     * Default class to be assigned to nonterminals with no explicit class.
     */
    private Class<?> defaultClass = null;

    /**
     * Diagnostics emitted during processing, e.g., return type mismatches.
     */
    private final List<String> diagnostics = new ArrayList<String>();

    /**
     * @param visitorClassName  the name of the visitor class.
     * @param nodeClassName     the name of the nodes' class.
     * @param nonterminalClass  the class of the nonterminals.
     */
    public JavaSemantics(String visitorClassName, String nodeClassName, String nodeTypeClassName, String nonterminalClassName)
    {
        this.visitorClass       = getClass(visitorClassName);
        this.nodeClass          = getClass(nodeClassName);
        this.nodeTypeClass      = getClass(nodeTypeClassName);
        this.nonterminalClass   = getClass(nonterminalClassName);
    }

    /**
     * Construct a JavaSemantics instance with no clear idea
     * of the actual semantics of the grammar; useful for
     * ad-hoc construction of HostRoutine instances where
     * a method is already available.
     */
    public JavaSemantics()
    {
        this("java.lang.Object", "java.lang.Object", "java.lang.Object", "java.lang.Object");
    }

    /**
     * Create a nonterminal-host class mapping.
     * @param nt    the nonterminal.
     * @param clazz the host language class.
     */
    public void setNonterminalClass(Object ntName, Object clazz)
    {
        Object nt = getNonterminal(ntName);

        if (nonterminalClasses.containsKey(nt)) {
            throw new IllegalArgumentException(String.format("Nonterminal %s already mapped to %s", nt, clazz));
        } else {
            nonterminalClasses.put(nt,getClass(clazz));
        }
    }

    public void setDefaultNonterminalClass(Object defaultClass)
    {
        if (this.defaultClass == null) {
            this.defaultClass = getClass(defaultClass);
        } else if (defaultClass.equals(this.defaultClass)) {
            // noop.
        } else {
            throw new IllegalStateException(String.format("Default nonterminal class already set to %s", this.defaultClass.getName()));
        }
    }

    Class<?> getClass(Object className)
    {
        try {
            return Class.forName(className.toString());
        } catch (Exception noSuchClass) {
            throw new IllegalArgumentException(String.format("Unable to locate class \"%s\" due to %s", className, noSuchClass));
        }
    }

    /**
     * Get a pre-order callback method, by name.
     * @param methodName    the callback method's name.
     */
    public HostRoutine getPreCallback(String methodName)
    throws Exception
    {
        Class<?>[] parameterTypes = {nodeClass, nonterminalClass};
        return findMethod(methodName, Void.TYPE, parameterTypes);
    }

    /**
     * Get a predicate method, by name.
     * @param methodName    the predicate method's name.
     */
    public HostRoutine getPredicate(String methodName)
    throws Exception
    {
        Class<?>[] parameterTypes = {nodeClass};
        return findMethod(methodName, Boolean.TYPE, parameterTypes);
    }

    /**
     * Get a post-order callback method, by name.
     * @param methodName    the callback method's name.
     * @param isVariadic    true if the production is variadic.
     * @param producesNt    the nonterminal callback's production produces.
     * @param nonterminals  the nonterminals of the callback's production's children.
     */
    @SafeVarargs
    final public HostRoutine getPostCallback(String methodName, boolean isVariadic, Object producesNt, Object ... nonterminals)
    throws Exception
    {
        Class<?> returnType         = getClassFor(producesNt, false);
        Class<?> parameterTypes[]   = new Class<?>[nonterminals.length + 1];

        parameterTypes[0] = nodeClass;

        for (int i = 0; i < nonterminals.length; i++) {
            boolean isLastParam = i+1 == nonterminals.length;
            parameterTypes[i+1] = getClassFor(nonterminals[i], isVariadic && isLastParam);
        }

        return findMethod(methodName, returnType, parameterTypes);
    }

    /**
     * Get a fixed-arity post-order callback method, by name.
     * @param methodName    the callback method's name.
     * @param producesNt    the nonterminal callback's production produces.
     * @param nonterminals  the nonterminals of the callback's production's children.
     */
    @SafeVarargs
    final public HostRoutine getFixedArityPostCallback(String methodName, Object producesNt, Object ... nonterminals)
    throws Exception
    {
        return getPostCallback(methodName, false, producesNt, nonterminals);
    }

    /**
     * Get a variadic post-order callback method, by name.
     * @param methodName    the callback method's name.
     * @param producesNt    the nonterminal callback's production produces.
     * @param nonterminals  the nonterminals of the callback's production's children.
     */
    @SafeVarargs
    final public HostRoutine getVariadicPostCallback(String methodName, Object producesNt, Object ... nonterminals)
    throws Exception
    {
        return getPostCallback(methodName, true, producesNt, nonterminals);
    }

    public Object getNonterminal(Object ntName)
    {
        return ntName.toString();
    }

    /**
     * Find a method by name.
     * Limited support for overloading is provided; if a method cannot be found
     * with the exact signature requested, but there is one and only one method
     * whose parameter types are assignable from the requested parameter types,
     * then that method will be returned.
     * @param   methodName      the name of the desired method.
     * @param   returnType      the method's return type. Used to validate semantics.
     * @param   parameterTypes  the types of the parameters; as noted above, limited
     * support for overloading is provided.
     */
    private HostRoutine findMethod(String methodName, Class<?> returnType, Class<?>[] parameterTypes)
    throws Exception
    {
        Method result = null;

        try {
            result = visitorClass.getMethod(methodName, parameterTypes);

        } catch (Exception noMethod) {

            Method[] allMethods = visitorClass.getDeclaredMethods();

            int candidate1 = findOverloadedMethod(allMethods, 0, methodName, parameterTypes);
            
            if (candidate1 != -1) {
                int candidate2 = findOverloadedMethod(allMethods, candidate1+1, methodName, parameterTypes);

                if (candidate2 != -1) {
                    // Emulating the Java compiler is one alternative here, but
                    // cross-platform compiler simulation would complicate things.
                    throw new IllegalStateException(
                        String.format("Unable to disambiguate methods %s and %s", allMethods[candidate1], allMethods[candidate2])
                    );
                }

                result = allMethods[candidate1];
            } else {
                throw noMethod;
            }
        }

        if (result.getReturnType() != returnType) {
            diagnostics.add(String.format("Method %s produces %s, expected %s", methodName, result.getReturnType(), returnType));
        }

        return new MethodWrapperHostRoutine(result);
    }

    /**
     * Try and find one and only one method with the given name,
     * whose parameter types are assignable from the given types.
     * @param   methods     the visitor class' declared methods.
     * @param   startPos    the position to start the search.
     * @param   methodName  the name of the desired method.
     * @param   argTypes    the types of the method's "actual" arguments.
     */
    @SafeVarargs
    final private int findOverloadedMethod(Method[] methods, int startPos, String methodName,  Class<?> ... argTypes)
    {
        for (int i = startPos; i < methods.length; i++) {
            Method candidate = methods[i];

            if (candidate.getName().equals(methodName)) {
                Class<?>[] parameterTypes = candidate.getParameterTypes();

                if (argTypes.length == parameterTypes.length) {
                    boolean validParameterTypes = true;

                    for (int j = 0; validParameterTypes && j < argTypes.length; j++) {
                        validParameterTypes = parameterTypes[j].isAssignableFrom(argTypes[j]);
                    }

                    if (validParameterTypes) {
                        return i;
                    }
                }
            }
        }

        return -1;
    }

    /**
     * Get the class corresponding to a nonterminal.
     * @param nt        the nonterminal.
     * @param isVarArgs true if the result class should
     * be that of an array of the corresponding type.
     */
    private Class<?> getClassFor(Object ntName, boolean isVarArgs)
    {
        Object nt = getNonterminal(ntName);

        // First find the class mapped to the nonterminal.
        Class<?> baseClass;

        if (nonterminalClasses.containsKey(nt)) {
            baseClass = nonterminalClasses.get(nt);

        } else if (defaultClass != null) {
            baseClass = defaultClass;

        } else {
            throw new IllegalArgumentException(String.format("Nonterminal %s has no mapping", nt));
        }

        // If the argument is variadic, the result class must reflect that.
        Class<?> result;
        if (isVarArgs) {

            try {
                result = Array.newInstance(baseClass, 0).getClass();
            } catch (Exception ex) {
                throw new IllegalArgumentException(
                    String.format("Unable to translate variadic NT %s to %s[] due to %s",
                    nt, baseClass, ex.getMessage()
                ));
            }

        } else {
            result = baseClass;
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public NodeType getNodeType(Object typeName)
    {
        for (Object nt: nodeTypeClass.getEnumConstants()) {
            if (nt.toString().equals(typeName.toString())) {
                    return (NodeType)nt;
            }
        }

        throw new IllegalArgumentException(String.format("enumeration %s does not contain %s", nodeTypeClass, typeName));
    }

    /**
     * Get the host class a nonterminal maps to.
     * @param ntName the name of the nonterminal.
     * @return the mapped host class for that nonterminal.
     */
    public Object getNonterminalMapping(Object ntName)
    {
        if (this.nonterminalClasses.containsKey(ntName.toString())) {
            return this.nonterminalClasses.get(ntName.toString()).getName();
        } else if (this.defaultClass != null) {
            return this.defaultClass.getName();
        } else {
            throw new IllegalArgumentException(String.format("Nonterminal %s has no type mapping",ntName));
        }
    }

    public HostRoutine getHostRoutine(Method m)
    {
        return new MethodWrapperHostRoutine(m);
    }

    class MethodWrapperHostRoutine extends HostRoutine<Class>
    {
        final Method        m;
        final Class<?>[]    parameterTypes;
        MethodWrapperHostRoutine(Method m)
        {
            this.m = m;
            this.parameterTypes = m.getParameterTypes();
        }

        public String getName()
        {
            return m.getName();
        }

        public Object getDeclaringClass()
        {
            return m.getDeclaringClass();
        }

        public int getParameterCount()
        {
            return m.getParameterCount();
        }

        public Class[] getParameterTypes()
        {
            return parameterTypes;
        }

        public Class getParameterType(int index)
        {
            return parameterTypes[index];
        }

        public boolean isVarArgs()
        {
            return m.isVarArgs();
        }

        public Class getVariadicComponentType()
        {
            assert isVarArgs();
            return parameterTypes[parameterTypes.length-1].getComponentType();
        }

        public int getVariadicOffset()
        {
            assert isVarArgs();
            assert parameterTypes.length > 1;
            return parameterTypes.length - 2;
        }

        public Object invoke(Object receiver, Object... args)
        throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
        {
            return m.invoke(receiver, args);
        }

        public BURMSemantics getSemantics()
        {
            return JavaSemantics.this;
        }
    }
}
