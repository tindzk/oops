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

	public Scope scope = null;

	protected VariableSymbol context = null;
	protected boolean isStaticContext = false;
	protected boolean generateContextCode = true;

	public List<Expression> arguments = new LinkedList<>();

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

	public void generateContextCode(boolean value) {
		this.generateContextCode = value;
	}

	/**
	 * Sets a (static) context. By default, methods are called dynamically by resolving the target
	 * index from the VMT to which the object counts to. A static context bypasses the VMT and calls
	 * the method directly. This method can be used for calling methods in the base class.
	 *
	 * @param context
	 */
	public void setContext(VariableSymbol context, boolean isStatic) {
		this.context = context;
		this.isStaticContext = isStatic;
	}

	@Override
	public void refPass(SemanticAnalysis sem) throws CompileException {
		Scope resolveScope = this.scope == null ? sem.currentScope().get()
				.getThis() : this.scope;

		/* Resolve variable or method. */
		this.ref.declaration_$eq(new Some<>(resolveScope
				.checkedResolve(this.ref.identifier())));

		/* Check arguments. */
		if (!(this.ref.declaration().get() instanceof MethodSymbol)) {
			if (this.arguments.size() != 0) {
				throw new CompileException(
						"Arguments cannot be passed to a variable.", this.ref
								.identifier().position());
			}
		}

		/* Resolve method or attribute context. */
		if (this.generateContextCode) {
			if (this.ref.declaration().get() instanceof MethodSymbol
					|| this.ref.declaration().get() instanceof AttributeSymbol) {
				if (this.context == null && sem.currentMethod() != null) {
					this.context = sem.currentMethod().self();
				}
			}
		}

		/* Propagate resolved type. */
		if (this.ref.declaration().get() instanceof VariableSymbol) {
			VariableSymbol var = (VariableSymbol) this.ref.declaration().get();
			this.type = var.getResolvedType();
			this.lValue = true;
		} else if (this.ref.declaration().get() instanceof MethodSymbol) {
			MethodSymbol method = (MethodSymbol) this.ref.declaration().get();
			this.type = method.getResolvedReturnType();

			/* Verify that the passed arguments match the expected parameters. */
			MethodSymbol decl = (MethodSymbol) this.ref.declaration().get();

			if (this.arguments.size() != decl.parameters().size()) {
				throw new CompileException(String.format(
						"Parameter count mismatch: %d expected, %d given.",
						decl.parameters().size(), this.arguments.size()),
						this.ref.identifier().position());
			}

			Iterator<Expression> args = this.arguments.iterator();

			scala.collection.Iterator<VariableSymbol> params = decl
					.parameters().iterator();

			for (int num = 1; args.hasNext(); num++) {
				Expression arg = args.next();
				VariableSymbol param = params.next();

				arg.refPass(sem);

				if (!arg.type.isA(sem, param.getResolvedType())) {
					throw new CompileException(String.format(
							"Argument %d mismatches: %s expected, %s given.",
							num,
							param.resolvedType().get().identifier().name(),
							arg.type.name()), this.ref.identifier().position());
				}
			}
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

	protected void _generateContextCode(CodeStream code) {
		if (this.context != null) {
			code.println("; Context: " + this.context.identifier().name());
			VarOrCall var = new VarOrCall(new ResolvableSymbol(
					this.context.identifier(), new Some<Symbol>(this.context)));
			var.lValue = true;
			var.generateCode(code, false);
			code.println("; End context.");
		} else {
			code.println("; No context.");
		}
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
			this._generateContextCode(code);

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

			if (this.context != null && this.isStaticContext) {
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
						+ this.context.resolvedType().get()
								.resolveAsmMethodName(m.identifier().name()));
			} else {
				this._generateContextCode(code);

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
		}
	}
}