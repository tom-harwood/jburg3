package jburg;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BURMSemantics' primary responsibility is finding
 * callback routines for rules; it also validates
 * these callback routines' return types to ensure
 * that they are compatible with the BURM's declared
 * nonterminal-to-host type mappings.
 */
public class BURMSemantics<Nonterminal>
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
     * The class of the nonterminal enumeration, which is passed to pre callbacks.
     * We can't get this from the Nonterminal template parameter because Java
     * uses type erasure to implement templates.
     */
    private final Class<?> nonterminalClass;

    /**
     * Nonterminals whose classes are known.
     */
    private final Map<Nonterminal,Class<?>> nonterminalClasses = new HashMap<Nonterminal,Class<?>>();

    /**
     * Default class to be assigned to nonterminals with no explicit class.
     */
    private Class<?> defaultClass = null;

    /**
     * Diagnostics emitted during processing, e.g., return type mismatches.
     */
    private final List<String> diagnostics = new ArrayList<String>();

    public BURMSemantics(Class<?> visitorClass, Class<?> nodeClass, Class<?> nonterminalClass)
    {
        this.visitorClass       = visitorClass;
        this.nodeClass          = nodeClass;
        this.nonterminalClass   = nonterminalClass;
    }

    /**
     * Create a nonterminal-host class mapping.
     * @param nt    the nonterminal.
     * @param clazz the host language class.
     */
    public void setNonterminalClass(Nonterminal nt, Class<?> clazz)
    {
        if (nonterminalClasses.containsKey(nt)) {
            throw new IllegalArgumentException(String.format("Nonterminal %s already mapped to %s", nt, clazz.getName()));
        } else {
            nonterminalClasses.put(nt,clazz);
        }
    }
    public void setDefaultNonterminalClass(Class<?> defaultClass)
    {
        if (this.defaultClass == null) {
            this.defaultClass = defaultClass;
        } else if (defaultClass.equals(this.defaultClass)) {
            // noop.
        } else {
            throw new IllegalStateException(String.format("Default nonterminal class already set to %s", this.defaultClass.getName()));
        }
    }

    /**
     * Get a pre-order callback method, by name.
     * @param methodName    the callback method's name.
     */
    public Method getPreCallback(String methodName)
    throws NoSuchMethodException
    {
        Class<?>[] parameterTypes = {nodeClass, nonterminalClass};
        return findMethod(methodName, Void.TYPE, parameterTypes);
    }

    /**
     * Get a predicate method, by name.
     * @param methodName    the predicate method's name.
     */
    public Method getPredicate(String methodName)
    throws NoSuchMethodException
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
    final public Method getPostCallback(String methodName, boolean isVariadic, Nonterminal producesNt, Nonterminal ... nonterminals)
    throws NoSuchMethodException
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
    final public Method getFixedArityPostCallback(String methodName, Nonterminal producesNt, Nonterminal ... nonterminals)
    throws NoSuchMethodException
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
    final public Method getVariadicPostCallback(String methodName, Nonterminal producesNt, Nonterminal ... nonterminals)
    throws NoSuchMethodException
    {
        return getPostCallback(methodName, true, producesNt, nonterminals);
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
    private Method findMethod(String methodName, Class<?> returnType, Class<?>[] parameterTypes)
    throws NoSuchMethodException
    {
        Method result = null;

        try {
            result = visitorClass.getMethod(methodName, parameterTypes);

        } catch (NoSuchMethodException noMethod) {

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

        return result;
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
    private Class<?> getClassFor(Nonterminal nt, boolean isVarArgs)
    {
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
}
