package org.oopsc.statement;
import java.util.Stack;

import org.oopsc.ClassDeclaration;
import org.oopsc.CodeStream;
import org.oopsc.CompileException;
import org.oopsc.Declarations;
import org.oopsc.TreeStream;
import org.oopsc.expression.Expression;

/**
 * Die Klasse repräsentiert die Anweisung WRITE im Syntaxbaum.
 */
public class WriteStatement extends Statement {
	/** Der Ausdruck, der als ein Zeichen ausgegeben wird. */
	public Expression operand;

	/**
	 * Konstruktor.
	 *
	 * @param operand
	 *        Der Ausdruck, der als ein Zeichen ausgegeben wird.
	 */
	public WriteStatement(Expression operand) {
		this.operand = operand;
	}

	/**
	 * Die Methode führt die Kontextanalyse für diese Anweisung durch.
	 *
	 * @param declarations
	 *        Die an dieser Stelle gültigen Deklarationen.
	 * @throws CompileException
	 *         Während der Kontextanylyse wurde ein Fehler
	 *         gefunden.
	 */
	@Override
	public void contextAnalysis(Declarations declarations) throws CompileException {
		this.operand = this.operand.contextAnalysis(declarations);
		this.operand = this.operand.unBox();
		this.operand.type.check(ClassDeclaration.intType, this.operand.position);
	}

	/**
	 * Die Methode gibt diese Anweisung in einer Baumstruktur aus.
	 *
	 * @param tree
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	@Override
	public void print(TreeStream tree) {
		tree.println("WRITE");
		tree.indent();
		this.operand.print(tree);
		tree.unindent();
	}

	/**
	 * Die Methode generiert den Assembler-Code für diese Anweisung. Sie geht
	 * davon aus, dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
	 *
	 * @param code
	 *        Der Strom, in den die Ausgabe erfolgt.
	 * @param contexts
	 *        Current stack of contexts, may be used to inject instructions for
	 *        unwinding the stack (as needed for RETURN statements in TRY blocks).
	 */
	@Override
	public void generateCode(CodeStream code, Stack<Context> contexts) {
		code.println("; WRITE operand code");
		this.operand.generateCode(code);
		code.println("; WRITE");
		code.println("MRM R5, (R2)");
		code.println("SUB R2, R1");
		code.println("SYS 1, 5");
	}
}