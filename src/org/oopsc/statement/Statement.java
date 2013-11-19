package org.oopsc.statement;

import java.util.Stack;

import org.oopsc.CodeStream;
import org.oopsc.CompileException;
import org.oopsc.SemanticAnalysis;
import org.oopsc.TreeStream;

/**
 * Die abstrakte Basisklasse für alle Anweisungen im Syntaxbaum.
 */
abstract public class Statement {
	public enum Context {
		Default, TryBlock
	}

	/**
	 * Die Methode führt die Kontextanalyse für diese Anweisung durch.
	 *
	 * @param sem
	 *        Die an dieser Stelle gültigen Deklarationen.
	 * @throws CompileException
	 *         Während der Kontextanylyse wurde ein Fehler
	 *         gefunden.
	 */
	public void defPass(SemanticAnalysis sem) throws CompileException {

	}

	public void refPass(SemanticAnalysis sem) throws CompileException {

	}

	/**
	 * Die Methode gibt diese Anweisung in einer Baumstruktur aus.
	 *
	 * @param tree
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	abstract public void print(TreeStream tree);

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
	abstract public void generateCode(CodeStream code, Stack<Context> contexts);
}