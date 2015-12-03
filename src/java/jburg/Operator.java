package jburg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class Operator<Nonterminal, NodeType>
{
    final NodeType nodeType;

    final List<Set<RepresenterState<Nonterminal,NodeType>>> reps;

    final Map<List<RepresenterState<Nonterminal,NodeType>>, State<Nonterminal,NodeType>> transitionTable;

    Operator(NodeType nodeType, int arity)
    {
        this.nodeType = nodeType;
        this.reps = new ArrayList<Set<RepresenterState<Nonterminal,NodeType>>>();
        this.transitionTable = new HashMap<List<RepresenterState<Nonterminal,NodeType>>, State<Nonterminal,NodeType>>();

        for (int i = 0; i < arity; i++) {
            reps.add(new HashSet<RepresenterState<Nonterminal,NodeType>>());
        }
    }

    int getArity()
    {
        return reps.size();
    }

    public String toString()
    {
        return String.format("Operator %s%s", nodeType, transitionTable);
    }

    void addTransition(List<RepresenterState<Nonterminal,NodeType>> childStates, State<Nonterminal,NodeType> s)
    {
        // Make a copy of the childStates key; the caller may mutate it.
        List<RepresenterState<Nonterminal,NodeType>> key = new ArrayList<RepresenterState<Nonterminal,NodeType>>();
        key.addAll(childStates);
        transitionTable.put(key, s);
        //System.out.printf("Added transition %s=State %d to %s\n", childStates, s.number, this);
    }
}
