package org.oopsc;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 * Die Klasse repräsentiert eine Methode im Syntaxbaum.
 */
public class MethodDeclaration extends Declaration {
	/** Die lokale Variable SELF. */
	public VarDeclaration self = new VarDeclaration(new Identifier("_self", null),
			VarDeclaration.Type.Local);

	/** Die lokale Variable BASE. */
	public VarDeclaration base = new VarDeclaration(new Identifier("_base", null),
			VarDeclaration.Type.Local);

	/** Die Parameter der Methode. */
	public List<VarDeclaration> parameters = new LinkedList<>();

	/** Die lokalen Variablen der Methode. */
	public List<VarDeclaration> locals = new LinkedList<>();

	/** Die Anweisungen der Methode, d.h. der Methodenrumpf. */
	public List<Statement> statements = new LinkedList<>();

	/** Return type. Default type matches ClassDeclaration.voidType. */
	public ResolvableIdentifier retType = null;

	public int vmtIndex = -1;

	/**
	 * Konstruktor.
	 *
	 * @param name
	 *        Der Name der deklarierten Methode.
	 */
	public MethodDeclaration(Identifier name) {
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
	public void contextAnalysis(Declarations declarations, boolean initialPass)
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

		// BASE represents the inherited class.
		this.base.type = new ResolvableIdentifier(
				declarations.currentClass.baseType.name, null);
		this.base.type.declaration = declarations.currentClass.baseType.declaration;

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

		// Insert SELF and BASE.
		declarations.add(this.self);
		declarations.add(this.base);

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

		// BASE has the same address on the stack as SELF, however the type of BASE
		// corresponds to the base type.
		this.base.offset = offset;

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
	public void print(TreeStream tree) {
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
		code.println("MMR (R2), R3 ; Save current stack frame in R2.");
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
		/* Calculate size of stack space occupied by this method and its call. */
		int size = this.locals.size() + 1 /* old stack frame */
				+ 1 /* return address */
				+ this.parameters.size();

		/* Make R2 point to the same address as before the method was called. */
		code.println("MRI R5, " + (size + 1));
		code.println("SUB R2, R5 ; Free the stack space.");

		if (customInstruction.length() != 0) {
			code.println(customInstruction);
		}

		/* Load the return address (R3 - 1) into R5, so that we can later jump to it. */
		code.println("SUB R3, R1");
		code.println("MRM R5, (R3) ; Get old return address.");
		code.println("ADD R3, R1");

		/* Make R3 point to the previous stack frame. */
		code.println("MRM R3, (R3)");

		/* Jump to the return address (R5). */
		code.println("MRR R0, R5 ; Jump back.");
		code.println("");
	}

	/**
	 * Generiert den Assembler-Code für diese Methode. Dabei wird davon ausgegangen,
	 * dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
	 *
	 * @param code
	 *        Der Strom, in den die Ausgabe erfolgt.
	 * @param contexts
	 *        Current stack of contexts, may be used to inject instructions for
	 *        unwinding the stack (as needed for RETURN statements in TRY blocks).
	 */
	public void generateCode(CodeStream code, Stack<Statement.Context> contexts) {
		code.println("; METHOD " + this.identifier.name);
		this.generateMethodPrologue(code);

		code.println("");
		code.println("; Statements");
		code.println("");

		for (Statement s : this.statements) {
			code.println("; Statement: " + s.getClass().getName());
			s.generateCode(code, contexts);
			code.println("");
		}

		code.println("; END METHOD " + this.identifier.name);

		if (this.getResolvedReturnType() == ClassDeclaration.voidType) {
			/* When at least one return statement is always executed, we do not need to generate the
			 * epilogue twice. */
			if (BranchEvaluator.terminates(this)) {
				return;
			}
		}

		this.generateMethodEpilogue(code, "");
	}

	/**
	 * Compares the signature for equality.
	 *
	 * @param m Comparison method.
	 * @return true if signatures are equal, false otherwise.
	 */
	public boolean signatureEquals(MethodDeclaration m) {
		if (!m.retType.name.equals(this.retType.name)) {
			return false;
		}

		if (m.parameters.size() != this.parameters.size()) {
			return false;
		}

		for (int i = 0; i < m.parameters.size(); i++) {
			String cmp = m.parameters.get(i).type.name;

			if (!this.parameters.get(i).type.name.equals(cmp)) {
				return false;
			}
		}

		return true;
	}
}