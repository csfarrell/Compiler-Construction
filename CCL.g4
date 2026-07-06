grammar CCL;

// Syntax Analyser Rules
program:  (decl_list function_list main) | EOF;

decl_list: (decl SEMICOL decl_list)?;

decl: var_decl | const_decl;

var_decl: VAR IDENTIFIER COLON type;

const_decl: CONST IDENTIFIER COLON type ASSIGN expression;

function_list: (function function_list)?;

function: type IDENTIFIER LBRACK parameter_list RBRACK
    LBRACE
        decl_list
        statement_block
        RETURN LBRACK (expression)? RBRACK SEMICOL 
    RBRACE
    ;

type: INTEGER | BOOLEAN | VOID;

parameter_list: (nemp_parameter_list)?;

nemp_parameter_list: IDENTIFIER COLON type | (IDENTIFIER COLON type COMMA nemp_parameter_list);

main: MAIN
    LBRACE
        decl_list
        statement_block
    RBRACE
    ;

statement_block: (statement statement_block)?;

statement: IDENTIFIER ASSIGN expression SEMICOL
    | IDENTIFIER LBRACK arg_list RBRACK SEMICOL
    | LBRACE statement_block RBRACE
    | IF condition LBRACE statement_block RBRACE ELSE LBRACE statement_block RBRACE
    | WHILE LBRACK condition RBRACK LBRACE statement_block RBRACE
    | SKIP_ SEMICOL
    ;

expression: fragment_ binary_arith_op fragment_
    | LBRACK expression RBRACK
    | IDENTIFIER LBRACK arg_list RBRACK
    | fragment_
    ;

binary_arith_op: PLUS | MINUS;

// Since fragment is a keyword function renamed to fragment_
// Also since left recursion appears between this and expression, l/r brackets for expression
fragment_: IDENTIFIER
    | MINUS IDENTIFIER
    | NUMBER
    | TRUE
    | FALSE
    | LBRACK expression RBRACK
    ;

condition: TILDE condition
    | LBRACK condition RBRACK
    | expression comp_op expression
    | condition OR condition
    | condition AND condition
    ;

comp_op: EQUAL | NOTEQU | LT | LE | GT | GE;

arg_list: nemp_arg_list?;

nemp_arg_list: IDENTIFIER | IDENTIFIER COMMA nemp_arg_list;


// Lexical Analyser Rules
// Fragments for each letter (j,q,x,y,z doesnt appear in terminals)
fragment A: 'a' | 'A';
fragment B: 'b' | 'B';
fragment C: 'c' | 'C';
fragment D: 'd' | 'D';
fragment E: 'e' | 'E';
fragment F: 'f' | 'F';
fragment G: 'g' | 'G';
fragment H: 'h' | 'H';
fragment I: 'i' | 'I';

fragment K: 'k' | 'K';
fragment L: 'l' | 'L';
fragment M: 'm' | 'M';
fragment N: 'n' | 'N';
fragment O: 'o' | 'O';
fragment P: 'p' | 'P';

fragment R: 'r' | 'R';
fragment S: 's' | 'S';
fragment T: 't' | 'T';
fragment U: 'u' | 'U';
fragment V: 'v' | 'V';
fragment W: 'w' | 'W';


// Operation
// var, const, return, integer, boolean, void, main, if, else, true, false, while and skip.
// Since skip is an inbuilt function, renamed to skip_
VAR:        V A R;
RETURN:     R E T U R N;
CONST:      C O N S T;
INTEGER:    I N T E G E R;
BOOLEAN:    B O O L E A N;
VOID:       V O I D;
MAIN:       M A I N;
IF:         I F;
ELSE:       E L S E;
WHILE:      W H I L E;
SKIP_:      S K I P;
TRUE:       T R U E;
FALSE:      F A L S E;

// , ; : = { } ( ) 
COMMA:      ',';
SEMICOL:    ';';
COLON:      ':';
LBRACK:     '(';
RBRACK:     ')';
LBRACE:     '{';
RBRACE:     '}';

// + - ∼ || && == != < <= > >=
ASSIGN:     '=';
PLUS:       '+';
MINUS:      '-';
TILDE:      '~';
OR:         '||';
AND:        '&&';
EQUAL:      '==';
NOTEQU:     '!=';
LT:         '<';
LE:         '<=';
GT:         '>';
GE:         '>=';

// Integers are represented by a string of one or more digits (‘0’-‘9’) that do not start with the digit ‘0’, but may start with a minus sign (‘-’), e.g. 123, -456

// Identifiers are represented by a string of letters, digits or underscore character (‘ ’) beginning with a letter or underscore character. Identifiers cannot be reserved words

// Numbers and Identifiers
fragment Letter: [a-zA-Z];
fragment Digit: [0-9];
fragment Underscore: '_';

NUMBER: '0' | MINUS [1-9] Digit* | [1-9] Digit*;
IDENTIFIER: [a-zA-Z_] (Letter | Digit | Underscore)*;

WS:     [ \t\n\r] -> skip;
COMMENT:    '//' ~[\r\n]* -> skip;
BLOCK_COM: '/*' (BLOCK_COM | .)*? '*/' -> skip;