package jburg;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A PatternMatcher encodes a opcode(operand, operand) pattern match.
 * Note that PatternMatcher uses identity semantics for its hashCode()
 * and equals() methods (i.e., it doesn't override Object), which
 * is used by the State objects that contain aggregations of
 * PatternMatcher and Closure productions, which can create an unbounded
 * number of candidate States that only differ in that their costs
 * are increasing due to closures; these states don't contribute any
 * novel information and so the State defines its hashCode and equals
 * methods in terms of the PatternMatcher map's hashCode() and equals().
 */
public class PatternMatcher<Nonterminal, NodeType>
{
    final Nonterminal                       target;
    final NodeType                          nodeType;
    final List<Nonterminal>                 childTypes;
    final int                               ownCost;
    final Method                            postCallback;

    // TODO: @SafeVarargs would be a better annotation,
    // but that would require Java 1.7 or above.
    @SuppressWarnings({"unchecked"})
    public PatternMatcher(Nonterminal target, NodeType nodeType, int cost, Method postCallback, Nonterminal... childTypes)
    {
        this.target         = target;
        this.nodeType       = nodeType;
        this.childTypes     = Arrays.asList(childTypes);
        this.ownCost        = cost;
        this.postCallback   = postCallback;
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

    public boolean acceptsDimension(int dim)
    {
        // TODO: Variadics.
        return size() == dim;
    }

    @Override
    public String toString()
    {
        return String.format("PatternMatcher %s=%s%s cost:%s %s)", target, nodeType, childTypes, ownCost, getCallbackName("post", postCallback));
    }

    private String getCallbackName(String prefix, Method callback)
    {
        if (callback != null) {
            return String.format("%sCallback:%s", prefix, callback.getName());
        } else {
            return "";
        }
    }
}
