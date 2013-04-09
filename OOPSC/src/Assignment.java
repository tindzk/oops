/**
 * Die Klasse repräsentiert die Zuweisung im Syntaxbaum.
 */
class Assignment extends Statement {
    /** Der L-Wert, dem ein neuer Wert zugewiesen wird. */
    Expression leftOperand;
    
    /** Der Ausdruck, dessen Ergebnis zugewiesen wird. */
    Expression rightOperand;
    
    /**
     * Konstruktor.
     * @param leftOperand Der L-Wert, dem ein neuer Wert zugewiesen wird.
     * @param rightOperand Der Ausdruck, dessen Ergebnis zugewiesen wird.
     */
    Assignment(Expression leftOperand, Expression rightOperand) {
        this.leftOperand = leftOperand;
        this.rightOperand = rightOperand;
    }
    
    /**
     * Die Methode führt die Kontextanalyse für diese Anweisung durch.
     * @param declarations Die an dieser Stelle gültigen Deklarationen.
     * @throws CompileException Während der Kontextanylyse wurde ein Fehler
     *         gefunden.
     */
    void contextAnalysis(Declarations declarations) throws CompileException {
        leftOperand = leftOperand.contextAnalysis(declarations);
        rightOperand = rightOperand.contextAnalysis(declarations);
        if (!leftOperand.lValue) {
            throw new CompileException("L-Wert erwartet", leftOperand.position);
        }
        rightOperand = rightOperand.box(declarations);
        rightOperand.type.check(leftOperand.type, rightOperand.position);
    }

    /**
     * Die Methode gibt diese Anweisung in einer Baumstruktur aus.
     * @param tree Der Strom, in den die Ausgabe erfolgt.
     */
    void print(TreeStream tree) {
        tree.println("ASSIGNMENT");
        tree.indent();
        leftOperand.print(tree);
        rightOperand.print(tree);
        tree.unindent();
    }

    /**
     * Die Methode generiert den Assembler-Code für diese Anweisung. Sie geht 
     * davon aus, dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
     * @param code Der Strom, in den die Ausgabe erfolgt.
     */
    void generateCode(CodeStream code) {
    	code.println("; ASSIGNMENT code for left operand");
        leftOperand.generateCode(code);
    	code.println("; ASSIGNMENT code for right operand");
        rightOperand.generateCode(code);
        code.println("; ASSIGNMENT");
        code.println("MRM R5, (R2) ; Rechten Wert vom Stapel nehmen");
        code.println("SUB R2, R1");
        code.println("MRM R6, (R2) ; Referenz auf linken Wert vom Stapel nehmen");
        code.println("SUB R2, R1");
        code.println("MMR (R6), R5 ; Zuweisen");
    }
}
