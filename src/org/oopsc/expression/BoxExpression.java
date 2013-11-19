package org.oopsc.expression;

import org.oopsc.*;
import org.oopsc.symbol.*;

/**
 * Die Klasse repräsentiert einen Ausdruck im Syntaxbaum, der einen Wert vom
 * vom Typ eines Basisdatentyps "boxt", d.h. in ein Objekt verpackt.
 * Dieser Ausdruck wird immer nachträglich während der Kontextanalyse in
 * den Syntaxbaum eingefügt.
 */
public class BoxExpression extends Expression {
	/** Der Ausdruck, der den zu verpackenden Wert liefert. */
	public Expression operand;

	/** Ein Ausdruck, der das entsprechende Rahmenobjekt erzeugt. */
	public Expression newType;

	/**
	 * Konstruktor.
	 * Der Konstruktor stellt fest, von welchem Basisdatentyp der zu
	 * verpackende Ausdruck ist und erzeugt dann ein passendes Rahmenobjekt.
	 *
	 * @param operand
	 *        Der Ausdruck, der den zu verpackenden Wert liefert.
	 * @param sem
	 *        Die an dieser Stelle gültigen Deklarationen.
	 * @throws CompileException
	 *         Während der Kontextanylyse des neuen Objekts
	 *         wurde ein Fehler gefunden.
	 */
	public BoxExpression(Expression operand, SemanticAnalysis sem)
			throws CompileException {
		super(operand.position);
		this.operand = operand;

		if (operand.type.isA(sem, sem.types().intType())) {
			this.type = sem.types().intClass();
			this.newType = new NewExpression(new ResolvableSymbol(
					new Identifier("Integer", new Position(0, 0)), null));
		} else if (operand.type.isA(sem, sem.types().boolType())) {
			this.type = sem.types().boolClass();
			this.newType = new NewExpression(new ResolvableSymbol(
					new Identifier("Boolean", new Position(0, 0)), null));
		} else {
			assert false;
		}

		this.newType = this.newType.refPass(sem);
	}

	@Override
	public void print(TreeStream tree) {
		tree.println("BOX"
				+ (this.type == null ? "" : " : " + this.type.name()));
		tree.indent();
		this.operand.print(tree);
		tree.unindent();
	}

	@Override
	public void generateCode(CodeStream code) {
		this.newType.generateCode(code);
		this.operand.generateCode(code);

		code.println("; BOX");
		code.println("MRM R5, (R2) ; Wert vom Stapel nehmen");
		code.println("SUB R2, R1");
		code.println("MRM R6, (R2) ; Referenz auf neues Objekt holen (bleibt auf Stapel)");
		code.println("MRI R7, " + ClassSymbol.HEADERSIZE());
		code.println("ADD R6, R7 ; Speicherstelle in neuem Objekt berechnen");
		code.println("MMR (R6), R5 ; Wert in Objekt speichern");
	}
}