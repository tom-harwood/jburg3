package tl3;

/**
 * Runtime provides runtime support routines for TL3 programs.
 */
public class Runtime
{
    /**
     * Javascript-like addition.
     */
    public static Object add(Object lhs, Object rhs)
    {
        if (lhs instanceof Integer && rhs instanceof Integer) {
            return Integer.valueOf((Integer)lhs + (Integer)rhs);
        } else if (lhs == null && rhs instanceof Integer) {
            return rhs;
        } else if (lhs instanceof Integer && rhs == null) {
            return rhs;
        } else if (lhs != null && rhs != null) {
            return lhs.toString() + rhs.toString();
        } else if (lhs != null) {
            return lhs;
        } else {
            return rhs;
        }
    }

    /**
     * Equality.
     */
    public static boolean areEqual(Object lhs, Object rhs)
    {
        return lhs != null?
            lhs.equals(rhs):
            rhs == null;
    }

    public static String verify(Object condition, Object text)
    {
        if (!Boolean.TRUE.equals(condition)) {
            throw new IllegalArgumentException(String.format("%s FAILED", text));
        }

        return condition.toString();
    }
}
