package jburg;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Production<Nonterminal, NodeType>
{
    final Nonterminal                       target;
    final NodeType                          nodeType;
    final List<Nonterminal>                 childTypes;
    final int                               ownCost;
    final Method                            postCallback;
    final Production<Nonterminal,NodeType>  antecedent;

    public Production(Nonterminal target, NodeType nodeType, int cost, Method postCallback, Production<Nonterminal,NodeType> antecedent, Nonterminal... childTypes)
    {
        this.target         = target;
        this.nodeType       = nodeType;
        this.childTypes     = Arrays.asList(childTypes);
        this.ownCost        = cost;
        this.antecedent     = antecedent;
        this.postCallback   = postCallback;
    }

    public Production(Nonterminal target, NodeType nodeType, int cost, Method postCallback, Nonterminal... childTypes)
    {
        this(target, nodeType, cost, postCallback, null, childTypes);
    }

    public Production(Nonterminal target, NodeType nodeType, Method postCallback, Nonterminal... childTypes)
    {
        this(target, nodeType, 1, postCallback, childTypes);
    }

    public boolean hasProduction(Nonterminal nt)
    {
        return target == nt;
    }

    public Nonterminal getNonterminal(int index)
    {
        return childTypes.get(index);
    }

    public boolean usesNonterminalAt(Nonterminal n, int index)
    {
        return index < childTypes.size() && getNonterminal(index) == n;
    }

    public boolean isLeaf()
    {
        return childTypes.isEmpty();
    }

    public int size()
    {
        return childTypes.size();
    }

    @Override
    public String toString()
    {
        if (antecedent == null) {
            return String.format("Production %s=%s%s: %s %s", target, nodeType, childTypes, ownCost, postCallback);
        } else {
            return String.format("Production (antecedent %s) %s=%s%s: %s %s", antecedent.target, target, nodeType, childTypes, ownCost, postCallback);
        }
    }

}
