package jburg.emitter;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jburg.semantics.HostRoutine;

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
            HostRoutine m = (HostRoutine)o;
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
            HostRoutine m = (HostRoutine)o;

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

        } else if ("postCallback.variadicType".equals(formatString)) {
            HostRoutine m = (HostRoutine)o;
            assert(m.isVarArgs());
            Class<?>[] parameterTypes = m.getParameterTypes();
            return parameterTypes[parameterTypes.length-1].getComponentType().getSimpleName();

        } else if ("postCallback.variadicOffset".equals(formatString)) {
            PatternMatcher pattern = (PatternMatcher)o;
            assert(pattern.getIsVarArgs());
            return Integer.valueOf(pattern.getNonVariadicChildDescriptors().size()).toString();

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
