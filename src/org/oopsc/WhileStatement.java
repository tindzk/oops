package org.oopsc;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.oopsc.expression.Expression;

/**
 * Die Klasse repräsentiert die Anweisung WHILE im Syntaxbaum.
 */
public class WhileStatement extends Statement {
	/** Die Bedingung der WHILE-Anweisung. */
	public Expression condition;

	/** Die Anweisungen im Schleifenrumpf. */
	public List<Statement> statements = new LinkedList<>();

	/**
	 * Konstruktor.
	 *
	 * @param condition
	 *        Die Bedingung der WHILE-Anweisung.
	 */
	public WhileStatement(Expression condition) {
		this.condition = condition;
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
		this.condition = this.condition.contextAnalysis(declarations);
		this.condition = this.condition.unBox();
		this.condition.type.check(ClassDeclaration.boolType,
				this.condition.position);

		for (Statement s : this.statements) {
			s.contextAnalysis(declarations);
		}
	}

	/**
	 * Die Methode gibt diese Anweisung in einer Baumstruktur aus.
	 *
	 * @param tree
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	@Override
	public void print(TreeStream tree) {
		tree.println("WHILE");
		tree.indent();
		this.condition.print(tree);
		if (!this.statements.isEmpty()) {
			tree.println("DO");
			tree.indent();
			for (Statement s : this.statements) {
				s.print(tree);
			}
			tree.unindent();
		}
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
		String whileLabel = code.nextLabel();
		String endLabel = code.nextLabel();

		code.println("; WHILE");
		code.println(whileLabel + ":");
		this.condition.generateCode(code);

		code.println("MRM R5, (R2) ; Bedingung vom Stapel nehmen");
		code.println("SUB R2, R1");
		code.println("ISZ R5, R5 ; Wenn 0, dann");
		code.println("JPC R5, " + endLabel + " ; Schleife verlassen");
		code.println("; DO");

		for (Statement s : this.statements) {
			s.generateCode(code, contexts);
		}

		code.println("; END WHILE");
		code.println("MRI R0, " + whileLabel);
		code.println(endLabel + ":");
	}
}