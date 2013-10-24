package org.oopsc.statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;

import org.oopsc.ClassDeclaration;
import org.oopsc.CodeStream;
import org.oopsc.CompileException;
import org.oopsc.Declarations;
import org.oopsc.TreeStream;
import org.oopsc.expression.Expression;

/**
 * Die Klasse repräsentiert die Anweisung IF-THEN im Syntaxbaum.
 */
public class IfStatement extends Statement {
	/** Die Bedingung der IF-Anweisung. */
	public Expression condition;

	/** Die Anweisungen im THEN-Teil. */
	public List<Statement> thenStatements = new LinkedList<>();

	/** Die ELSE-IF-Anweisungen und der ELSE-Block. */
	public HashMap<Expression, List<Statement>> elseStatements = new HashMap<>();

	/**
	 * Konstruktor.
	 *
	 * @param condition
	 *        Die Bedingung der IF-Anweisung.
	 */
	public IfStatement(Expression condition, List<Statement> thenStatements) {
		this.condition = condition;
		this.thenStatements = thenStatements;
		this.elseStatements.put(null, new LinkedList<Statement>());
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
		this.condition = this.condition.contextAnalysis(declarations);

		/* this.condition.type is either boolType or boolClass. Enforce boolType via unboxing. */
		this.condition = this.condition.unBox();
		this.condition.type.check(ClassDeclaration.boolType,
				this.condition.position);

		for (Statement s : this.thenStatements) {
			s.contextAnalysis(declarations);
		}

		HashMap<Expression, List<Statement>> newElseStatements = new HashMap<>();

		for (Entry<Expression, List<Statement>> entry : this.elseStatements
				.entrySet()) {
			Expression cond = entry.getKey();

			if (cond != null) {
				cond = cond.contextAnalysis(declarations);
				cond = cond.unBox();
				cond.type.check(ClassDeclaration.boolType, cond.position);
			}

			newElseStatements.put(cond, entry.getValue());

			for (Statement s : entry.getValue()) {
				s.contextAnalysis(declarations);
			}
		}

		this.elseStatements = newElseStatements;
	}

	/**
	 * @param condition
	 * @param stmts
	 */
	public void addIfElse(Expression condition, List<Statement> stmts) {
		this.elseStatements.put(condition, stmts);
	}

	/**
	 * @param stmts
	 */
	public void setElse(List<Statement> stmts) {
		this.elseStatements.put(null, stmts);
	}

	private void print(TreeStream tree, Expression condition,
			List<Statement> stmts) {
		tree.indent();

		if (condition != null) {
			condition.print(tree);
		} else {
			tree.println("ELSE");
		}

		if (!stmts.isEmpty()) {
			if (condition != null) {
				tree.println("THEN");
			}

			tree.indent();

			for (Statement s : stmts) {
				s.print(tree);
			}

			tree.unindent();
		}

		tree.unindent();
	}

	/**
	 * Die Methode gibt diese Anweisung in einer Baumstruktur aus.
	 *
	 * @param tree
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	@Override
	public void print(TreeStream tree) {
		tree.println("IF");
		this.print(tree, this.condition, this.thenStatements);

		for (Entry<Expression, List<Statement>> entry : this.elseStatements
				.entrySet()) {
			if (entry.getKey() == null) {
				continue;
			}

			this.print(tree, entry.getKey(), entry.getValue());
		}

		List<Statement> elseStmts = this.elseStatements.get(null);

		if (elseStmts.size() != 0) {
			this.print(tree, null, elseStmts);
		}
	}

	private void generateCode(CodeStream code, Stack<Context> contexts,
			Expression condition, List<Statement> stmts, String nextLabel,
			String endLabel) {
		condition.generateCode(code);

		code.println("MRM R5, (R2) ; Bedingung vom Stapel nehmen");
		code.println("SUB R2, R1");
		code.println("ISZ R5, R5 ; Wenn 0, dann");
		code.println("JPC R5, " + nextLabel
				+ " ; Sprung zu END IF bzw. nächstem ELSEIF/ELSE");
		code.println("; THEN");

		for (Statement s : stmts) {
			s.generateCode(code, contexts);
		}

		code.println("MRI R0, " + endLabel + " ; Sprung zu END IF");
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
		code.println("; IF");

		String endLabel = code.nextLabel();
		String nextLabel = code.nextLabel();

		this.generateCode(code, contexts, this.condition, this.thenStatements,
				nextLabel, endLabel);

		for (Entry<Expression, List<Statement>> entry : this.elseStatements
				.entrySet()) {
			if (entry.getKey() == null) {
				/* Deal with ELSE block separately. */
				continue;
			}

			code.println("; ELSEIF");
			code.println(nextLabel + ":");

			nextLabel = code.nextLabel();

			this.generateCode(code, contexts, entry.getKey(), entry.getValue(),
					nextLabel, endLabel);

			code.println("; END ELSEIF");
		}

		code.println(nextLabel + ":");

		List<Statement> elseStmts = this.elseStatements.get(null);

		if (elseStmts.size() != 0) {
			code.println("; ELSE");

			for (Statement s : elseStmts) {
				s.generateCode(code, contexts);
			}

			code.println("; END ELSE");
		}

		code.println("; END IF");
		code.println(endLabel + ":");
	}
}