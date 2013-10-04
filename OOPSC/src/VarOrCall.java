import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Die Klasse repräsentiert einen Ausdruck im Syntaxbaum, der dem Zugriff auf eine
 * Variable oder ein Attribut bzw. einem Methodenaufruf entspricht.
 */
class VarOrCall extends Expression {
	/** Der Name des Attributs, der Variablen oder der Methode. */
	ResolvableIdentifier identifier;

	List<Expression> arguments = new LinkedList<>();

	/**
	 * Konstruktor.
	 *
	 * @param identifier
	 *        Der Name des Attributs, der Variablen oder der Methode.
	 */
	VarOrCall(ResolvableIdentifier identifier) {
		super(identifier.position);
		this.identifier = identifier;
	}

	public void addArgument(Expression e) {
		this.arguments.add(e);
	}

	/**
	 * Die Methode führt die Kontextanalyse für diesen Ausdruck durch.
	 * Dabei wird ein Zugriff über SELF in den Syntaxbaum eingefügt,
	 * wenn dieser Ausdruck ein Attribut oder eine Methode bezeichnet.
	 * Diese Methode wird niemals für Ausdrücke aufgerufen, die rechts
	 * vom Objekt-Zugriffsoperator stehen.
	 *
	 * @param declarations
	 *        Die an dieser Stelle gültigen Deklarationen.
	 * @return Dieser Ausdruck oder ein neuer Ausdruck, falls ein Boxing
	 *         oder der Zugriff über SELF eingefügt wurde.
	 * @throws CompileException
	 *         Während der Kontextanalyse wurde ein Fehler
	 *         gefunden.
	 */
	@Override
	Expression contextAnalysis(Declarations declarations)
			throws CompileException {
		// TODO refactor
		this.contextAnalysisForMember(declarations);
		this.contextAnalysisForArguments(declarations);

		if (this.identifier.declaration instanceof MethodDeclaration
				|| this.identifier.declaration instanceof VarDeclaration
				&& ((VarDeclaration) this.identifier.declaration).isAttribute) {
			AccessExpression a = new AccessExpression(new VarOrCall(
					new ResolvableIdentifier("_self", this.position)), this);
			a.leftOperand = a.leftOperand.contextAnalysis(declarations);
			a.leftOperand = a.leftOperand.box(declarations);
			a.type = this.type;
			a.lValue = this.lValue;
			return a;
		} else {
			return this;
		}
	}

	/**
	 * Die Methode führt die Kontextanalyse für diesen Ausdruck durch.
	 * Diese Methode wird auch für Ausdrücke aufgerufen, die rechts
	 * vom Objekt-Zugriffsoperator stehen.
	 *
	 * @param declarations
	 *        Die an dieser Stelle gültigen Deklarationen.
	 * @throws CompileException
	 *         Während der Kontextanylyse wurde ein Fehler
	 *         gefunden.
	 */
	void contextAnalysisForMember(Declarations declarations)
			throws CompileException {
		// TODO refactor
		declarations.resolveVarOrMethod(this.identifier);

		if (this.identifier.declaration instanceof VarDeclaration) {
			VarDeclaration var = (VarDeclaration) this.identifier.declaration;
			this.type = var.getResolvedType();
			this.lValue = true;
		} else if (this.identifier.declaration instanceof MethodDeclaration) {
			MethodDeclaration method = (MethodDeclaration) this.identifier.declaration;
			this.type = method.getResolvedReturnType();
		} else {
			assert false;
		}
	}

	/**
	 * Contextual analysis for arguments.
	 *
	 * @param declarations
	 * @throws CompileException
	 */
	void contextAnalysisForArguments(Declarations declarations)
			throws CompileException {
		if (!(this.identifier.declaration instanceof MethodDeclaration)) {
			if (this.arguments.size() != 0) {
				throw new CompileException(
						"Arguments cannot be passed to a variable.",
						this.identifier.position);
			}
		} else {
			/* Verify that the passed arguments match the expected parameters. */
			MethodDeclaration decl = (MethodDeclaration) this.identifier.declaration;

			if (this.arguments.size() != decl.parameters.size()) {
				throw new CompileException(String.format(
						"Parameter count mismatch: %d expected, %d given.",
						decl.parameters.size(), this.arguments.size()),
						this.identifier.position);
			}

			List<Expression> boxed = new LinkedList<>();

			Iterator<Expression> args = this.arguments.iterator();
			Iterator<VarDeclaration> params = decl.parameters.iterator();

			for (int num = 1; args.hasNext(); num++) {
				Expression arg = args.next();
				VarDeclaration param = params.next();

				arg = arg.contextAnalysis(declarations);

				/* Parameters expect boxed values, i.e., Integer instead of _Integer. */
				arg = arg.box(declarations);
				boxed.add(arg);

				assert (param.type.declaration instanceof ClassDeclaration);

				if (!arg.type.isA((ClassDeclaration) param.type.declaration)) {
					throw new CompileException(String.format(
							"Argument %d mismatches: %s expected, %s given.",
							num, param.type.declaration.identifier.name,
							arg.type.identifier.name), this.identifier.position);
				}
			}

			this.arguments = boxed;
		}
	}

	/**
	 * Die Methode gibt diesen Ausdruck in einer Baumstruktur aus.
	 * Wenn der Typ des Ausdrucks bereits ermittelt wurde, wird er auch ausgegeben.
	 *
	 * @param tree
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	@Override
	void print(TreeStream tree) {
		tree.println(this.identifier.name
				+ (this.type == null ? "" : " : " + (this.lValue ? "REF " : "")
						+ this.type.identifier.name));
	}

	/**
	 * Die Methode generiert den Assembler-Code für diesen Ausdruck. Sie geht
	 * davon aus, dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
	 *
	 * @param code
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	@Override
	void generateCode(CodeStream code) {
		if (this.identifier.declaration instanceof VarDeclaration) {
			VarDeclaration v = (VarDeclaration) this.identifier.declaration;
			if (v.isAttribute) {
				code.println("; Referencing attribute " + this.identifier.name);
				code.println("MRM R5, (R2)");
				code.println("MRI R6, " + v.offset);
				code.println("ADD R5, R6");
				code.println("MMR (R2), R5");
			} else {
				code.println("; Referencing variable " + this.identifier.name);
				code.println("MRI R5, " + v.offset);
				code.println("ADD R5, R3");
				code.println("ADD R2, R1");
				code.println("MMR (R2), R5");
			}
		} else if (this.identifier.declaration instanceof MethodDeclaration) {
			code.println("; Static method call: " + this.identifier.name);
			MethodDeclaration m = (MethodDeclaration) this.identifier.declaration;
			String returnLabel = code.nextLabel();

			code.println("; Arguments");
			code.println("");

			int i = 1;
			for (Expression e : this.arguments) {
				code.println("; Argument " + i);
				code.println("; " + e.getClass());
				e.generateCode(code);
				i++;
			}

			code.println("MRI R5, " + returnLabel + " ; Return address.");
			code.println("ADD R2, R1");

			code.println("");

			code.println("MMR (R2), R5 ; Save return address on the stack.");
			code.println("MRI R0, " + m.self.type.name + "_"
					+ m.identifier.name + " ; Jump to method by overwriting PC.");
			code.println(returnLabel + ":");
		} else {
			assert false;
		}
	}
}