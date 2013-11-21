package org.oopsc.statement;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;

import org.oopsc.*;
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

	public void defPass(SemanticAnalysis sem) throws CompileException {
		for (Statement s : this.thenStatements) {
			s.defPass(sem);
		}

		for (Entry<Expression, List<Statement>> entry : this.elseStatements
				.entrySet()) {
			for (Statement s : entry.getValue()) {
				s.defPass(sem);
			}
		}
	}

	@Override
	public void refPass(SemanticAnalysis sem) throws CompileException {
		this.condition = this.condition.refPass(sem);

		/* this.condition.type is either boolType or boolClass. Enforce boolType via unboxing. */
		this.condition.types = sem.types();
		this.condition.type.check(sem, sem.types().boolType(),
				this.condition.position);

		for (Statement s : this.thenStatements) {
			s.refPass(sem);
		}

		// TODO not necessary
		HashMap<Expression, List<Statement>> newElseStatements = new HashMap<>();

		for (Entry<Expression, List<Statement>> entry : this.elseStatements
				.entrySet()) {
			Expression cond = entry.getKey();

			if (cond != null) {
				cond = cond.refPass(sem);
				cond.types = sem.types();
				cond.type.check(sem, sem.types().boolType(), cond.position);
			}

			newElseStatements.put(cond, entry.getValue());

			for (Statement s : entry.getValue()) {
				s.refPass(sem);
			}
		}

		this.elseStatements = newElseStatements;
	}

	public void addIfElse(Expression condition, List<Statement> stmts) {
		this.elseStatements.put(condition, stmts);
	}

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
		condition.generateCode(code, false);

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