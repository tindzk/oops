package org.oopsc.expression;

import org.oopsc.*;
import org.oopsc.symbol.ClassSymbol;

/**
 * Die Klasse repräsentiert einen Ausdruck im Syntaxbaum, der ein Objekt in
 * einen Wert eines Basisdatentyps auspackt ("unboxing").
 * Dieser Ausdruck wird immer nachträglich während der Kontextanalyse in
 * den Syntaxbaum eingefügt.
 */
public class UnBoxExpression extends Expression {
	/** Der Ausdruck, der das auszupackende Objekt berechnet. */
	public Expression operand;

	/**
	 * Konstruktor.
	 * Der Konstruktor stellt fest, von welcher Klasse der auszupackende
	 * Ausdruck ist bestimmt den entsprechenden Basisdatentyp.
	 *
	 * @param operand
	 *        Der Ausdruck, der das auszupackende Objekt berechnet.
	 */
	public UnBoxExpression(SemanticAnalysis sem, Expression operand) {
		super(operand.position);
		this.operand = operand;

		if (operand.type.isA(sem, sem.types().intClass())) {
			this.type = sem.types().intType();
		} else if (operand.type.isA(sem, sem.types().boolClass())) {
			this.type = sem.types().boolType();
		}
	}

	@Override
	public void print(TreeStream tree) {
		tree.println("UNBOX"
				+ (this.type == null ? "" : " : " + this.type.name()));
		tree.indent();
		this.operand.print(tree);
		tree.unindent();
	}

	@Override
	public void generateCode(CodeStream code) {
		this.operand.generateCode(code);
		code.println("; UNBOX");
		code.println("MRM R5, (R2) ; Objektreferenz vom Stapel lesen");
		code.println("MRI R6, " + ClassSymbol.HEADERSIZE());
		code.println("ADD R5, R6 ; Adresse des Werts bestimmen");
		code.println("MRM R5, (R5) ; Wert auslesen");
		code.println("MMR (R2), R5 ; und auf den Stapel schreiben");
	}
}