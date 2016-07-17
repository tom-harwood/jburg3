package tql;

import java.util.HashMap;
import jburg.Reducer;
import jburg.ProductionTable;

class TreeParser
{
    Node<SemanticLabel,TQLNodeType> root;
    Object visitor = null;

    TreeParser(Node<SemanticLabel,TQLNodeType> root)
    throws Exception
    {
        // TODO: This can be done incrementally
        // by the NodeBuilder.
        Reducer<SemanticLabel,TQLNodeType> reducer = new Reducer<SemanticLabel,TQLNodeType>(null, productions);
        reducer.label(root);

        this.root = root;
    }

    boolean canProduce(SemanticLabel goal)
    throws Exception
    {
        return productions.canProduce(root, goal, visitor);
    }

    static final ProductionTable<SemanticLabel,TQLNodeType> productions = new ProductionTable<SemanticLabel,TQLNodeType>();

    static final java.lang.reflect.Method noCallback = null;

    static
    {
        productions.addVarArgsPatternMatch(SemanticLabel.SQL, TQLNodeType.SQL, noCallback, SemanticLabel.Statement);

        productions.addPatternMatch(
            SemanticLabel.Statement, TQLNodeType.Select, noCallback,
            SemanticLabel.SelectionList, SemanticLabel.TableExpression
        );

        productions.addPatternMatch(
            SemanticLabel.Statement, TQLNodeType.Select, noCallback,
            SemanticLabel.SelectionList, SemanticLabel.TableExpression, SemanticLabel.Where
        );

        productions.addVarArgsPatternMatch(
            SemanticLabel.SelectionList, TQLNodeType.SelectionList, noCallback, SemanticLabel.SelectionItem
        );

        productions.addClosure(SemanticLabel.SelectionItem, SemanticLabel.Expression);

        productions.addClosure(SemanticLabel.Expression, SemanticLabel.Literal);
        productions.addClosure(SemanticLabel.Expression, SemanticLabel.Identifier);

        productions.addPatternMatch(SemanticLabel.Literal, TQLNodeType.StringLiteral, noCallback);

        productions.addVarArgsPatternMatch(SemanticLabel.TableExpression, TQLNodeType.TableExpression, noCallback, SemanticLabel.TableName);

        productions.addPatternMatch(SemanticLabel.TableName, TQLNodeType.TableName, noCallback, SemanticLabel.Identifier);

        productions.addPatternMatch(SemanticLabel.Identifier, TQLNodeType.Identifier, noCallback);

        productions.addPatternMatch(SemanticLabel.SelectionItem, TQLNodeType.UnqualifiedGlobbedSelection, noCallback);

        productions.addPatternMatch(SemanticLabel.Where, TQLNodeType.Where, noCallback, SemanticLabel.Expression);

        //productions.setVerboseTrigger(".*");
        productions.generateStates();
        productions.dump("/tmp/tqlTable.xml", "xml.stg", new HashMap<String,String>());
    }

}
