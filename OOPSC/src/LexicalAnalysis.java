import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * Die Klasse führt die lexikalische Analyse durch. Es werden alle
 * Terminale der bei {@link SyntaxAnalysis SyntaxAnalysis} beschriebenen
 * Grammaktik erkannt. Bezeichner und Zahlen folgen dieser Syntax:
 * <pre>
 * identifier   ::= letter { letter | digit }
 *
 * number       ::= digit { digit }
 *
 * letter       ::= 'A' .. 'Z' | 'a' .. 'z'
 *
 * digit        ::= '0' .. '9'
 *
 * character    ::= ''' ( Sichtbares-US-ASCII-Zeichen-kein-backslash
 *                      |  '\' 'n' | '\' '\' ) '''
 * </pre>
 * Kommentare zwischen geschweiften Klammern ('{' ... '}') bzw. hinter
 * senkrechten Strichen ('|') werden ignoriert.
 */
class LexicalAnalysis {
    /** Die Menge aller Schlüsselworte mit ihren zugeordneten Symbolen. */
    private final HashMap<String, Symbol.Id> keywords;

    /** Der Datenstrom, aus dem der Quelltext gelesen wird. */
    private final InputStreamReader reader;

    /** Sollen die erkannten Symbole auf der Konsole ausgegeben werden? */
    private final boolean printSymbols;

    /** Die aktuelle Position im Quelltext. */
    private final Position position;

    /** Das zuletzt gelesene Zeichen. */
    private int c;

    /** Das zuletzt erkannte Symbol. */
    Symbol symbol;

    /**
     * Die Methode liest das nächste Zeichen aus dem Quelltext.
     * Dieses wird im Attribut {@link #c c} bereitgestellt.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    private void nextChar() throws IOException {
        this.position.next((char) this.c);
        this.c = this.reader.read();
    }

    /**
     * Konstruktor.
     * @param fileName Der Name des Quelltexts.
     * @param printSymbols Sollen die erkannten Symbole auf der Konsole
     *         ausgegeben werden?
     * @throws FileNotFoundException Der Quelltext wurde nicht gefunden.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    LexicalAnalysis(String fileName, boolean printSymbols)
            throws FileNotFoundException, IOException {
        FileInputStream stream = new FileInputStream(fileName);
        this.reader = new InputStreamReader(stream);
        this.printSymbols = printSymbols;

        this.keywords = new HashMap<String, Symbol.Id>();
        this.keywords.put("BEGIN", Symbol.Id.BEGIN);
        this.keywords.put("END", Symbol.Id.END);
        this.keywords.put("CLASS", Symbol.Id.CLASS);
        this.keywords.put("IS", Symbol.Id.IS);
        this.keywords.put("METHOD", Symbol.Id.METHOD);
        this.keywords.put("READ", Symbol.Id.READ);
        this.keywords.put("WRITE", Symbol.Id.WRITE);
        this.keywords.put("IF", Symbol.Id.IF);
        this.keywords.put("THEN", Symbol.Id.THEN);
        this.keywords.put("WHILE", Symbol.Id.WHILE);
        this.keywords.put("DO", Symbol.Id.DO);
        this.keywords.put("MOD", Symbol.Id.MOD);
        this.keywords.put("NEW", Symbol.Id.NEW);
        this.keywords.put("SELF", Symbol.Id.SELF);
        this.keywords.put("NULL", Symbol.Id.NULL);
        this.keywords.put("TRUE", Symbol.Id.TRUE);
        this.keywords.put("FALSE", Symbol.Id.FALSE);
        this.keywords.put("NOT", Symbol.Id.NOT);
        this.keywords.put("AND", Symbol.Id.AND);
        this.keywords.put("OR", Symbol.Id.OR);
        this.keywords.put("ELSE", Symbol.Id.ELSE);
        this.keywords.put("ELSEIF", Symbol.Id.ELSEIF);

        this.position = new Position(1, 0);
        this.nextChar();
    }

    /**
     * Die Methode liest das nächste Symbol. Dieses wird im Attribut
     * {@link #symbol symbol} bereitgestellt.
     * @throws CompileException Der Quelltext entspricht nicht der Syntax.
     * @throws IOException Ein Lesefehler ist aufgetreten.
     */
    void nextSymbol() throws CompileException, IOException {
        for(;;) {
            // Leerraum ignorieren
            while (this.c != -1 && Character.isWhitespace((char) this.c)) {
                this.nextChar();
            }
            if (this.c == '{') { // Geklammerter Kommentar
                this.nextChar();
                while (this.c != -1 && this.c != '}') {
                    this.nextChar();
                }
                if (this.c == -1) {
                    throw new CompileException("Unerwartetes Dateiende im Kommentar", this.position);
                }
                this.nextChar();
            } else if (this.c == '|') { // Kommentar bis Zeilenende
                this.nextChar();
                while (this.c != -1 && this.c != '\n') {
                    this.nextChar();
                }
                this.nextChar();
            } else {
                break;
            }
        }

        switch (this.c) {
        case -1:
            this.symbol = new Symbol(Symbol.Id.EOF, this.position);;
            break;
        case ':':
            this.symbol = new Symbol(Symbol.Id.COLON, this.position);
            this.nextChar();
            if (this.c == '=') {
                this.symbol.id = Symbol.Id.BECOMES;
                this.nextChar();
            }
            break;
        case ';':
            this.symbol = new Symbol(Symbol.Id.SEMICOLON, this.position);
            this.nextChar();
            break;
        case ',':
            this.symbol = new Symbol(Symbol.Id.COMMA, this.position);
            this.nextChar();
            break;
        case '.':
            this.symbol = new Symbol(Symbol.Id.PERIOD, this.position);
            this.nextChar();
            break;
        case '(':
            this.symbol = new Symbol(Symbol.Id.LPAREN, this.position);
            this.nextChar();
            break;
        case ')':
            this.symbol = new Symbol(Symbol.Id.RPAREN, this.position);
            this.nextChar();
            break;
        case '=':
            this.symbol = new Symbol(Symbol.Id.EQ, this.position);
            this.nextChar();
            break;
        case '#':
            this.symbol = new Symbol(Symbol.Id.NEQ, this.position);
            this.nextChar();
            break;
        case '>':
            this.symbol = new Symbol(Symbol.Id.GT, this.position);
            this.nextChar();
            if (this.c == '=') {
                this.symbol.id = Symbol.Id.GTEQ;
                this.nextChar();
            }
            break;
        case '<':
            this.symbol = new Symbol(Symbol.Id.LT, this.position);
            this.nextChar();
            if (this.c == '=') {
                this.symbol.id = Symbol.Id.LTEQ;
                this.nextChar();
            }
            break;
        case '+':
            this.symbol = new Symbol(Symbol.Id.PLUS, this.position);
            this.nextChar();
            break;
        case '-':
            this.symbol = new Symbol(Symbol.Id.MINUS, this.position);
            this.nextChar();
            break;
        case '*':
            this.symbol = new Symbol(Symbol.Id.TIMES, this.position);
            this.nextChar();
            break;
        case '/':
            this.symbol = new Symbol(Symbol.Id.DIV, this.position);
            this.nextChar();
            break;
        default:
            if (Character.isDigit((char) this.c)) {
                this.symbol = new Symbol(Symbol.Id.NUMBER, this.position);
                this.symbol.number = this.c - '0';
                this.nextChar();
                while (this.c != -1 && Character.isDigit((char) this.c)) {
                    this.symbol.number = this.symbol.number * 10 + this.c - '0';
                    this.nextChar();
                }
            } else if (Character.isLetter((char) this.c)) {
                this.symbol = new Symbol(Symbol.Id.IDENT, this.position);
                String ident = "" + (char) this.c;
                this.nextChar();
                while (this.c != -1 && Character.isLetterOrDigit((char) this.c)) {
                    ident = ident + (char) this.c;
                    this.nextChar();
                }
                Symbol.Id id = this.keywords.get(ident);
                if (id != null) {
                    this.symbol.id = id;
                } else {
                    this.symbol.ident = ident;
                }
            } else if (this.c=='\'') {
                this.symbol = new Symbol(Symbol.Id.NUMBER, this.position);
            	int ch;
            	this.nextChar();
            	if (this.c=='\\') {
            	    this.nextChar();
            	    switch (this.c) {
            	    case 'n': ch='\n'; break;
            	    case '\\': ch='\\'; break;
            	    default: throw new CompileException("Zeichenliteral nicht erlaubt: "
            	    		+ "'\\" + (char) this.c + " (Code " + this.c + ")", this.position);
            	    }
            	} else if (this.c<' ' || this.c>'~') {
					throw new CompileException("Unbekanntes Zeichen im Zeichenliteral (Code " + this.c + ").", this.position);
				} else {
					ch=this.c;
				}
            	this.nextChar();

            	if (this.c!='\'') {
            		throw new CompileException("Zeichenliteral nicht abgeschlossen.", this.position);
            	}
            	this.nextChar();
            	this.symbol.number=ch;
            } else {
                throw new CompileException("Unerwartetes Zeichen: " + (char) this.c + " (Code " + this.c + ")", this.position);
            }
        }
        if (this.printSymbols) {
            System.out.println(this.symbol.toString());
        }
    }
}
