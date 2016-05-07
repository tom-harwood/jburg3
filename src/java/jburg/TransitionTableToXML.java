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

    /**
     * Dump an XML rendering of a Method.
     * @param m the method.
     */
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
     */
    @SuppressWarnings("unchecked")
    void dumpState(State<Nonterminal, NodeType> state)
    throws java.io.IOException
    {
        out.printf("<state number=\"%d\" nodeType=\"%s\" arity=\"%s\">", state.number, state.nodeType, state.arityKind);

        if (state.nonClosureProductions.size() > 0) {
            out.println("<patterns>");

            for (Nonterminal nt: state.nonClosureProductions.keySet()) {

                Production<Nonterminal> p = state.nonClosureProductions.get(nt);

                if (!(p instanceof NullPointerProduction)) {
                    dumpPattern(nt, (PatternMatcher<Nonterminal, NodeType>)p, state.getCost(nt));
                }
            }
            out.printf("</patterns>");

            out.printf("<costMap>");
            for (Nonterminal nt: state.getNonterminals()) {
                out.printf("<cost nonterminal=\"%s\" cost=\"%d\"/>\n", nt, state.getCost(nt));
            }
            out.printf("</costMap>");

            if (state.closures.size() > 0) {
                out.println("<closures>");

                for (Closure<Nonterminal> closure: state.closures.values()) {
                    dumpClosure(closure);
                }
                out.println("</closures>");
            }
        }

        out.println("<predicates>");

        for (Method m: state.predicates) {
            dumpMethod(m);
        }

        out.println("</predicates>");
        out.println("</state>");
    }

    void dumpPattern(Nonterminal nt, PatternMatcher<Nonterminal, NodeType> p, long cost)
    throws IOException
    {
        out.printf("<pattern nodeType=\"%s\" nonterminal=\"%s\" cost=\"%d\" variadic=\"%s\">\n", p.nodeType, nt, cost, p.isVarArgs);

        if (p.childTypes.size() > 0) {
            out.println("<childTypes>");
            for (Nonterminal c: p.childTypes) {
                out.printf("<childType nonterminal=\"%s\"/>\n", c);
            }
            out.println("</childTypes>");
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
        out.printf(String.format("<closure nonterminal=\"%s\" source=\"%s\" cost=\"%d\">", closure.target, closure.source, closure.ownCost));
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

            if (h.isVarArgs()) {
                out.println("<variadic/>");
            }

            out.println("<finalDimension>");

            for (Integer stateNum: h.finalDimIndexMap.keySet()) {
                Integer idx = h.finalDimIndexMap.get(stateNum);
                out.printf("<entry stateNumber=\"%d\" index=\"%d\">\n", stateNum, idx);
                dumpPredicatedState(h.finalDimension.get(idx));
                out.printf("</entry>\n");
            }

            out.println("</finalDimension>");
        }

        if (h.nextDimension.size() > 0) {

            // Don't emit the start tag until we find
            // a transition to a different hyperplane.
            boolean emittedStartTag = false;

            for (int stateIndex = 0; stateIndex < h.nextDimension.size(); stateIndex++) {

                if (h.nextDimension.get(stateIndex) != h) {

                    if (!emittedStartTag) {
                        out.println("<nextDimension>");
                        emittedStartTag = true;
                    }

                    out.printf("<hyperPlane index=\"%d\">\n", stateIndex);

                    out.println("<mappedStates>");
                    for (Integer mappedState: h.getStatesForPlane(stateIndex)) {
                        out.printf("<mappedState stateNumber=\"%d\"/>\n", mappedState);
                    }
                    out.println("</mappedStates>");
                    dumpHyperPlane(h.nextDimension.get(stateIndex));
                    out.println("</hyperPlane>\n");
                }
            }

            if (emittedStartTag) {
                out.println("</nextDimension>");
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

        for (State<Nonterminal, NodeType> s: ps.states.values()) {
            dumpState(s);
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
            out.println("<hyperPlane>");
            dumpHyperPlane(op.transitionTable);
            out.println("</hyperPlane>");

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
