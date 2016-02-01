package jburg;

import java.lang.reflect.Method;
import java.util.*;
import java.io.*;

public class TransitionTableToXML<Nonterminal, NodeType>
{
    PrintWriter out;
    TransitionTableToXML(PrintWriter out)
    {
        this.out = out;
    }

    void dumpMethod(Method m)
    throws IOException
    {
        out.printf("<method name=\"%s\" class=\"%s\">\n", m.getName(), m.getDeclaringClass().getName());
        Class<?>[] parameterTypes = m.getParameterTypes();

        if (parameterTypes.length > 0) {
            out.println("<parameterTypes>");

            for (Class<?> ptype: parameterTypes) {
                out.printf("<parameter type=\"%s\"/>\n", ptype.getName());
            }

            out.println("</parameterTypes>");

        } else {
            out.println("<parameterTypes/>");
        }
        out.println("</method>");
    }

    /**
     * Dump an XML rendering of a state.
     * @param state the state.
     * @param out   the output sink.
     */
    @SuppressWarnings("unchecked")
    void dumpState(State<Nonterminal, NodeType> state)
    throws java.io.IOException
    {
        out.printf("<state number=\"%d\" nodeType=\"%s\" arity=\"%s\">", state.number, state.nodeType, state.arityKind);

        // Pattern/closure information is used at transition table
        // build time; it's included in the dump for debugging.
        if (state.nonClosureProductions.size() > 0) {
            out.println("<patterns>");

            for (Nonterminal nt: state.nonClosureProductions.keySet()) {

                Production<Nonterminal> p = state.nonClosureProductions.get(nt);

                if (!(p instanceof NullPointerProduction)) {
                    dumpPattern(nt, (PatternMatcher<Nonterminal, NodeType>)p, state.getCost(nt));
                }
            }
            out.printf("</patterns>");
        }

        if (state.closures.size() > 0) {
            out.println("<closures>");
            for (Closure<Nonterminal> closure: state.closures.values()) {
                dumpClosure(closure);
            }
            out.println("</closures>");
        }

        out.println("</state>");
    }

    void dumpPattern(Nonterminal nt, PatternMatcher<Nonterminal, NodeType> p, long cost)
    throws IOException
    {
        out.printf("<pattern nodeType=\"%s\" nonterminal=\"%s\" cost=\"%d\">\n", p.nodeType, nt, cost);

        if (p.childTypes.size() > 0) {
            out.println("<childTypes>");
            for (Nonterminal c: p.childTypes) {
                out.printf("<childType nonterminal=\"%s\"/>\n", c);
            }
            out.println("</childTypes>");
        } else {
            out.println("<childTypes/>");
        }

        dumpProduction(p);

        out.println("</pattern>");
    }

    void dumpProduction(Production<Nonterminal> p)
    throws IOException
    {
        if (p.preCallback != null) {
            out.println("<preCallback>");
            dumpMethod(p.preCallback);
            out.println("</preCallback>");
        }

        if (p.postCallback != null) {
            out.println("<postCallback>");
            dumpMethod(p.postCallback);
            out.println("</postCallback>");
        }
    }

    void dumpClosure(Closure<Nonterminal> closure)
    throws IOException
    {
        out.printf(String.format("<closure nt=\"%s\" source=\"%s\">", closure.target, closure.source));
        dumpProduction(closure);
        out.println("</closure>");
    }

    /**
     * Dump an XML rendering of a HyperPlane.
     * @param h     the HyperPlane to dump.
     */
    void dumpHyperPlane(HyperPlane<Nonterminal, NodeType> h)
    throws java.io.IOException
    {
        if (!h.finalDimension.isEmpty()) {

            out.println("<finalDimension>");
            int idx = 0;
            for (PredicatedState<Nonterminal, NodeType> s: h.finalDimension) {
                out.printf("<entry index=\"%d\">\n",idx++);
                dumpPredicatedState(s);
                out.printf("</entry>\n");
            }
            out.println("</finalDimension>");
        }

        for (int i = 0; i < h.nextDimension.size(); i++) {

            if (h.nextDimension.get(i) != h) {
                out.printf("<plane index=\"%d\">\n", i);

                out.println("<mappedStates>");
                for (Integer idx: h.getStatesForPlane(i)) {
                    out.printf("<mappedState number=\"%d\"/>\n", idx);
                }
                out.println("</mappedStates>");
                dumpHyperPlane(h.nextDimension.get(i));
                out.println("</plane>\n");
            } else {
                out.println("<variadic/>\n");
            }
        }
    }

    /**
     * Dump an XML rendering of a PredicatedState.
     * @param ps    the PredicatedState to dump.
     */
    void dumpPredicatedState(PredicatedState<Nonterminal, NodeType> ps)
    throws java.io.IOException
    {
        out.printf("<predicatedState arityKind=\"%s\">\n", ps.getArityKind());

        for (List<Method> predicateKey: ps.states.keySet()) {

            if (predicateKey.isEmpty()) {
                out.printf("<default state=\"%d\"/>\n", ps.states.get(predicateKey).number);

            } else {
                out.printf("<predicated state=\"%d\">\n", ps.states.get(predicateKey).number, predicateKey);

                for (Method m: predicateKey) {
                    dumpMethod(m);
                }

                out.printf("</predicated>\n");
            }
        }

        out.println("</predicatedState>");
    }

    /**
     * Dump an XML rendering of an Operator.
     * @param op    the Operator to dump.
     */
    void dumpOperator(Operator<Nonterminal, NodeType> op)
    throws java.io.IOException
    {
        out.printf("<operator nodeType=\"%s\" arity=\"%d\">\n", op.nodeType, op.size());

        if (op.transitionTable != null) {
            dumpHyperPlane(op.transitionTable);

        } else if (op.leafState != null) {
            dumpPredicatedState(op.leafState);
        }

        out.println("</operator>");
    }

    /**
     * Dump an XML rendering of a ProductionTable.
     * @param pt    the ProductionTable to dump.
     */
    public void dumpProductionTable(ProductionTable<Nonterminal, NodeType> pt)
    throws java.io.IOException
    {
        out.printf("<burmDump date=\"%s\">\n", new Date());

        out.println("<stateTable>");

        for (State<Nonterminal, NodeType> s: pt.statesInEntryOrder) {
            dumpState(s);
        }
        out.println("</stateTable>");

        out.println();

        out.println("<transitionTable>");

        for (NodeType nodeType: pt.operators.keySet()) {

            for (Operator<Nonterminal,NodeType> op: pt.operators.get(nodeType)) {

                if (op != null) {
                    dumpOperator(op);
                }
            }
        }
        out.println("</transitionTable>");

        out.println("</burmDump>");

        out.flush();
    }
}
