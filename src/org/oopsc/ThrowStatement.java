package org.oopsc;
import java.util.Stack;

/**
 * Statement for triggering an exception.
 */
class ThrowStatement extends Statement {
	/** The integer value to be thrown. */
	Expression value;

	/** Position in the source code associated with this statement. */
	Position position;

	/**
	 * Constructor.
	 *
	 * @param value
	 *        An integer value that will be thrown.
	 * @param position
	 *        The position in the source code.
	 */
	ThrowStatement(Expression value, Position position) {
		this.value = value;
		this.position = position;
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
		this.value = this.value.contextAnalysis(declarations);
		this.value = this.value.unBox();

		this.value.type.check(ClassDeclaration.intType, this.value.position);
	}

	/**
	 * Die Methode gibt diese Anweisung in einer Baumstruktur aus.
	 *
	 * @param tree
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	@Override
	void print(TreeStream tree) {
		tree.println("THROW");
		tree.indent();
		this.value.print(tree);
		tree.unindent();
	}

	public static void throwException(CodeStream code) {
		/* Load the pointer to the global variable _currentExceptionFrame into R5. */
		code.println("MRI R5, _currentExceptionFrame");

		/* Dereference the value, i.e., load the exception frame. */
		code.println("MRM R5, (R5)");

		/* Load the address of the exception handler from the current exception frame
		 * and jump to it. */
		code.println("MRM R0, (R5)");
	}

	/**
	 * Die Methode generiert den Assembler-Code für diese Anweisung. Sie geht
	 * davon aus, dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
	 *
	 * @param code
	 *        Der Strom, in den die Ausgabe erfolgt.
	 * @param contexts
	 *        Current stack of contexts, may be used to inject instructions for
	 *        unwinding the stack (as needed for RETURN statements in TRY blocks).
	 */
	@Override
	void generateCode(CodeStream code, Stack<Context> contexts) {
		code.println("; THROW");

		/* Push the exception code on the stack. */
		this.value.generateCode(code);

		/* Copy the exception code to R7. */
		code.println("MRM R7, (R2)");

		/* Pop the exception code from the stack. */
		code.println("SUB R2, R1");

		/* Pass the exception to the inner-most exception handler and propagate it if
		 * necessary. */
		throwException(code);

		code.println("; END THROW");
	}
}