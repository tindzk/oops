package org.oopsc.expression;

import org.oopsc.*;

/**
 * Die Klasse repräsentiert einen Ausdruck mit einem unären Operator im Syntaxbaum.
 */
public class UnaryExpression extends Expression {
	public enum Operator {
		MINUS, NOT
	}

	/** Der Operator. */
	public Operator operator;

	/** Der Operand, auf den der Operator angewendet wird. */
	public Expression operand;

	/**
	 * Konstruktor.
	 *
	 * @param operator
	 *        Der Operator.
	 * @param operand
	 *        Der Operand, auf den der Operator angewendet wird.
	 * @param position
	 *        Die Position, an der dieser Ausdruck im Quelltext beginnt.
	 */
	public UnaryExpression(Operator operator, Expression operand,
			Position position) {
		super(position);
		this.operator = operator;
		this.operand = operand;
	}

	@Override
	public void refPass(SemanticAnalysis sem) throws CompileException {
		this.operand.refPass(sem);

		switch (this.operator) {
			case NOT:
				this.operand.type.check(sem, Types.boolType(),
						this.operand.position);
				break;

			case MINUS:
				this.operand.type.check(sem, Types.intType(),
						this.operand.position);
				break;

			default:
				assert false;
		}

		this.type = this.operand.type;
	}

	@Override
	public void print(TreeStream tree) {
		tree.println(this.operator
				+ (this.type == null ? "" : " : " + this.type.name()));
		tree.indent();
		this.operand.print(tree);
		tree.unindent();
	}

	@Override
	public void generateCode(CodeStream code) {
		this.operand.generateCode(code, false);
		code.println("; " + this.operator);
		code.println("MRM R5, (R2)");
		switch (this.operator) {
			case NOT:
				/* TODO Optimise. */
				code.println("MRI R6, 1");
				code.println("SUB R6, R5");
				code.println("MMR (R2), R6");
			case MINUS:
				code.println("MRI R6, 0");
				code.println("SUB R6, R5");
				code.println("MMR (R2), R6");
				break;
			default:
				assert false;
		}
	}
}