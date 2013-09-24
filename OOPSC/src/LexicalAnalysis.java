import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileNotFoundException;
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
    private HashMap<String, Symbol.Id> keywords;

    /** Der Datenstrom, aus dem der Quelltext gelesen wird. */
    private InputStreamReader reader;

    /** Sollen die erkannten Symbole auf der Konsole ausgegeben werden? */
    private boolean printSymbols;

    /** Die aktuelle Position im Quelltext. */
    private Position position;

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
        position.next((char) c);
        c = reader.read();
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
        reader = new InputStreamReader(stream);
        this.printSymbols = printSymbols;

        keywords = new HashMap<String, Symbol.Id>();
        keywords.put("BEGIN", Symbol.Id.BEGIN);
        keywords.put("END", Symbol.Id.END);
        keywords.put("CLASS", Symbol.Id.CLASS);
        keywords.put("IS", Symbol.Id.IS);
        keywords.put("METHOD", Symbol.Id.METHOD);
        keywords.put("READ", Symbol.Id.READ);
        keywords.put("WRITE", Symbol.Id.WRITE);
        keywords.put("IF", Symbol.Id.IF);
        keywords.put("THEN", Symbol.Id.THEN);
        keywords.put("WHILE", Symbol.Id.WHILE);
        keywords.put("DO", Symbol.Id.DO);
        keywords.put("MOD", Symbol.Id.MOD);
        keywords.put("NEW", Symbol.Id.NEW);
        keywords.put("SELF", Symbol.Id.SELF);
        keywords.put("NULL", Symbol.Id.NULL);

        position = new Position(1, 0);
        nextChar();
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
            while (c != -1 && Character.isWhitespace((char) c)) {
                nextChar();
            }
            if (c == '{') { // Geklammerter Kommentar
                nextChar();
                while (c != -1 && c != '}') {
                    nextChar();
                }
                if (c == -1) {
                    throw new CompileException("Unerwartetes Dateiende im Kommentar", position);
                }
                nextChar();
            } else if (c == '|') { // Kommentar bis Zeilenende
                nextChar();
                while (c != -1 && c != '\n') {
                    nextChar();
                }
                nextChar();
            } else {
                break;
            }
        }

        switch (c) {
        case -1:
            symbol = new Symbol(Symbol.Id.EOF, position);;
            break;
        case ':':
            symbol = new Symbol(Symbol.Id.COLON, position);
            nextChar();
            if (c == '=') {
                symbol.id = Symbol.Id.BECOMES;
                nextChar();
            }
            break;
        case ';':
            symbol = new Symbol(Symbol.Id.SEMICOLON, position);
            nextChar();
            break;
        case ',':
            symbol = new Symbol(Symbol.Id.COMMA, position);
            nextChar();
            break;
        case '.':
            symbol = new Symbol(Symbol.Id.PERIOD, position);
            nextChar();
            break;
        case '(':
            symbol = new Symbol(Symbol.Id.LPAREN, position);
            nextChar();
            break;
        case ')':
            symbol = new Symbol(Symbol.Id.RPAREN, position);
            nextChar();
            break;
        case '=':
            symbol = new Symbol(Symbol.Id.EQ, position);
            nextChar();
            break;
        case '#':
            symbol = new Symbol(Symbol.Id.NEQ, position);
            nextChar();
            break;
        case '>':
            symbol = new Symbol(Symbol.Id.GT, position);
            nextChar();
            if (c == '=') {
                symbol.id = Symbol.Id.GTEQ;
                nextChar();
            }
            break;
        case '<':
            symbol = new Symbol(Symbol.Id.LT, position);
            nextChar();
            if (c == '=') {
                symbol.id = Symbol.Id.LTEQ;
                nextChar();
            }
            break;
        case '+':
            symbol = new Symbol(Symbol.Id.PLUS, position);
            nextChar();
            break;
        case '-':
            symbol = new Symbol(Symbol.Id.MINUS, position);
            nextChar();
            break;
        case '*':
            symbol = new Symbol(Symbol.Id.TIMES, position);
            nextChar();
            break;
        case '/':
            symbol = new Symbol(Symbol.Id.DIV, position);
            nextChar();
            break;
        default:
            if (Character.isDigit((char) c)) {
                symbol = new Symbol(Symbol.Id.NUMBER, position);
                symbol.number = c - '0';
                nextChar();
                while (c != -1 && Character.isDigit((char) c)) {
                    symbol.number = symbol.number * 10 + c - '0';
                    nextChar();
                }
            } else if (Character.isLetter((char) c)) {
                symbol = new Symbol(Symbol.Id.IDENT, position);
                String ident = "" + (char) c;
                nextChar();
                while (c != -1 && Character.isLetterOrDigit((char) c)) {
                    ident = ident + (char) c;
                    nextChar();
                }
                Symbol.Id id = keywords.get(ident);
                if (id != null) {
                    symbol.id = id;
                } else {
                    symbol.ident = ident;
                }
            } else if (c=='\'') {
                symbol = new Symbol(Symbol.Id.NUMBER, position);
            	int ch;
            	nextChar();
            	if (c=='\\') {
            	    nextChar();
            	    switch (c) {
            	    case 'n': ch='\n'; break;
            	    case '\\': ch='\\'; break;
            	    default: throw new CompileException("Zeichenliteral nicht erlaubt: "
            	    		+ "'\\" + (char) c + " (Code " + c + ")", position);
            	    }
            	} else if (c<' ' || c>'~')
        		    throw new CompileException("Unbekanntes Zeichen im Zeichenliteral (Code " + c + ").", position);
            	else
            		ch=c;
            	nextChar();

            	if (c!='\'') {
            		throw new CompileException("Zeichenliteral nicht abgeschlossen.", position);
            	}
            	nextChar();
            	symbol.number=ch;
            } else {
                throw new CompileException("Unerwartetes Zeichen: " + (char) c + " (Code " + c + ")", position);
            }
        }
        if (printSymbols) {
            System.out.println(symbol.toString());
        }
    }
}
