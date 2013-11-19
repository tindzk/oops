package org.oopsc.expression;

import org.oopsc.*;
import org.oopsc.symbol.*;

/**
 * Die Klasse repräsentiert einen Ausdruck mit einem Literal im Syntaxbaum.
 */
public class LiteralExpression extends Expression {
	/** Der Wert des Literals. */
	public int value;

	/**
	 * Konstruktor.
	 *
	 * @param value
	 *        Der Wert des Literals.
	 * @param type
	 *        Der Typ des Literals.
	 * @param position
	 *        Die Position, an der dieser Ausdruck im Quelltext beginnt.
	 */
	public LiteralExpression(int value, ClassSymbol type, Position position) {
		super(position);
		this.value = value;
		this.type = type;
	}

	/**
	 * Die Methode gibt dieses Literal und seinen Typ in einer Baumstruktur aus.
	 *
	 * @param tree
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	@Override
	public void print(TreeStream tree) {
		tree.println(this.value + " : " + this.type.name());
	}

	/**
	 * Die Methode generiert den Assembler-Code für diesen Ausdruck.
	 *
	 * Pushes the value on the stack.
	 *
	 * @param code
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	@Override
	public void generateCode(CodeStream code) {
		code.println("; " + this.value + " : " + this.type.name());

		/* Load value into R5. */
		code.println("MRI R5, " + this.value);

		/* Allocate space on the stack. */
		code.println("ADD R2, R1");

		/* Copy value from R5 to the newly allocated space on the stack. */
		code.println("MMR (R2), R5");
	}

	@Override
	public boolean isAlwaysTrue(SemanticAnalysis sem) {
		return this.value == 1
				&& (this.type.isA(sem, sem.types().intType()) || this.type.isA(
						sem, sem.types().boolType()));
	}
}