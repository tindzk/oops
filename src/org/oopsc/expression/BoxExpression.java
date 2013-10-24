package org.oopsc.expression;

import org.oopsc.ClassDeclaration;
import org.oopsc.CodeStream;
import org.oopsc.CompileException;
import org.oopsc.Declarations;
import org.oopsc.ResolvableIdentifier;
import org.oopsc.TreeStream;

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
	 * @param declarations
	 *        Die an dieser Stelle gültigen Deklarationen.
	 * @throws CompileException
	 *         Während der Kontextanylyse des neuen Objekts
	 *         wurde ein Fehler gefunden.
	 */
	public BoxExpression(Expression operand, Declarations declarations)
			throws CompileException {
		super(operand.position);
		this.operand = operand;

		if (operand.type.isA(ClassDeclaration.intType)) {
			this.type = ClassDeclaration.intClass;
			this.newType = new NewExpression(new ResolvableIdentifier(
					"Integer", null), null);
		} else if (operand.type.isA(ClassDeclaration.boolType)) {
			this.type = ClassDeclaration.boolClass;
			this.newType = new NewExpression(new ResolvableIdentifier(
					"Boolean", null), null);
		} else {
			assert false;
		}

		this.newType = this.newType.contextAnalysis(declarations);
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
		tree.println("BOX"
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
		this.newType.generateCode(code);
		this.operand.generateCode(code);

		code.println("; BOX");
		code.println("MRM R5, (R2) ; Wert vom Stapel nehmen");
		code.println("SUB R2, R1");
		code.println("MRM R6, (R2) ; Referenz auf neues Objekt holen (bleibt auf Stapel)");
		code.println("MRI R7, " + ClassDeclaration.HEADERSIZE);
		code.println("ADD R6, R7 ; Speicherstelle in neuem Objekt berechnen");
		code.println("MMR (R6), R5 ; Wert in Objekt speichern");
	}
}