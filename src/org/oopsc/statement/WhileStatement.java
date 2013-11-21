package org.oopsc.statement;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.oopsc.*;
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

	public void defPass(SemanticAnalysis sem) throws CompileException {
		for (Statement s : this.statements) {
			s.defPass(sem);
		}
	}

	@Override
	public void refPass(SemanticAnalysis sem) throws CompileException {
		this.condition = this.condition.refPass(sem);
		this.condition.types = sem.types();
		this.condition.type.check(sem, sem.types().boolType(),
				this.condition.position);

		for (Statement s : this.statements) {
			s.refPass(sem);
		}
	}

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

	@Override
	public void generateCode(CodeStream code, Stack<Context> contexts) {
		String whileLabel = code.nextLabel();
		String endLabel = code.nextLabel();

		code.println("; WHILE");
		code.println(whileLabel + ":");
		this.condition.generateCode(code, false);

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