class Testcase
{
    final String        name;
    final Nonterminal   type;
    final String        expected;
    final String        expectedException;
    Node                root;

    Testcase(String name, Nonterminal type, String expected, String expectedException)
    {
        this.name = name;
        this.type = type;
        this.expected = expected;
        this.expectedException = expectedException;
    }

    public String toString()
    {
        return String.format("Testcase type=%s expected=%s tree=%s", type, expected, root);
    }
}
