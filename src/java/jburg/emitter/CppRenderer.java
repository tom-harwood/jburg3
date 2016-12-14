package jburg.emitter;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jburg.semantics.HostRoutine;

import org.stringtemplate.v4.AttributeRenderer;

import jburg.Operator;
import jburg.PatternMatcher;

@SuppressWarnings("unchecked")
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
            HostRoutine<String> m = (HostRoutine<String>)o;
            return String.format("%s(node, result)", m.getName());

        } else if ("postCallback".equals(formatString)) {
            HostRoutine<String> m = (HostRoutine<String>)o;

            StringBuilder result = new StringBuilder(m.getName());
            result.append("(");
            String[] parameterTypes = m.getParameterTypes();
            boolean isVariadic = m.isVarArgs();
            int lastFixedArg = isVariadic? parameterTypes.length - 1: parameterTypes.length;
            result.append("node");

            for (int i = 1; i < lastFixedArg; i++) {
                result.append(String.format(", result%d", i-1));
            }

            if (isVariadic) {
                result.append(", variadicActuals");
            }
            result.append(")");
            return result.toString();

        } else if ("postCallback.variadicOffset".equals(formatString)) {
            PatternMatcher patternMatcher = (PatternMatcher)o;

            if (patternMatcher.getPostCallback() != null) {
                return String.valueOf(patternMatcher.getPostCallback().getVariadicOffset());
            } else {
                return "0";
            }

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

        } else if ("interfaceClosureCallback".equals(formatString)) {
            HostRoutine<String> routine = (HostRoutine<String>)o;
            return String.format("%s(%s,%s)", routine.getName(), attributes.get("nodeType.class"), routine.getParameterTypes()[0]); 

        } else if ("interfacePreCallback".equals(formatString)) {
            HostRoutine<String> routine = (HostRoutine<String>)o;
            return String.format("%s(%s,%s)", routine.getName(), attributes.get("nodeType.class"), routine.getParameterTypes()[0]); 

        } else if ("interfacePostCallback".equals(formatString)) {
            HostRoutine<String> routine = (HostRoutine<String>)o;

            StringBuilder result = new StringBuilder(routine.getName());
            result.append("(");
            String[] parameterTypes = routine.getParameterTypes();
            boolean isVariadic = routine.isVarArgs();
            int lastFixedArg = isVariadic? parameterTypes.length - 1: parameterTypes.length;
            result.append(attributes.get("nodeClass"));
            result.append("* node");

            for (int i = 1; i < lastFixedArg; i++) {
                result.append(", ");
                result.append(parameterTypes[i]);
            }

            if (isVariadic) {
                result.append(", std::vector<");
                result.append(parameterTypes[lastFixedArg]);
                result.append(">");
            }
            result.append(");");

            return result.toString();

        } else if ("grammar.name".equals(formatString)) {
            return (attributes.containsKey(formatString))?
                " from " + attributes.get(formatString)
                : "";

        } else if (attributes.containsKey(formatString)) {
            return attributes.get(formatString);

        } else {
            return o.toString();
        }
    }
}
