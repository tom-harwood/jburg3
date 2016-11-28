package jburg.semantics;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

    public static HostRoutine getHostRoutine(Method m)
    {
        return new MethodWrapperHostRoutine(m);
    }

    public int compareTo(HostRoutine x)
    {
        return System.identityHashCode(this) - System.identityHashCode(x);
    }

    static class MethodWrapperHostRoutine extends HostRoutine<Class>
    {
        final Method        m;
        final Class<?>[]    parameterTypes;
        MethodWrapperHostRoutine(Method m)
        {
            this.m = m;
            this.parameterTypes = m.getParameterTypes();
        }

        public String getName()
        {
            return m.getName();
        }

        public Object getDeclaringClass()
        {
            return m.getDeclaringClass();
        }

        public int getParameterCount()
        {
            return m.getParameterCount();
        }

        public Class[] getParameterTypes()
        {
            return parameterTypes;
        }

        public Class getParameterType(int index)
        {
            return parameterTypes[index];
        }

        public boolean isVarArgs()
        {
            return m.isVarArgs();
        }

        public Class getVariadicComponentType()
        {
            assert isVarArgs();
            return parameterTypes[parameterTypes.length-1].getComponentType();
        }

        public int getVariadicOffset()
        {
            assert isVarArgs();
            assert parameterTypes.length > 1;
            return parameterTypes.length - 2;
        }

        public Object invoke(Object receiver, Object... args)
        throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
        {
            return m.invoke(receiver, args);
        }
    }
}
