package jburg.emitter;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import java.lang.reflect.Method;

import org.stringtemplate.v4.AttributeRenderer;

import jburg.Operator;

public class JavaRenderer implements AttributeRenderer
{

    final Map<Object,Integer> leafStates;
    final Map<String,String>  attributes;

    public JavaRenderer(Map<Object,Integer> leafStates, Map<String,String> attributes)
    {
        this.leafStates = leafStates;
        this.attributes = attributes;
    }

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


        } else if ("closurePostCallback".equals(formatString)) {
            Method m = (Method)o;
            StringBuilder result = new StringBuilder(m.getName());
            result.append("(");
            result.append("node, ");
            result.append("(");
            result.append(m.getParameterTypes()[1].getName());
            result.append(")");
            result.append("result");
            result.append(")");
            return result.toString();

        } else if ("postCallback".equals(formatString)) {
            Method m = (Method)o;

            StringBuilder result = new StringBuilder(m.getName());
            result.append("(");
            Class<?>[] parameterTypes = m.getParameterTypes();
            boolean isVariadic = m.isVarArgs();
            int lastFixedArg = isVariadic? parameterTypes.length - 1: parameterTypes.length;
            result.append("node");

            for (int i = 1; i < lastFixedArg; i++) {
                result.append(", (");
                result.append(parameterTypes[i].getName());
                result.append(")");
                result.append(String.format("result%d", i-1));
            }

            if (isVariadic) {
                // TODO: Marshal the variadic arguments...
            }
            result.append(")");
            return result.toString();

        } else if ("version".equals(formatString)) {
            return jburg.version.JBurgVersion.version;

        } else if (attributes.containsKey(formatString)) {
            return attributes.get(formatString);

        } else {
            return o.toString();
        }
    }
}
