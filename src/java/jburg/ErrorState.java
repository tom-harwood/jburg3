package jburg;

/**
 * The ErrorState is a state that cannot perform any reduction.
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
    }
}
