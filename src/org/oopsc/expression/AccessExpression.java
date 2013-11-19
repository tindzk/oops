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
	public Expression refPass(SemanticAnalysis sem) throws CompileException {
		// TODO refactor
		this.leftOperand = this.leftOperand.refPass(sem);

		// Dereferenzieren. Außerdem könnte man einen Ausdruck wie z.B. 5.print
		// schreiben, wenn Integer Methoden hätte.
		this.leftOperand = this.leftOperand.box(sem);

		// Der rechte Operand hat einen Deklarationsraum, der sich aus dem
		// Ergebnistyp des linken Operanden ergibt.
		this.rightOperand.contextAnalysisForMember(this.leftOperand.type);

		/* Contextual analysis for arguments, but with the original declaration context. */
		this.rightOperand.contextAnalysisForArguments(sem);

		// Der Typ dieses Ausdrucks ist immer der des rechten Operanden.
		this.type = this.rightOperand.type;
		this.lValue = this.rightOperand.lValue;

		/* Deal with accesses to methods or attributes in the base class. */
		if (this.leftOperand instanceof DeRefExpression) {
			if (((DeRefExpression) this.leftOperand).operand instanceof VarOrCall) {
				VarOrCall call = (VarOrCall) ((DeRefExpression) this.leftOperand).operand;

				if (call.ref.identifier().name().equals("_base")) {
					VariableSymbol vdec = (VariableSymbol) call.ref
							.declaration().get();
					this.rightOperand.setStaticContext(vdec.resolvedType()
							.get());
				}
			}
		}

		return this;
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
		this.leftOperand.generateCode(code);
		this.rightOperand.generateCode(code);
	}
}