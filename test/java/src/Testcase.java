class Testcase
{
    final String        name;
    final Nonterminal   type;
    final String        expected;
    Node                root;

    Testcase(String name, Nonterminal type, String expected)
    {
        this.name = name;
        this.type = type;
        this.expected = expected;
    }

    public String toString()
    {
        return String.format("Testcase type=%s expected=%s tree=%s", type, expected, root);
    }
}
