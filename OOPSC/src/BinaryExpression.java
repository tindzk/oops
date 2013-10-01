/**
 * Die Klasse repräsentiert einen Ausdruck mit einem binären Operator im Syntaxbaum.
 */
class BinaryExpression extends Expression {
	/** Der linke Operand. */
	Expression leftOperand;

	/** Der Operator. */
	Symbol.Id operator;

	/** Der rechte Operand. */
	Expression rightOperand;

	/**
	 * Konstruktor.
	 *
	 * @param operator
	 *        Der Operator.
	 * @param leftOperand
	 *        Der linke Operand.
	 * @param rightOperand
	 *        Der rechte Operand.
	 */
	BinaryExpression(Expression leftOperand, Symbol.Id operator,
			Expression rightOperand) {
		super(leftOperand.position);
		this.leftOperand = leftOperand;
		this.operator = operator;
		this.rightOperand = rightOperand;
	}

	/**
	 * Die Methode führt die Kontextanalyse für diesen Ausdruck durch.
	 *
	 * @param declarations
	 *        Die an dieser Stelle gültigen Deklarationen.
	 * @return Dieser Ausdruck.
	 * @throws CompileException
	 *         Während der Kontextanylyse wurde ein Fehler
	 *         gefunden.
	 */
	@Override
	Expression contextAnalysis(Declarations declarations)
			throws CompileException {
		this.leftOperand = this.leftOperand.contextAnalysis(declarations);
		this.rightOperand = this.rightOperand.contextAnalysis(declarations);
		switch (this.operator) {
			case AND:
			case OR:
				this.leftOperand = this.leftOperand.unBox();
				this.rightOperand = this.rightOperand.unBox();
				this.leftOperand.type.check(ClassDeclaration.boolType,
						this.leftOperand.position);
				this.rightOperand.type.check(ClassDeclaration.boolType,
						this.rightOperand.position);
				this.type = ClassDeclaration.boolType;
				break;
			case PLUS:
			case MINUS:
			case TIMES:
			case DIV:
			case MOD:
				this.leftOperand = this.leftOperand.unBox();
				this.rightOperand = this.rightOperand.unBox();
				this.leftOperand.type.check(ClassDeclaration.intType,
						this.leftOperand.position);
				this.rightOperand.type.check(ClassDeclaration.intType,
						this.rightOperand.position);
				this.type = ClassDeclaration.intType;
				break;
			case GT:
			case GTEQ:
			case LT:
			case LTEQ:
				this.leftOperand = this.leftOperand.unBox();
				this.rightOperand = this.rightOperand.unBox();
				this.leftOperand.type.check(ClassDeclaration.intType,
						this.leftOperand.position);
				this.rightOperand.type.check(ClassDeclaration.intType,
						this.rightOperand.position);
				this.type = ClassDeclaration.boolType;
				break;
			case EQ:
			case NEQ:
				// Wenn einer der beiden Operanden NULL ist, muss der andere
				// ein Objekt sein (oder auch NULL)
				if (this.leftOperand.type == ClassDeclaration.nullType) {
					this.rightOperand = this.rightOperand.box(declarations);
				} else if (this.rightOperand.type == ClassDeclaration.nullType) {
					this.leftOperand = this.leftOperand.box(declarations);
				} else {
					// ansonsten wird versucht, die beiden Operanden in
					// Basisdatentypen zu wandeln
					this.leftOperand = this.leftOperand.unBox();
					this.rightOperand = this.rightOperand.unBox();
				}

				// Nun muss der Typ mindestens eines Operanden gleich oder eine
				// Ableitung des Typs des anderen Operanden sein.
				if (!this.leftOperand.type.isA(this.rightOperand.type)
						&& !this.rightOperand.type.isA(this.leftOperand.type)) {
					ClassDeclaration.typeError(this.leftOperand.type,
							this.rightOperand.position);
				}
				this.type = ClassDeclaration.boolType;
				break;
			default:
				assert false;
		}
		return this;
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
		tree.println(this.operator
				+ (this.type == null ? "" : " : " + this.type.identifier.name));
		tree.indent();
		this.leftOperand.print(tree);
		this.rightOperand.print(tree);
		tree.unindent();
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
		this.leftOperand.generateCode(code);
		this.rightOperand.generateCode(code);

		code.println("; " + this.operator);
		code.println("MRM R5, (R2)");
		code.println("SUB R2, R1");
		code.println("MRM R6, (R2)");

		switch (this.operator) {
			case AND:
				code.println("AND R6, R5");
			case OR:
				code.println("OR R6, R5");
			case PLUS:
				code.println("ADD R6, R5");
				break;
			case MINUS:
				code.println("SUB R6, R5");
				break;
			case TIMES:
				code.println("MUL R6, R5");
				break;
			case DIV:
				code.println("DIV R6, R5");
				break;
			case MOD:
				code.println("MOD R6, R5");
				break;
			case GT:
				code.println("SUB R6, R5");
				code.println("ISP R6, R6");
				break;
			case GTEQ:
				code.println("SUB R6, R5");
				code.println("ISN R6, R6");
				code.println("XOR R6, R1");
				break;
			case LT:
				code.println("SUB R6, R5");
				code.println("ISN R6, R6");
				break;
			case LTEQ:
				code.println("SUB R6, R5");
				code.println("ISP R6, R6");
				code.println("XOR R6, R1");
				break;
			case EQ:
				code.println("SUB R6, R5");
				code.println("ISZ R6, R6");
				break;
			case NEQ:
				code.println("SUB R6, R5");
				code.println("ISZ R6, R6");
				code.println("XOR R6, R1");
				break;
			default:
				assert false;
		}

		code.println("MMR (R2), R6");
	}
}
