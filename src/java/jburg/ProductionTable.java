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
    private List<PatternMatcher<Nonterminal, NodeType>>         patternMatchers = new ArrayList<PatternMatcher<Nonterminal, NodeType>>();;
    private List<Closure<Nonterminal>>                          closures        = new ArrayList<Closure<Nonterminal>>();
    private Set<Nonterminal>                                    nonterminals    = new TreeSet<Nonterminal>();
    private Set<NodeType>                                       nodeTypes       = new TreeSet<NodeType>();
    private Set<State<Nonterminal, NodeType>>                   states          = new HashSet<State<Nonterminal, NodeType>>();
    private Map<NodeType, List<Operator<Nonterminal,NodeType>>> operators       = new TreeMap<NodeType, List<Operator<Nonterminal,NodeType>>>();

    /**
     * RepresenterStates, keyed by themselves
     * so they can be efficaciously retrieved.
     */
    private Map<RepresenterState<Nonterminal,NodeType>, RepresenterState<Nonterminal,NodeType>> repStates =
        new HashMap<RepresenterState<Nonterminal,NodeType>, RepresenterState<Nonterminal,NodeType>>();

    /**
     * States mapped to a particular RepresenterState.
     */
    private Map<RepresenterState<Nonterminal,NodeType>, List<State<Nonterminal,NodeType>>> representedStates =
        new HashMap<RepresenterState<Nonterminal, NodeType>, List<State<Nonterminal, NodeType>>>();

    /**
     * Pattern matchers by node type.
     */
    private Map<NodeType, List<PatternMatcher<Nonterminal,NodeType>>> patternMatchersByNodeType =
        new TreeMap<NodeType, List<PatternMatcher<Nonterminal, NodeType>>>();


    // TODO: @SafeVarargs would be a better annotation,
    // but that would require Java 1.7 or above.
    @SuppressWarnings({"unchecked"})
    public PatternMatcher addPatternMatch(Nonterminal nt, NodeType nodeType, Method method, Nonterminal... childTypes)
    {
        PatternMatcher<Nonterminal,NodeType> result = new PatternMatcher<Nonterminal,NodeType>(nt, nodeType, 1, method, childTypes);
        nonterminals.add(nt);
        addPatternMatcher(result);
        return result;
    }

    public Closure addClosure(Nonterminal targetNt, Nonterminal sourceNt, Method method)
    {
        Closure<Nonterminal> closure = new Closure<Nonterminal>(targetNt, sourceNt, method);
        closures.add(closure);
        return closure;
    }

    public void addClosure(Nonterminal targetNt, Nonterminal sourceNt)
    {
        addClosure(targetNt, sourceNt, null);
    }

    public void generateStates()
    {
        Stack<State<Nonterminal, NodeType>> worklist = generateLeafStates();

        while (!worklist.empty()) {
            
            State<Nonterminal,NodeType> state = worklist.pop();

            for (List<Operator<Nonterminal,NodeType>> opList: operators.values()) {

                for (int i = 1; i < opList.size(); i++) {
                    Operator<Nonterminal,NodeType> op = opList.get(i);

                    if (op != null) {
                        computeTransitions(op, state, worklist);
                    }
                }
            }
        }
    }

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

    private List<PatternMatcher<Nonterminal,NodeType>> getPatternsForNodeType(NodeType op)
    {
        if (!patternMatchersByNodeType.containsKey(op)) {
            patternMatchersByNodeType.put(op, new ArrayList<PatternMatcher<Nonterminal,NodeType>>());
        }

        return patternMatchersByNodeType.get(op);
    }

    private final List<RepresenterState<Nonterminal, NodeType>> noChildStates = new ArrayList<RepresenterState<Nonterminal, NodeType>>();

    private Stack<State<Nonterminal, NodeType>> generateLeafStates()
    {
        Stack<State<Nonterminal, NodeType>> result = new Stack<State<Nonterminal, NodeType>>();

        for (NodeType nodeType: operators.keySet()) {
            State<Nonterminal, NodeType> state = new State<Nonterminal, NodeType>(nodeType);

            for (PatternMatcher<Nonterminal, NodeType> p: patternMatchers) {
                if (p.nodeType == nodeType && p.isLeaf() && p.ownCost < state.getCost(p.target)) {
                    state.setPatternMatcher(p, p.ownCost);
                }
            }

            if (state.size() > 0) {
                closure(state);
                result.push(addState(state));
                operators.get(nodeType).get(0).addTransition(noChildStates, state);
            }

        }

        return result;
    }

    private void computeTransitions(Operator<Nonterminal,NodeType> op, State<Nonterminal,NodeType> state, List<State<Nonterminal, NodeType>> workList)
    {
        for (int i = 0; i < op.size(); i++) {

            RepresenterState<Nonterminal,NodeType> pState = project(op, i, state);

            if (!pState.isEmpty() && !op.reps.get(i).contains(pState)) {
                //System.out.printf("Added representer state {%s} to operator {%s}[%d]\n", pState, op, i);
                op.reps.get(i).add(pState);

                // Try all permutations of the operator's nonterminal children
                // as operands to the rules applicable to the operator.
                /*
                for (List<RepresenterState<Nonterminal,NodeType>> repSet: generatePermutations(op, i, pState)) {
                }
                */
                
                List<RepresenterState<Nonterminal,NodeType>> prefix = new ArrayList<RepresenterState<Nonterminal,NodeType>>();
                permute(op, 0, i, pState, prefix, workList);
            }
        }
    }

    private RepresenterState<Nonterminal,NodeType> project(Operator<Nonterminal,NodeType> op, int i, State<Nonterminal,NodeType> state)
    {
        RepresenterState<Nonterminal,NodeType> candidate = new RepresenterState<Nonterminal,NodeType>(op.nodeType);

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
     * Permute the operator's set of representer states
     * around a possibly novel, 'pivot' representer state,
     * and add any new states discovered to the worklist.
     * @param op        the current operator.
     * @param dim       the next dimension
     * @param pDim      the pivot dimension.
     * @param pivot     the projected state being pivoted.
     * @param prefix    known states so far.
     * @param workList  the worklist for new states.
     */
    private void permute(
        Operator<Nonterminal,NodeType> op,
        int dim,
        int pDim,
        RepresenterState<Nonterminal,NodeType> pivot,
        List<RepresenterState<Nonterminal, NodeType>> prefix,
        List<State<Nonterminal, NodeType>> workList)
    {
        // TODO: Also analyze variadic productions.
        if (dim == op.size()) {
            //System.out.printf("analyzing rules for %s arity %d against %s\n", op, dim, prefix);

            State<Nonterminal,NodeType> result = new State<Nonterminal,NodeType>(op.nodeType);

            for (PatternMatcher<Nonterminal, NodeType> p: getPatternsForNodeType(op.nodeType)) {
                // for each state in the prefix:
                //   for each nonterminal in the state:
                //      cost = p.cost + sum of costs in prefix for the current nonterminals;
                //      if (cost < result.getCost(n))
                //          result.addRule(n, cost, p);
                // if result isn't empty:
                //    closure(result);
                //    if (result not in states)
                //        result = addState(result);
                //        worklist.add(result);
                if (p.acceptsDimension(dim)) {
                    long cost = 0;
                    for (int i = 0; i < dim && cost < Integer.MAX_VALUE; i++) {
                        cost += prefix.get(i).getCost(p.getNonterminal(i));
                    }

                    if (cost < result.getCost(p.target)) {
                        result.setPatternMatcher(p,cost);
                    }
                }
            }

            if (!result.isEmpty()) {

                if (!states.contains(result)) {
                    closure(result);
                    workList.add(result);
                }

                // Cache the canonical state in the operator;
                // it may be this new state, or it may
                // be a equivalent previous instance.
                State<Nonterminal, NodeType> canonicalState = addState(result);
                op.addTransition(prefix, canonicalState);
            }
        }
        else if (dim == pDim) {
            prefix.add(pivot);
            permute(op,dim+1,pDim,pivot,prefix,workList);
            prefix.remove(dim);
        } else {
            for (RepresenterState<Nonterminal,NodeType> s: op.reps.get(dim)) {
                prefix.add(s);
                permute(op, dim+1, pDim, pivot, prefix, workList);
                prefix.remove(dim);
            }
        }
    }

    private void addPatternMatcher(PatternMatcher<Nonterminal,NodeType> patternMatcher)
    {
        NodeType nodeType = patternMatcher.nodeType;

        patternMatchers.add(patternMatcher);
        getPatternsForNodeType(nodeType).add(patternMatcher);
        
        if (!operators.containsKey(nodeType)) {
            operators.put(nodeType, new ArrayList<Operator<Nonterminal,NodeType>>());
        }

        List<Operator<Nonterminal,NodeType>> ops = operators.get(nodeType);

        if (ops.size() < patternMatcher.size() + 1) {
            ops.addAll(Collections.nCopies((patternMatcher.size() + 1) - ops.size(), (Operator<Nonterminal,NodeType>)null));
        }

        ops.set(patternMatcher.size(), new Operator<Nonterminal,NodeType>(nodeType, patternMatcher.size()));
    }

    private State<Nonterminal, NodeType> addState(State<Nonterminal, NodeType> state)
    {
        if (this.states.add(state)) {
            state.number = this.states.size();
            return state;
        } else {
            Iterator<State<Nonterminal,NodeType>> it = this.states.iterator();
            while (it.hasNext()) {
                State<Nonterminal,NodeType> s = it.next();
                if (state.hashCode() == s.hashCode() && state.equals(s)) {
                    return s;
                }
            }
        }
        throw new IllegalStateException(String.format("State %s not added and not present",state));
    }

    private RepresenterState<Nonterminal,NodeType> addRepresenterState(RepresenterState<Nonterminal,NodeType> rs)
    {
        if (!repStates.containsKey(rs)) {
            repStates.put(rs, rs);
        }

        return repStates.get(rs);
    }

    public void dump(java.io.PrintWriter out)
    throws java.io.IOException
    {
        List<State<Nonterminal,NodeType>> sortedStates = Collections.list(Collections.enumeration(states));
        Collections.sort(sortedStates);

        for (State<Nonterminal, NodeType> s: sortedStates) {
            out.println(s);
        }

        out.println();


        for (NodeType nodeType: operators.keySet()) {

            for (Operator<Nonterminal,NodeType> op: operators.get(nodeType)) {

                if (op != null) {
                    out.printf("%s(%d)\n", op.nodeType, op.size());

                    for (int i = 0; i < op.size(); i++) {
                        out.printf("\tdimension %d\n", i);
                        Set<RepresenterState<Nonterminal, NodeType>> repSet = op.reps.get(i);

                        for (RepresenterState<Nonterminal, NodeType> rep: repSet) {
                            for (State<Nonterminal, NodeType> s: rep.representedStates) {
                                out.printf("\t\tstate %d\n", s.number);
                            }
                        }
                    }
                }
            }
        }

        out.flush();
    }
}
