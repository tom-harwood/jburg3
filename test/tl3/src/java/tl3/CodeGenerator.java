package tl3;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import static javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;


import jburg.ProductionTable;
import jburg.Reducer;

import static tl3.Nonterminal.*;
import static tl3.NodeType.*;

public class CodeGenerator
{
    private final Node root;

    static class ScopeNamespace
    {
        ScopeNamespace(Object nsName)
        {
            this.nsName = nsName != null? nsName.toString(): null;
        }

        final String nsName;
        final Map<String,String>  names = new HashMap<String,String>();

        String addDefinition(String name)
        {
            names.put(name, String.format("%s%x", name, System.identityHashCode(name)));
            return names.get(name);
        }

        boolean hasDefinition(String name)
        {
            return names.containsKey(name);
        }

        String resolve(String name)
        {
            if (this.hasDefinition(name)) {
                return names.get(name);
            } else {
                throw new IllegalArgumentException("Unresolved name " + name);
            }
        }
    }

    private final Stack<ScopeNamespace> namespaces = new Stack<ScopeNamespace>();

    CodeGenerator(Node root, String fileName)
    {
        this.root = root;
    }

    static class JavaSourceFromString extends SimpleJavaFileObject
    {
        final String code;

        JavaSourceFromString(String name, String code)
        {
            super(URI.create("string:///" + name.replace('.','/') + Kind.SOURCE.extension),Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors)
        {
            return code;
        }
    }

    void generate(String className)
    throws Exception
    {
        Reducer<Nonterminal, NodeType> reducer = new Reducer<Nonterminal,NodeType>(this, productions);
        reducer.label(root);

        String source = String.format(
            "import tl3.Runtime;\nclass %s {\n" +
            "public static void main(String[] args) {\n%s\n}" +
            "}",
            className,
            reducer.reduce(root, MainProgram)
        );

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        JavaFileObject file = new JavaSourceFromString(className, source);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(file);
        ArrayList<String> options = new ArrayList<String>();
        options.add("-d");
        options.add("classes");
        CompilationTask task = compiler.getTask(null, null, diagnostics, options, null, compilationUnits);

        boolean success = task.call();

        for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
            System.err.println(diagnostic.getCode());
            System.err.println(diagnostic.getKind());
        }
    }

    static final ProductionTable<Nonterminal, NodeType> productions = new ProductionTable<Nonterminal,NodeType>();

    final static Method noPreCallback = null;
    final static Method noPostCallback = null;

    static
    {
        Method statementList = null;

        try {
            statementList = getPostCallback("statementList", Class.forName("[Ljava.lang.String;"));
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }

        productions.addPatternMatch(MainProgram,    Scope,  1, getPreCallback("namedScope"),    getPostCallback("exitScope", String.class, Object.class), Statements, Name);
        productions.addPatternMatch(MainProgram,    Scope,  1, getPreCallback("unnamedScope"),  getPostCallback("exitScope", String.class, Object.class), Statements, NullPtr);
        productions.addPatternMatch(Statement,      Scope,  1, getPreCallback("namedScope"),    getPostCallback("exitScope", String.class, Object.class), Statements, Name);
        productions.addPatternMatch(Statement,      Scope,  1, getPreCallback("unnamedScope"),  getPostCallback("exitScope", String.class, Object.class), Statements, NullPtr);

        productions.addVarArgsPatternMatch(Statements, ScopeContents, 1, noPreCallback,  statementList, Statement);

        productions.addPatternMatch(Statement,  VarDef,         1, noPreCallback, getPostCallback("varDefNoInitializer", String.class), Name);
        productions.addPatternMatch(Statement,  VarDef,         1, noPreCallback, getPostCallback("varDefWithInitializer", String.class, String.class), Name, Expression);
        productions.addPatternMatch(Statement,  Assignment,     1, noPreCallback, getPostCallback("assignmentStmt", String.class, String.class), LValue, Expression);
        productions.addPatternMatch(Statement,  Print,          1, noPreCallback, getPostCallback("printStmt", String.class), Expression);
        productions.addPatternMatch(Statement,  Verify,         1, noPreCallback, getPostCallback("verify", String.class, String.class), Expression, Expression);

        // Identifiers
        productions.addPatternMatch(LValue,     Identifier,     1, noPreCallback, getPostCallback("identifier", String.class), Name);
        productions.addPatternMatch(Expression, Identifier,     1, getPredicate("isAlias"), noPreCallback, getPostCallback("aliasedIdentifier", String.class), false, Name);
        productions.addPatternMatch(LValue,     Identifier,     1, noPreCallback, getPostCallback("identifier", String.class, String.class), Name, Name);
        productions.addPatternMatch(Name,       IdentifierPart, 1, noPreCallback, getPostCallback("nameRef"));

        // Binary operators
        productions.addPatternMatch(Expression, Equal,          1, noPreCallback, getPostCallback("equality", String.class, String.class), Expression, Expression);
        productions.addPatternMatch(Expression, Add,            1, noPreCallback, getPostCallback("addition", String.class, String.class), Expression, Expression);

        // Leaf expressions
        productions.addPatternMatch(Expression, IntegerLiteral, getPostCallback("integerLiteral"));
        productions.addPatternMatch(Expression, StringLiteral,  getPostCallback("stringLiteral"));
        productions.addClosure(Expression, LValue, 2);

        productions.addNullPointerProduction(NullPtr, 1, null);

        productions.generateStates();
        productions.dump(System.getenv("BURMDUMP"));
    }

    static Method getPostCallback(String methodName, Class<?>... formalTypes)
    {
        Class<?>[] formalsWithNode = new Class<?>[formalTypes.length+1];
        formalsWithNode[0] = Node.class;
        for (int i = 0; i < formalTypes.length; i++) {
            formalsWithNode[i+1] = formalTypes[i];
        }

        try {
            return CodeGenerator.class.getDeclaredMethod(methodName, formalsWithNode);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }

        return null;
    }

    static Method getPreCallback(String methodName)
    {
        try {
            return CodeGenerator.class.getDeclaredMethod(methodName, Node.class, Nonterminal.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    static Method getPredicate(String methodName)
    {
        try {
            return CodeGenerator.class.getDeclaredMethod(methodName, Node.class);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    public String nameRef(Node node)
    {
        return node.content.toString();
    }

    public String identifier(Node node, String name)
    {
        for (int idx = namespaces.size() - 1; idx >= 0; idx--) {

            if (namespaces.get(idx).hasDefinition(name)) {
                return namespaces.get(idx).resolve(name);
            }
        }
        return namespaces.peek().resolve(name);
    }

    public String aliasedIdentifier(Node node, String name)
    {
        throw new UnsupportedOperationException();
    }

    public String identifier(Node node, String qualifier, String name)
    {
        ScopeNamespace ns = null;

        for (int idx = namespaces.size() - 1; ns == null && idx >= 0; idx--) {
            if (qualifier.equals(namespaces.get(idx).nsName)) {
                ns = namespaces.get(idx);
            }
        }

        if (ns == null) {
            throw new IllegalArgumentException("Unresolved namespace " + qualifier);
        }

        return ns.resolve(name);
    }

    public String integerLiteral(Node node)
    {
        return node.content.toString();
    }

    public String stringLiteral(Node node)
    {
        return node.content.toString();
    }

    public String addition(Node node, String lhs, String rhs)
    {
        return String.format("Runtime.add(%s,%s)", lhs, rhs);
    }

    public String equality(Node node, String lhs, String rhs)
    {
        return String.format("Runtime.areEqual(%s,%s)", lhs, rhs);
    }

    public String verify(Node node, String condition, String text)
    {
        return String.format("Runtime.verify(%s,%s);", condition, text);
    }

    public String statementList(Node node, String... statements)
    {
        StringBuilder builder = new StringBuilder();

        for (String s: statements) {
            builder.append(s);
            builder.append("\n");
        }

        return builder.toString();
    }

    public String identity(Node n, String... s)
    {
        return s[0];
    }

    public String varDefNoInitializer(Node node, String varName)
    {
        return String.format("Object %s = null;", namespaces.peek().addDefinition(varName));
    }

    public String varDefWithInitializer(Node node, String varName, String initializer)
    {
        return String.format("Object %s = %s;", namespaces.peek().addDefinition(varName), initializer);
    }

    public String assignmentStmt(Node node, String lvalue, String rvalue)
    {
        return String.format("%s = %s;", lvalue, rvalue);
    }

    public String printStmt(Node node, String exp)
    {
        return String.format("System.out.println(%s);", exp);
    }

    public void namedScope(Node scope, Nonterminal goal)
    {
        namespaces.push(new ScopeNamespace(scope.getSubtree(1).content));
    }

    public void unnamedScope(Node scope, Nonterminal goal)
    {
        namespaces.push(new ScopeNamespace(null));
    }

    public String exitScope(Node scope, String statements, Object scopeName)
    {
        // TODO: check scope name
        namespaces.pop();
        return statements;
    }

    /*
     * Semantic predicates
     */
    public boolean isAlias(Node n)
    {
        return false;
    }
}
