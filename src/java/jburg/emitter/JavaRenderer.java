package jburg.emitter;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.stringtemplate.v4.AttributeRenderer;

import jburg.Operator;

public class JavaRenderer implements AttributeRenderer
{

    Map<Object,Integer> leafStates = new HashMap<Object,Integer>();

    @Override
    public String toString(Object o, String formatString, Locale locale)
    {
        if ("operatorSignature".equals(formatString)) {
            Operator<?,?> op = (Operator<?,?>)o;
            return String.format("%s_%d_%s", op.nodeType, op.getSize(), op.getArityKind());

        } else if ("leafState".equals(formatString)) {

            if (!leafStates.containsKey(o)) {
                leafStates.put(o, leafStates.size());
            }

            return String.format("leafState%s", leafStates.get(o));

        } else {
            return o.toString();
        }
    }
}
