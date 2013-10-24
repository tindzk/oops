package org.oopsc.expression;

import org.oopsc.ClassDeclaration;
import org.oopsc.CodeStream;
import org.oopsc.TreeStream;

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
	public UnBoxExpression(Expression operand) {
		super(operand.position);
		this.operand = operand;

		if (operand.type.isA(ClassDeclaration.intClass)) {
			this.type = ClassDeclaration.intType;
		} else if (operand.type.isA(ClassDeclaration.boolClass)) {
			this.type = ClassDeclaration.boolType;
		} else {
			assert false;
		}
	}

	/**
	 * Die Methode gibt diesen Ausdruck in einer Baumstruktur aus.
	 * Wenn der Typ des Ausdrucks bereits ermittelt wurde, wird er auch ausgegeben.
	 *
	 * @param tree
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	@Override
	public void print(TreeStream tree) {
		tree.println("UNBOX"
				+ (this.type == null ? "" : " : " + this.type.identifier.name));
		tree.indent();
		this.operand.print(tree);
		tree.unindent();
	}

	/**
	 * Die Methode generiert den Assembler-Code für diesen Ausdruck. Sie geht
	 * davon aus, dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
	 *
	 * @param code
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	@Override
	public void generateCode(CodeStream code) {
		this.operand.generateCode(code);
		code.println("; UNBOX");
		code.println("MRM R5, (R2) ; Objektreferenz vom Stapel lesen");
		code.println("MRI R6, " + ClassDeclaration.HEADERSIZE);
		code.println("ADD R5, R6 ; Adresse des Werts bestimmen");
		code.println("MRM R5, (R5) ; Wert auslesen");
		code.println("MMR (R2), R5 ; und auf den Stapel schreiben");
	}
}