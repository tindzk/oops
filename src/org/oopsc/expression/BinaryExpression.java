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
				this.leftOperand.type.check(sem, Types.boolType(),
						this.leftOperand.position);
				this.rightOperand.type.check(sem, Types.boolType(),
						this.rightOperand.position);
				this.type = Types.boolType();
				break;

			case PLUS:
			case MINUS:
			case MUL:
			case DIV:
			case MOD:
				this.leftOperand.type.check(sem, Types.intType(),
						this.leftOperand.position);
				this.rightOperand.type.check(sem, Types.intType(),
						this.rightOperand.position);
				this.type = Types.intType();
				break;

			case GT:
			case GTEQ:
			case LT:
			case LTEQ:
				this.leftOperand.type.check(sem, Types.intType(),
						this.leftOperand.position);
				this.rightOperand.type.check(sem, Types.intType(),
						this.rightOperand.position);
				this.type = Types.boolType();
				break;

			case EQ:
			case NEQ:
				// Nun muss der Typ mindestens eines Operanden gleich oder eine
				// Ableitung des Typs des anderen Operanden sein.
				if (!this.leftOperand.type.isA(sem, this.rightOperand.type)
						&& !this.rightOperand.type.isA(sem,
								this.leftOperand.type)) {
					ClassSymbol.typeError(this.leftOperand.type,
							this.rightOperand.type, this.rightOperand.position);
				}
				this.type = Types.boolType();
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
		/* If one of the operands is NULL, then the other one must be an object.
		 * Box the value if this is the case. */
		this.leftOperand.generateCode(code,
				(this.leftOperand.type == Types.nullType()));
		this.rightOperand.generateCode(code,
				(this.rightOperand.type == Types.nullType()));

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