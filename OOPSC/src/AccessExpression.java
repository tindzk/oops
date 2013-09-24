/**
 * Die Klasse repräsentiert einen Ausdruck mit einem Attribut- bzw.
 * Methoden-Zugriffsoperator (d.h. der Punkt) im Syntaxbaum.
 */
class AccessExpression extends Expression {
    /** Der linke Operand. */
    Expression leftOperand;

    /** Der rechte Operand. */
    VarOrCall rightOperand;

    /**
     * Konstruktor.
     * @param leftOperand Der linke Operand.
     * @param rightOperand Der rechte Operand.
     */
    AccessExpression(Expression leftOperand, VarOrCall rightOperand) {
        super(leftOperand.position);
        this.leftOperand = leftOperand;
        this.rightOperand = rightOperand;
    }

    /**
     * Die Methode führt die Kontextanalyse für diesen Ausdruck durch.
     * @param declarations Die an dieser Stelle gültigen Deklarationen.
     * @return Dieser Ausdruck.
     * @throws CompileException Während der Kontextanylyse wurde ein Fehler
     *         gefunden.
     */
    Expression contextAnalysis(Declarations declarations) throws CompileException {
        leftOperand = leftOperand.contextAnalysis(declarations);

        // Dereferenzieren. Außerdem könnte man einen Ausdruck wie z.B. 5.print
        // schreiben, wenn Integer Methoden hätte.
        leftOperand = leftOperand.box(declarations);

        // Der rechte Operand hat einen Deklarationsraum, der sich aus dem
        // Ergebnistyp des linken Operanden ergibt.
        rightOperand.contextAnalysisForMember(leftOperand.type.declarations);

        // Der Typ dieses Ausdrucks ist immer der des rechten Operanden.
        type = rightOperand.type;
        lValue = rightOperand.lValue;

        return this;
    }

    /**
     * Die Methode gibt diesen Ausdruck in einer Baumstruktur aus.
     * Wenn der Typ des Ausdrucks bereits ermittelt wurde, wird er auch ausgegeben.
     * @param tree Der Strom, in den die Ausgabe erfolgt.
     */
    void print(TreeStream tree) {
        tree.println("PERIOD" + (type == null ? "" : " : " +
                (lValue ? "REF " : "") + type.identifier.name));
        tree.indent();
        leftOperand.print(tree);
        rightOperand.print(tree);
        tree.unindent();
    }

    /**
     * Die Methode generiert den Assembler-Code für diesen Ausdruck. Sie geht
     * davon aus, dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
     * @param code Der Strom, in den die Ausgabe erfolgt.
     */
    void generateCode(CodeStream code) {
        leftOperand.generateCode(code);
        rightOperand.generateCode(code);
    }
}
