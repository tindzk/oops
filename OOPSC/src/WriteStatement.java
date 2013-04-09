/**
 * Die Klasse repräsentiert die Anweisung WRITE im Syntaxbaum.
 */
public class WriteStatement extends Statement {
    /** Der Ausdruck, der als ein Zeichen ausgegeben wird. */
    Expression operand;
    
    /**
     * Konstruktor.
     * @param operand Der Ausdruck, der als ein Zeichen ausgegeben wird.
     */
    WriteStatement(Expression operand) {
        this.operand = operand;
    }

    /**
     * Die Methode führt die Kontextanalyse für diese Anweisung durch.
     * @param declarations Die an dieser Stelle gültigen Deklarationen.
     * @throws CompileException Während der Kontextanylyse wurde ein Fehler
     *         gefunden.
     */
    void contextAnalysis(Declarations declarations) throws CompileException {
        operand = operand.contextAnalysis(declarations);
        operand = operand.unBox();
        operand.type.check(ClassDeclaration.intType, operand.position);
    }

    /**
     * Die Methode gibt diese Anweisung in einer Baumstruktur aus.
     * @param tree Der Strom, in den die Ausgabe erfolgt.
     */
    void print(TreeStream tree) {
        tree.println("WRITE");
        tree.indent();
        operand.print(tree);
        tree.unindent();
    }

    /**
     * Die Methode generiert den Assembler-Code für diese Anweisung. Sie geht 
     * davon aus, dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
     * @param code Der Strom, in den die Ausgabe erfolgt.
     */
    void generateCode(CodeStream code) {
    	code.println("; WRITE operand code");
        operand.generateCode(code);
        code.println("; WRITE");
        code.println("MRM R5, (R2)");
        code.println("SUB R2, R1");
        code.println("SYS 1, 5");
    }
}
