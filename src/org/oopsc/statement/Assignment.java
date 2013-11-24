package org.oopsc.statement;

import java.util.Stack;

import org.oopsc.CodeStream;
import org.oopsc.CompileException;
import org.oopsc.SemanticAnalysis;
import org.oopsc.TreeStream;
import org.oopsc.expression.Expression;

/**
 * Die Klasse repräsentiert die Zuweisung im Syntaxbaum.
 */
public class Assignment extends Statement {
	/** Der L-Wert, dem ein neuer Wert zugewiesen wird. */
	public Expression leftOperand;

	/** Der Ausdruck, dessen Ergebnis zugewiesen wird. */
	public Expression rightOperand;

	/**
	 * Konstruktor.
	 *
	 * @param leftOperand
	 *        Der L-Wert, dem ein neuer Wert zugewiesen wird.
	 * @param rightOperand
	 *        Der Ausdruck, dessen Ergebnis zugewiesen wird.
	 */
	public Assignment(Expression leftOperand, Expression rightOperand) {
		this.leftOperand = leftOperand;
		this.rightOperand = rightOperand;
	}

	@Override
	public void refPass(SemanticAnalysis sem) throws CompileException {
		this.leftOperand.refPass(sem);
		this.rightOperand.refPass(sem);

		if (!this.leftOperand.lValue()) {
			throw new CompileException("Lvalue expected",
					this.leftOperand.position());
		}

        System.out.println("lo = " + this.leftOperand);
        System.out.println("lot = " + this.leftOperand.type());
        System.out.println("ro = " + this.rightOperand);

		this.rightOperand.type().check(sem, this.leftOperand.type(),
				this.rightOperand.position());
	}

	@Override
	public void print(TreeStream tree) {
		tree.println("ASSIGNMENT");
		tree.indent();
		this.leftOperand.print(tree);
		this.rightOperand.print(tree);
		tree.unindent();
	}

	@Override
	public void generateCode(CodeStream code, Stack<Context> contexts) {
		code.println("; ASSIGNMENT code for left operand");
		this.leftOperand.generateCode(code);
		code.println("; ASSIGNMENT code for right operand");
		this.rightOperand.generateCode(code, true);
		code.println("; ASSIGNMENT");
		code.println("MRM R5, (R2) ; Rechten Wert vom Stapel nehmen");
		code.println("SUB R2, R1");
		code.println("MRM R6, (R2) ; Referenz auf linken Wert vom Stapel nehmen");
		code.println("SUB R2, R1");
		code.println("MMR (R6), R5 ; Zuweisen");
	}
}