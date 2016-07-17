package tql;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class TQL
{
    public static void main(String[] args)
    throws Exception
    {
        parseFile(args[0], "TQLMain");
    }

    public static void parseFile(String inputFile, String className)
    throws Exception
    {
        // Parse the input
        Lexer lexer = new tqlLexer(new ANTLRFileStream(inputFile));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tqlParser parser = new tqlParser(tokens);
        ParserRuleContext parseTree = parser.sql();
			
        // Convert concrete syntax tree to AST
        NodeBuilder builder = new NodeBuilder(tokens);
        new ParseTreeWalker().walk(builder, parseTree);

        // Analyze the abstract syntax tree
        Node root = builder.getRoot();
        TreeParser treeParser = new TreeParser(root);

        if (treeParser.canProduce(SemanticLabel.SQL)) {
            System.out.printf("Succeeded: labeled SQL\n");
        } else {
            System.err.printf("FAILED to label: %s\n", root);
            System.exit(1);
        }
	}
}
