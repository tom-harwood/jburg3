package jburg;

import java.lang.reflect.*;
import java.util.*;

/**
 * A ProductionTable hosts the data structures that
 * describe the client's BURS system, and the logic
 * that translates them into a transition table.
 */
public class ProductionTable<Nonterminal, NodeType>
{
    /**
     * Patterns applicable to null pointers; these are assembled
     * into a singleton state, with no operator, and the resulting
     * state is applied to all null pointers.
     */
    private List<NullPointerProduction<Nonterminal>> nullProductions = new ArrayList<NullPointerProduction<Nonterminal>>();;

    /**
     * The State that "derives" all null pointers.
     */
    State<Nonterminal, NodeType>    nullState = null;

    /**
     * Closures from the specification in entry order.
     */
    private List<Closure<Nonterminal>>  closures = new ArrayList<Closure<Nonterminal>>();

    /**
     * Nonterminals from the specification; there may be
     * multiple references to a nonterminal, and their
     * order here is not important.
     */
    private Set<Nonterminal>    nonterminals = new TreeSet<Nonterminal>();

    /**
     * Unique states, computed by permuting all inputs to each operator.
     * The states are mapped to themselves so they can be efficaciously retrieved.
     */
    private Map<State<Nonterminal, NodeType>, State<Nonterminal, NodeType>> states = new HashMap<State<Nonterminal, NodeType>, State<Nonterminal, NodeType>>();

    /**
     * States in entry order, used for faster lookup by state number
     * and to emit states in their "natural" order.
     */
    private List<State<Nonterminal, NodeType>> statesInEntryOrder = new ArrayList<State<Nonterminal, NodeType>>();

    /**
     * Operators computed from the specification, keyed by the nonterminal they produce.
     */
    private Map<NodeType, List<Operator<Nonterminal,NodeType>>> operators =
        new TreeMap<NodeType, List<Operator<Nonterminal,NodeType>>>();

    /**
     * RepresenterStates, mapped to themselves
     * so they can be efficaciously retrieved.
     */
    private Map<RepresenterState<Nonterminal,NodeType>, RepresenterState<Nonterminal,NodeType>> repStates =
        new HashMap<RepresenterState<Nonterminal,NodeType>, RepresenterState<Nonterminal,NodeType>>();

    /**
     * Pattern matchers by node type and nominal arity.
     * A fixed-arity pattern matcher's nominal arity is its number of children;
     * a variadic pattern matcher has entries in this list for every arity where
     * it overlaps with a fixed-arity pattern matcher, plus one extra nominal arity
     * where it does not overlap any fixed-arity pattern matcher that represents
     * the variadic tail.
     */
    private Map<NodeType, List<PatternMatcher<Nonterminal,NodeType>>> patternMatchersByNodeType =
        new TreeMap<NodeType, List<PatternMatcher<Nonterminal, NodeType>>>();

    /**
     * Add a pattern-matching production to the grammar.
     * @param nt            the nonterminal this production produces.
     * @param nodeType      the node type of the root of the subtree matched.
     * @param cost          the cost of this production.
     * @param preCallback   the callback run before deriving the subtree's child nodes.
     * @param postCallback  the callback run after deriving child nodes.
     * @param childTypes    the nonterminals the subtree's children must be able to produce.
     */
    @SuppressWarnings({"unchecked"})// TODO: @SafeVarargs would be a better annotation, but that would require Java 1.7 or above.
    public void addPatternMatch(Nonterminal nt, NodeType nodeType, int cost, Method preCallback, Method postCallback, Nonterminal... childTypes)
    {
        addPatternMatch(nt, nodeType, cost, null, preCallback, postCallback, false, childTypes);
    }

    /**
     * Add a pattern-matching production to the grammar, with unit cost.
     * @param nt            the nonterminal this production produces.
     * @param nodeType      the node type of the root of the subtree matched.
     * @param postCallback  the callback run after deriving the child nodes.
     * @param childTypes    the nonterminals the subtree's children must be able to produce.
     */
    @SuppressWarnings({"unchecked"})// TODO: @SafeVarargs would be a better annotation, but that would require Java 1.7 or above.
    public void addPatternMatch(Nonterminal nt, NodeType nodeType, Method postCallback, Nonterminal... childTypes)
    {
        addPatternMatch(nt, nodeType, 1, null, null, postCallback, false, childTypes);
    }

    /**
     * Add a variadic pattern-matching production to the grammar.
     * @param nt            the nonterminal this production produces.
     * @param nodeType      the node type of the root of the subtree matched.
     * @param cost          the cost of this production.
     * @param preCallback   the callback run before deriving the child nodes.
     * @param postCallback  the callback run after deriving the child nodes.
     * @param childTypes    the nonterminals the subtree's children must be able to produce;
     * the last nonterminal may be used more than once to cover the "tail" of a subtree's children.
     */
    @SuppressWarnings({"unchecked"})// TODO: @SafeVarargs would be a better annotation, but that would require Java 1.7 or above.
    public void addVarArgsPatternMatch(Nonterminal nt, NodeType nodeType, int cost, Method preCallback, Method postCallback, Nonterminal... childTypes)
    {
        addPatternMatch(nt, nodeType, cost, null, preCallback, postCallback, true, childTypes);
    }

    /**
     * Add a pattern matcher to its operator.
     * @param nt            the nonterminal this production produces.
     * @param nodeType      the node type of the root of the subtree matched.
     * @param cost          the cost of this production.
     * @param predicate     the semantic predicate guarding this pattern match,
     * or null if the pattern match has no predicate guard.
     * @param preCallback   the callback run before deriving the child nodes.
     * @param postCallback  the callback run after deriving the child nodes.
     * @param isVariadic    if true, then the final nonterminal in childTypes
     * may be used more than once to cover the "tail" of a subtree's children.
     * @param childTypes    the nonterminals the subtree's children must be able to produce.
     */
    @SuppressWarnings({"unchecked"})// TODO: @SafeVarargs would be a better annotation, but that would require Java 1.7 or above.
    public PatternMatcher<Nonterminal, NodeType> addPatternMatch(Nonterminal nt, NodeType nodeType, int cost, Method predicate, Method preCallback, Method postCallback, boolean isVarArgs, Nonterminal... childTypes)
    {
        PatternMatcher<Nonterminal,NodeType> patternMatcher = new PatternMatcher<Nonterminal,NodeType>(nt, nodeType, cost, predicate, preCallback, postCallback, isVarArgs, childTypes);
        nonterminals.add(nt);
        getPatternsForNodeType(nodeType).add(patternMatcher);

        // Add an Operator or Operators to handle this pattern, if one is not already present.
        if (fetchOperator(nodeType, patternMatcher.size()) == null) {

            if (!operators.containsKey(nodeType)) {
                operators.put(nodeType, new ArrayList<Operator<Nonterminal,NodeType>>());
            }

            List<Operator<Nonterminal,NodeType>> ops = operators.get(nodeType);

            if (ops.size() < patternMatcher.size() + 1) {
                ops.addAll(Collections.nCopies((patternMatcher.size() + 1) - ops.size(), (Operator<Nonterminal,NodeType>)null));
            }

            ops.set(patternMatcher.size(), new Operator<Nonterminal,NodeType>(nodeType, patternMatcher.size(), this));

            if (isVarArgs && patternMatcher.size() + 1 <= ops.size()) {
                int limit = ops.size();

                for (int i = patternMatcher.size() + 1; i <= limit; i++) {
                    ops.add(new Operator<Nonterminal,NodeType>(nodeType, patternMatcher.size(), this));
                }
            }
        }

        return patternMatcher;
    }

    /**
     * Add a closure to the grammar.
     * @param targetNt      the nonterminal this closure produces.
     * @param sourceNt      the nonterminal that must be produced before this closure runs.
     * @param cost          the closure's cost metric.
     * @param postCallback  the callback method run after the method that produces the source nonterminal completes.
     */
    public Closure<Nonterminal> addClosure(Nonterminal targetNt, Nonterminal sourceNt, int cost, Method method)
    {
        Closure<Nonterminal> closure = new Closure<Nonterminal>(targetNt, sourceNt, cost, method);
        closures.add(closure);
        return closure;
    }

    /**
     * Add a closure to the grammar, with no callback.
     * @param targetNt      the nonterminal this closure produces.
     * @param sourceNt      the nonterminal that must be produced before this closure runs.
     * @param cost          the closure's cost metric.
     */
    public Closure<Nonterminal> addClosure(Nonterminal targetNt, Nonterminal sourceNt, int cost)
    {
        return addClosure(targetNt, sourceNt, cost, null);
    }

    /**
     * Add a closure to the grammar, with unit cost.
     * @param targetNt      the nonterminal this closure produces.
     * @param sourceNt      the nonterminal that must be produced before this closure runs.
     * @param postCallback  the callback method run after the method that produces the source nonterminal completes.
     */
    public Closure<Nonterminal> addClosure(Nonterminal targetNt, Nonterminal sourceNt, Method method)
    {
        return addClosure(targetNt, sourceNt, 1, method);
    }

    /**
     * Add a closure to the grammar, with unit cost and no callbacks.
     */
    public Closure<Nonterminal> addClosure(Nonterminal targetNt, Nonterminal sourceNt)
    {
        return addClosure(targetNt, sourceNt, 1, null);
    }

    /**
     * Generate the states and transition tables for a grammar.
     * <ul>
     * <li> Begin by computing states for all leaf operators;
     * this forms the initial worklist.
     * <li> While the worklist contains candidate entries,
     * compute potentially novel transition table entries for
     * every non-leaf operator. This process generates new states,
     * which are appended to the worklist; the algorithm terminates
     * when all operators have processed all relevant transitions
     * and the worklist empties.
     * </ul>
     * @post states contains all state entries for the grammar,
     * and each operator's transition table contains mappings
     * to the corresponding states.
     */
    public void generateStates()
    {

        statesInEntryOrder.add(new ErrorState<Nonterminal, NodeType>());

        Queue<State<Nonterminal, NodeType>> worklist = generateLeafStates();

        if (!getNullPointerState().isEmpty()) {
            worklist.add(getNullPointerState());
        }

        while (worklist.peek() != null) {
            State<Nonterminal, NodeType> state = worklist.remove();

            for (List<Operator<Nonterminal,NodeType>> opList: operators.values()) {

                // Skip the leaf nodes; they're already done.
                for (int i = 1; i < opList.size(); i++) {
                    Operator<Nonterminal,NodeType> op = opList.get(i);

                    if (op != null) {
                        computeTransitions(op, state, worklist);
                    }
                }
            }
        }
    }

    /**
     * Add a production to act on a null pointer.
     * @param nt            the nonterminal this production produces.
     * @param cost          the cost of this production.
     * @param postCallback  the callback run by this production.
     */
    public Production<Nonterminal> addNullPointerProduction(Nonterminal nt, int cost, Method postCallback)
    {
        NullPointerProduction<Nonterminal> np = new NullPointerProduction<Nonterminal>(nt, cost, postCallback);
        nullProductions.add(np);
        nonterminals.add(nt);
        return np;
    }

    /**
     * Generate or retrieve the singleton state that specifies valid transitions for a null pointer.
     * If the grammar did not record any null productions, then this state is empty.
     * @return the state that specifies valid transitions for a null pointer.
     */
    State<Nonterminal, NodeType> getNullPointerState()
    {
        if (nullState == null) {
            nullState = new State<Nonterminal, NodeType>();

            for (NullPointerProduction<Nonterminal> p: nullProductions) {

                if (p.ownCost < nullState.getCost(p.target)) {
                    nullState.setNonClosureProduction(p, p.ownCost);
                }
            }

            closure(nullState);
            State<Nonterminal, NodeType> canonicalNullState = addState(nullState);
            assert canonicalNullState == nullState;
        }

        return nullState;
    }

    /**
     * Record a state's closure set.
     * @param state the state.
     */
    private void closure(State<Nonterminal,NodeType> state)
    {
        boolean closureRecorded;

        do {
            closureRecorded = false;

            for (Closure<Nonterminal> closure: this.closures) {
                closureRecorded = state.addClosure(closure);
            }

        } while (closureRecorded);
    }

    /**
     * Generate the table's leaf states.
     * @return the initial worklist, populated with the leaf states.
     */
    private Queue<State<Nonterminal, NodeType>> generateLeafStates()
    {
        Queue<State<Nonterminal, NodeType>> result = new ArrayDeque<State<Nonterminal, NodeType>>();

        for (NodeType nodeType: operators.keySet()) {
            Operator<Nonterminal, NodeType> leafOperator = fetchOperator(nodeType, 0);

            if (leafOperator != null) {
                List<State<Nonterminal, NodeType>> newStates = new ArrayList<State<Nonterminal, NodeType>>();
                newStates.add(new State<Nonterminal,NodeType>(leafOperator.nodeType));

                for (PatternMatcher<Nonterminal, NodeType> p: getPatternsForNodeType(nodeType)) {
                    if (p.isLeaf()) {
                        coalesceProduction(Collections.emptyList(), p, newStates);
                    }
                }

                // The set of non-empty leaf states is the basis for
                // computing the transition tables.
                for (State<Nonterminal, NodeType> state: newStates) {

                    if (state.size() > 0) {
                        closure(state);
                        result.add(addState(state));
                    }
                }

                leafOperator.createLeafState(newStates);
            }

        }

        return result;
    }

    /**
     * Try all permuations of an operator against a new state.
     * For each dimension of the operator, project a representer
     * state, from the new state, and permute the operator's sets
     * of representer states with the projected state to form
     * candidate states; add all applicable productions to the
     * candidate states, and then match candidates with any viable
     * productions against the master list of states to see if we've
     * found a new state. If so, append it to the worklist.
     * @param op        the operator
     * @param state     the new state.
     * @param workList  [out] the work list of states to process;
     * any novel states discovered while permuting the input state
     * against the operator are appended to worklist for subsequent
     * iterations of the driver loop.
     */
    private void computeTransitions(Operator<Nonterminal,NodeType> op, State<Nonterminal,NodeType> state, Queue<State<Nonterminal, NodeType>> workList)
    {
        int arity = op.size();


        for (int dim = 0; dim < arity; dim++) {

            RepresenterState<Nonterminal,NodeType> pState = project(op, dim, state);

            if (!pState.isEmpty() && !op.reps.get(dim).contains(pState)) {
                op.reps.get(dim).add(pState);

                // Try all permutations of the operator's nonterminal children
                // as operands to the pattern matching productions applicable
                // to the operator.
                for (List<RepresenterState<Nonterminal,NodeType>> repStates: op.generatePermutations(pState, dim)) {
                    // Each permutation generates a candidate state (called "result"
                    // in Proebsting's algorithm) with a cost matrix determined by
                    // the production's cost, plus the cost of the current permuatation
                    // of representer states; if this aggregate cost is less than the "unfeasible" cost
                    // Integer.MAX_VALUE and also less than the candidate's current best cost for
                    // the production's nonterminal, then add the production to the candidate.
                    List<State<Nonterminal, NodeType>> newStates = new ArrayList<State<Nonterminal, NodeType>>();
                    newStates.add(new State<Nonterminal,NodeType>(op.nodeType));

                    for (PatternMatcher<Nonterminal, NodeType> p: getPatternsForNodeType(op.nodeType)) {
                        if (p.acceptsDimension(arity)) {
                            coalesceProduction(repStates, p, newStates);
                        }
                    }

                    List<State<Nonterminal, NodeType>> nontrivialStates = new ArrayList<State<Nonterminal, NodeType>>();

                    for (int i = 0; i < newStates.size(); i++) {
                        State<Nonterminal, NodeType> resultState = newStates.get(i);

                        if (!resultState.isEmpty()) {

                            if (!this.states.containsKey(resultState)) {
                                // We know that we will be using this state as the canonical
                                // state, since no equivalent state is already stored. So it's
                                // safe to add it to the worklist now; it will get its state
                                // number via the call to addState().
                                closure(resultState);
                                workList.add(resultState);
                                nontrivialStates.add(addState(resultState));
                            } else {
                                // Get the canonical representation of the result state
                                // from the state table; the canonical representation
                                // is equivalent to the result state, but has a unique
                                // state number.
                                State<Nonterminal, NodeType> canonicalState = addState(resultState);
                                nontrivialStates.add(canonicalState);
                            }
                        }
                    }

                    if (nontrivialStates.size() > 0) {
                        op.addTransition(repStates, new PredicatedState<Nonterminal, NodeType>(nontrivialStates));
                    }
                }
            } else if (!pState.isEmpty()) {
                op.addRepresentedState(state, dim, pState);
            }
        }
    }

    /**
     * Coalesce a PatternMatcher into a list of States, potentially creating
     * new States if the PatternMatcher has a predicate that has not been seen.
     * @param repStates the active representer states for the in-flight transition.
     * repStates is empty if the PatternMatcher is a leaf.
     * @param p         the PatternMatcher.
     * @param newStates the list of states.
     * @post if the PatternMatcher has a predicate that does not appear in a
     * State previously added to the list, then that State will be copied
     * into a new State that includes predicate. This is done even if the
     * PatternMatcher is not the best fit for its nonterminal in the state;
     * this is so that the compile-time logic has a full permutation of
     * all the predicates, and can thus do a simple selection of the
     * appropriate State by running all predicates and forming a key
     * of all successful ones.
     * @post for all States in the list, both incumbent and new, the
     * PatternMatcher will recorded as the State's production for the
     * PatternMatcher's target nonterminal if the State can satisfy the
     * predicate method and the new PatternMatcher is the best fit.
     */
    void coalesceProduction(List<RepresenterState<Nonterminal,NodeType>> repStates, PatternMatcher<Nonterminal, NodeType> p, List<State<Nonterminal, NodeType>> newStates)
    {
        long cost = p.ownCost;

        for (int j = 0; j < repStates.size() && cost < Integer.MAX_VALUE; j++) {
            cost += repStates.get(j).getCost(p.getNonterminal(j));
        }

        for (int i = 0; i < newStates.size(); i++) {
            State<Nonterminal, NodeType> candidate = newStates.get(i);

            if (p.hasPredicate()) {

                if (candidate.satisfiesPredicate(p.predicate)) {

                    if (cost < candidate.getCost(p.target)) {
                        candidate.setNonClosureProduction(p,cost);
                    }

                } else {
                    State<Nonterminal, NodeType> predicatedState = new State<Nonterminal, NodeType>(candidate, p.predicate);
                    newStates.add(predicatedState);

                    if (cost < predicatedState.getCost(p.target)) {
                        predicatedState.setNonClosureProduction(p,cost);
                    }
                }
            } else if (cost < candidate.getCost(p.target)) {
                candidate.setNonClosureProduction(p,cost);
            }
        }
    }

    /**
     * Form the representer state for a tuple (operator, dimension, state).
     * @param op    the Operator.
     * @param i     the dimension.
     * @param state the potentially novel state.
     * @return a representer state, which has best-cost entries for all patterns
     * relevant to the operator that can use the state's i'th dimension.
     * Note that the returned representer state has the input state's node type,
     * not the operator's node type; the projected state is a representation of
     * the input state against the operator, and so it needs to know the originating
     * state's node type to preserve the originating state's associative behavior.
     */
    private RepresenterState<Nonterminal,NodeType> project(Operator<Nonterminal,NodeType> op, int i, State<Nonterminal,NodeType> state)
    {
        RepresenterState<Nonterminal,NodeType> candidate = new RepresenterState<Nonterminal,NodeType>(state.nodeType);

        for (Nonterminal n: nonterminals) {
            for (PatternMatcher<Nonterminal, NodeType> p: getPatternsForNodeType(op.nodeType)) {
                if (p.usesNonterminalAt(n, i) && state.getCost(n) < candidate.getCost(n)) {
                    candidate.setCost(n, state.getCost(n));
                }
            }
        }

        RepresenterState<Nonterminal, NodeType> result = addRepresenterState(candidate);
        result.representedStates.add(state);
        return result;
    }

    /**
     * Get the list of pattern matchers for a node type.
     * @param nodeType  the node type of interest.
     * @return the list of pattern matchers for this node type.
     * @post If no list was present, it has been created.
     */
    private List<PatternMatcher<Nonterminal,NodeType>> getPatternsForNodeType(NodeType nodeType)
    {
        if (!patternMatchersByNodeType.containsKey(nodeType)) {
            patternMatchersByNodeType.put(nodeType, new ArrayList<PatternMatcher<Nonterminal,NodeType>>());
        }

        return patternMatchersByNodeType.get(nodeType);
    }

    /**
     * Add a potentially novel state.
     * @param state the potentially novel state.
     * @return the canonical representation of state;
     * this may be the input state, if it is in fact novel,
     * or it may be an existing equivalent state.
     */
    private State<Nonterminal, NodeType> addState(State<Nonterminal, NodeType> state)
    {
        if (!this.states.containsKey(state)) {
            states.put(state,state);
            state.number = this.states.size();
            statesInEntryOrder.add(state);
        }

        return states.get(state);
    }

    /**
     * Add a potentially novel representer state.
     * It is not as important to create a minimized set of representer states
     * as it is to create a minimized set of states; this is mostly done as a
     * convenience for table-generating logic, which can thus rely on the mapping
     * from represented state to states in the canonical representer state.
     * @param rs    the potentially novel representer state.
     * @return the canonical version of the state.
     */
    private RepresenterState<Nonterminal,NodeType> addRepresenterState(RepresenterState<Nonterminal,NodeType> rs)
    {
        if (!repStates.containsKey(rs)) {
            repStates.put(rs, rs);
        }

        return repStates.get(rs);
    }

    /**
     * Get a state by number.
     */
    State<Nonterminal, NodeType> getState(int stateNumber) {

        if (stateNumber < 0 || stateNumber > statesInEntryOrder.size()) {
            throw new IllegalArgumentException(String.format("State number %d is out of range 0..%d", stateNumber, states.size()));
        }

        return statesInEntryOrder.get(stateNumber);
    }


    /**
     * Get the Operator that handles a nodeType(nt*) tuple.
     * @param nodeType  the node type of interest.
     * @param arity     the actual arity of the subtree.
     */
    Operator<Nonterminal, NodeType> getOperator(NodeType nodeType, int arity)
    {
        List<Operator<Nonterminal, NodeType>> opsForNodeType = operators.get(nodeType);

        if (opsForNodeType != null) {
            if (opsForNodeType.size() > arity) {
                return opsForNodeType.get(arity);
            } else if (opsForNodeType.get(opsForNodeType.size()-1).isVarArgs()) {
                return opsForNodeType.get(opsForNodeType.size()-1);
            }
        }

        return null;
    }

    /**
     * Get the Operator of a specific node type and arity.
     * This differs from getOperator(), the external API,
     * in that fetchOperator() handles variadic operators
     * using their fixed arity; this is necessary to build
     * the operator lists in the first place.
     * @param nodeType  the node type of interest.
     * @param arity     the actual arity of the subtree.
     */
    private Operator<Nonterminal, NodeType> fetchOperator(NodeType nodeType, int arity)
    {
        List<Operator<Nonterminal, NodeType>> opsForNodeType = operators.get(nodeType);

        if (opsForNodeType != null && opsForNodeType.size() > arity) {
            return opsForNodeType.get(arity);
        } else {
            return null;
        }
    }

    /**
     * Dump the production table, if a destination was provided.
     * @param dumpPath  a pathname for the dump, or null.
     */
    public void dump(String dumpPath)
    {
        if (dumpPath != null) {
            try {
                dump(new java.io.PrintWriter(dumpPath));
            } catch (java.io.IOException cannotDump) {
                cannotDump.printStackTrace();
            }
        }
    }

    /**
     * Dump the production table.
     * @param out   the sink.
     */
    public void dump(java.io.PrintWriter out)
    throws java.io.IOException
    {
        out.printf("<burmDump date=\"%s\">\n", new Date());

        out.println("<stateTable>");
        for (State<Nonterminal, NodeType> s: statesInEntryOrder) {
            s.dump(out);
        }
        out.println("</stateTable>");

        out.println();

        out.println("<transitionTable>");
        for (NodeType nodeType: operators.keySet()) {

            for (Operator<Nonterminal,NodeType> op: operators.get(nodeType)) {

                if (op != null) {
                    op.dump(out);
                }
            }
        }
        out.println("</transitionTable>");

        out.println("</burmDump>");

        out.flush();
    }
}
