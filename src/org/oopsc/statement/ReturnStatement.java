package org.oopsc.statement;

import java.util.Stack;

import org.oopsc.*;
import org.oopsc.symbol.*;
import org.oopsc.expression.*;

/**
 * Die Klasse repräsentiert die Anweisung RETURN im Syntaxbaum.
 */
public class ReturnStatement extends Statement {
	/** The value to be returned. */
	public Expression value = null;

	/** Die Quelltextposition, an der dieser Ausdruck beginnt. */
	public Position position;

	protected MethodSymbol method = null;

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

	@Override
	public void refPass(SemanticAnalysis sem) throws CompileException {
		this.method = sem.currentMethod();

		ClassSymbol retType = sem.currentMethod().getResolvedReturnType();

		if (this.value == null) {
			if (retType != Types.voidType()) {
				throw new CompileException("Return value of type "
						+ retType.name() + " expected.", this.position);
			}

		} else {
			this.value.refPass(sem);

			if (retType == Types.voidType()) {
				throw new CompileException("No return value expected.",
						this.value.position);
			}

			this.value.type.check(sem, retType, this.value.position);
		}
	}

	@Override
	public void print(TreeStream tree) {
		tree.println("RETURN");

		if (this.value != null) {
			tree.indent();
			this.value.print(tree);
			tree.unindent();
		}
	}

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
			this.value.generateCode(code, true);

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