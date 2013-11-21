package org.oopsc.expression;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.oopsc.*;
import org.oopsc.scope.*;
import org.oopsc.symbol.*;

import scala.Some;

/**
 * Die Klasse repräsentiert einen Ausdruck im Syntaxbaum, der dem Zugriff auf eine
 * Variable oder ein Attribut bzw. einem Methodenaufruf entspricht.
 */
public class VarOrCall extends Expression {
	/** Der Name des Attributs, der Variablen oder der Methode. */
	public ResolvableSymbol ref;

	public List<Expression> arguments = new LinkedList<>();

	public ClassSymbol context = null;

	/**
	 * Konstruktor.
	 *
	 * @param ref
	 *        Der Name des Attributs, der Variablen oder der Methode.
	 */
	public VarOrCall(ResolvableSymbol ref) {
		super(ref.identifier().position());
		this.ref = ref;
	}

	/**
	 * Sets a static context. By default, methods are called dynamically by resolving the target
	 * index from the VMT to which the object counts to. A static context bypasses the VMT and calls
	 * the method directly. This method can be used for calling methods in the base class.
	 *
	 * @param context
	 */
	public void setStaticContext(ClassSymbol context) {
		this.context = context;
	}

	@Override
	public Expression refPass(SemanticAnalysis sem) throws CompileException {
		// TODO refactor
		this.contextAnalysisForMember(sem.currentScope().get().getThis());
		this.contextAnalysisForArguments(sem);

		if (this.ref.declaration().get() instanceof MethodSymbol
				|| this.ref.declaration().get() instanceof AttributeSymbol) {
			AccessExpression a = new AccessExpression(new VarOrCall(
					new ResolvableSymbol(
							new Identifier("_self", this.position), null)),
					this);

			a.leftOperand = a.leftOperand.refPass(sem);
			a.leftOperand.types = sem.types();
			a.type = this.type;
			a.lValue = this.lValue;

			return a;
		}

		return this;
	}

	/**
	 * Die Methode führt die Kontextanalyse für diesen Ausdruck durch.
	 * Diese Methode wird auch für Ausdrücke aufgerufen, die rechts
	 * vom Objekt-Zugriffsoperator stehen.
	 *
	 * @throws CompileException
	 *         Während der Kontextanylyse wurde ein Fehler
	 *         gefunden.
	 */
	public void contextAnalysisForMember(Scope scope) throws CompileException {
		/* Resolve variable or method. */
		this.ref.declaration_$eq(new Some<>(scope.checkedResolve(this.ref
				.identifier())));

		/* Propagate resolved type. */
		if (this.ref.declaration().get() instanceof VariableSymbol) {
			VariableSymbol var = (VariableSymbol) this.ref.declaration().get();
			this.type = var.getResolvedType();
			this.lValue = true;
		} else if (this.ref.declaration().get() instanceof MethodSymbol) {
			MethodSymbol method = (MethodSymbol) this.ref.declaration().get();
			this.type = method.getResolvedReturnType();
		} else {
			assert false;
		}
	}

	/**
	 * Contextual analysis for arguments.
	 *
	 * @param sem
	 * @throws CompileException
	 */
	public void contextAnalysisForArguments(SemanticAnalysis sem)
			throws CompileException {
		if (!(this.ref.declaration().get() instanceof MethodSymbol)) {
			if (this.arguments.size() != 0) {
				throw new CompileException(
						"Arguments cannot be passed to a variable.", this.ref
								.identifier().position());
			}
		} else {
			/* Verify that the passed arguments match the expected parameters. */
			MethodSymbol decl = (MethodSymbol) this.ref.declaration().get();

			if (this.arguments.size() != decl.parameters().size()) {
				throw new CompileException(String.format(
						"Parameter count mismatch: %d expected, %d given.",
						decl.parameters().size(), this.arguments.size()),
						this.ref.identifier().position());
			}

			// TODO list not necessary
			List<Expression> boxed = new LinkedList<>();

			Iterator<Expression> args = this.arguments.iterator();

			scala.collection.Iterator<VariableSymbol> params = decl
					.parameters().iterator();

			for (int num = 1; args.hasNext(); num++) {
				Expression arg = args.next();
				VariableSymbol param = params.next();

				arg = arg.refPass(sem);
				arg.types = sem.types();

				/* Parameters expect boxed values, i.e., Integer instead of _Integer. */
				boxed.add(arg);

				if (!arg.type.isA(sem, param.getResolvedType())) {
					throw new CompileException(String.format(
							"Argument %d mismatches: %s expected, %s given.",
							num,
							param.resolvedType().get().identifier().name(),
							arg.type.name()), this.ref.identifier().position());
				}
			}

			this.arguments = boxed;
		}
	}

	/**
	 * Die Methode gibt diesen Ausdruck in einer Baumstruktur aus.
	 * Wenn der Typ des Ausdrucks bereits ermittelt wurde, wird er auch ausgegeben.
	 *
	 * @param tree
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	@Override
	public void print(TreeStream tree) {
		tree.println(this.ref.identifier().name()
				+ (this.type == null ? "" : " : " + (this.lValue ? "REF " : "")
						+ this.type.name()));
	}

	/**
	 * Die Methode generiert den Assembler-Code für diesen Ausdruck. Sie geht
	 * davon aus, dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
	 *
	 * @param code
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	@Override
	public void generateCode(CodeStream code) {
		Symbol decl = this.ref.declaration().get();

		if (decl instanceof AttributeSymbol) {
			/* Stored in the class object. */
			code.println("; Referencing attribute "
					+ this.ref.identifier().name());
			code.println("MRM R5, (R2)");
			code.println("MRI R6, " + ((AttributeSymbol) decl).offset());
			code.println("ADD R5, R6");
			code.println("MMR (R2), R5");
		} else if (decl instanceof VariableSymbol) {
			/* Stored in the stack frame. */
			code.println("; Referencing local variable "
					+ this.ref.identifier().name());
			code.println("MRI R5, " + ((VariableSymbol) decl).offset());
			code.println("ADD R5, R3");
			code.println("ADD R2, R1");
			code.println("MMR (R2), R5");
		} else if (decl instanceof MethodSymbol) {
			String returnLabel = code.nextLabel();
			MethodSymbol m = (MethodSymbol) decl;

			if (this.context != null) {
				code.println("; Static method call: "
						+ this.ref.identifier().name());

				code.println("; Arguments");
				code.println("");

				/* Push arguments on the stack. */
				int i = 1;
				for (Expression e : this.arguments) {
					code.println("; Argument " + i);
					code.println("; " + e.getClass());
					e.generateCode(code, true);
					i++;
				}

				/* Push return address on the stack. */
				code.println("MRI R5, " + returnLabel + " ; Return address.");
				code.println("ADD R2, R1");
				code.println("MMR (R2), R5 ; Save return address on the stack.");

				/* Jump to method by overwriting PC. */
				code.println("MRI R0, "
						+ this.context.resolveAsmMethodName(m.identifier()
								.name()));
			} else {
				code.println("; Dynamic method call: "
						+ this.ref.identifier().name());
				code.println("; VMT index = " + m.vmtIndex());

				code.println("; Arguments");
				code.println("");

				/* Push arguments on the stack. */
				int i = 1;
				for (Expression e : this.arguments) {
					code.println("; Argument " + i);
					code.println("; " + e.getClass());
					e.generateCode(code, true);
					i++;
				}

				/* Push return address on the stack. */
				code.println("MRI R5, " + returnLabel + " ; Return address.");
				code.println("ADD R2, R1");
				code.println("MMR (R2), R5 ; Save return address on the stack.");

				/* Resolve function address from VMT. */
				code.println("MRR R5, R2");
				code.println("MRI R6, " + (1 + this.arguments.size()));
				code.println("SUB R5, R6");

				code.println("MRM R6, (R5)"); // R5 = Object address.
				code.println("MRM R6, (R6)"); // R6 = VMT address.

				code.println("MRI R5, " + m.vmtIndex());
				code.println("ADD R6, R5");

				/* Jump to method by overwriting PC. */
				code.println("MRM R0, (R6)");
			}

			code.println(returnLabel + ":");
		} else {
			assert false;
		}
	}
}