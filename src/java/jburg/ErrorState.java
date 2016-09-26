package jburg;

/**
 * A ProductionTable has one ErrorState, at predefined index zero.
 * The ProductionTable assigns the ErrorState to inputs that don't
 * match any entry in the transition table. An ErrorState may have
 * productions associated with it via the ProductionTable's
 * addErrorProduction API.
 */
public class ErrorState<Nonterminal, NodeType> extends State<Nonterminal, NodeType>
{
    /**
     * Construct an ErrorState. There is normally one ErrorState per ProductionTable,
     * which is conventionally placed at index zero in the statesInEntryOrder list.
     */
    ErrorState()
    {
        super();
        super.number = ProductionTable.ERROR_STATE_NUM;
    }

    /**
     * Get the Production for a nonterminal.
     * @param goal  the Nonterminal to be produced.
     * @return the corresponding ErrorHandlerProduction.
     * @throws IllegalArgumentException if this state
     * has no production for the specified nonterminal.
     */
    Production<Nonterminal> getProduction(Nonterminal goal)
    {
        try {
            return super.getProduction(goal);
        } catch(Exception noProduction) {
            throw new IllegalArgumentException(String.format("No error handler produces %s", goal));
        }
    }

    public Nonterminal getNonterminal()
    {
        assert nonClosureProductions.size() <= 1: String.format("expected zero or one production(s), found %d", nonClosureProductions.size());
        for (Nonterminal nt: nonClosureProductions.keySet()) {
            return nt;
        }

        return null;
    }

    public Production getErrorHandler()
    {
        assert nonClosureProductions.size() <= 1: String.format("expected zero or one production(s), found %d", nonClosureProductions.size());
        for (Production<Nonterminal> p: nonClosureProductions.values()) {
            return p;
        }

        return null;
    }
}
