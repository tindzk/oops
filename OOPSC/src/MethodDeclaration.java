import java.util.LinkedList;
import java.util.List;

/**
 * Die Klasse repräsentiert eine Methode im Syntaxbaum.
 */
class MethodDeclaration extends Declaration {
	/** Die lokale Variable SELF. */
	VarDeclaration self = new VarDeclaration(new Identifier("_self", null),
			false);

	/** Die Parameter der Methode. */
	List<VarDeclaration> parameters = new LinkedList<>();

	/** Die lokalen Variablen der Methode. */
	List<VarDeclaration> locals = new LinkedList<>();

	/** Die Anweisungen der Methode, d.h. der Methodenrumpf. */
	List<Statement> statements = new LinkedList<>();

	/**
	 * Konstruktor.
	 *
	 * @param name
	 *        Der Name der deklarierten Methode.
	 */
	MethodDeclaration(Identifier name) {
		super(name);
	}

	/**
	 * Setzt die Parameter der Methode.
	 *
	 * @param params Liste mit den Parametern.
	 */
	public void setParameters(List<VarDeclaration> params) {
		this.parameters = params;
	}

	/**
	 * Führt die Kontextanalyse für diese Methoden-Deklaration durch.
	 *
	 * @param declarations
	 *        Die an dieser Stelle gültigen Deklarationen.
	 * @throws CompileException
	 *         Während der Kontextanylyse wurde ein Fehler
	 *         gefunden.
	 */
	void contextAnalysis(Declarations declarations, boolean initialPass) throws CompileException {
		// SELF ist Variable vom Typ dieser Klasse
		this.self.type = new ResolvableIdentifier(
				declarations.currentClass.identifier.name, null);
		this.self.type.declaration = declarations.currentClass;

		// Löse Typen aller Parameter auf
		for (VarDeclaration v : this.parameters) {
			v.contextAnalysis(declarations);
		}

		// Löse Typen aller Variablen auf
		for (VarDeclaration v : this.locals) {
			v.contextAnalysis(declarations);
		}

		// Neuen Deklarationsraum schaffen
		declarations.enter();

		// SELF eintragen
		declarations.add(this.self);

		// SELF liegt vor der Rücksprungadresse auf dem Stapel
		this.self.offset = -2;

		// Rücksprungadresse und alten Rahmenzeiger überspringen
		int offset = 1;

		// Lokale Variablen eintragen
		for (VarDeclaration v : this.locals) {
			declarations.add(v);
			v.offset = offset++;
		}

		if (!initialPass) {
			// Kontextanalyse aller Anweisungen durchführen
			for (Statement s : this.statements) {
				s.contextAnalysis(declarations);
			}
		}

		// Alten Deklarationsraum wiederherstellen
		declarations.leave();
	}

	/**
	 * @note Methode sollte nicht aufgerufen werden.
	 */
	@Override
	void contextAnalysis(Declarations declarations) throws CompileException {
		throw new CompileException(null, null);
	}

	/**
	 * Die Methode gibt diese Deklaration in einer Baumstruktur aus.
	 *
	 * @param tree
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	@Override
	void print(TreeStream tree) {
		tree.println("METHOD " + this.identifier.name);
		tree.indent();

		if (!this.locals.isEmpty()) {
			tree.println("VARIABLES");
			tree.indent();

			for (VarDeclaration v : this.locals) {
				v.print(tree);
			}

			tree.unindent();
		}

		if (!this.statements.isEmpty()) {
			tree.println("BEGIN");
			tree.indent();

			for (Statement s : this.statements) {
				s.print(tree);
			}

			tree.unindent();
		}

		tree.unindent();
	}

	/**
	 * Generiert den Assembler-Code für diese Methode. Dabei wird davon ausgegangen,
	 * dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
	 *
	 * @param code
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	void generateCode(CodeStream code) {
		code.setNamespace(this.self.type.name + "_" + this.identifier.name);
		code.println("; METHOD " + this.identifier.name);
		code.println(this.self.type.name + "_" + this.identifier.name + ":");
		code.println("ADD R2, R1");
		code.println("MMR (R2), R3 ; Alten Stapelrahmen sichern");
		code.println("MRR R3, R2 ; Aktuelle Stapelposition ist neuer Rahmen");

		if (!this.locals.isEmpty()) {
			code.println("MRI R5, " + this.locals.size());
			code.println("ADD R2, R5 ; Platz für lokale Variablen schaffen");
		}
		for (Statement s : this.statements) {
			s.generateCode(code);
		}

		code.println("; END METHOD " + this.identifier.name);
		code.println("MRI R5, " + (this.locals.size() + 3));
		code.println("SUB R2, R5 ; Stack korrigieren");
		code.println("SUB R3, R1");
		code.println("MRM R5, (R3) ; Rücksprungadresse holen");
		code.println("ADD R3, R1");
		code.println("MRM R3, (R3) ; Alten Stapelrahmen holen");
		code.println("MRR R0, R5 ; Rücksprung");
		code.println("");
	}
}
