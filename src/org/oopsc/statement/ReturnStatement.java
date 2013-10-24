package org.oopsc.statement;
import java.util.Stack;

import org.oopsc.ClassDeclaration;
import org.oopsc.CodeStream;
import org.oopsc.CompileException;
import org.oopsc.Declarations;
import org.oopsc.MethodDeclaration;
import org.oopsc.Position;
import org.oopsc.TreeStream;
import org.oopsc.expression.Expression;

/**
 * Die Klasse repräsentiert die Anweisung RETURN im Syntaxbaum.
 */
public class ReturnStatement extends Statement {
	/** The value to be returned. */
	public Expression value = null;

	/** Die Quelltextposition, an der dieser Ausdruck beginnt. */
	public Position position;

	protected MethodDeclaration method = null;

	/**
	 * Constructor.
	 */
	public ReturnStatement(Position position) {
		this.position = position;
	}

	/**
	 * Constructor.
	 *
	 * @param value
	 *        The return value.
	 */
	public ReturnStatement(Expression value, Position position) {
		this.value = value;
		this.position = position;
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
		this.method = declarations.currentMethod;

		ClassDeclaration retType = declarations.currentMethod
				.getResolvedReturnType();

		if (this.value == null) {
			if (retType != ClassDeclaration.voidType) {
				throw new CompileException("Return value of type "
						+ retType.identifier.name + " expected.", this.position);
			}

		} else {
			this.value = this.value.contextAnalysis(declarations);
			this.value = this.value.box(declarations);

			if (retType == ClassDeclaration.voidType) {
				throw new CompileException("No return value expected.",
						this.value.position);
			}

			this.value.type.check(retType, this.value.position);
		}
	}

	/**
	 * Die Methode gibt diese Anweisung in einer Baumstruktur aus.
	 *
	 * @param tree
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	@Override
	public void print(TreeStream tree) {
		tree.println("RETURN");

		if (this.value != null) {
			tree.indent();
			this.value.print(tree);
			tree.unindent();
		}
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
		code.println("; RETURN");

		if (this.value == null) {
			/* For each RETURN statement within a TRY block, we need to unwind the stack
			 * accordingly. */
			for (Context c : contexts) {
				if (c == Context.TryBlock) {
					TryStatement.popException(code, false);
				}
			}

			this.method.generateMethodEpilogue(code, "");
		} else {
			this.value.generateCode(code);

			/* Back up the value R2 points to by copying it to the register R7. R2 points
			 * to the result of this.value. */
			code.println("MRM R7, (R2)");

			/* For each RETURN statement within a TRY block, we need to unwind the stack
			 * accordingly. */
			for (Context c : contexts) {
				if (c == Context.TryBlock) {
					TryStatement.popException(code, false);
				}
			}

			/* The epilogue modifies R2 by making it point to its original value before
			 * the method call. Inject the following instruction to restore our copy of
			 * the return value in the register R7. */
			String customInstruction = "MMR (R2), R7";
			this.method.generateMethodEpilogue(code, customInstruction);
		}

		code.println("; END RETURN");
	}
}