/**
 * Die Klasse repräsentiert ein Symbol, das von der lexikalischen
 * Analyses erkannt wurde.
 */
class Symbol extends Position {
    /** Alle definierten Symbole. */
    enum Id {
        IDENT, NUMBER,
        BEGIN, END,
        CLASS, IS, METHOD,
        READ, WRITE,
        IF, THEN,
        WHILE, DO,
        COLON, SEMICOLON, COMMA, PERIOD,
        LPAREN, RPAREN,
        EQ, NEQ, GT, GTEQ, LT, LTEQ,
        PLUS, MINUS, TIMES, DIV, MOD,
        BECOMES, NEW,
        SELF,
        NULL,
        TRUE, FALSE,
        NOT, AND, OR,
        ELSE, ELSEIF,
        EOF
    };

    /** Das Symbol. */
    Id id;

    /** Wenn das Symbol NUMBER ist, steht die gelesene Zahl in diesem Attribut. */
    int number;

    /** Wenn das Symbol IDENT ist, steht der gelesene Bezeichner in diesem Attribut. */
    String ident;

    /**
     * Konstruktor.
     * @param id Das erkannte Symbol.
     * @param position Die Quelltextstelle, an der das Symbol erkannt wurde.
     */
    Symbol(Id id, Position position) {
        super(position.line, position.column);
        this.id = id;
    }

    /**
     * Die Methode erzeugt aus diesem Objekt eine darstellbare Zeichenkette.
     * @return Die Zeichenkette.
     */
    @Override
	public String toString() {
        switch (this.id) {
        case IDENT:
            return "IDENT: " + this.ident;
        case NUMBER:
            return "NUMBER: " + this.number;
        default:
            return this.id.toString();
        }
    }
}
