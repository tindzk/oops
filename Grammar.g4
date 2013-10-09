/* See also http://media.pragprog.com/titles/tpantlr2/code/tour/Java.g4 */
grammar Grammar;

AND: 'AND';
OR:  'OR';
MOD: 'MOD';
MUL: '*';
DIV: '/';
ADD: '+';
SUB: '-';
LEQ: '<=';
GEQ: '>=';
LT:  '<';
GT:  '>';
EQ:  '=';
NEQ: '#';
TRUE: 'TRUE';
FALSE: 'FALSE';

program
  : classDeclaration*;

classDeclaration
  : 'CLASS' name=Identifier
    ('EXTENDS' extendsClass=Identifier)?
    'IS'
    memberDeclaration*
    'END CLASS'
  ;

memberDeclaration
  : memberVariableDeclaration ';'
  | methodDeclaration
  ;

memberVariableDeclaration
  : variableDeclaration
  ;

methodDeclaration
  : 'METHOD' name=Identifier
    ('(' variableDeclaration (';' variableDeclaration)* ')')?
    (':' type)?
    'IS' methodBody
  ;

methodBody
  : (variableDeclaration ';')*
    'BEGIN' statements
    'END METHOD'
  ;

variableDeclaration
  : Identifier (',' Identifier)* ':' type
  ;

type
  : Identifier
  ;

statements
  : statement*
  ;

statement
  : 'IF' expression 'THEN' statements
    ('ELSEIF' expression 'THEN' statements)*
    ('ELSE' statements)?
    'END IF'                       # ifStatement
  | 'TRY' statements
    ('CATCH' literal 'DO' statements)+
    'END TRY'                      # tryStatement
  | 'WHILE' expression
    'DO' statements
    'END WHILE'                    # whileStatement
  | 'READ' expression ';'          # readStatement
  | 'WRITE' expression ';'         # writeStatement
  | 'RETURN' expression? ';'       # returnStatement
  | 'THROW' expression ';'         # throwStatement
  | expression ':=' expression ';' # assignStatement
  | expression ';'                 # expressionStatement
  ;

expression
  : '(' expression ')'                              # bracketsExpression
  | expression '.' call                             # callExpression
  | call                                            # call2Expression
  | expression '.' Identifier                       # memberAccessExpression
  | Identifier                                      # memberAccess2Expression
  | literal                                         # literalExpression
  | 'SELF'                                          # selfExpression
  | '-' expression                                  # minusExpression
  | 'NOT' expression                                # negateExpression
  | 'NEW' Identifier                                # instantiateExpression
  | expression op=(MUL | DIV | MOD) expression      # mulDivModExpression
  | expression op=(ADD | SUB) expression            # addSubExpression
  | expression op=(LEQ | GEQ | LT | GT) expression  # compareExpression
  | expression
    (EQ<assoc=right> | NEQ<assoc=right>)
    expression                                      # equalityExpression
  | expression AND expression                       # conjunctionExpression
  | expression OR expression                        # disjunctionExpression
  ;

call
  : Identifier '(' (expression (',' expression)*)? ')'
  ;

literal
  : IntegerLiteral       # integerLiteral
  | CharacterLiteral     # characterLiteral
  | StringLiteral        # stringLiteral
  | value=(TRUE | FALSE) # booleanLiteral
  | 'NULL'               # nullLiteral
  ;

Identifier
  : LETTER (LETTER | DIGIT)*
  ;

IntegerLiteral
  : DIGIT+
  ;

DIGIT
  : [0-9]
  ;

LETTER
  : [a-zA-Z]
  ;

CharacterLiteral
  : '\'' (EscapeSequence | .?) '\''
  ;

StringLiteral
  : '\'' (EscapeSequence | ~('\\'|'\''))* '\''
  ;

fragment
EscapeSequence
  : '\\' ('b'|'t'|'n'|'f'|'r'|'\"'|'\''|'\\')
  ;

/* Match anything between { and }. */
COMMENT
  : '{' .*? '}' -> channel(HIDDEN)
  ;

LINE_COMMENT
  : '|' ~[\r\n]* '\r'? '\n' -> channel(HIDDEN)
  ;

/* Toss out whitespaces and newlines. */
WS
  : [ \t\n]+ -> skip
  ;