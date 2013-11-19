package org.oopsc.statement;

import java.util.Stack;

import org.oopsc.*;
import org.oopsc.expression.*;
import org.oopsc.symbol.*;

/**
 * Die Klasse repräsentiert die Anweisung READ im Syntaxbaum.
 */
public class ReadStatement extends Statement {
	/** Die Variable, in der das eingelesene Zeichen gespeichert wird. */
	public Expression operand;

	/** Ein Ausdruck, der ein neues Objekt vom Typ Integer erzeugen kann. */
	public Expression newInt = new NewExpression(new ResolvableSymbol(
			new Identifier("Integer", new Position(0, 0)), null));

	/**
	 * Konstruktor.
	 *
	 * @param operand
	 *        Die Variable, in der das eingelesene Zeichen gespeichert wird.
	 */
	public ReadStatement(Expression operand) {
		this.operand = operand;
	}

	@Override
	public void refPass(SemanticAnalysis sem) throws CompileException {
		this.operand = this.operand.refPass(sem);

		if (!this.operand.lValue) {
			throw new CompileException("Lvalue expected", this.operand.position);
		}

		this.operand.type.check(sem, sem.types().intClass(),
				this.operand.position);
		this.newInt = this.newInt.refPass(sem);
	}

	@Override
	public void print(TreeStream tree) {
		tree.println("READ");
		tree.indent();
		this.operand.print(tree);
		tree.unindent();
	}

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
		code.println("MRI R6, " + ClassSymbol.HEADERSIZE());
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