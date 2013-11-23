package org.oopsc.expression;

import org.oopsc.*;
import org.oopsc.symbol.*;

/**
 * Die Klasse repräsentiert einen Ausdruck mit einem Attribut- bzw.
 * Methoden-Zugriffsoperator (d.h. der Punkt) im Syntaxbaum.
 */
public class AccessExpression extends Expression {
	/** Der linke Operand. */
	public Expression leftOperand;

	/** Der rechte Operand. */
	public VarOrCall rightOperand;

	/**
	 * Konstruktor.
	 *
	 * @param leftOperand
	 *        Der linke Operand.
	 * @param rightOperand
	 *        Der rechte Operand.
	 */
	public AccessExpression(Expression leftOperand, VarOrCall rightOperand) {
		super(leftOperand.position);
		this.leftOperand = leftOperand;
		this.rightOperand = rightOperand;
	}

	@Override
	public void refPass(SemanticAnalysis sem) throws CompileException {
		this.leftOperand.refPass(sem);

		/* The left operand denotes the context. The right operand therefore does not
		 * need to resolve the context. */
		this.rightOperand.generateContextCode(false);

		/* Deal with accesses to methods or attributes in the base class. */
		if (this.leftOperand instanceof VarOrCall) {
			VarOrCall call = (VarOrCall) this.leftOperand;

			if (call.ref.identifier().name().equals("_base")) {
				this.rightOperand.generateContextCode(true);
				this.rightOperand.setContext((VariableSymbol) call.ref
						.declaration().get(), true);
			}
		}

		/* The scope of the right operand consists of the result type of the left
		 * operand. */
		this.rightOperand.scope = this.leftOperand.type;

		this.rightOperand.refPass(sem);

		/* The type of this expression is always the type of the right operand. */
		this.type = this.rightOperand.type;
		this.lValue = this.rightOperand.lValue;
	}

	@Override
	public void print(TreeStream tree) {
		tree.println("PERIOD"
				+ (this.type == null ? "" : " : " + (this.lValue ? "REF " : "")
						+ this.type.name()));
		tree.indent();
		this.leftOperand.print(tree);
		this.rightOperand.print(tree);
		tree.unindent();
	}

	@Override
	public void generateCode(CodeStream code) {
		this.leftOperand.generateCode(code, true);
		this.rightOperand.generateCode(code);
	}
}