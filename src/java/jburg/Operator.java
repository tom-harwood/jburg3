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
        StringBuilder builder = new StringBuilder("Operator ");
        builder.append(nodeType);

        if (transitionTable.size() > 0) {
            boolean firstTime = true;

            for (List<RepresenterState<Nonterminal,NodeType>> childStates: transitionTable.keySet()) {

                if (firstTime) {
                    firstTime = false;
                } else {
                    builder.append(",");
                }

                if (transitionTable.get(childStates) != null) {
                    builder.append(String.format("%s=%d",childStates,transitionTable.get(childStates).number));
                } else {
                    builder.append(String.format("%s=?", childStates));
                }
            }

        } else {
            builder.append(" -empty-");
        }

        return builder.toString();
    }

    private String formatStateList(List<RepresenterState<Nonterminal,NodeType>> stateList)
    {
        StringBuilder builder = new StringBuilder("[");

        for (int i = 0; i < stateList.size(); i++) {
            if (i > 0) {
                builder.append(",");
            }

            builder.append(String.valueOf(stateList.get(i).number));
        }

        builder.append("]");

        return builder.toString();
    }

    void addTransition(List<RepresenterState<Nonterminal,NodeType>> childStates, State<Nonterminal,NodeType> s)
    {
        transitionTable.put(childStates, s);
    }
}
