package tql;

import org.antlr.v4.runtime.CommonTokenStream;
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
        super.exitSql(a0);
    }

    @Override
    public void
    exitQuery_expression(tql.tqlParser.Query_expressionContext a0)
    {
        createNode(Select);
        super.exitQuery_expression(a0);
    }

    @Override
    public void
    exitSelect_list(tql.tqlParser.Select_listContext a0)
    {
        createNode(SelectionList);
        super.exitSelect_list(a0);
    }

    @Override
    public void
    exitTable_expression(tql.tqlParser.Table_expressionContext a0)
    {
        createNode(TableExpression);
        super.exitTable_expression(a0);
    }

    @Override
    public void
    exitTable_name(tql.tqlParser.Table_nameContext a0)
    {
        createNode(TableName);
        super.exitTable_name(a0);
    }

    @Override
    public void
    exitWhere(tql.tqlParser.WhereContext a0)
    {
        createNode(Where);
        super.exitWhere(a0);
    }

    @Override
    public void
    exitGlobbed_selection_item(tql.tqlParser.Globbed_selection_itemContext a0)
    {
        createNode(UnqualifiedGlobbedSelection);
        super.exitGlobbed_selection_item(a0);
    }

    @Override
    public void
    exitIdentifier(tql.tqlParser.IdentifierContext a0)
    {
        createNode(Identifier, tokenStream.getText(a0));
        super.exitIdentifier(a0);
    }


    @Override
    public void
    exitString_literal(tql.tqlParser.String_literalContext a0)
    {
        createNode(StringLiteral, tokenStream.getText(a0));
        super.exitString_literal(a0);
    }
}
