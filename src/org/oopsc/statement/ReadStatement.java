package org.oopsc.statement;
import java.util.Stack;

import org.oopsc.ClassDeclaration;
import org.oopsc.CodeStream;
import org.oopsc.CompileException;
import org.oopsc.Declarations;
import org.oopsc.ResolvableIdentifier;
import org.oopsc.TreeStream;
import org.oopsc.expression.Expression;
import org.oopsc.expression.NewExpression;

/**
 * Die Klasse repräsentiert die Anweisung READ im Syntaxbaum.
 */
public class ReadStatement extends Statement {
	/** Die Variable, in der das eingelesene Zeichen gespeichert wird. */
	public Expression operand;

	/** Ein Ausdruck, der ein neues Objekt vom Typ Integer erzeugen kann. */
	public Expression newInt = new NewExpression(new ResolvableIdentifier("Integer",
			null), null);

	/**
	 * Konstruktor.
	 *
	 * @param operand
	 *        Die Variable, in der das eingelesene Zeichen gespeichert wird.
	 */
	public ReadStatement(Expression operand) {
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
		if (!this.operand.lValue) {
			throw new CompileException("L-Wert erwartet", this.operand.position);
		}
		this.operand.type.check(ClassDeclaration.intClass, this.operand.position);
		this.newInt = this.newInt.contextAnalysis(declarations);
	}

	/**
	 * Die Methode gibt diese Anweisung in einer Baumstruktur aus.
	 *
	 * @param tree
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	@Override
	public void print(TreeStream tree) {
		tree.println("READ");
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
		// TODO refactor
		code.println("; READ lvalue ablegen");
		this.operand.generateCode(code);

		code.println("; READ Speicher allokieren");
		this.newInt.generateCode(code);

		code.println("; READ");
		code.println("MRM R5, (R2)"); // R2 zeigt auf ein boxed Integer

		/* Skip header. */
		code.println("MRI R6, " + ClassDeclaration.HEADERSIZE);
		code.println("ADD R5, R6");

		code.println("SYS 0, 6 ; Gelesenen Wert in R6 ablegen");
		code.println("MMR (R5), R6 ; Zeichen in neuen Integer schreiben");
		code.println("MRM R5, (R2) ; Neuen Integer vom Stapel entnehmen");
		code.println("SUB R2, R1");
		code.println("MRM R6, (R2) ; Ziel vom Stapel entnehmen");
		code.println("SUB R2, R1");
		code.println("MMR (R6), R5 ; Zuweisen");
	}
}