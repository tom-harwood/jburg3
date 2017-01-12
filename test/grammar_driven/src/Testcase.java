class Testcase
{
    public enum TestType { Normal, Negative, CanProduce, CannotProduce };

    final String        name;
    final Nonterminal   type;
    final TestType      testType;
    final Nonterminal   canProduceType;
    final String        expected;
    Node                root;

    Testcase(String name, Nonterminal type, String expected, TestType testType)
    {
        this.name = name;
        this.type = type;
        this.expected = expected;
        this.canProduceType = null;
        this.testType = testType;
    }

    Testcase(String name, Nonterminal nonterminal, TestType testType)
    {
        this.name = name;
        this.type = null;
        this.testType = testType;
        this.expected = null;

        if (testType == TestType.CanProduce || testType == TestType.CannotProduce) {
            this.canProduceType = nonterminal;
        } else {
            throw new IllegalArgumentException(String.format("Unsupported TestType %s -- needs expected result or expected Exception text"));
        }
    }


    public String toString()
    {
        return String.format("Testcase type=%s expected=%s tree=%s", type, expected, root);
    }
}
