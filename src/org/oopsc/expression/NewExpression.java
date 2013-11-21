package org.oopsc.expression;

import org.oopsc.*;
import org.oopsc.symbol.*;

import scala.Some;

/**
 * Die Klasse repräsentiert einen Ausdruck im Syntaxbaum, der ein neues Objekt erzeugt.
 */
public class NewExpression extends Expression {
	/** Der Typ des neuen Objekts. */
	public ResolvableClassSymbol newType;

	/**
	 * Konstruktor.
	 *
	 * @param newType
	 *        Der Typ des neuen Objekts.
	 */
	public NewExpression(ResolvableClassSymbol newType) {
		super(newType.identifier().position());
		this.newType = newType;
	}

	/**
	 * Die Methode führt die Kontextanalyse für diesen Ausdruck durch.
	 *
	 * @param sem
	 *        Die an dieser Stelle gültigen Deklarationen.
	 * @return Dieser Ausdruck.
	 * @throws CompileException
	 *         Während der Kontextanylyse wurde ein Fehler
	 *         gefunden.
	 */
	@Override
	public Expression refPass(SemanticAnalysis sem) throws CompileException {
		this.newType.declaration_$eq(new Some<>(sem.currentScope().get()
				.resolveClass(this.newType.identifier())));
		this.type = this.newType.declaration().get();
		return this;
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
		tree.println("NEW " + this.newType.identifier().name()
				+ (this.type == null ? "" : " : " + this.type.name()));
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
		code.println("; NEW " + this.newType.identifier().name());
		code.println("ADD R2, R1");
		code.println("MMR (R2), R4 ; Referenz auf neues Objekt auf den Stapel legen");
		code.println("MRI R5, " + this.newType.declaration().get().objectSize());

		/* Insert the address pointing to the VMT at the relative position 0 of the
		 * object. The offsets 1.. denote the attributes. */
		code.println("MRI R6, " + this.newType.identifier().name());
		code.println("MMR (R4), R6");

		code.println("ADD R4, R5 ; Heap weiter zählen");
	}
}