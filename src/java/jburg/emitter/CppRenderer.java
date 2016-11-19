package jburg.emitter;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import java.lang.reflect.Method;

import org.stringtemplate.v4.AttributeRenderer;

import jburg.Operator;
import jburg.PatternMatcher;

public class CppRenderer implements AttributeRenderer
{

    final Map<Object,Integer> leafStates;
    final Map<String,String>  attributes;

    public CppRenderer(Map<Object,Integer> leafStates, Map<String,String> attributes)
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
                throw new IllegalStateException(String.format("Leaf state %s not in unique states table",o));
            }
            return String.format("leafState%s", leafStates.get(o));


        } else if ("closurePostCallback".equals(formatString)) {
            Method m = (Method)o;
            StringBuilder result = new StringBuilder(m.getName());
            result.append("(");
            result.append("node, ");
            /*
             * TODO: Java vs. C++ types
            result.append("(");
            result.append(m.getParameterTypes()[1].getName());
            result.append(")");
            */
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
            /*
             * TODO: Have to translate types for the C++ emitter,
               or reason about them using a host-agnostic format.
                result.append(", (");
                result.append(parameterTypes[i].getName());
                result.append(")");
            */
                result.append(", ");
                result.append(String.format("result%d", i-1));
            }

            if (isVariadic) {
                // TODO: C++ centric variadic handling -- va_list
                // isn't feasible, perhaps a std::vector?
                result.append(", variadicActuals");
            }
            result.append(")");
            return result.toString();

        } else if ("postCallback.lastNonterminal".equals(formatString)) {
            @SuppressWarnings("unchecked")
            List<PatternMatcher.PatternChildDescriptor> descriptors = ((PatternMatcher)o).getChildDescriptors();
            return descriptors.get(descriptors.size()-1).getNonterminal().toString();

        } else if ("postCallback.variadicType".equals(formatString)) {
            Method m = (Method)o;
            assert(m.isVarArgs());
            Class<?>[] parameterTypes = m.getParameterTypes();
            return parameterTypes[parameterTypes.length-1].getComponentType().getSimpleName();

        } else if ("postCallback.variadicOffset".equals(formatString)) {
            Method m = (Method)o;
            assert(m.isVarArgs());
            Class<?>[] parameterTypes = m.getParameterTypes();
            assert(parameterTypes.length > 1);
            return Integer.valueOf(parameterTypes.length - 2).toString();

        } else if ("timestamp".equals(formatString)) {
            return new java.util.Date().toString();

        } else if ("version".equals(formatString)) {
            return jburg.version.JBurgVersion.version;

        } else if ("include".equals(formatString)) {
            return String.format("#include \"%s\"\n", o);

        } else if ("result.type".equals(formatString)) {

            if (attributes.containsKey(formatString)) {
                return attributes.get(formatString);
            } else {
                return "Object";
            }

        } else if (attributes.containsKey(formatString)) {
            return attributes.get(formatString);

        } else {
            return o.toString();
        }
    }
}
