package jburg;

import jburg.semantics.HostRoutine;
import java.util.*;

import jburg.emitter.*;

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
     * The State that is assigned to inputs that don't
     * match any transition in the table.
     */
     ErrorState<Nonterminal,NodeType> errorState = new ErrorState<Nonterminal, NodeType>();

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
    private Set<Object>    nonterminals = new HashSet<Object>();
    public Set<Object> getNonterminals()
    {
        return nonterminals;
    }

    /**
     * Unique states, computed by permuting all inputs to each operator.
     * The states are mapped to themselves so they can be efficaciously retrieved.
     */
    private Map<State<Nonterminal, NodeType>, State<Nonterminal, NodeType>> states = new HashMap<State<Nonterminal, NodeType>, State<Nonterminal, NodeType>>();

    /** Manifest constant input to the the label routine; label subtrees and the root node. */
    public static boolean LABEL_DEEP = true;

    /** Manifest constant input to the the label routine; only label the root node. */
    public static boolean LABEL_SHALLOW = false;

    /**
     * States in entry order, used for faster lookup by state number
     * and to emit states in their "natural" order.
     */
    List<State<Nonterminal, NodeType>> statesInEntryOrder = new ArrayList<State<Nonterminal, NodeType>>();

    List<State<Nonterminal, NodeType>> getStateTable()
    {
        return statesInEntryOrder;
    }

    /**
     * Operators computed from the specification, keyed by their node type.
     */
    Map<NodeType, List<Operator<Nonterminal,NodeType>>> operators =
        new HashMap<NodeType, List<Operator<Nonterminal,NodeType>>>();

    /**
     * @return the operators in this production table.
     */
    public Collection<List<Operator<Nonterminal,NodeType>>> getOperators()
    {
        return operators.values();
    }

    /**
     * @return the node type to operator mappings in this production table.
     */
    public Map<NodeType, List<Operator<Nonterminal,NodeType>>> getOperatorsByNodeType()
    {
        return operators;
    }

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
        new HashMap<NodeType, List<PatternMatcher<Nonterminal, NodeType>>>();

    /**
     * The predetermined state number of the error state.
     */
    public static final int ERROR_STATE_NUM = 0;
    public static final int NULL_STATE_NUM = 1;

    /** Emit diagnostic information when this is set. */
    boolean verbose = false;

    private final HostRoutine noPredicate = null;
    private final HostRoutine noPreCallback = null;
    private final HostRoutine noPostCallback = null;

    /** If an operator matches this pattern (when it's set), turn on verbose mode for its state transition computations. */
    String  verboseTrigger = null;

    /**
     * Add a pattern-matching production to the grammar, with unit cost.
     * @param nt            the nonterminal this production produces.
     * @param nodeType      the node type of the root of the subtree matched.
     * @param postCallback  the callback run after deriving the child nodes.
     * @param childTypes    the nonterminals the subtree's children must be able to produce.
     */
    final public void addPatternMatch(Object nt, NodeType nodeType, HostRoutine postCallback, List<Object> childTypes)
    {
        addPatternMatch(nt, nodeType, 1, noPredicate, noPreCallback, postCallback, false, childTypes);
    }

    /**
     * Add a pattern-matching production to the grammar, with unit cost.
     * @param nt            the nonterminal this production produces.
     * @param nodeType      the node type of the root of the subtree matched.
     * @param preCallback   the callback run before deriving the child nodes.
     * @param postCallback  the callback run after deriving the child nodes.
     * @param childTypes    the nonterminals the subtree's children must be able to produce.
     */
    final public void addPatternMatch(Object nt, NodeType nodeType, HostRoutine preCallback, HostRoutine postCallback, List<Object> childTypes)
    {
        addPatternMatch(nt, nodeType, 1, noPredicate, preCallback, postCallback, false, childTypes);
    }

    /**
     * Add a pattern-matching production to the grammar, with unit cost.
     * @param nt            the nonterminal this production produces.
     * @param nodeType      the node type of the root of the subtree matched.
     * @param predicate     the semantic predicate guarding this pattern match.
     * @param preCallback   the callback run before deriving the child nodes.
     * @param postCallback  the callback run after deriving the child nodes.
     * @param childTypes    the nonterminals the subtree's children must be able to produce.
     */
    final public void addPatternMatch(Object nt, NodeType nodeType, HostRoutine predicate, HostRoutine preCallback, HostRoutine postCallback, List<Object> childTypes)
    {
        addPatternMatch(nt, nodeType, 1, predicate, preCallback, postCallback, false, childTypes);
    }

    /**
     * Add a pattern-matching production to the grammar, with unit cost.
     * @param nt            the nonterminal this production produces.
     * @param nodeType      the node type of the root of the subtree matched.
     * @param postCallback  the callback run after deriving the child nodes.
     */
    final public void addPatternMatch(Object nt, NodeType nodeType, HostRoutine postCallback)
    {
        addPatternMatch(nt, nodeType, 1, noPredicate, noPreCallback, postCallback, false, Collections.emptyList());
    }

    /**
     * Add a pattern-matching production to the grammar, with unit cost.
     * @param nt            the nonterminal this production produces.
     * @param nodeType      the node type of the root of the subtree matched.
     * @param preCallback   the callback run before deriving the child nodes.
     * @param postCallback  the callback run after deriving the child nodes.
     */
    final public void addPatternMatch(Object nt, NodeType nodeType, HostRoutine preCallback, HostRoutine postCallback)
    {
        addPatternMatch(nt, nodeType, 1, noPredicate, preCallback, postCallback, false, Collections.emptyList());
    }

    /**
     * Add a pattern-matching production to the grammar, with unit cost.
     * @param nt            the nonterminal this production produces.
     * @param nodeType      the node type of the root of the subtree matched.
     * @param predicate     the semantic predicate guarding this pattern match.
     * @param preCallback   the callback run before deriving the child nodes.
     * @param postCallback  the callback run after deriving the child nodes.
     */
    final public void addPatternMatch(Object nt, NodeType nodeType, HostRoutine predicate, HostRoutine preCallback, HostRoutine postCallback)
    {
        addPatternMatch(nt, nodeType, 1, predicate, preCallback, postCallback, false, Collections.emptyList());
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
    final public void addVarArgsPatternMatch(Object nt, NodeType nodeType, int cost, HostRoutine preCallback, HostRoutine postCallback, List<Object> childTypes)
    {
        addPatternMatch(nt, nodeType, cost, noPredicate, preCallback, postCallback, true, childTypes);
    }

    /**
     * Add a variadic pattern-matching production to the grammar, with unit cost and no precallback.
     * @param nt            the nonterminal this production produces.
     * @param nodeType      the node type of the root of the subtree matched.
     * @param cost          the cost of this production.
     * @param postCallback  the callback run after deriving the child nodes.
     * @param childTypes    the nonterminals the subtree's children must be able to produce;
     * the last nonterminal may be used more than once to cover the "tail" of a subtree's children.
     */
    final public void addVarArgsPatternMatch(Object nt, NodeType nodeType, HostRoutine postCallback, List<Object> childTypes)
    {
        addPatternMatch(nt, nodeType, 1, noPredicate, noPreCallback, postCallback, true, childTypes);
    }

    /**
     * Add a variadic pattern-matching production to the grammar, with unit cost and no precallback.
     * @param nt            the nonterminal this production produces.
     * @param nodeType      the node type of the root of the subtree matched.
     * @param cost          the cost of this production.
     * @param postCallback  the callback run after deriving the child nodes.
     * @param childTypes    the nonterminals the subtree's children must be able to produce;
     * the last nonterminal may be used more than once to cover the "tail" of a subtree's children.
     */
    final public void addVarArgsPatternMatch(Object nt, NodeType nodeType, HostRoutine preCallback, HostRoutine postCallback, List<Object> childTypes)
    {
        addPatternMatch(nt, nodeType, 1, noPredicate, preCallback, postCallback, true, childTypes);
    }

    /**
     * Add a variadic pattern-matching production to the grammar, with unit cost and no precallback.
     * @param nt            the nonterminal this production produces.
     * @param nodeType      the node type of the root of the subtree matched.
     * @param cost          the cost of this production.
     * @param postCallback  the callback run after deriving the child nodes.
     * @param childTypes    the nonterminals the subtree's children must be able to produce;
     * the last nonterminal may be used more than once to cover the "tail" of a subtree's children.
     */
    final public void addVarArgsPatternMatch(Object nt, NodeType nodeType, HostRoutine postCallback)
    {
        addPatternMatch(nt, nodeType, 1, noPredicate, noPreCallback, postCallback, true, Collections.emptyList());
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
    public PatternMatcher<Nonterminal, NodeType> addPatternMatch(Object nt, NodeType nodeType, int cost, HostRoutine predicate, HostRoutine preCallback, HostRoutine postCallback, boolean isVarArgs, List<Object> childTypes)
    {
        PatternMatcher<Nonterminal,NodeType> patternMatcher = new PatternMatcher<Nonterminal,NodeType>(nt, nodeType, cost, predicate, preCallback, postCallback, isVarArgs, childTypes);
        nonterminals.add(nt);
        getPatternsForNodeType(nodeType).add(patternMatcher);

        // Add an Operator to handle this pattern, if one is not already present.
        if (fetchOperator(nodeType, patternMatcher.size()) == null) {

            if (!operators.containsKey(nodeType)) {
                operators.put(nodeType, new ArrayList<Operator<Nonterminal,NodeType>>());
            }

            List<Operator<Nonterminal,NodeType>> ops = operators.get(nodeType);

            if (ops.size() < patternMatcher.size() + 1) {
                ops.addAll(Collections.nCopies((patternMatcher.size() + 1) - ops.size(), (Operator<Nonterminal,NodeType>)null));
            }

            ops.set(patternMatcher.size(), new Operator<Nonterminal,NodeType>(nodeType, patternMatcher.size(), this));
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
    public Closure<Nonterminal> addClosure(Object targetNt, Object sourceNt, int cost, HostRoutine method)
    {
        Closure<Nonterminal> closure = new Closure<Nonterminal>(targetNt, sourceNt, cost, method);
        closures.add(closure);
        nonterminals.add(targetNt);
        return closure;
    }

    /**
     * Add a closure to the grammar, with no callback.
     * @param targetNt      the nonterminal this closure produces.
     * @param sourceNt      the nonterminal that must be produced before this closure runs.
     * @param cost          the closure's cost metric.
     */
    public Closure<Nonterminal> addClosure(Object targetNt, Object sourceNt, int cost)
    {
        return addClosure(targetNt, sourceNt, cost, noPostCallback);
    }

    /**
     * Add a closure to the grammar, with unit cost.
     * @param targetNt      the nonterminal this closure produces.
     * @param sourceNt      the nonterminal that must be produced before this closure runs.
     * @param postCallback  the callback method run after the method that produces the source nonterminal completes.
     */
    public Closure<Nonterminal> addClosure(Object targetNt, Object sourceNt, HostRoutine method)
    {
        return addClosure(targetNt, sourceNt, 1, method);
    }

    /**
     * Add a closure to the grammar, with unit cost and no callbacks.
     */
    public Closure<Nonterminal> addClosure(Object targetNt, Object sourceNt)
    {
        return addClosure(targetNt, sourceNt, 1, noPostCallback);
    }

    /**
     * Add an error-handling production.
     * @param targetNt      the nonterminal this error handler produces.
     * @param errorCallback the error-handling callback method.
     */
    public void addErrorHandler(Object targetNt, HostRoutine errorCallback)
    {
        nonterminals.add(targetNt);
        this.errorState.setNonClosureProduction(new ErrorHandlerProduction<Nonterminal>(targetNt, errorCallback), 1);
    }

    /**
     * @return true if any error handling productions are present.
     */
    public boolean hasErrorHandler()
    {
        return this.errorState.size() > 0;
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
        // Put the error state at index zero so it has a consistent number.
        statesInEntryOrder.add(this.errorState);

        // Bootstrap the worklist with the null pointer state -- again, placed
        // at a known index in the state table -- and the states generated by
        // leaf operators.
        Queue<State<Nonterminal, NodeType>> worklist = new ArrayDeque<State<Nonterminal, NodeType>>();
        worklist.add(getNullPointerState());
        generateLeafStates(worklist);

        if (errorState.size() > 0) {
            closure(errorState);
            worklist.add(errorState);
        }

        // Examine all permutations of states
        // as operands to the non-leaf operators;
        // novel (or better) combinations of operators
        // and operands generate new states, which
        // replenish the worklist.
        while (worklist.peek() != null) {
            State<Nonterminal, NodeType> state = worklist.remove();

            for (List<Operator<Nonterminal,NodeType>> opList: operators.values()) {

                // Skip leaf operators; they're already done.
                for (int i = 1; i < opList.size(); i++) {
                    computeTransitions(opList.get(i), state, worklist);
                }
            }
        }

        errorState.finishCompilation();

        // Compile the operators' transition tables.
        for (List<Operator<Nonterminal,NodeType>> opList: operators.values()) {

            for (Operator<Nonterminal, NodeType> op: opList) {
                if (op != null) {
                    op.finishCompilation();
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
    public Production<Nonterminal> addNullPointerProduction(Object nt, int cost, HostRoutine postCallback)
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
    public State<Nonterminal, NodeType> getNullPointerState()
    {
        if (nullState == null) {
            nullState = new State<Nonterminal, NodeType>();

            for (NullPointerProduction<Nonterminal> p: nullProductions) {

                if (p.ownCost < nullState.getCost(p.target)) {
                    nullState.setNonClosureProduction(p, p.ownCost);
                }
            }

            closure(nullState);
            nullState.finishCompilation();
            State<Nonterminal, NodeType> canonicalNullState = addState(nullState);
            assert canonicalNullState == nullState && nullState.number == NULL_STATE_NUM;
        }

        return nullState;
    }

    /**
     * Record a state's closure set.
     * @param state the state.
     */
    private void closure(State<Nonterminal,NodeType> state)
    {
        if (verbose) System.out.printf("\tclosure(%s)\n", state);
        boolean closureRecorded;

        do {
            closureRecorded = false;

            for (Closure<Nonterminal> closure: this.closures) {
                String closureRationale = verbose? state.getClosureRationale(closure): null;
                boolean thisClosureRecorded = state.addClosure(closure);
                closureRecorded |= thisClosureRecorded;
                if (verbose) System.out.printf("\t\t%s %s: %s\n", thisClosureRecorded? "<-":"--", closure, closureRationale);
            }

        } while (closureRecorded);
    }

    /**
     * Generate the table's leaf states.
     * @param worklist  the compilation's worklist.
     * @post the initial worklist is populated with the leaf states.
     */
    private void generateLeafStates(Queue<State<Nonterminal, NodeType>> worklist)
    {

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
                // computing the transition table.
                for (State<Nonterminal, NodeType> state: newStates) {

                    if (state.size() > 0) {
                        closure(state);
                        worklist.add(addState(state));
                    }
                }

                leafOperator.createLeafState(newStates);
            }
        }
    }

    /**
     * Set the pattern that turns on verbose mode for specific operators.
     * @param pattern   a regex describing the Operator(s) of interest.
     */
    public String setVerboseTrigger(String pattern)
    {
        String result = this.verboseTrigger;
        verboseTrigger = pattern;
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
        // null operators exist due to holes in the operator lists.
        if (op == null) {
            return;
        }

        int arity = op.size();

        verbose = verboseTrigger != null && op.toString().matches(verboseTrigger);
        if (verbose) System.out.printf("\ncomputeTransitions(%s,%s)\n",op,state);

        for (int dim = 0; dim < arity; dim++) {

            RepresenterState<Nonterminal,NodeType> pState = project(op, dim, state);

            if (!pState.isEmpty()) {

                boolean novelPState = !op.reps.get(dim).contains(pState);

                if (novelPState) {
                    if (verbose) System.out.printf("\tnovel pState %s\n\t..reps[%d]=%s\n", pState, dim, op.reps.get(dim));
                } else {
                    if (verbose) System.out.printf("\tsame old pState %s\n\t..reps[%d]=%s\n", pState, dim, op.reps.get(dim));
                }

                op.reps.get(dim).add(pState);

                // Try all permutations of the operator's nonterminal children
                // as operands to the pattern matching productions applicable
                // to the operator.
                for (List<RepresenterState<Nonterminal,NodeType>> repStates: op.generatePermutations(pState, dim)) {
                    List<State<Nonterminal, NodeType>> newStates = new ArrayList<State<Nonterminal, NodeType>>();
                    newStates.add(new State<Nonterminal,NodeType>(op.nodeType));

                    for (PatternMatcher<Nonterminal, NodeType> p: getPatternsForNodeType(op.nodeType)) {
                        if (p.acceptsDimension(arity)) {
                            coalesceProduction(repStates, p, newStates);
                        }
                    }

                    for (State<Nonterminal, NodeType> resultState: newStates) {

                        if (!resultState.isEmpty()) {

                            if (!this.states.containsKey(resultState)) {
                                // This is a novel state to the production table as a whole;
                                // add it to the operator and the table, and put it on the
                                // worklist to see if it can generate additional transitions.
                                addState(resultState);
                                closure(resultState);
                                workList.add(resultState);
                                if (verbose) System.out.printf("\tadding novel %s->%s to %s\n",repStates, resultState, op);
                                op.addTransition(repStates, resultState);
                            } else {
                                // An equivalent state is already known to the production table;
                                // use the previously stored state as the canonical representation.
                                State<Nonterminal, NodeType> canonicalState = addState(resultState);
                                if (verbose) System.out.printf("\tadding canonical %s->%s to %s\n", repStates, canonicalState, op);
                                op.addTransition(repStates, canonicalState);
                            }
                        }
                    }
                }
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

        for (int i = 0; cost < Integer.MAX_VALUE && i < newStates.size(); i++) {
            State<Nonterminal, NodeType> candidate =  newStates.get(i);

            if (p.hasPredicate()) {

                if (candidate.satisfiesPredicate(p.predicate)) {

                    // The candidate state already has a production
                    // guarded by this predicate; replace that production
                    // if this production's cost is better.
                    if (cost < candidate.getCost(p.target)) {
                        candidate.setNonClosureProduction(p,cost);
                    }

                } else {
                    // The candidate state does not have this
                    // predicate in its predicates list; copy-create
                    // a new state with this predicate and add it
                    // to the permutation of predicates.
                    State<Nonterminal, NodeType> predicatedState = new State<Nonterminal, NodeType>(candidate, p.predicate);
                    newStates.add(predicatedState);

                    // Even though this predicate is novel, it
                    // may not be the best solution, so check.
                    // (The new state is still required, because
                    // the presence of the predicate itself is
                    // new information).
                    if (cost < predicatedState.getCost(p.target)) {
                        predicatedState.setNonClosureProduction(p,cost);
                    }
                }
            } else {
                // The production does not have a predicate, so
                // its cost is the only factor to consider.
                // TODO: This could potentially remove candidate
                // states from the list, if an unpredicated production
                // replaced a predicated production that was the only
                // use of a particular predicate; leaving that predicate
                // in the predicate maps only means that label-time logic
                // will run a meaningless "predicate" method, and one may
                // hope there will be relatively few such grammars.
                if (cost < candidate.getCost(p.target)) {
                    candidate.setNonClosureProduction(p,cost);
                }
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
        if (verbose) System.out.printf("\tproject(%s,%d,%s)\n", op, i, state.getStateNumber());

        for (Object n: nonterminals) {

            if (state.getCost(n) < Integer.MAX_VALUE) {
                if (verbose) System.out.printf("\t\tChecking nonterminal %s\n", n);

                for (PatternMatcher<Nonterminal, NodeType> p: getPatternsForNodeType(op.nodeType)) {
                    if (verbose) System.out.printf("\t\t\tchecking against pattern %s\n", op, i, state.getStateNumber(), p);

                    if (p.usesNonterminalAt(n, i))  {
                        if (verbose) System.out.printf("\t\t\t\tPossible winner, nt %s, dim %d, state cost %d%s\n", n, i,
                            state.getCost(n),
                            candidate.getCost(n) < Integer.MAX_VALUE?  String.format(", candidate cost %d", candidate.getCost(n)): ""
                        );

                        if (state.getCost(n) < candidate.getCost(n)) {
                            if (verbose) System.out.printf("\t\t\t\tsucceeded with nt %s, cost %s\n", n, state.getCost(n));
                            candidate.setCost(n, state.getCost(n));
                        }
                    }
                }
            } else {
                // This is noisy.
                //if (verbose) System.out.printf("\tNogo: nonterminal %s unfeasible? %s\n", n, state.getCost(n) == Integer.MAX_VALUE);
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
            return getErrorState();
        } else {
            return statesInEntryOrder.get(stateNumber);
        }
    }

    /**
     * Get the error state.
     */
    public State<Nonterminal, NodeType> getErrorState()
    {
        return this.errorState;
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

            if (opsForNodeType.size() > arity && opsForNodeType.get(arity) != null && opsForNodeType.get(arity).isComplete()) {
                // TODO: Search backwards for a variadic operator.
                return opsForNodeType.get(arity);

            } else if (arity >= opsForNodeType.size() && opsForNodeType.get(opsForNodeType.size()-1) != null && opsForNodeType.get(opsForNodeType.size()-1).isComplete() && opsForNodeType.get(opsForNodeType.size()-1).isVarArgs()) {
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
     * Load an operator into the operator table.
     * @param op    the operator to be loaded.
     */
    void loadOperator(Operator<Nonterminal, NodeType> op)
    {
        NodeType nodeType = op.nodeType;
        int arity = op.size();
        List<Operator<Nonterminal, NodeType>> opsForNodeType = operators.get(nodeType);

        if (opsForNodeType == null) {
            opsForNodeType = new ArrayList<Operator<Nonterminal, NodeType>>();
            operators.put(nodeType, opsForNodeType);
        }
        
        while(opsForNodeType.size() <= arity) {
            opsForNodeType.add(null);
        }

        opsForNodeType.set(arity, op);
    }

    /**
     * Dump the production table, if a destination was provided.
     * @param dumpPath      a pathname for the dump, or null.
     * @param templateGroup the template group file used to render the table.
     * @param attributes    attributes used during rendering.
     * @param defaults      default attributes.
     * @todo  TODO: add descriptions of the attributes.
     */
    public boolean dump(String dumpPath, String templateGroup, Map<String,String> attributes, Map<String,Object> defaultAttributes)
    {
        if (dumpPath != null) {

            try {
                java.io.PrintWriter out = new java.io.PrintWriter(dumpPath);
                TemplateGroup stg = new TemplateGroup("templates", templateGroup);

                Map<Object,Integer> uniqueStates = findUniqueStates();
                stg.setDefaultAttribute("uniqueStates", uniqueStates);

                for (String defaultKey: defaultAttributes.keySet()) {
                    stg.setDefaultAttribute(defaultKey, defaultAttributes.get(defaultKey));
                }

                if ("java.stg".equals(templateGroup)) {
                    stg.registerRenderer(Object.class, new JavaRenderer(uniqueStates, attributes));

                } else if (templateGroup.startsWith("cpp")) {
                    stg.registerRenderer(Object.class, new CppRenderer(uniqueStates, attributes));

                } else if ("xml.stg".equals(templateGroup)) {
                    stg.registerRenderer(Object.class, new JavaRenderer(uniqueStates, attributes));

                } else {
                    throw new IllegalArgumentException(String.format("Unknown emitter \"%s\"", templateGroup));
                }

                out.println(stg.getTemplate("start", "table", this).render());
                out.flush();
                out.close();

            } catch (java.io.IOException cannotDump) {
                cannotDump.printStackTrace();
                return false;
            }
        }

        return true;
    }

    private Map<Object,Integer> findUniqueStates()
    {
        Map<Object,Integer> result = new HashMap<Object,Integer>();

        for (List<Operator<Nonterminal,NodeType>> operators: getOperators()) {

            for(Operator<Nonterminal,NodeType> operator: operators) {

                if (operator != null) {

                    if (operator.getTransitionTable() == null) {
                        if (operator.leafState == null) {
                            System.out.printf("Funky operator: %s\n", operator);
                        }
                        findUniqueStates(result, operator.leafState);
                    } else {
                        findUniqueStates(result, operator.getTransitionTable());
                    }
                }
            }
        }

        return result;
    }

    void findUniqueStates(Map<Object,Integer> uniqueLeaves, TransitionTableLeaf<Nonterminal, NodeType> leaf)
    {
        if (leaf == null) {
            throw new IllegalStateException("Expected non-null leaf");
        }

        if (!uniqueLeaves.containsKey(leaf)) {
            uniqueLeaves.put(leaf,uniqueLeaves.size());
        }
    }

    void findUniqueStates(Map<Object,Integer> uniqueLeaves, TransitionPlane<Nonterminal,NodeType> plane)
    {
        if (plane.getNextDimension().isEmpty()) {

            for (TransitionTableLeaf<Nonterminal, NodeType> leaf: plane.getFinalDimension()) {
                findUniqueStates(uniqueLeaves, leaf);
            }
        } else {

            for (TransitionPlane<Nonterminal, NodeType> nextDimension: plane.getNextDimension()) {
                findUniqueStates(uniqueLeaves, nextDimension);
            }
        }
    }

    /**
     * Label a tree; this is the first pass
     * of the rewrite engine. After a tree
     * is labeled, it can be analyzed by walking
     * it with a visitor, or by directly querying
     * the available nonterminal states of its root's
     * transition table leaf.
     * @param node the root of the tree to label.
     */
    public void label(BurgInput<Nonterminal, NodeType> node, Object visitor, boolean labelSubtrees)
    throws Exception
    {
        // Null subtrees all share a singleton state in the production table;
        // it's precomputed into the transition table and the operators use it
        // when they encounter a null subtree.
        if (node != null) {

            int subtreeCount = node.getSubtreeCount();

            for (int i = 0; labelSubtrees && i < subtreeCount; i++) {
                label(node.getSubtree(i), visitor, labelSubtrees);
            }

            Operator<Nonterminal, NodeType> op = getOperator(node.getNodeType(), node.getSubtreeCount());

            if (op != null) {

                if (subtreeCount > 0) {
                    op.assignState(node, visitor);
                } else {
                    op.setLeafState(node, visitor);
                }
            } else {
                node.setStateNumber(0);
                node.setTransitionTableLeaf(errorState);
            }
        } 
    }

    public boolean canProduce(BurgInput<Nonterminal,NodeType> node, Nonterminal goal, Object visitor)
    throws Exception
    {
        if (node != null) {
            Object rawTransition = node.getTransitionTableLeaf();

            if (rawTransition != null && rawTransition instanceof State) {
                @SuppressWarnings("unchecked")
                State<Nonterminal, NodeType> s = (State<Nonterminal, NodeType>) rawTransition;
                return s.getCost(goal) < Integer.MAX_VALUE;
            }
        } else if (nullState != null) {
            return nullState.getCost(goal) < Integer.MAX_VALUE;
        }
        // else
        return false;
    }
}
