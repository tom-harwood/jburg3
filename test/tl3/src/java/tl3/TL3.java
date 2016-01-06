package tl3;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class TL3
{
    public static void main(String[] args)
    throws Exception
    {
        parseFile(args[0], "tl3Main");
    }

    public static void parseFile(String inputFile, String className)
    throws Exception
    {
        // Parse the input
        Lexer lexer = new tl3Lexer(new ANTLRFileStream(inputFile));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tl3Parser parser = new tl3Parser(tokens);
        ParserRuleContext parseTree = parser.scope();
			
        // Convert concrete syntax tree to AST
        NodeBuilder builder = new NodeBuilder();
        new ParseTreeWalker().walk(builder, parseTree);

        // Generate code
        CodeGenerator generator = new CodeGenerator(builder.getResult(), className);
        generator.generate(className);
	}
}
