package org.oopsc.statement;

import java.util.Stack;

import org.oopsc.*;
import org.oopsc.expression.Expression;

/**
 * Die Klasse repräsentiert die Anweisung "Methodenaufruf" im Syntaxbaum.
 */
public class CallStatement extends Statement {
	/** Der Ausdruck, der den Methodenaufruf repräsentiert. */
	public Expression call;

	/**
	 * Konstruktor.
	 *
	 * @param call
	 *        Der Ausdruck, der den Methodenaufruf repräsentiert.
	 */
	public CallStatement(Expression call) {
		this.call = call;
	}

	@Override
	public void refPass(SemanticAnalysis sem) throws CompileException {
		this.call.refPass(sem);
		this.call.type().check(sem, Types.voidType(), this.call.position());
	}

	@Override
	public void print(TreeStream tree) {
		tree.println("CALL");
		tree.indent();
		this.call.print(tree);
		tree.unindent();
	}

	@Override
	public void generateCode(CodeStream code, Stack<Context> contexts) {
		code.println("; CALL");
		this.call.generateCode(code);
	}
}