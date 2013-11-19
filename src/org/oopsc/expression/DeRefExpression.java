package org.oopsc.expression;

import org.oopsc.CodeStream;
import org.oopsc.TreeStream;

/**
 * Die Klasse repräsentiert einen Ausdruck im Syntaxbaum, der eine Referenz
 * dereferenziert, d.h. aus einer Variablen, deren Adresse gegeben ist, den
 * Wert ausliest.
 * Dieser Ausdruck wird immer nachträglich während der Kontextanalyse in
 * den Syntaxbaum eingefügt.
 */
public class DeRefExpression extends Expression {
	/** Der Ausdruck, der die Adresse berechnet. */
	public Expression operand;

	/**
	 * Konstruktor.
	 *
	 * @param operand
	 *        Der Ausdruck, der die Adresse berechnet.
	 */
	public DeRefExpression(Expression operand) {
		super(operand.position);
		this.operand = operand;
		this.type = operand.type;
	}

	@Override
	public void print(TreeStream tree) {
		tree.println("DEREF"
				+ (this.type == null ? "" : " : " + this.type.name()));
		tree.indent();
		this.operand.print(tree);
		tree.unindent();
	}

	@Override
	public void generateCode(CodeStream code) {
		code.println("; DEREF Argument code");
		this.operand.generateCode(code);
		code.println("; DEREF");
		code.println("MRM R5, (R2)");
		code.println("MRM R5, (R5)");
		code.println("MMR (R2), R5");
	}
}