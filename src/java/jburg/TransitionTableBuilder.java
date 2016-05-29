package jburg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A TransitionTableBuilder holds a map of the transition tuples
 * that will comprise an operator's transition table, and the logic
 * to transform the tuples into a lookup table.
 */
class TransitionTableBuilder<Nonterminal, NodeType>
{
    final int arity;

    TransitionMap transitions = new TransitionMap();

    TransitionTableBuilder(int arity)
    {
        this.arity = arity;
    }

    void addTransition(List<RepresenterState<Nonterminal, NodeType>> repSet, State<Nonterminal, NodeType> state)
    {
        transitions.addState(repSet, state);
        if (!transitions.containsKey(repSet)) {
            transitions.put(repSet, new HashSet<State<Nonterminal, NodeType>>());
        }

        transitions.get(repSet).add(state);
    }

    boolean isEmpty()
    {
        return transitions.isEmpty();
    }

    HyperPlane<Nonterminal, NodeType> buildTransitionTable()
    {
        return buildTransitionPlane(transitions, 0);
    }

    HyperPlane<Nonterminal, NodeType> buildTransitionPlane(TransitionMap transitions, int dim)
    {
        boolean isFinalDimension = dim+1 == arity;

        HyperPlane<Nonterminal, NodeType> result = new HyperPlane<Nonterminal, NodeType>();

        TransitionPivot pivot = new TransitionPivot(transitions, dim);

        for (Set<State<Nonterminal, NodeType>> states: pivot.getStateSets()) {

            TransitionMap transition = pivot.getTransitions(states);

            if (isFinalDimension) {

                for (State<Nonterminal, NodeType> s: states) {
                    result.finalDimIndexMap.put(s.number, result.finalDimension.size());
                }

                result.finalDimension.add(createLeaf(transition.values()));

            } else {

                for (State<Nonterminal, NodeType> s: states) {
                    result.nextDimIndexMap.put(s.number, result.nextDimension.size());
                }

                result.nextDimension.add(buildTransitionPlane(transition, dim+1));
            }
        }

        return result;
    }

    PredicatedState<Nonterminal, NodeType> createLeaf(Collection<Set<State<Nonterminal, NodeType>>> stateSets)
    {
        PredicatedState<Nonterminal, NodeType> result = new PredicatedState<Nonterminal, NodeType>();

        for (Set<State<Nonterminal,NodeType>> states: stateSets) {

            for (State<Nonterminal, NodeType> state: states) {
                result.addTransition(state);
            }
        }

        return result;
    }

    private class TransitionMap extends HashMap<List<RepresenterState<Nonterminal, NodeType>>, Set<State<Nonterminal, NodeType>>>
    {
        void addState(List<RepresenterState<Nonterminal, NodeType>> repStates, State<Nonterminal, NodeType> state)
        {
            if (!this.containsKey(repStates)) {
                this.put(repStates, new HashSet<State<Nonterminal, NodeType>>());
            }

            this.get(repStates).add(state);
        }

        TransitionMap split(Set<State<Nonterminal, NodeType>> relevantStates, int dim)
        {
            TransitionMap result = new TransitionMap();

            for (List<RepresenterState<Nonterminal, NodeType>> rsTuple: keySet()) {

                for (State<Nonterminal, NodeType> s: rsTuple.get(dim).representedStates) {
                    if (relevantStates.contains(s)) {
                        result.put(rsTuple, get(rsTuple));
                        break;
                    }
                }
            }

            return result;
        }

        Collection<TransitionTableEntry> getEntries()
        {
            Collection<TransitionTableEntry> result = new ArrayList<TransitionTableEntry>();

            for (Map.Entry<List<RepresenterState<Nonterminal, NodeType>>, Set<State<Nonterminal, NodeType>>> entry: entrySet()) {
                result.add(new TransitionTableEntry(entry));
            }

            return result;
        }
    }

    /**
     * TransitionTableEntry is a "typedef'd" map entry for TransitionMap.
     */
    private class TransitionTableEntry
    {
        List<RepresenterState<Nonterminal, NodeType>>   rsTuple;
        Set<State<Nonterminal, NodeType>>               states;
        TransitionTableEntry(Map.Entry<List<RepresenterState<Nonterminal, NodeType>>, Set<State<Nonterminal, NodeType>>> entry)
        {
            this.rsTuple = entry.getKey();
            this.states = entry.getValue();
        }
    }

    private class TransitionPivot
    {
        // TODO: better name.
        Map<Set<State<Nonterminal, NodeType>>, TransitionMap> mappings = new HashMap<Set<State<Nonterminal, NodeType>>, TransitionMap>();

        TransitionPivot(TransitionMap transitions, int dim)
        {
            // First, partition the entries by their representer state in this dimension; mappings
            // for states sharing a unique representer state map to a common transition table entry.
            Map<RepresenterState<Nonterminal, NodeType>, List<TransitionTableEntry>> buckets = new HashMap<RepresenterState<Nonterminal, NodeType>, List<TransitionTableEntry>>();

            for (TransitionTableEntry entry: transitions.getEntries()) {
                RepresenterState<Nonterminal, NodeType> key = entry.rsTuple.get(dim);

                if (!buckets.containsKey(key)) {
                    buckets.put(key, new ArrayList<TransitionTableEntry>());
                }

                buckets.get(key).add(entry);
            }

            // Now find "critical states," which are represented by more than one RepresenterState
            // in this dimension; these states incorporate transitions from all the representing
            // RepresenterStates, and need their own transition.
            Set<State<Nonterminal, NodeType>> processedStates = new HashSet<State<Nonterminal, NodeType>>();

            for (RepresenterState<Nonterminal, NodeType> bucketKey: buckets.keySet()) {

                for (State<Nonterminal, NodeType> s: bucketKey.representedStates) {

                    if (!processedStates.contains(s)) {

                        for (RepresenterState<Nonterminal, NodeType> otherBucketKey: buckets.keySet()) {

                            if (otherBucketKey != bucketKey && otherBucketKey.representedStates.contains(s)) {
                                // Collision detected; split out the transition entries relevant to this
                                // state into a new map.
                                Set<State<Nonterminal, NodeType>> criticalState = new HashSet<State<Nonterminal, NodeType>>();
                                criticalState.add(s);
                                mappings.put(criticalState, transitions.split(criticalState, dim));
                                processedStates.add(s);
                                break;
                            }
                        }
                    }
                }
            }

            // Finally, create transitions for all representer states that have represented states
            // that weren't "critical;" all the represented states can share a common transition.
            for (List<RepresenterState<Nonterminal, NodeType>> rsTuple: transitions.keySet()) {

                RepresenterState<Nonterminal, NodeType> rsForDim = rsTuple.get(dim);

                Set<State<Nonterminal, NodeType>> pendingStates = new HashSet<State<Nonterminal, NodeType>>();

                for (State<Nonterminal, NodeType> s: rsForDim.representedStates) {
                    if (!processedStates.contains(s) && !s.isEmpty()) {
                        pendingStates.add(s);
                    }
                }

                if (!pendingStates.isEmpty()) {
                    mappings.put(pendingStates, transitions.split(pendingStates, dim));
                }
            }
        }

        Collection<Set<State<Nonterminal, NodeType>>> getStateSets()
        {
            return mappings.keySet();
        }

        TransitionMap getTransitions(Set<State<Nonterminal, NodeType>> key)
        {
            assert mappings.containsKey(key);
            return mappings.get(key);
        }
    }
}
