package org.oopsc.statement;

import java.util.Stack;

import org.oopsc.*;
import org.oopsc.expression.Expression;

/**
 * Die Klasse repräsentiert die Anweisung WRITE im Syntaxbaum.
 */
public class WriteStatement extends Statement {
	/** Der Ausdruck, der als ein Zeichen ausgegeben wird. */
	public Expression operand;

	/**
	 * Konstruktor.
	 *
	 * @param operand
	 *        Der Ausdruck, der als ein Zeichen ausgegeben wird.
	 */
	public WriteStatement(Expression operand) {
		this.operand = operand;
	}

	@Override
	public void refPass(SemanticAnalysis sem) throws CompileException {
		this.operand = this.operand.refPass(sem);
		this.operand.types = sem.types();
		this.operand.type.check(sem, sem.types().intType(),
				this.operand.position);
	}

	@Override
	public void print(TreeStream tree) {
		tree.println("WRITE");
		tree.indent();
		this.operand.print(tree);
		tree.unindent();
	}

	@Override
	public void generateCode(CodeStream code, Stack<Context> contexts) {
		code.println("; WRITE operand code");
		this.operand.generateCode(code, false);
		code.println("; WRITE");
		code.println("MRM R5, (R2)");
		code.println("SUB R2, R1");
		code.println("SYS 1, 5");
	}
}