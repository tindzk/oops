package org.oopsc.statement;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import org.oopsc.ClassDeclaration;
import org.oopsc.CodeStream;
import org.oopsc.CompileException;
import org.oopsc.Declarations;
import org.oopsc.Position;
import org.oopsc.TreeStream;
import org.oopsc.expression.LiteralExpression;

/**
 * Implements a TRY statement which is needed for exception handling.
 */
public class TryStatement extends Statement {
	/** All statements associated with the TRY block. */
	public List<Statement> tryStatements = new LinkedList<>();

	/** Position in the source code associated with this statement. */
	public Position position;

	/**
	 * CATCH branches assigning a statement block to a value that needs to be caught in order for
	 * the statements to be executed.
	 */
	public Map<LiteralExpression, List<Statement>> catchStatements = new LinkedHashMap<>();

	/**
	 * Konstruktor.
	 *
	 * @param condition
	 *        Die Bedingung der IF-Anweisung.
	 * @param position
	 *        The position in the source code.
	 */
	public TryStatement(List<Statement> tryStatements, Position position) {
		this.tryStatements = tryStatements;
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
		for (Statement s : this.tryStatements) {
			s.contextAnalysis(declarations);
		}

		for (Entry<LiteralExpression, List<Statement>> entry : this.catchStatements
				.entrySet()) {
			LiteralExpression expr = entry.getKey();
			expr.type.check(ClassDeclaration.intType, expr.position);

			for (Statement s : entry.getValue()) {
				s.contextAnalysis(declarations);
			}
		}

		if (this.catchStatements.size() == 0) {
			throw new CompileException(
					"At least one catch block is required in a TRY statement.",
					this.position);
		}
	}

	/**
	 * Adds a CATCH block.
	 *
	 * @param condition
	 * @param stmts
	 *        List of statements.
	 */
	public void addCatchBlock(LiteralExpression condition, List<Statement> stmts) {
		this.catchStatements.put(condition, stmts);
	}

	/**
	 * Die Methode gibt diese Anweisung in einer Baumstruktur aus.
	 *
	 * @param tree
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	@Override
	public void print(TreeStream tree) {
		tree.println("TRY");

		if (!this.tryStatements.isEmpty()) {
			tree.indent();

			for (Statement s : this.tryStatements) {
				s.print(tree);
			}

			tree.unindent();
		}

		for (Entry<LiteralExpression, List<Statement>> entry : this.catchStatements.entrySet()) {
			tree.println("CATCH");
			tree.indent();

			entry.getKey().print(tree);

			for (Statement s : entry.getValue()) {
				s.print(tree);
			}

			tree.unindent();
		}
	}

	/**
	 * Die Methode generiert den Assembler-Code für diese Anweisung. Sie geht
	 * davon aus, dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
	 *
	 * We create a new exception frame on the stack. An exception frame consists of
	 * two variables: the address of the last exception frame and the address where
	 * to continue the execution when an exception was thrown.
	 *
	 * @param code
	 *        Der Strom, in den die Ausgabe erfolgt.
	 * @param contexts
	 *        Current stack of contexts, may be used to inject instructions for
	 *        unwinding the stack (as needed for RETURN statements in TRY blocks).
	 */
	@Override
	public void generateCode(CodeStream code, Stack<Context> contexts) {
		code.println("; TRY");

		/* Push the frame pointer on the stack as we will need to restore it later. */
		code.println("ADD R2, R1");
		code.println("MMR (R2), R3");

		String catchLabel = code.nextLabel();

		/* Push address to the exception handler on the stack, denotes at the same
		 * time the beginning of our new exception frame. */
		code.println("MRI R5, " + catchLabel);
		code.println("ADD R2, R1");
		code.println("MMR (R2), R5");

		/* Push the address pointing to the current exception frame on the stack. */
		code.println("MRI R5, _currentExceptionFrame");
		code.println("MRM R5, (R5)"); /* Dereference the value. */
		code.println("ADD R2, R1");
		code.println("MMR (R2), R5");

		/* Overwrite the global exception frame pointer with the address of the catch label.
		 * R5 = address of the global variable _currentExceptionFrame
		 * R6 = address of our new current exception frame */
		code.println("MRR R6, R2");
		code.println("SUB R6, R1");
		code.println("MRI R5, _currentExceptionFrame");
		code.println("MMR (R5), R6");

		String endLabel = code.nextLabel();

		contexts.push(Context.TryBlock);

		for (Statement s : this.tryStatements) {
			s.generateCode(code, contexts);
		}

		contexts.pop();

		/* This instruction is only reached if no exception was thrown. */
		code.println("MRI R0, " + endLabel);

		for (Entry<LiteralExpression, List<Statement>> entry : this.catchStatements
				.entrySet()) {
			/* An exception was thrown. */
			code.println("; CATCH " + entry.getKey().value);
			code.println(catchLabel + ":");
			catchLabel = code.nextLabel();

			/* When an exception is thrown, the associated error code is stored in R7. */
			code.println("MRI R5, " + entry.getKey().value);
			code.println("SUB R5, R7");

			/* If entry.getKey().value != error code, jump to the next `catch' branch. */
			code.println("ISZ R5, R5");
			code.println("XOR R5, R1");
			code.println("JPC R5, " + catchLabel);

			/* The exception was caught. Therefore, pop the exception off the stack
			 * before executing the statements. */
			popException(code, true);

			for (Statement stmt : entry.getValue()) {
				stmt.generateCode(code, contexts);
			}

			/* Jump to the end of the TRY block. */
			code.println("MRI R0, " + endLabel);
			code.println("; END CATCH");
		}

		/* The exception could not be dealt with. */
		code.println(catchLabel + ":");

		/* Pop the exception off the stack, restoring the stack frame pointer. */
		popException(code, true);

		/* Propagate the exception to the next exception handler. */
		ThrowStatement.throwException(code);

		code.println("; END TRY");
		code.println(endLabel + ":");
	}

	/**
	 * After the exception frame allocated by a TRY block is not used anymore, this
	 * method must be called to unwind the stack to its state before.
	 */
	public static void popException(CodeStream code, boolean restoreStackFp) {
		/* Load current exception frame and dereference it. */
		code.println("MRI R6, _currentExceptionFrame");
		code.println("MRM R6, (R6)");

		/* Fix up the stack by making it point to the position right before the
		 * exception frame. */
		code.println("MRR R2, R6");
		code.println("SUB R2, R1");

		if (restoreStackFp) {
			code.println("MRM R3, (R2)");
		}

		code.println("SUB R2, R1");

		/* Load the previous exception frame pointer into R6. */
		code.println("ADD R6, R1");
		code.println("MRM R6, (R6)"); /* Dereference value. */

		/* Load the pointer to the global variable _currentExceptionFrame into R5. */
		code.println("MRI R5, _currentExceptionFrame");

		/* Make the global exception frame marker point to the previous exception
		 * frame pointer. */
		code.println("MMR (R5), R6");
	}
}