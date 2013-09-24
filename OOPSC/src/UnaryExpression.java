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
    Expression contextAnalysis(Declarations declarations) throws CompileException {
        operand = operand.contextAnalysis(declarations);
        operand = operand.unBox();
        switch (operator) {
        case MINUS:
            operand.type.check(ClassDeclaration.intType, operand.position);
            break;
        default:
            assert false;
        }
        type = operand.type;
        return this;
    }

    /**
     * Die Methode gibt diesen Ausdruck in einer Baumstruktur aus.
     * Wenn der Typ des Ausdrucks bereits ermittelt wurde, wird er auch ausgegeben.
     * @param tree Der Strom, in den die Ausgabe erfolgt.
     */
    void print(TreeStream tree) {
        tree.println(operator + (type == null ? "" : " : " + type.identifier.name));
        tree.indent();
        operand.print(tree);
        tree.unindent();
    }

    /**
     * Die Methode generiert den Assembler-Code für diesen Ausdruck. Sie geht
     * davon aus, dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
     * @param code Der Strom, in den die Ausgabe erfolgt.
     */
    void generateCode(CodeStream code) {
        operand.generateCode(code);
        code.println("; " + operator);
        code.println("MRM R5, (R2)");
        switch (operator) {
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
