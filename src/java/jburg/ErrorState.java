package jburg;

import java.util.HashMap;
import java.util.Map;

/**
 * A ProductionTable has one ErrorState, at predefined index zero.
 * The ProductionTable assigns the ErrorState to inputs that don't
 * match any entry in the transition table. An ErrorState may have
 * a production and closures associated with it via the ProductionTable's
 * addErrorProduction API.
 */
public class ErrorState<Nonterminal, NodeType> extends State<Nonterminal, NodeType>
{
    /**
     * Construct an ErrorState. There is one ErrorState per ProductionTable,
     * which is assigned state number ERROR_STATE_NUM, i.e., zero.
     */
    ErrorState()
    {
        super();
        super.number = ProductionTable.ERROR_STATE_NUM;
    }

    public final Map<Object,Object> viableNonterminals = new HashMap<Object,Object>();

    /**
     * Get the Production for a nonterminal.
     * @param goal  the Nonterminal to be produced.
     * @return the corresponding ErrorHandlerProduction.
     * @throws IllegalArgumentException if this state
     * has no production for the specified nonterminal.
     */
    Production<Nonterminal> getProduction(Object goal)
    {
        try {
            return super.getProduction(goal);
        } catch(Exception noProduction) {
            throw new IllegalArgumentException(String.format("No error handler produces %s", goal));
        }
    }

    /**
     * @return the nonterminal state the error handler's production produces.
     */
    public Object getNonterminal()
    {
        assert nonClosureProductions.size() <= 1: String.format("expected zero or one production(s), found %d", nonClosureProductions.size());
        for (Object nt: nonClosureProductions.keySet()) {
            return nt;
        }

        return null;
    }

    /**
     * @return the error-handling &quot;pattern matcher.&quot;
     */
    public Production getErrorHandler()
    {
        assert nonClosureProductions.size() <= 1: String.format("expected zero or one production(s), found %d", nonClosureProductions.size());
        for (Production<Nonterminal> p: nonClosureProductions.values()) {
            return p;
        }

        return null;
    }

    void finishCompilation()
    {
        super.finishCompilation();

        Object nt = getNonterminal();

        if (nt != null) {
            viableNonterminals.put(nt,nt);
        }

        for (Object closureNt: closures.keySet()) {
            viableNonterminals.put(closureNt,closureNt);
        }
    }
}
