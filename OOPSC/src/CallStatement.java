/**
 * Die Klasse repräsentiert die Anweisung "Methodenaufruf" im Syntaxbaum.
 */
class CallStatement extends Statement {
	/** Der Ausdruck, der den Methodenaufruf repräsentiert. */
	Expression call;

	/**
	 * Konstruktor.
	 *
	 * @param call
	 *        Der Ausdruck, der den Methodenaufruf repräsentiert.
	 */
	CallStatement(Expression call) {
		this.call = call;
	}

	/**
	 * Die Methode führt die Kontextanalyse für diese Anweisung durch.
	 *
	 * @param declarations
	 *        Die an dieser Stelle gültigen Deklarationen.
	 * @throws CompileException
	 *         Während der Kontextanylyse wurde ein Fehler
	 *         gefunden.
	 */
	@Override
	void contextAnalysis(Declarations declarations) throws CompileException {
		this.call = this.call.contextAnalysis(declarations);
		this.call.type.check(ClassDeclaration.voidType, this.call.position);
	}

	/**
	 * Die Methode gibt diese Anweisung in einer Baumstruktur aus.
	 *
	 * @param tree
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	@Override
	void print(TreeStream tree) {
		tree.println("CALL");
		tree.indent();
		this.call.print(tree);
		tree.unindent();
	}

	/**
	 * Die Methode generiert den Assembler-Code für diese Anweisung. Sie geht
	 * davon aus, dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
	 *
	 * @param code
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	@Override
	void generateCode(CodeStream code) {
		code.println("; CALL");
		this.call.generateCode(code);
	}
}
