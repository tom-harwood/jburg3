package jburg;

import java.lang.reflect.Method;
import java.util.*;
import java.io.*;

import org.stringtemplate.v4.*;
import jburg.emitter.TemplateGroup;

/**
 * TransitionTableSerializer serializes a transition table
 * using a string template group.
 */
public class TransitionTableSerializer<Nonterminal, NodeType>
{
    TemplateGroup stg;
    PrintWriter         out;

    public TransitionTableSerializer(PrintWriter out, TemplateGroup stg)
    {
        this.out = out;
        this.stg = stg;
    }

    /**
     * Dump a ProductionTable.
     * @param pt    the ProductionTable to dump.
     */
    public void dump(ProductionTable<Nonterminal, NodeType> pt)
    throws java.io.IOException
    {
        out.println(stg.getTemplate("productionTable", "t", pt).render());
        out.flush();
    }
}
