package jburg;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * A PredicatedState is a composite state whose constituents
 * are permutations of a set of prerequisite predicates.
 */
class PredicatedState<Nonterminal, NodeType>
{
    /**
     * The constituent states, keyed by the methods satisfied;
     * all states in the permutation of the methods should be
     * present, except possibly the state with that satisfied
     * no predicates. The key lists are sorted by the methods'
     * hashCode, so that they have consistent hash and equality
     * semantics; the ordering has no other meaning to the BURM.
     */
    Map<List<Method>, State<Nonterminal, NodeType>> states = new HashMap<List<Method>, State<Nonterminal, NodeType>>();

    /**
     * A sorted and de-dup'd list of all predicate methods
     * found in the constituent states. Used to create
     * search keys for to-be-labeled nodes.
     */
    List<Method> predicates = new ArrayList<Method>();

    /**
     * The arity kind of the constituent states.
     */
    ArityKind compositeArityKind = null;

    /**
     * @param states this PredicatedState's constituent states.
     */
    PredicatedState(List<State<Nonterminal, NodeType>> states)
    {
        // Don't store duplicates.
        Set<Method> uniqueMethods = new HashSet<Method>();

        for (State<Nonterminal, NodeType> s: states) {
            assert !this.states.containsKey(s.predicates);

            if (compositeArityKind == null) {

                if (s.arityKind != null) {
                    compositeArityKind = s.arityKind;
                }
            } else if (s.arityKind != null && s.arityKind != compositeArityKind) {
                throw new IllegalStateException("Cannot mix fixed-size and variadic productions");
            }

            this.states.put(s.predicates, s);
            uniqueMethods.addAll(s.predicates);
        }

        for (State<Nonterminal, NodeType> s: states) {
            if (s.arityKind == null) {
                s.arityKind = compositeArityKind;
            }
        }

        this.predicates.addAll(uniqueMethods);
        Collections.sort(this.predicates, new MethodComparator());
    }

    /**
     * Get the state that matches an input node, by running
     * all available predicates and matching the resulting
     * list of satisfied methods to the available states.
     */
    State<Nonterminal, NodeType> getState(BurgInput<NodeType> node, Object visitor)
    throws IllegalAccessException, InvocationTargetException
    {
        List<Method> satisfiedPredicates = new ArrayList<Method>();

        for (Method m: this.predicates) {
            Boolean success = (Boolean)m.invoke(visitor, node);

            if (success) {
                satisfiedPredicates.add(m);
            }
        }

        // The state generator generated all
        // permutations of the predicates, with
        // the possible exception of the state
        // that satisfies no predicates.
        if (this.states.containsKey(satisfiedPredicates)) {
            return this.states.get(satisfiedPredicates);
        } else {
            assert satisfiedPredicates.isEmpty();
            return new State<Nonterminal,NodeType>();
        }
    }

    /**
     * Is this PredicatedState variadic?
     * @return true if the state accepts variadic inputs.
     */
    boolean isVarArgs()
    {
        return getArityKind() == ArityKind.Variadic;
    }

    /**
     * @return the states' composite ArityKind.
     */
    ArityKind getArityKind()
    {
        assert compositeArityKind != null;
        return compositeArityKind;
    }

    /**
     * Dump an abbreviated representation
     * of this composite state.
     * @param out   the dump sink.
     */
    void miniDump(java.io.PrintWriter out)
    throws java.io.IOException
    {
        out.printf("<predicatedState>\n");
        out.println("</predicatedState>");
    }
}
