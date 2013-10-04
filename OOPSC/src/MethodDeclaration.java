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

	/** Return type. Default type matches ClassDeclaration.voidType. */
	ResolvableIdentifier retType = null;

	/**
	 * Konstruktor.
	 *
	 * @param name
	 *        Der Name der deklarierten Methode.
	 */
	MethodDeclaration(Identifier name) {
		super(name);

		this.retType = new ResolvableIdentifier("_Void", null);
		this.retType.declaration = ClassDeclaration.voidType;
	}

	/**
	 * Setzt die Parameter der Methode.
	 *
	 * @param params
	 *        Liste mit den Parametern.
	 */
	public void setParameters(List<VarDeclaration> params) {
		this.parameters = params;
	}

	/**
	 * Setzt den Rückgabetypen.
	 *
	 * @param retType
	 */
	public void setReturnType(ResolvableIdentifier retType) {
		this.retType = retType;
	}

	/**
	 * Return type.
	 */
	public ClassDeclaration getResolvedReturnType() {
		return (ClassDeclaration) this.retType.declaration;
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
	@Override
	void contextAnalysis(Declarations declarations, boolean initialPass)
			throws CompileException {
		/* Resolve return type. */
		declarations.resolveType(this.retType);

		if (declarations.currentClass.identifier.name.equals("Main")
				&& this.identifier.name.equals("main")) {
			if (this.parameters.size() != 0) {
				throw new CompileException(
						"Main.main() must not have any parameters.",
						this.identifier.position);
			} else if (this.getResolvedReturnType() != ClassDeclaration.voidType) {
				throw new CompileException(
						"Main.main() must not have a non-void return type.",
						this.identifier.position);
			}
		}

		// SELF ist Variable vom Typ dieser Klasse
		this.self.type = new ResolvableIdentifier(
				declarations.currentClass.identifier.name, null);
		this.self.type.declaration = declarations.currentClass;

		// Löse Typen aller Parameter auf
		for (VarDeclaration v : this.parameters) {
			v.contextAnalysis(declarations, initialPass);
		}

		// Löse Typen aller Variablen auf
		for (VarDeclaration v : this.locals) {
			v.contextAnalysis(declarations, initialPass);
		}

		// Neuen Deklarationsraum schaffen
		declarations.enter();

		// SELF eintragen
		declarations.add(this.self);

		// Parameter eintragen; liegen vor der Rücksprungadresse (-1) auf dem Stapel
		int offset = -2;
		for (int i = this.parameters.size() - 1; i >= 0; i--) {
			VarDeclaration v = this.parameters.get(i);
			declarations.add(v);
			v.offset = offset;
			offset--;
		}

		// SELF liegt vor den Parametern auf dem Stapel
		this.self.offset = offset;

		// Rücksprungadresse und alten Rahmenzeiger überspringen
		offset = 1;

		// Lokale Variablen eintragen
		for (VarDeclaration v : this.locals) {
			declarations.add(v);
			v.offset = offset++;
		}

		if (!initialPass) {
			if (this.getResolvedReturnType() != ClassDeclaration.voidType) {
				/* Determines whether at least one return statement is always executed. */
				if (!BranchEvaluator.terminates(this)) {
					throw new CompileException(
							"Method needs a return statement that is always reachable.",
							this.retType.position);
				}
			}

			// Kontextanalyse aller Anweisungen durchführen
			for (Statement s : this.statements) {
				s.contextAnalysis(declarations);
			}
		}

		// Alten Deklarationsraum wiederherstellen
		declarations.leave();
	}

	/**
	 * Die Methode gibt diese Deklaration in einer Baumstruktur aus.
	 *
	 * @param tree
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	@Override
	void print(TreeStream tree) {
		/* TODO Print parameters and return type. */
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

	protected void generateMethodPrologue(CodeStream code) {
		String ns = this.self.type.name + "_" + this.identifier.name;
		code.setNamespace(ns);

		code.println(ns + ":");
		code.println("ADD R2, R1");
		code.println("MMR (R2), R3 ; Save old stack frame in R3.");
		code.println("MRR R3, R2 ; Save current stack position in the new stack frame.");

		if (!this.locals.isEmpty()) {
			code.println("MRI R5, " + this.locals.size());
			code.println("ADD R2, R5 ; Allocate space for local variables.");
		}
	}

	/**
	 * @param customInstruction
	 *        Will be inserted after fixing up the stack.
	 */
	public void generateMethodEpilogue(CodeStream code, String customInstruction) {
		/* Make R2 point to the same address as before the method call.
		 * R2 -= this.locals.size() + 3 */
		/* TODO Is it necessary to add this.parameters.size()? */
		code.println("MRI R5, "
				+ (this.locals.size() + this.parameters.size() + 3)
				+ " ; Stack space.");
		code.println("SUB R2, R5 ; Fix up stack.");

		if (customInstruction.length() != 0) {
			code.println(customInstruction);
		}

		code.println("SUB R3, R1");
		code.println("MRM R5, (R3) ; Get old return address.");
		code.println("ADD R3, R1");
		code.println("MRM R3, (R3) ; Get old stack frame.");
		code.println("MRR R0, R5 ; Jump back.");
		code.println("");
	}

	/**
	 * Generiert den Assembler-Code für diese Methode. Dabei wird davon ausgegangen,
	 * dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
	 *
	 * @param code
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	void generateCode(CodeStream code) {
		code.println("; METHOD " + this.identifier.name);
		this.generateMethodPrologue(code);

		code.println("");
		code.println("; Statements");
		code.println("");

		for (Statement s : this.statements) {
			code.println("; Statement: " + s.getClass().getName());
			s.generateCode(code);
			code.println("");
		}

		code.println("; END METHOD " + this.identifier.name);
		this.generateMethodEpilogue(code, "");
	}
}
