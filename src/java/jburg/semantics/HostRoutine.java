package jburg.semantics;

import java.lang.reflect.InvocationTargetException;

public abstract class HostRoutine<ParameterType> implements Comparable<HostRoutine>
{
    public abstract String getName();
    public abstract Object getDeclaringClass();
    public abstract int getParameterCount();
    public abstract ParameterType[] getParameterTypes();
    public abstract ParameterType getParameterType(int index);
    public abstract ParameterType getVariadicComponentType();
    public abstract boolean isVarArgs();
    public abstract int getVariadicOffset();
    public abstract Object invoke(Object receiver, Object... args)
        throws IllegalAccessException,
               IllegalArgumentException,
               InvocationTargetException;

    public abstract BURMSemantics getSemantics();

    public int compareTo(HostRoutine x)
    {
        return System.identityHashCode(this) - System.identityHashCode(x);
    }
}
