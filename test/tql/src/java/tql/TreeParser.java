package tql;

import java.util.Arrays;
import java.util.HashMap;
import jburg.Reducer;
import jburg.ProductionTable;
import jburg.semantics.HostRoutine;

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

    static final HostRoutine noCallback = null;

    static
    {
        productions.addVarArgsPatternMatch(SemanticLabel.SQL, TQLNodeType.SQL, noCallback, Arrays.asList(SemanticLabel.Statement));

        productions.addPatternMatch(
            SemanticLabel.Statement, TQLNodeType.Select, noCallback,
            Arrays.asList(SemanticLabel.SelectionList, SemanticLabel.TableExpression)
        );

        productions.addPatternMatch(
            SemanticLabel.Statement, TQLNodeType.Select, noCallback,
            Arrays.asList(SemanticLabel.SelectionList, SemanticLabel.TableExpression, SemanticLabel.Where)
        );

        productions.addVarArgsPatternMatch(SemanticLabel.SelectionList, TQLNodeType.SelectionList, noCallback,
            Arrays.asList(SemanticLabel.SelectionItem)
        );

        productions.addClosure(SemanticLabel.SelectionItem, SemanticLabel.Expression);

        productions.addClosure(SemanticLabel.Expression, SemanticLabel.Literal);
        productions.addClosure(SemanticLabel.Expression, SemanticLabel.Identifier);

        productions.addPatternMatch(SemanticLabel.Literal, TQLNodeType.StringLiteral, noCallback);
        productions.addPatternMatch(SemanticLabel.Literal, TQLNodeType.UnsignedLiteral, noCallback);

        productions.addPatternMatch(SemanticLabel.Expression, TQLNodeType.And, noCallback, Arrays.asList(SemanticLabel.Expression, SemanticLabel.Expression));
        productions.addPatternMatch(SemanticLabel.Expression, TQLNodeType.Or, noCallback, Arrays.asList(SemanticLabel.Expression, SemanticLabel.Expression));
        productions.addPatternMatch(SemanticLabel.Expression, TQLNodeType.Not, noCallback, Arrays.asList(SemanticLabel.Expression));

        productions.addPatternMatch(SemanticLabel.Expression, TQLNodeType.Plus, noCallback, Arrays.asList(SemanticLabel.Expression, SemanticLabel.Expression));
        productions.addPatternMatch(SemanticLabel.Expression, TQLNodeType.Plus, noCallback, Arrays.asList(SemanticLabel.Expression));
        productions.addPatternMatch(SemanticLabel.Expression, TQLNodeType.Minus, noCallback, Arrays.asList(SemanticLabel.Expression, SemanticLabel.Expression));
        productions.addPatternMatch(SemanticLabel.Expression, TQLNodeType.Minus, noCallback, Arrays.asList(SemanticLabel.Expression));
        productions.addPatternMatch(SemanticLabel.Expression, TQLNodeType.Star, noCallback, Arrays.asList(SemanticLabel.Expression, SemanticLabel.Expression));
        productions.addPatternMatch(SemanticLabel.Expression, TQLNodeType.Slash, noCallback, Arrays.asList(SemanticLabel.Expression, SemanticLabel.Expression));
        productions.addPatternMatch(SemanticLabel.Expression, TQLNodeType.Equal, noCallback, Arrays.asList(SemanticLabel.Expression, SemanticLabel.Expression));
        productions.addPatternMatch(SemanticLabel.Expression, TQLNodeType.NotEqual, noCallback, Arrays.asList(SemanticLabel.Expression, SemanticLabel.Expression));
        productions.addPatternMatch(SemanticLabel.Expression, TQLNodeType.Less, noCallback, Arrays.asList(SemanticLabel.Expression, SemanticLabel.Expression));
        productions.addPatternMatch(SemanticLabel.Expression, TQLNodeType.LessEqual, noCallback, Arrays.asList(SemanticLabel.Expression, SemanticLabel.Expression));
        productions.addPatternMatch(SemanticLabel.Expression, TQLNodeType.Greater, noCallback, Arrays.asList(SemanticLabel.Expression, SemanticLabel.Expression));
        productions.addPatternMatch(SemanticLabel.Expression, TQLNodeType.GreaterEqual, noCallback, Arrays.asList(SemanticLabel.Expression, SemanticLabel.Expression));

        productions.addVarArgsPatternMatch(SemanticLabel.TableExpression, TQLNodeType.TableExpression, noCallback, Arrays.asList(SemanticLabel.TableName));

        productions.addPatternMatch(SemanticLabel.TableName, TQLNodeType.TableName, noCallback, Arrays.asList(SemanticLabel.Identifier));

        productions.addPatternMatch(SemanticLabel.Identifier, TQLNodeType.Identifier, noCallback);

        productions.addPatternMatch(SemanticLabel.SelectionItem, TQLNodeType.Star, noCallback);

        productions.addPatternMatch(SemanticLabel.Where, TQLNodeType.Where, noCallback, Arrays.asList(SemanticLabel.Expression));

        productions.addPatternMatch(SemanticLabel.Statement, TQLNodeType.VariableDeclaration, noCallback, Arrays.asList(SemanticLabel.Identifier, SemanticLabel.Expression));
        productions.addPatternMatch(SemanticLabel.Statement, TQLNodeType.VariableDeclaration, noCallback, Arrays.asList(SemanticLabel.Identifier));
        productions.addPatternMatch(SemanticLabel.Statement, TQLNodeType.AssignmentStatement, noCallback, Arrays.asList(SemanticLabel.Identifier, SemanticLabel.Expression));

        productions.generateStates();
    }
}
