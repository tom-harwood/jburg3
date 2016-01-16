package jburg;

import java.lang.reflect.Method;
import java.util.Comparator;

/**
 * A MethodComparator orders methods by their hashCode;
 * we need lists with a consistent ordering so their
 * hashCode and equals methods will yield consistent
 * results, but the semantics of the ordering itself
 * are otherwise unimportant.
 */
class MethodComparator implements Comparator<Method>
{
    /** Order two methods by their hashCode. */
    public int compare(Method one, Method two)
    {
        return System.identityHashCode(one) - System.identityHashCode(two);
    }
}
