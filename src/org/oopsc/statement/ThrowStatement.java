package org.oopsc.statement;

import java.util.Stack;

import org.oopsc.*;
import org.oopsc.expression.Expression;

/**
 * Statement for triggering an exception.
 */
public class ThrowStatement extends Statement {
	/** The integer value to be thrown. */
	public Expression value;

	/** Position in the source code associated with this statement. */
	public Position position;

	/**
	 * Constructor.
	 *
	 * @param value
	 *        An integer value that will be thrown.
	 * @param position
	 *        The position in the source code.
	 */
	public ThrowStatement(Expression value, Position position) {
		this.value = value;
		this.position = position;
	}

	@Override
	public void refPass(SemanticAnalysis sem) throws CompileException {
		this.value.refPass(sem);
		this.value.type.check(sem, Types.intType(), this.value.position);
	}

	@Override
	public void print(TreeStream tree) {
		tree.println("THROW");
		tree.indent();
		this.value.print(tree);
		tree.unindent();
	}

	public static void throwException(CodeStream code) {
		/* Load the pointer to the global variable _currentExceptionFrame into R5. */
		code.println("MRI R5, _currentExceptionFrame");

		/* Dereference the value, i.e., load the exception frame. */
		code.println("MRM R5, (R5)");

		/* Load the address of the exception handler from the current exception frame
		 * and jump to it. */
		code.println("MRM R0, (R5)");
	}

	@Override
	public void generateCode(CodeStream code, Stack<Context> contexts) {
		code.println("; THROW");

		/* Push the exception code on the stack. */
		this.value.generateCode(code, false);

		/* Copy the exception code to R7. */
		code.println("MRM R7, (R2)");

		/* Pop the exception code from the stack. */
		code.println("SUB R2, R1");

		/* Pass the exception to the inner-most exception handler and propagate it if
		 * necessary. */
		throwException(code);

		code.println("; END THROW");
	}
}