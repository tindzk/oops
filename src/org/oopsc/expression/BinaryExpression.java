package org.oopsc.expression;

import org.oopsc.*;
import org.oopsc.symbol.ClassSymbol;

/**
 * Die Klasse repräsentiert einen Ausdruck mit einem binären Operator im Syntaxbaum.
 */
public class BinaryExpression extends Expression {
	public enum Operator {
		EQ, NEQ, GT, GTEQ, LT, LTEQ, PLUS, MINUS, MUL, DIV, MOD, AND, OR, NOT
	}

	/** Der linke Operand. */
	public Expression leftOperand;

	/** Der Operator. */
	public Operator operator;

	/** Der rechte Operand. */
	public Expression rightOperand;

	/**
	 * Konstruktor.
	 *
	 * @param operator
	 *        Der Operator.
	 * @param leftOperand
	 *        Der linke Operand.
	 * @param rightOperand
	 *        Der rechte Operand.
	 */
	public BinaryExpression(Expression leftOperand, Operator operator,
			Expression rightOperand) {
		super(leftOperand.position);
		this.leftOperand = leftOperand;
		this.operator = operator;
		this.rightOperand = rightOperand;
	}

	@Override
	public Expression refPass(SemanticAnalysis sem) throws CompileException {
		// TODO refactor
		this.leftOperand = this.leftOperand.refPass(sem);
		this.rightOperand = this.rightOperand.refPass(sem);
		switch (this.operator) {
			case AND:
			case OR:
				this.leftOperand = this.leftOperand.unBox(sem);
				this.rightOperand = this.rightOperand.unBox(sem);
				this.leftOperand.type.check(sem, sem.types().boolType(),
						this.leftOperand.position);
				this.rightOperand.type.check(sem, sem.types().boolType(),
						this.rightOperand.position);
				this.type = sem.types().boolType();
				break;
			case PLUS:
			case MINUS:
			case MUL:
			case DIV:
			case MOD:
				this.leftOperand = this.leftOperand.unBox(sem);
				this.rightOperand = this.rightOperand.unBox(sem);
				this.leftOperand.type.check(sem, sem.types().intType(),
						this.leftOperand.position);
				this.rightOperand.type.check(sem, sem.types().intType(),
						this.rightOperand.position);
				this.type = sem.types().intType();
				break;
			case GT:
			case GTEQ:
			case LT:
			case LTEQ:
				this.leftOperand = this.leftOperand.unBox(sem);
				this.rightOperand = this.rightOperand.unBox(sem);
				this.leftOperand.type.check(sem, sem.types().intType(),
						this.leftOperand.position);
				this.rightOperand.type.check(sem, sem.types().intType(),
						this.rightOperand.position);
				this.type = sem.types().boolType();
				break;
			case EQ:
			case NEQ:
				// Wenn einer der beiden Operanden NULL ist, muss der andere
				// ein Objekt sein (oder auch NULL)
				if (this.leftOperand.type == sem.types().nullType()) {
					this.rightOperand = this.rightOperand.box(sem);
				} else if (this.rightOperand.type == sem.types().nullType()) {
					this.leftOperand = this.leftOperand.box(sem);
				} else {
					// ansonsten wird versucht, die beiden Operanden in
					// Basisdatentypen zu wandeln
					this.leftOperand = this.leftOperand.unBox(sem);
					this.rightOperand = this.rightOperand.unBox(sem);
				}

				// Nun muss der Typ mindestens eines Operanden gleich oder eine
				// Ableitung des Typs des anderen Operanden sein.
				if (!this.leftOperand.type.isA(sem, this.rightOperand.type)
						&& !this.rightOperand.type.isA(sem,
								this.leftOperand.type)) {
					ClassSymbol.typeError(this.leftOperand.type,
							this.rightOperand.type, this.rightOperand.position);
				}
				this.type = sem.types().boolType();
				break;
			default:
				assert false;
		}
		return this;
	}

	@Override
	public void print(TreeStream tree) {
		tree.println(this.operator
				+ (this.type == null ? "" : " : " + this.type.name()));
		tree.indent();
		this.leftOperand.print(tree);
		this.rightOperand.print(tree);
		tree.unindent();
	}

	@Override
	public void generateCode(CodeStream code) {
		this.leftOperand.generateCode(code);
		this.rightOperand.generateCode(code);

		code.println("; " + this.operator);
		code.println("MRM R5, (R2)");
		code.println("SUB R2, R1");
		code.println("MRM R6, (R2)");

		switch (this.operator) {
			case AND:
				code.println("AND R6, R5");
			case OR:
				code.println("OR R6, R5");
			case PLUS:
				code.println("ADD R6, R5");
				break;
			case MINUS:
				code.println("SUB R6, R5");
				break;
			case MUL:
				code.println("MUL R6, R5");
				break;
			case DIV:
				code.println("DIV R6, R5");
				break;
			case MOD:
				code.println("MOD R6, R5");
				break;
			case GT:
				code.println("SUB R6, R5");
				code.println("ISP R6, R6");
				break;
			case GTEQ:
				code.println("SUB R6, R5");
				code.println("ISN R6, R6");
				code.println("XOR R6, R1");
				break;
			case LT:
				code.println("SUB R6, R5");
				code.println("ISN R6, R6");
				break;
			case LTEQ:
				code.println("SUB R6, R5");
				code.println("ISP R6, R6");
				code.println("XOR R6, R1");
				break;
			case EQ:
				code.println("SUB R6, R5");
				code.println("ISZ R6, R6");
				break;
			case NEQ:
				code.println("SUB R6, R5");
				code.println("ISZ R6, R6");
				code.println("XOR R6, R1");
				break;
			default:
				assert false;
		}

		code.println("MMR (R2), R6");
	}
}