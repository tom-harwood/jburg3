package tql;

import java.util.List;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.pattern.ParseTreePattern;
import static tql.TQLNodeType.*;

class NodeBuilder extends BaseNodeBuilder
{
    CommonTokenStream tokenStream;

    NodeBuilder(CommonTokenStream tokenStream)
    {
        this.tokenStream = tokenStream;
    }

    @Override
    public void exitSql(tql.tqlParser.SqlContext a0)
    {
        createNode(SQL);
    }

    @Override
    public void
    exitQuery_expression(tql.tqlParser.Query_expressionContext a0)
    {
        createNode(Select);
    }

    @Override
    public void
    exitSelect_list(tql.tqlParser.Select_listContext a0)
    {
        createNode(SelectionList);
    }

    @Override
    public void
    exitTable_expression(tql.tqlParser.Table_expressionContext a0)
    {
        createNode(TableExpression);
    }

    @Override
    public void
    exitTable_name(tql.tqlParser.Table_nameContext a0)
    {
        createNode(TableName);
    }

    @Override
    public void
    exitWhere(tql.tqlParser.WhereContext a0)
    {
        createNode(Where);
    }

    @Override
    public void
    exitGlobbed_selection_item(tql.tqlParser.Globbed_selection_itemContext a0)
    {
        createNode(Star);
    }

    @Override
    public void
    exitIdentifier(tql.tqlParser.IdentifierContext a0)
    {
        createNode(Identifier, tokenStream.getText(a0));
    }


    @Override
    public void
    exitString_literal(tql.tqlParser.String_literalContext a0)
    {
        createNode(StringLiteral, tokenStream.getText(a0));
    }

    public void exitUnsigned_literal(tql.tqlParser.Unsigned_literalContext context)
    {
        createNode(UnsignedLiteral, tokenStream.getText(context));
    }

    @Override
    public void exitBoolean_expression(tql.tqlParser.Boolean_expressionContext context)
    {
        binop(context);
    }

    @Override
    public void exitComparison_expression(tql.tqlParser.Comparison_expressionContext context)
    {
        binop(context);
    }

    @Override
    public void exitArithmetic_expression(tql.tqlParser.Arithmetic_expressionContext context)
    {
        binop(context);
    }

    @Override
    public void exitTerm(tql.tqlParser.TermContext context)
    {
        binop(context);
    }

    @Override
    public void exitFactor(tql.tqlParser.FactorContext context)
    {
        binop(context);
    }

 
    @Override
    public void exitUnary_expression(tql.tqlParser.Unary_expressionContext context)
    {
        unop(context);
    }

    @Override
    public void exitNegated_boolean_expression(tql.tqlParser.Negated_boolean_expressionContext context)
    {
        unop(context);
    }

    @Override
    public void exitVar_decl(tql.tqlParser.Var_declContext context)
    {
        createNode(VariableDeclaration);
    }

    @Override
    public void exitAssignment_stmt(tql.tqlParser.Assignment_stmtContext context)
    {
        createNode(AssignmentStatement);
    }

    protected void binop(ParserRuleContext context)
    {
        if (pendingNodeCount() == 2) {
            //System.out.printf("binop: %s\n", tokenStream.getText(context));
            createNode(getNodeType(context));
        } else {
            //System.out.printf("binop found %d pending nodes, %d children (%s)\n", pendingNodeCount(), context.getChildCount(), tokenStream.getText(context));
            assert pendingNodeCount() < 2: String.format("Expected zero or one pending nodes, found %d", pendingNodeCount());
            scopeStack.pop();
        }
    }

    protected void unop(ParserRuleContext context)
    {
        if (pendingNodeCount() == 1) {
            //System.out.printf("unop: %s\n", tokenStream.getText(context));
            createNode(getNodeType(context));
        } else {
            //System.out.printf("binop found %d pending nodes, %d children (%s)\n", pendingNodeCount(), context.getChildCount(), tokenStream.getText(context));
            assert pendingNodeCount() == 0: String.format("Expected zero pending nodes, found %d", pendingNodeCount());
            scopeStack.pop();
        }
    }

    protected TQLNodeType getNodeType(ParserRuleContext context)
    {
        String candidateOp = null;

        switch (context.getChildCount()) {

            case 2: {
                candidateOp = context.getChild(0).getText();

                if ("-".equals(candidateOp)) {
                    return Minus;
                } else if ("+".equals(candidateOp)) {
                    return Plus;
                } else if ("NOT".equalsIgnoreCase(candidateOp)) {
                    return Not;
                }

                break;
            }

            case 3: {
                candidateOp = context.getChild(1).getText();

                if (binop_Equal.matches(context.getChild(1))) {
                    return Equal;
                } else if ("<>".equals(candidateOp)) {
                    return NotEqual;
                } else if (">".equals(candidateOp)) {
                    return Greater;
                } else if (">=".equals(candidateOp)) {
                    return GreaterEqual;
                } else if ("<".equals(candidateOp)) {
                    return Less;
                } else if ("<=".equals(candidateOp)) {
                    return LessEqual;
                } else if ("-".equals(candidateOp)) {
                    return Minus;
                } else if ("+".equals(candidateOp)) {
                    return Plus;
                } else if ("*".equals(candidateOp)) {
                    return Star;
                } else if ("AND".equalsIgnoreCase(candidateOp)) {
                    return And;
                } else if ("OR".equalsIgnoreCase(candidateOp)) {
                    return Or;
                }

                break;
            }

            default:
                throw new IllegalStateException(String.format("Unsupported arity %d", context.getChildCount()));
        }

        throw new IllegalStateException(String.format("No translation for candidateOp %s (%s)", candidateOp, tokenStream.getText(context)));
    }

    static ParseTreePattern binop_And;
    static ParseTreePattern binop_Equal;
    static ParseTreePattern binop_Greater;
    static ParseTreePattern binop_GreaterEqual;
    static ParseTreePattern binop_Less;
    static ParseTreePattern binop_LessEqual;
    static ParseTreePattern binop_Minus;
    static ParseTreePattern binop_NotEqual;
    static ParseTreePattern binop_Or;
    static ParseTreePattern binop_Plus;
    static ParseTreePattern binop_Star;

    static ParseTreePattern unop_Minus;
    static ParseTreePattern unop_Not;
    static ParseTreePattern unop_Plus;

    static
    {
        tqlLexer lexer = new tqlLexer(new ANTLRInputStream());
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tqlParser parser = new tqlParser(tokens);

         binop_And          = parser.compileParseTreePattern("<AND>", tqlParser.RULE_boolean_connector);
         binop_Equal        = parser.compileParseTreePattern("<EQU>", tqlParser.RULE_comparator);
         binop_Greater      = parser.compileParseTreePattern("<GTR>", tqlParser.RULE_comparator);
         binop_GreaterEqual = parser.compileParseTreePattern("<GTE>", tqlParser.RULE_comparator);
         binop_Less         = parser.compileParseTreePattern("<LSS>", tqlParser.RULE_comparator);
         binop_LessEqual    = parser.compileParseTreePattern("<LTE>", tqlParser.RULE_comparator);
         binop_Minus        = parser.compileParseTreePattern("<MINUS>", tqlParser.RULE_unary_operator);
         binop_Or           = parser.compileParseTreePattern("<OR>", tqlParser.RULE_boolean_connector);
    }
}
