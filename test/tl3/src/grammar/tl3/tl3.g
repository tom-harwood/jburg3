grammar tl3;
@header {
    package tl3;
}

scope  : '{' (':' ID)? scopeContents '}';

scopeContents:
    (
    scope
    | statement
    )*
    ;

statement:
    assignment
    | print
    | varDef
    | exprStmt
    ;

assignment: identifier '=' expression ';';

print: 'print' expression ';';

varDef: 'var' ID ('=' expression)? ';';

exprStmt: expression ';';

expression: booleanExpression;

booleanExpression:
    arithmeticExpression ('==' arithmeticExpression)?
    ;

arithmeticExpression:
    primaryExpression ('+' primaryExpression)?
    ;


primaryExpression:
    builtinProcedure
    | identifier
    | INT
    | StringLiteral
    | '(' expression ')'
    ;

identifier: ID ('.' ID)?;

builtinProcedure:
    'verify' '(' expression ',' expression ')'
    ;

ID : [a-zA-Z][a-zA-Z0-9]*;
INT: [0-9]+;
WS : [ \t\r\n]+ -> skip ;

StringLiteral
    :   '"' StringCharacters? '"'
    ;
fragment
StringCharacters
    :   StringCharacter+
    ;
fragment
StringCharacter
    :   ~["\\]
    |   EscapeSequence
    ;
// §3.10.6 Escape Sequences for Character and String Literals
fragment
EscapeSequence
    :   '\\' [btnfr"'\\]
    /*
    |   OctalEscape
    |   UnicodeEscape
    */
    ;
