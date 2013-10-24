package org.oopsc;
import java.util.Stack;

import org.oopsc.expression.Expression;

/**
 * Die Klasse repräsentiert die Zuweisung im Syntaxbaum.
 */
public class Assignment extends Statement {
	/** Der L-Wert, dem ein neuer Wert zugewiesen wird. */
	Expression leftOperand;

	/** Der Ausdruck, dessen Ergebnis zugewiesen wird. */
	Expression rightOperand;

	/**
	 * Konstruktor.
	 *
	 * @param leftOperand
	 *        Der L-Wert, dem ein neuer Wert zugewiesen wird.
	 * @param rightOperand
	 *        Der Ausdruck, dessen Ergebnis zugewiesen wird.
	 */
	Assignment(Expression leftOperand, Expression rightOperand) {
		this.leftOperand = leftOperand;
		this.rightOperand = rightOperand;
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
	public void contextAnalysis(Declarations declarations) throws CompileException {
		this.leftOperand = this.leftOperand.contextAnalysis(declarations);
		this.rightOperand = this.rightOperand.contextAnalysis(declarations);
		if (!this.leftOperand.lValue) {
			throw new CompileException("L-Wert erwartet", this.leftOperand.position);
		}
		this.rightOperand = this.rightOperand.box(declarations);
		this.rightOperand.type.check(this.leftOperand.type, this.rightOperand.position);
	}

	/**
	 * Die Methode gibt diese Anweisung in einer Baumstruktur aus.
	 *
	 * @param tree
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	@Override
	public void print(TreeStream tree) {
		tree.println("ASSIGNMENT");
		tree.indent();
		this.leftOperand.print(tree);
		this.rightOperand.print(tree);
		tree.unindent();
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
	public void generateCode(CodeStream code, Stack<Context> contexts) {
		code.println("; ASSIGNMENT code for left operand");
		this.leftOperand.generateCode(code);
		code.println("; ASSIGNMENT code for right operand");
		this.rightOperand.generateCode(code);
		code.println("; ASSIGNMENT");
		code.println("MRM R5, (R2) ; Rechten Wert vom Stapel nehmen");
		code.println("SUB R2, R1");
		code.println("MRM R6, (R2) ; Referenz auf linken Wert vom Stapel nehmen");
		code.println("SUB R2, R1");
		code.println("MMR (R6), R5 ; Zuweisen");
	}
}