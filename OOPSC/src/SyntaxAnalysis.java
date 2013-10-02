import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Die Klasse realisiert die syntaktische Analyse für die folgende Grammatik.
 * Terminale stehen dabei in Hochkommata oder sind groß geschrieben:
 * <pre>
 * program      ::= { classdecl }
 *
 * classdecl    ::= CLASS identifier IS
 *                  { memberdecl }
 *                  END CLASS
 *
 * memberdecl   ::= vardecl ';'
 *                | METHOD identifier ['(' vardecl { ';' vardecl } ')']
 *                  IS methodbody
 *
 * vardecl      ::= identifier { ',' identifier } ':' identifier
 *
 * methodbody   ::= { vardecl ';' }
 *                  BEGIN statements
 *                  END METHOD
 *
 * statements   ::= { statement }
 *
 * statement    ::= READ memberaccess ';'
 *                | WRITE expression ';'
 *                | IF relation
 *                  THEN statements
 *                  { ELSEIF relation THEN statements }
 *                  [ ELSE statements ]
 *                  END IF
 *                | WHILE relation
 *                  DO statements
 *                  END WHILE
 *                | memberaccess [ ':=' relation ] ';'
 *
 * relation     ::= expression [ ( 'AND' | 'OR' | '=' | '#' | '<' | '>' | '<=' | '>=' ) expression ]
 *                | NOT (relation)
 *
 * expression   ::= term { ( '+' | '-' ) term }
 *
 * term         ::= factor { ( '*' | '/' | MOD ) factor }
 *
 * factor       ::= '-' factor
 *                | memberaccess
 *
 * memberaccess ::= literal { '.' varorcall }
 *
 * literal    ::= number
 *                | character
 *                | NULL
 *                | TRUE
 *                | FALSE
 *                | SELF
 *                | NEW identifier
 *                | '(' relation ')'
 *                | varorcall
 *
 * varorcall    ::= identifier ['(' relation { ',' relation } ')']
 * </pre>
 * Daraus wird der Syntaxbaum aufgebaut, dessen Wurzel die Klasse
 * {@link Program Program} ist.
 */
class SyntaxAnalysis extends LexicalAnalysis {
	/**
	 * Die Methode erzeugt einen "Unerwartetes Symbol"-Fehler.
	 *
	 * @throws CompileException
	 *         Die entsprechende Fehlermeldung.
	 */
	private void unexpectedSymbol(Symbol.Id id) throws CompileException {
		if (id != null) {
			throw new CompileException("Unerwartetes Symbol "
					+ this.symbol.id.toString() + " (statt " + id.toString()
					+ ")", this.symbol);
		}

		throw new CompileException("Unerwartetes Symbol "
				+ this.symbol.id.toString(), this.symbol);
	}

	/**
	 * Die Methode überprüft, ob das aktuelle Symbol das erwartete ist. Ist dem so,
	 * wird das nächste Symbol gelesen, ansonsten wird eine Fehlermeldung erzeugt.
	 *
	 * @param id
	 *        Das erwartete Symbol.
	 * @throws CompileException
	 *         Ein unerwartetes Symbol wurde gelesen.
	 * @throws IOException
	 *         Ein Lesefehler ist aufgetreten.
	 */
	private void expectSymbol(Symbol.Id id) throws CompileException,
			IOException {
		if (id != this.symbol.id) {
			this.unexpectedSymbol(id);
		}
		this.nextSymbol();
	}

	/**
	 * Die Methode überprüft, ob das aktuelle Symbol ein Bezeichner ist. Ist dem so,
	 * wird er zurückgeliefert, ansonsten wird eine Fehlermeldung erzeugt.
	 *
	 * @throws CompileException
	 *         Ein unerwartetes Symbol wurde gelesen.
	 * @throws IOException
	 *         Ein Lesefehler ist aufgetreten.
	 */
	private Identifier expectIdent() throws CompileException, IOException {
		if (this.symbol.id != Symbol.Id.IDENT) {
			this.unexpectedSymbol(Symbol.Id.IDENT);
		}
		Identifier i = new Identifier(this.symbol.ident, new Position(
				this.symbol.line, this.symbol.column));
		this.nextSymbol();
		return i;
	}

	/**
	 * Die Methode überprüft, ob das aktuelle Symbol ein Bezeichner ist. Ist dem so,
	 * wird er in Form eines Bezeichners mit noch aufzulösender Vereinbarung
	 * zurückgeliefert, ansonsten wird eine Fehlermeldung erzeugt.
	 *
	 * @throws CompileException
	 *         Ein unerwartetes Symbol wurde gelesen.
	 * @throws IOException
	 *         Ein Lesefehler ist aufgetreten.
	 */
	private ResolvableIdentifier expectResolvableIdent()
			throws CompileException, IOException {
		if (this.symbol.id != Symbol.Id.IDENT) {
			this.unexpectedSymbol(Symbol.Id.IDENT);
		}
		ResolvableIdentifier r = new ResolvableIdentifier(this.symbol.ident,
				new Position(this.symbol.line, this.symbol.column));
		this.nextSymbol();
		return r;
	}

	/**
	 * Die Methode parsiert eine Klassendeklaration entsprechend der oben angegebenen
	 * Syntax und liefert diese zurück.
	 *
	 * @return Die Klassendeklaration.
	 * @throws CompileException
	 *         Der Quelltext entspricht nicht der Syntax.
	 * @throws IOException
	 *         Ein Lesefehler ist aufgetreten.
	 */
	private ClassDeclaration classdecl() throws CompileException, IOException {
		this.expectSymbol(Symbol.Id.CLASS);
		ClassDeclaration c = new ClassDeclaration(this.expectIdent());
		this.expectSymbol(Symbol.Id.IS);
		while (this.symbol.id != Symbol.Id.END) {
			this.memberdecl(c.attributes, c.methods);
		}
		this.nextSymbol();
		this.expectSymbol(Symbol.Id.CLASS);
		return c;
	}

	/**
	 * Die Methode parsiert die Deklaration eines Attributs bzw. einer Methode
	 * entsprechend der oben angegebenen Syntax und hängt sie an eine von
	 * zwei Listen an.
	 *
	 * @param attributes
	 *        Die Liste der Attributdeklarationen der aktuellen Klasse.
	 * @param methods
	 *        Die Liste der Methodendeklarationen der aktuellen Klasse.
	 * @throws CompileException
	 *         Der Quelltext entspricht nicht der Syntax.
	 * @throws IOException
	 *         Ein Lesefehler ist aufgetreten.
	 */
	private void memberdecl(List<VarDeclaration> attributes,
			List<MethodDeclaration> methods) throws CompileException,
			IOException {
		if (this.symbol.id == Symbol.Id.METHOD) {
			this.nextSymbol();
			MethodDeclaration m = new MethodDeclaration(this.expectIdent());

			if (this.symbol.id == Symbol.Id.LPAREN) {
				List<VarDeclaration> vars = new LinkedList<>();

				do {
					this.nextSymbol();
					this.vardecl(vars, false);
				} while (this.symbol.id == Symbol.Id.SEMICOLON);

				this.expectSymbol(Symbol.Id.RPAREN);

				m.setParameters(vars);
			}

			this.expectSymbol(Symbol.Id.IS);
			this.methodbody(m.vars, m.statements);
			methods.add(m);
		} else {
			this.vardecl(attributes, true);
			this.expectSymbol(Symbol.Id.SEMICOLON);
		}
	}

	/**
	 * Die Methode parsiert die Deklaration eines Attributs bzw. einer Variablen
	 * entsprechend der oben angegebenen Syntax und hängt sie an eine Liste an.
	 *
	 * @param vars
	 *        Die Liste der Attributdeklarationen der aktuellen Klasse oder
	 *        der Variablen der aktuellen Methode.
	 * @param isAttribute
	 *        Ist die Variable ein Attribut?.
	 * @throws CompileException
	 *         Der Quelltext entspricht nicht der Syntax.
	 * @throws IOException
	 *         Ein Lesefehler ist aufgetreten.
	 */
	private void vardecl(List<VarDeclaration> vars, boolean isAttribute)
			throws CompileException, IOException {
		List<VarDeclaration> temp = new LinkedList<>();
		temp.add(new VarDeclaration(this.expectIdent(), isAttribute));
		while (this.symbol.id == Symbol.Id.COMMA) {
			this.nextSymbol();
			temp.add(new VarDeclaration(this.expectIdent(), isAttribute));
		}
		this.expectSymbol(Symbol.Id.COLON);
		ResolvableIdentifier ident = this.expectResolvableIdent();
		for (VarDeclaration v : temp) {
			v.type = ident;
			vars.add(v);
		}
	}

	/**
	 * Die Methode parsiert die Deklaration eines Methodenrumpfes entsprechend der
	 * oben angegebenen Syntax. Lokale Variablendeklarationen und Anweisungen werden
	 * an die entsprechenden Listen angehängt.
	 *
	 * @param vars
	 *        Die Liste der lokalen Variablendeklarationen der aktuellen Methode.
	 * @param statements
	 *        Die Liste der Anweisungen der aktuellen Methode.
	 * @throws CompileException
	 *         Der Quelltext entspricht nicht der Syntax.
	 * @throws IOException
	 *         Ein Lesefehler ist aufgetreten.
	 */
	private void methodbody(List<VarDeclaration> vars,
			List<Statement> statements) throws CompileException,
			IOException {
		while (this.symbol.id != Symbol.Id.BEGIN) {
			this.vardecl(vars, false);
			this.expectSymbol(Symbol.Id.SEMICOLON);
		}
		this.nextSymbol();
		this.statements(statements);
		this.expectSymbol(Symbol.Id.END);
		this.expectSymbol(Symbol.Id.METHOD);
	}

	/**
	 * Die Methode parsiert eine Folge von Anweisungen entsprechend der
	 * oben angegebenen Syntax und hängt sie an eine Liste an.
	 *
	 * @param statements
	 *        Die Liste der Anweisungen.
	 * @throws CompileException
	 *         Der Quelltext entspricht nicht der Syntax.
	 * @throws IOException
	 *         Ein Lesefehler ist aufgetreten.
	 */
	private void statements(List<Statement> statements)
			throws CompileException, IOException {
		while (this.symbol.id != Symbol.Id.END
				&& this.symbol.id != Symbol.Id.ELSEIF
				&& this.symbol.id != Symbol.Id.ELSE) {
			this.statement(statements);
		}
	}

	/**
	 * Die Methode parsiert eine Anweisung entsprechend der oben angegebenen
	 * Syntax und hängt sie an eine Liste an.
	 *
	 * @param statements
	 *        Die Liste der Anweisungen.
	 * @throws CompileException
	 *         Der Quelltext entspricht nicht der Syntax.
	 * @throws IOException
	 *         Ein Lesefehler ist aufgetreten.
	 */
	private void statement(List<Statement> statements)
			throws CompileException, IOException {
		switch (this.symbol.id) {
			case READ:
				this.nextSymbol();
				statements.add(new ReadStatement(this.memberAccess()));
				this.expectSymbol(Symbol.Id.SEMICOLON);
				break;
			case WRITE:
				this.nextSymbol();
				statements.add(new WriteStatement(this.expression()));
				this.expectSymbol(Symbol.Id.SEMICOLON);
				break;
			case IF:
				this.nextSymbol();
				IfStatement s = new IfStatement(this.relation());
				statements.add(s);
				this.expectSymbol(Symbol.Id.THEN);
				this.statements(s.thenStatements);

				while (this.symbol.id == Symbol.Id.ELSEIF) {
					this.nextSymbol();

					List<Statement> stmts = new LinkedList<>();
					s.addIfElse(this.relation(), stmts);

					this.expectSymbol(Symbol.Id.THEN);
					this.statements(stmts);
				}

				if (this.symbol.id == Symbol.Id.ELSE) {
					this.nextSymbol();

					List<Statement> stmts = new LinkedList<>();
					s.setElse(stmts);

					this.statements(stmts);
				}

				this.expectSymbol(Symbol.Id.END);
				this.expectSymbol(Symbol.Id.IF);
				break;
			case WHILE:
				this.nextSymbol();
				WhileStatement w = new WhileStatement(this.relation());
				statements.add(w);
				this.expectSymbol(Symbol.Id.DO);
				this.statements(w.statements);
				this.expectSymbol(Symbol.Id.END);
				this.expectSymbol(Symbol.Id.WHILE);
				break;
			default:
				Expression e = this.memberAccess();
				if (this.symbol.id == Symbol.Id.BECOMES) {
					this.nextSymbol();
					statements.add(new Assignment(e, this.relation()));
				} else {
					statements.add(new CallStatement(e));
				}
				this.expectSymbol(Symbol.Id.SEMICOLON);
		}
	}

	/**
	 * Die Methode parsiert eine Relation entsprechend der oben angegebenen
	 * Syntax und liefert den Ausdruck zurück.
	 *
	 * @return Der Ausdruck.
	 * @throws CompileException
	 *         Der Quelltext entspricht nicht der Syntax.
	 * @throws IOException
	 *         Ein Lesefehler ist aufgetreten.
	 */
	private Expression relation() throws CompileException, IOException {
		Expression e = null;

		if (this.symbol.id == Symbol.Id.NOT) {
			this.nextSymbol();
			this.expectSymbol(Symbol.Id.LPAREN);

			Position position = new Position(this.symbol.line,
					this.symbol.column);
			e = new UnaryExpression(Symbol.Id.NOT, this.relation(), position);

			this.expectSymbol(Symbol.Id.RPAREN);
		} else {
			e = this.expression();

			switch (this.symbol.id) {
				case EQ:
				case NEQ:
				case GT:
				case GTEQ:
				case LT:
				case LTEQ:
					Symbol.Id operator = this.symbol.id;
					this.nextSymbol();
					e = new BinaryExpression(e, operator, this.expression());
			}
		}

		/* TODO Take into account operator precedence. */
		if (this.symbol.id == Symbol.Id.AND || this.symbol.id == Symbol.Id.OR) {
			Symbol.Id op = this.symbol.id;
			this.nextSymbol();
			return new BinaryExpression(e, op, this.relation());
		}

		return e;
	}

	/**
	 * Die Methode parsiert einen Ausdruck entsprechend der oben angegebenen
	 * Syntax und liefert ihn zurück.
	 *
	 * @return Der Ausdruck.
	 * @throws CompileException
	 *         Der Quelltext entspricht nicht der Syntax.
	 * @throws IOException
	 *         Ein Lesefehler ist aufgetreten.
	 */
	private Expression expression() throws CompileException, IOException {
		Expression e = this.term();
		while (this.symbol.id == Symbol.Id.PLUS
				|| this.symbol.id == Symbol.Id.MINUS) {
			Symbol.Id operator = this.symbol.id;
			this.nextSymbol();
			e = new BinaryExpression(e, operator, this.term());
		}
		return e;
	}

	/**
	 * Die Methode parsiert einen Term entsprechend der oben angegebenen
	 * Syntax und liefert den Ausdruck zurück.
	 *
	 * @return Der Ausdruck.
	 * @throws CompileException
	 *         Der Quelltext entspricht nicht der Syntax.
	 * @throws IOException
	 *         Ein Lesefehler ist aufgetreten.
	 */
	private Expression term() throws CompileException, IOException {
		Expression e = this.factor();
		while (this.symbol.id == Symbol.Id.TIMES
				|| this.symbol.id == Symbol.Id.DIV
				|| this.symbol.id == Symbol.Id.MOD) {
			Symbol.Id operator = this.symbol.id;
			this.nextSymbol();
			e = new BinaryExpression(e, operator, this.factor());
		}
		return e;
	}

	/**
	 * Die Methode parsiert einen Faktor entsprechend der oben angegebenen
	 * Syntax und liefert den Ausdruck zurück.
	 *
	 * @return Der Ausdruck.
	 * @throws CompileException
	 *         Der Quelltext entspricht nicht der Syntax.
	 * @throws IOException
	 *         Ein Lesefehler ist aufgetreten.
	 */
	private Expression factor() throws CompileException, IOException {
		switch (this.symbol.id) {
			case MINUS:
				Symbol.Id operator = this.symbol.id;
				Position position = new Position(this.symbol.line,
						this.symbol.column);
				this.nextSymbol();
				return new UnaryExpression(operator, this.factor(), position);
			default:
				return this.memberAccess();
		}
	}

	/**
	 * Die Methode parsiert den Zugriff auf ein Objektattribut bzw. eine
	 * Objektmethode entsprechend der oben angegebenen Syntax und liefert
	 * den Ausdruck zurück.
	 *
	 * @return Der Ausdruck.
	 * @throws CompileException
	 *         Der Quelltext entspricht nicht der Syntax.
	 * @throws IOException
	 *         Ein Lesefehler ist aufgetreten.
	 */
	private Expression memberAccess() throws CompileException, IOException {
		Expression e = this.literal();
		while (this.symbol.id == Symbol.Id.PERIOD) {
			this.nextSymbol();
			e = new AccessExpression(e, new VarOrCall(
					this.expectResolvableIdent()));
		}
		return e;
	}

	/**
	 * Die Methode parsiert ein Literal, die Erzeugung eines Objekts, einen
	 * geklammerten Ausdruck oder einen einzelnen Zugriff auf eine Variable,
	 * ein Attribut oder eine Methode entsprechend der oben angegebenen
	 * Syntax und liefert den Ausdruck zurück.
	 *
	 * @return Der Ausdruck.
	 * @throws CompileException
	 *         Der Quelltext entspricht nicht der Syntax.
	 * @throws IOException
	 *         Ein Lesefehler ist aufgetreten.
	 */
	private Expression literal() throws CompileException, IOException {
		Expression e = null;
		switch (this.symbol.id) {
			case NUMBER:
				e = new LiteralExpression(this.symbol.number,
						ClassDeclaration.intType, new Position(
								this.symbol.line, this.symbol.column));
				this.nextSymbol();
				break;
			case TRUE:
				e = new LiteralExpression(1, ClassDeclaration.boolType,
						new Position(this.symbol.line, this.symbol.column));
				this.nextSymbol();
				break;
			case FALSE:
				e = new LiteralExpression(0, ClassDeclaration.boolType,
						new Position(this.symbol.line, this.symbol.column));
				this.nextSymbol();
				break;
			case NULL:
				e = new LiteralExpression(0, ClassDeclaration.nullType,
						new Position(this.symbol.line, this.symbol.column));
				this.nextSymbol();
				break;
			case SELF:
				e = new VarOrCall(new ResolvableIdentifier("_self",
						new Position(this.symbol.line, this.symbol.column)));
				this.nextSymbol();
				break;
			case NEW:
				Position position = new Position(this.symbol.line,
						this.symbol.column);
				this.nextSymbol();
				e = new NewExpression(this.expectResolvableIdent(), position);
				break;
			case LPAREN:
				this.nextSymbol();
				e = this.relation();
				this.expectSymbol(Symbol.Id.RPAREN);
				break;
			case IDENT:
				e = this.varOrCall();
				break;
			default:
				this.unexpectedSymbol(null);
		}
		return e;
	}

	/**
	 * Parsiere Variablenzugriff oder Methodenaufruf.
	 *
	 * @return Der Ausdruck.
	 * @throws CompileException
	 *         Der Quelltext entspricht nicht der Syntax.
	 * @throws IOException
	 *         Ein Lesefehler ist aufgetreten.
	 */
	private Expression varOrCall() throws CompileException, IOException {
		VarOrCall e = new VarOrCall(this.expectResolvableIdent());

		if (this.symbol.id == Symbol.Id.LPAREN) {
			do {
				this.nextSymbol();
				e.addParameter(this.relation());
			} while (this.symbol.id == Symbol.Id.COMMA);

			this.expectSymbol(Symbol.Id.RPAREN);
		}

		return e;
	}

	/**
	 * Konstruktor.
	 *
	 * @param fileName
	 *        Der Name des Quelltexts.
	 * @param printSymbols
	 *        Die lexikalische Analyse gibt die erkannten
	 *        Symbole auf der Konsole aus.
	 * @throws CompileException
	 *         Der Quelltext entspricht nicht der Syntax.
	 * @throws FileNotFoundException
	 *         Der Quelltext wurde nicht gefunden.
	 * @throws IOException
	 *         Ein Lesefehler ist aufgetreten.
	 */
	SyntaxAnalysis(String fileName, boolean printSymbols)
			throws CompileException, FileNotFoundException, IOException {
		super(fileName, printSymbols);
		ResolvableIdentifier.init();
	}

	/**
	 * Die Methode parsiert den Quelltext und liefert die Wurzel des
	 * Syntaxbaums zurück.
	 *
	 * @throws CompileException
	 *         Der Quelltext entspricht nicht der Syntax.
	 * @throws IOException
	 *         Ein Lesefehler ist aufgetreten.
	 */
	Program parse() throws CompileException, IOException {
		this.nextSymbol();
		Program p = new Program();

		while (this.symbol.id != Symbol.Id.EOF) {
			p.addClass(this.classdecl());
		}

		return p;
	}
}
