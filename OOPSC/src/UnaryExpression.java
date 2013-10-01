/**
 * Die Klasse repräsentiert einen Ausdruck mit einem unären Operator im Syntaxbaum.
 */
class UnaryExpression extends Expression {
    /** Der Operator. */
    Symbol.Id operator;

    /** Der Operand, auf den der Operator angewendet wird. */
    Expression operand;

    /**
     * Konstruktor.
     * @param operator Der Operator.
     * @param operand Der Operand, auf den der Operator angewendet wird.
     * @param position Die Position, an der dieser Ausdruck im Quelltext beginnt.
     */
    UnaryExpression(Symbol.Id operator, Expression operand, Position position) {
        super(position);
        this.operator = operator;
        this.operand = operand;
    }

    /**
     * Die Methode führt die Kontextanalyse für diesen Ausdruck durch.
     * @param declarations Die an dieser Stelle gültigen Deklarationen.
     * @return Dieser Ausdruck.
     * @throws CompileException Während der Kontextanylyse wurde ein Fehler
     *         gefunden.
     */
    @Override
	Expression contextAnalysis(Declarations declarations) throws CompileException {
        this.operand = this.operand.contextAnalysis(declarations);
        this.operand = this.operand.unBox();
        switch (this.operator) {
        case NOT:
            this.operand.type.check(ClassDeclaration.boolType, this.operand.position);
            break;
        case MINUS:
            this.operand.type.check(ClassDeclaration.intType, this.operand.position);
            break;
        default:
            assert false;
        }
        this.type = this.operand.type;
        return this;
    }

    /**
     * Die Methode gibt diesen Ausdruck in einer Baumstruktur aus.
     * Wenn der Typ des Ausdrucks bereits ermittelt wurde, wird er auch ausgegeben.
     * @param tree Der Strom, in den die Ausgabe erfolgt.
     */
    @Override
	void print(TreeStream tree) {
        tree.println(this.operator + (this.type == null ? "" : " : " + this.type.identifier.name));
        tree.indent();
        this.operand.print(tree);
        tree.unindent();
    }

    /**
     * Die Methode generiert den Assembler-Code für diesen Ausdruck. Sie geht
     * davon aus, dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
     * @param code Der Strom, in den die Ausgabe erfolgt.
     */
    @Override
	void generateCode(CodeStream code) {
        this.operand.generateCode(code);
        code.println("; " + this.operator);
        code.println("MRM R5, (R2)");
        switch (this.operator) {
        case NOT:
        	/* TODO Optimise. */
            code.println("MRI R6, 1");
            code.println("SUB R6, R5");
            code.println("MMR (R2), R6");
        case MINUS:
            code.println("MRI R6, 0");
            code.println("SUB R6, R5");
            code.println("MMR (R2), R6");
            break;
        default:
            assert false;
        }
    }
}
