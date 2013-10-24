package org.oopsc.expression;

import org.oopsc.ClassDeclaration;
import org.oopsc.CodeStream;
import org.oopsc.CompileException;
import org.oopsc.Declarations;
import org.oopsc.TreeStream;
import org.oopsc.VarDeclaration;

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

	/**
	 * Die Methode führt die Kontextanalyse für diesen Ausdruck durch.
	 *
	 * @param declarations
	 *        Die an dieser Stelle gültigen Deklarationen.
	 * @return Dieser Ausdruck.
	 * @throws CompileException
	 *         Während der Kontextanylyse wurde ein Fehler
	 *         gefunden.
	 */
	@Override
	public Expression contextAnalysis(Declarations declarations)
			throws CompileException {
		// TODO refactor
		this.leftOperand = this.leftOperand.contextAnalysis(declarations);

		// Dereferenzieren. Außerdem könnte man einen Ausdruck wie z.B. 5.print
		// schreiben, wenn Integer Methoden hätte.
		this.leftOperand = this.leftOperand.box(declarations);

		// Der rechte Operand hat einen Deklarationsraum, der sich aus dem
		// Ergebnistyp des linken Operanden ergibt.
		this.rightOperand
				.contextAnalysisForMember(this.leftOperand.type.declarations);

		/* Contextual analysis for arguments, but with the original declaration context. */
		this.rightOperand.contextAnalysisForArguments(declarations);

		// Der Typ dieses Ausdrucks ist immer der des rechten Operanden.
		this.type = this.rightOperand.type;
		this.lValue = this.rightOperand.lValue;

		/* Deal with accesses to methods or attributes in the base class. */
		if (this.leftOperand instanceof DeRefExpression) {
			if (((DeRefExpression) this.leftOperand).operand instanceof VarOrCall) {
				VarOrCall call = (VarOrCall) ((DeRefExpression) this.leftOperand).operand;

				if (call.identifier.name.equals("_base")) {
					VarDeclaration vdec = (VarDeclaration) call.identifier.declaration;

					this.rightOperand
							.setStaticContext((ClassDeclaration) vdec.type.declaration);
				}
			}
		}

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
		tree.println("PERIOD"
				+ (this.type == null ? "" : " : " + (this.lValue ? "REF " : "")
						+ this.type.identifier.name));
		tree.indent();
		this.leftOperand.print(tree);
		this.rightOperand.print(tree);
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
		this.leftOperand.generateCode(code);
		this.rightOperand.generateCode(code);
	}
}