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
     * Construct a PredicatedState whose contents will
     * be added via successive calls to addTransition().
     */
    PredicatedState()
    {
    }

    /**
     * @param states this PredicatedState's constituent states.
     */
    PredicatedState(List<State<Nonterminal, NodeType>> states)
    {
        for (State<Nonterminal, NodeType> s: states) {
            // Each key should be unique.
            assert !this.states.containsKey(s.predicates);
            addArityKind(s);
            this.states.put(s.predicates, s);
            addPredicates(s.predicates);
        }

        for (State<Nonterminal, NodeType> s: states) {
            if (s.arityKind == null) {
                s.arityKind = compositeArityKind;
            }
        }

        Collections.sort(this.predicates, new MethodComparator());
    }

    /**
     * Add transition entries from an input state.
     * @param src   the state whose entries are to be coalesced.
     */
    void addTransition(State<Nonterminal, NodeType> src)
    {
        State<Nonterminal, NodeType> dst;

        if (this.states.containsKey(src.predicates)) {
            dst = this.states.get(src.predicates);

            for (Production<Nonterminal> p: src.getNonClosureProductions()) {

                if (src.getCost(p.target) < dst.getCost(p.target)) {
                    dst.setNonClosureProduction(p, src.getCost(p.target));

                    for (Closure<Nonterminal> c: src.getClosuresTo(p)) {
                        dst.addClosure(c);
                    }
                }
            }
        } else {
            dst = new State<Nonterminal, NodeType>(src);
            dst.number = src.number;
            this.states.put(dst.predicates, dst);
        }

        addPredicates(src.predicates);
        Collections.sort(this.predicates, new MethodComparator());
        addArityKind(src);
    }

    /**
     * Combine a new State's arity kind into this
     * PredicatedState's composite arity kind.
     * @param s the state.
     */
    private void addArityKind(State<Nonterminal, NodeType> s)
    {
        if (compositeArityKind == null) {

            if (s.arityKind != null) {
                compositeArityKind = s.arityKind;
            }
        } else if (s.arityKind != null && s.arityKind != compositeArityKind) {
            compositeArityKind = ArityKind.Variadic;
        }
    }

    /**
     * Coalesce a new predicate list into the
     * list of all predicates in this predicated state.
     * @param srcPredicates the new predicates. This list may be empty.
     */
    private void addPredicates(List<Method> srcPredicates)
    {
        for (Method m: srcPredicates) {
            if (!this.predicates.contains(m)) {
                this.predicates.add(m);
            }
        }
    }

    /**
     * Get the state that matches an input node, by running
     * all available predicates and matching the resulting
     * list of satisfied methods to the available states.
     */
    State<Nonterminal, NodeType> getState(BurgInput<Nonterminal, NodeType> node, Object visitor)
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

    private static final List<Method> noGuard = new ArrayList<Method>();
    @Override
    public String toString()
    {
        if (states.size() == 1 && states.containsKey(noGuard)) {
            return String.format("PredicatedState(trival) %s", states.get(noGuard));
        } else {
            return String.format("PredicatedState%s", states);
        }
    }
}
