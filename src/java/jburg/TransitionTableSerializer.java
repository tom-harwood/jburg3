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
     * Dump a Method.
     * @param m the method.
     */
    void dump(Method m)
    throws IOException
    {
        out.print(
            stg.getTemplate(
                "methodReference",
                "methodName", m.getName(),
                "parameterTypes", m.getParameterTypes()
            ).render()
        );
    }

    /**
     * Dump a state.
     * @param state the state.
     */
    @SuppressWarnings("unchecked")
    void dump(State<Nonterminal, NodeType> state)
    throws java.io.IOException
    {
        out.print(
            stg.getTemplate(
                "stateDefinition",
                "number", state.number,
                "nodeType", state.nodeType,
                "arity", state.arityKind,
                "patterns", state.nonClosureProductions,
                "costMap", state.patternCosts,
                "closures", state.closures,
                "predicates", state.predicates
            ).render()
        );
    }

    void dump(Nonterminal nt, PatternMatcher<Nonterminal, NodeType> p, long cost)
    throws IOException
    {
        out.print(
            stg.getTemplate(
                "patternDefinition",
                "nodeType", p.nodeType,
                "nonterminal", nt,
                "cost", cost,
                "variadic", p.isVarArgs,
                "preCallback", p.preCallback,
                "postCallback", p.postCallback
            ).render()
        );
    }

    void dump(Closure<Nonterminal> closure)
    throws IOException
    {
        out.print(
            stg.getTemplate(
                "closureDefinition",
                "nonterminal", closure.target,
                "source", closure.source,
                "cost", closure.ownCost,
                "preCallback", closure.preCallback,
                "postCallback", closure.postCallback
            ).render()
        );
    }

    /**
     * Dump a HyperPlane.
     * @param h     the HyperPlane to dump.
     */
    void dump(HyperPlane<Nonterminal, NodeType> h)
    throws java.io.IOException
    {
        out.println(
            stg.getTemplate(
                "hyperPlane",
                "variadic", h.isVarArgs(),
                "finalDimension", h.finalDimension,
                "nextDimension", h.nextDimension
            )
        );
    }

    /**
     * Dump a PredicatedState.
     * @param ps    the PredicatedState to dump.
     */
    void dump(PredicatedState<Nonterminal, NodeType> ps)
    throws java.io.IOException
    {
        out.println(
            stg.getTemplate(
                "predicatedStateDefinition",
                "arityKind", ps.getArityKind(),
                "states", ps.states
            ).render()
        );
    }

    /**
     * Dump an Operator.
     * @param op    the Operator to dump.
     */
    void dump(Operator<Nonterminal, NodeType> op)
    throws java.io.IOException
    {
        out.println(
            stg.getTemplate(
                "operatorDefinition",
                "nodeType", op.nodeType,
                "arity", op.size(),
                "transitionTable", op.transitionTable,
                "leafState", op.leafState
            ).render()
        );
    }

    /**
     * Dump a ProductionTable.
     * @param pt    the ProductionTable to dump.
     */
    public void dump(ProductionTable<Nonterminal, NodeType> pt)
    throws java.io.IOException
    {
        //out.printf("<burmDump date=\"%s\">\n", new Date());
        out.println("<burmDump>");
        out.println(stg.getTemplate("productionTable", "t", pt).render());
        out.println("</burmDump>");
        out.flush();
    }
}
