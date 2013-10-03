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
			this.type = (ClassDeclaration) ((VarDeclaration) this.identifier.declaration).type.declaration;
			this.lValue = true;
		} else if (this.identifier.declaration instanceof MethodDeclaration) {
			this.type = ClassDeclaration.voidType;
		} else {
			assert false;
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
				code.println("; Referenz auf Attribut " + this.identifier.name);
				code.println("MRM R5, (R2)");
				code.println("MRI R6, " + v.offset);
				code.println("ADD R5, R6");
				code.println("MMR (R2), R5");
			} else {
				code.println("; Referenz auf Variable " + this.identifier.name);
				code.println("MRI R5, " + v.offset);
				code.println("ADD R5, R3");
				code.println("ADD R2, R1");
				code.println("MMR (R2), R5");
			}
		} else if (this.identifier.declaration instanceof MethodDeclaration) {
			MethodDeclaration m = (MethodDeclaration) this.identifier.declaration;
			String returnLabel = code.nextLabel();
			code.println("MRI R5, " + returnLabel);
			code.println("ADD R2, R1");
			code.println("MMR (R2), R5 ; Rücksprungadresse auf den Stapel");
			code.println("; Statischer Aufruf von " + this.identifier.name);
			code.println("MRI R0, " + m.self.type.name + "_"
					+ m.identifier.name);
			code.println(returnLabel + ":");
		} else {
			assert false;
		}
	}
}