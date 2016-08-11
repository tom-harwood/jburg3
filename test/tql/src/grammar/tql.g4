grammar tql;

@header {
package tql;
}

@members {
}

/*
===============================================================================
  SQL statement (Start Symbol)
===============================================================================
*/
sql:
    (statement (SEMI_COLON)?)+ EOF
  ;

statement:
    data_statement
  | data_change_statement
  | schema_statement
  | tql_statement
  ;

data_statement:
    query_expression
  ;

data_change_statement:
    insert_statement
  ;

schema_statement:
    create_table_statement
  | drop_table_statement
  ;

tql_statement:
    var_decl
    | assignment_stmt
    ;

create_table_statement:
    CREATE TABLE table_name 
    LPAREN field_element (COMMA field_element)* RPAREN
    ;

table_name:
    identifier
    ;

data_type:
    identifier
    ;

field_element:
    name=identifier data_type
    ;

drop_table_statement:
    DROP TABLE table_name
    ;

insert_statement:
    INSERT INTO table_name value_list
    ;

value_list: VALUES LPAREN expression (COMMA expression)* RPAREN
    ;

var_decl: VAR identifier (EQU expression)?
    ;

assignment_stmt: identifier EQU expression
    ;

query_expression:
    SELECT select_list FROM table_expression (where)?
    ;

select_list:
    globbed_selection_item
    | expression (COMMA expression)*
    ;

globbed_selection_item: STAR;

table_expression:
    table_name
    ;

where:
    WHERE boolean_expression
    ;

expression: boolean_expression;

boolean_expression:
    comparison_expression
    | comparison_expression boolean_connector boolean_expression
    | negated_boolean_expression
    ;

boolean_connector : AND | OR
    ;

negated_boolean_expression:
    NOT boolean_expression
    ;



comparison_expression:
    arithmetic_expression (comparator arithmetic_expression)?
    ;

comparator: EQU | NEQ | LSS | LTE | GTE | GTR
    ;

arithmetic_expression:
term ((PLUS | MINUS) term)?
    ;

term: factor ((STAR | SLASH) term)?
    ;

factor: unary_expression | postfix_operator | primary
    ;

unary_expression: unary_operator primary
    ;

unary_operator: PLUS | MINUS
    ;

postfix_operator: primary PLUSPLUS
    ;

primary:
    literal
    | identifier
    | LPAREN expression RPAREN
    ;

literal:
    unsigned_literal
    | signed_literal
    | general_literal
    ;


signed_literal:
    (PLUS | MINUS) unsigned_literal
    ;

/*
===============================================================================
  5.2 <token and separator>
  Specifying lexical units (tokens and separators) that participate in SQL language
===============================================================================
*/

identifier:
    ID_TOKEN
  | nonreserved_keywords
  ;

nonreserved_keywords:
    AVG
  | BETWEEN
  | BY
  | CENTURY
  | CHARACTER
  | COALESCE
  | COLLECT
  | COLUMN
  | COUNT
  | CUBE
  | DAY
  | DEC
  | DECADE
  | DOW
  | DOY
  | DROP
  | EPOCH
  | EVERY
  | EXISTS
  | EXTERNAL
  | EXTRACT
  | FILTER
  | FIRST
  | FORMAT
  | FUSION
  | GROUPING
  | HASH
  | INDEX
  | INSERT
  | INTERSECTION
  | ISODOW
  | ISOYEAR
  | LAST
  | LESS
  | LIST
  | LOCATION
  | MAX
  | MAXVALUE
  | MICROSECONDS
  | MILLENNIUM
  | MILLISECONDS
  | MIN
  | MINUTE
  | MONTH
  | NATIONAL
  | NULLIF
  | OVERWRITE
  | PARTITION
  | PARTITIONS
  | PRECISION
  | PURGE
  | QUARTER
  | RANGE
  | REGEXP
  | RLIKE
  | ROLLUP
  | SECOND
  | SET
  | SIMILAR
  | STDDEV_POP
  | STDDEV_SAMP
  | SUBPARTITION
  | SUM
  | TABLESPACE
  | THAN
  | TIMEZONE
  | TIMEZONE_HOUR
  | TIMEZONE_MINUTE
  | TRIM
  | TO
  | UNKNOWN
  | VALUES
  | VAR_POP
  | VAR_SAMP
  | VARYING
  | WEEK
  | YEAR
  | ZONE

  | BIGINT
  | BIT
  | BLOB
  | BOOL
  | BOOLEAN
  | BYTEA
  | CHAR
  | DATE
  | DECIMAL
  | DOUBLE
  | FLOAT
  | FLOAT4
  | FLOAT8
  | INET4
  | INT
  | INT1
  | INT2
  | INT4
  | INT8
  | INTEGER
  | NCHAR
  | NUMERIC
  | NVARCHAR
  | REAL
  | SMALLINT
  | TEXT
  | TIME
  | TIMESTAMP
  | TIMESTAMPTZ
  | TIMETZ
  | TINYINT
  | VARBINARY
  | VARBIT
  | VARCHAR
  ;


unsigned_literal:
    unsigned_numeric_literal
  ;

unsigned_numeric_literal: INT_LITERAL;

general_literal:
    string_literal
  | datetime_literal
  | boolean_literal
  ;

string_literal: StringLiteral;

datetime_literal:
    timestamp_literal
  | time_literal
  | date_literal
  ;

time_literal:
    TIME time_string=StringLiteral
  ;

timestamp_literal:
    TIMESTAMP timestamp_string=StringLiteral
  ;

date_literal:
    DATE date_string=StringLiteral
  ;

boolean_literal:
    TRUE | FALSE
  ;

INT_LITERAL: [0-9]+;

WS : [ \t\r\n]+ -> skip ;

SINGLE_LINE_COMMENT:
    '--' ~[\r\n]* -> channel(HIDDEN)
    ;

MULTILINE_COMMENT:
    '/*' .*? ( '*/' | EOF ) -> channel(HIDDEN)
    ;

StringLiteral
    :   '\'' StringCharacters? '\''
    ;

fragment
StringCharacters
    :   StringCharacter+
    ;

fragment
StringCharacter
    :   ~[\'\\]
    |   EscapeSequence
    ;

fragment
EscapeSequence
    :   '\\' [btnfr"'\\]
    /*
    |   OctalEscape
    |   UnicodeEscape
    */
    ;

/*
 * Keywords; must come before ID_TOKEN
 * or other productions that consume letters
 */
AVG: A V G;
BETWEEN: B E T W E E N;
BY: B Y;
CENTURY: C E N T U R Y;
CHARACTER: C H A R A C T E R;
COALESCE: C O A L E S C E;
COLLECT: C O L L E C T;
COLUMN: C O L U M N;
COUNT: C O U N T;
CUBE: C U B E;
DAY: D A Y;
DEC: D E C;
DECADE: D E C A D E;
DOW: D O W;
DOY: D O Y;
DROP: D R O P;
EPOCH: E P O C H;
EVERY: E V E R Y;
EXISTS: E X I S T S;
EXTERNAL: E X T E R N A L;
EXTRACT: E X T R A C T;
FILTER: F I L T E R;
FIRST: F I R S T;
FORMAT: F O R M A T;
FUSION: F U S I O N;
GROUPING: G R O U P I N G;
HASH: H A S H ;
INDEX: I N D E X ; 
INSERT: I N S E R T;
INTERSECTION: I N T E R S E C T I O N;
ISODOW: I S O D O W ;
ISOYEAR: I S O Y E A R;
LAST: L A S T ; 
LESS: L E S S ;
LIST: L I S T ;
LOCATION: L O C A T I O N;
MAX: M A X ; 
MAXVALUE: M A X V A L U E;
MICROSECONDS: M I C R O S E C O N D S ;
MILLENNIUM: M I L L E N N I U M ;
MILLISECONDS: M I L L I S E C O N D S ;
MIN: M I N ;
MINUTE: M I N U T E;
MONTH: M O N T H ;
NATIONAL: N A T I O N A L;
NULLIF: N U L L I F ;
OVERWRITE: O V E R W R I T E;
PARTITION: P A R T I T I O N;
PARTITIONS: P A R T I T I O N S;
PRECISION: P R E C I S I O N ;
PURGE: P U R G E ;
QUARTER: Q U A R T E R;
RANGE: R A N G E ;
REGEXP: R E G E X P;
RLIKE: R L I K E;
ROLLUP: R O L L U P;
SECOND: S E C O N D;
SET: S E T ; 
SIMILAR: S I M I L A R;
STDDEV_POP: S T D D E V UNDERSCORE P O P;
STDDEV_SAMP: S T D D E V UNDERSCORE S A M P;
SUBPARTITION: S U B P A R T I T I O N;
SUM: S U M ;
TABLESPACE: T A B L E S P A C E;
THAN: T H A N ; 
TIMEZONE: T I M E Z O N E;
TIMEZONE_HOUR: T I M E Z O N E UNDERSCORE H O U R ;
TIMEZONE_MINUTE: T I M E Z O N E UNDERSCORE M I N U T E ; 
TRIM: T R I M;
TO: T O;
UNKNOWN: U N K N O W N;
VALUES: V A L U E S ;
VAR: V A R;
VAR_POP: V A R UNDERSCORE P O P;
VAR_SAMP: V A R UNDERSCORE S A M P;
VARYING: V A R Y I N G ;
WEEK: W E E K ;
YEAR: Y E A R ;
ZONE: Z O N E ;

BIGINT: B I G I N T;
BIT: B I T;
BLOB: B L O B ;
BOOL: B O O L ; 
BOOLEAN: B O O L E A N;
BYTEA: B Y T E A ;
CHAR: C H A R ; 
DATE: D A T E ;
DECIMAL: D E C I M A L;
DOUBLE: D O U B L E ;
FLOAT: F L O A T ; 
FLOAT4: F L O A T '4';
FLOAT8: F L O A T '8' ;
INET4: I N E T '4';
INT: I N T ; 
INT1: I N T '1' ;
INT2: I N T '2' ;
INT4: I N T '4' ;
INT8: I N T '8' ; 
INTEGER: I N T E G E R ;
NCHAR: N C H A R ;
NUMERIC: N U M E R I C;
NVARCHAR: N V A R C H A R;
REAL: R E A L ;
SMALLINT: S M A L L I N T;
TEXT: T E X T ; 
TIME: T I M E ;
TIMESTAMP: T I M E S T A M P ;
TIMESTAMPTZ: T I M E S T A M P T Z ;
TIMETZ: T I M E T Z ;
TINYINT: T I N Y I N T ;
VARBINARY: V A R B I N A R Y;
VARBIT: V A R B I T ;
VARCHAR: V A R C H A R ;

AND:    A N D ;
CREATE: C R E A T E ;
FALSE:  F A L S E;
FROM:   F R O M ;
INTO:   I N T O ;
NOT:    N O T;
OR:     O R;
SELECT: S E L E C T ;
TABLE:  T A B L E ;
TRUE:   T R U E ;
WHERE:  W H E R E ;

fragment A:('a'|'A');
fragment B:('b'|'B');
fragment C:('c'|'C');
fragment D:('d'|'D');
fragment E:('e'|'E');
fragment F:('f'|'F');
fragment G:('g'|'G');
fragment H:('h'|'H');
fragment I:('i'|'I');
fragment J:('j'|'J');
fragment K:('k'|'K');
fragment L:('l'|'L');
fragment M:('m'|'M');
fragment N:('n'|'N');
fragment O:('o'|'O');
fragment P:('p'|'P');
fragment Q:('q'|'Q');
fragment R:('r'|'R');
fragment S:('s'|'S');
fragment T:('t'|'T');
fragment U:('u'|'U');
fragment V:('v'|'V');
fragment W:('w'|'W');
fragment X:('x'|'X');
fragment Y:('y'|'Y');
fragment Z:('z'|'Z');

fragment UNDERSCORE:'_';

COMMA:      ',';
EQU:        '=';
GTR:        '>';
GTE:        '>=';
LPAREN:     '(';
LSS:        '<';
LTE:        '<=';
MINUS:      '-';
NEQ:        '<>';
PLUS:       '+';
PLUSPLUS:   '+';
RPAREN:     ')';
SEMI_COLON: ';';
SLASH:      '/';
STAR:       '*';

// This pattern must appear after
// the keywords and fragments, so
// that they are recognized as
// specific language elements, not
// as identifiers.
ID_TOKEN : [a-zA-Z_][a-zA-Z0-9_]*;
