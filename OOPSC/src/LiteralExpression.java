/**
 * Die Klasse repräsentiert einen Ausdruck mit einem Literal im Syntaxbaum.
 */
class LiteralExpression extends Expression {
	/** Der Wert des Literals. */
	int value;

	/**
	 * Konstruktor.
	 *
	 * @param value
	 *        Der Wert des Literals.
	 * @param type
	 *        Der Typ des Literals.
	 * @param position
	 *        Die Position, an der dieser Ausdruck im Quelltext beginnt.
	 */
	LiteralExpression(int value, ClassDeclaration type, Position position) {
		super(position);
		this.value = value;
		this.type = type;
	}

	/**
	 * Die Methode gibt dieses Literal und seinen Typ in einer Baumstruktur aus.
	 *
	 * @param tree
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	@Override
	void print(TreeStream tree) {
		tree.println(this.value + " : " + this.type.identifier.name);
	}

	/**
	 * Die Methode generiert den Assembler-Code für diesen Ausdruck.
	 *
	 * @param code
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	@Override
	void generateCode(CodeStream code) {
		code.println("; " + this.value + " : " + this.type.identifier.name);
		code.println("MRI R5, " + this.value);
		code.println("ADD R2, R1");
		code.println("MMR (R2), R5");
	}
}
