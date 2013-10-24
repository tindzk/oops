package org.oopsc.expression;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.oopsc.ClassDeclaration;
import org.oopsc.CodeStream;
import org.oopsc.CompileException;
import org.oopsc.Declarations;
import org.oopsc.MethodDeclaration;
import org.oopsc.ResolvableIdentifier;
import org.oopsc.TreeStream;
import org.oopsc.VarDeclaration;
import org.oopsc.VarDeclaration.Type;

/**
 * Die Klasse repräsentiert einen Ausdruck im Syntaxbaum, der dem Zugriff auf eine
 * Variable oder ein Attribut bzw. einem Methodenaufruf entspricht.
 */
public class VarOrCall extends Expression {
	/** Der Name des Attributs, der Variablen oder der Methode. */
	public ResolvableIdentifier identifier;

	public List<Expression> arguments = new LinkedList<>();

	public ClassDeclaration context = null;

	/**
	 * Konstruktor.
	 *
	 * @param identifier
	 *        Der Name des Attributs, der Variablen oder der Methode.
	 */
	public VarOrCall(ResolvableIdentifier identifier) {
		super(identifier.position);
		this.identifier = identifier;
	}

	public void addArgument(Expression e) {
		this.arguments.add(e);
	}

	/**
	 * Sets a static context. By default, methods are called dynamically by resolving the target
	 * index from the VMT to which the object counts to. A static context bypasses the VMT and calls
	 * the method directly. This method can be used for calling methods in the base class.
	 *
	 * @param context
	 */
	public void setStaticContext(ClassDeclaration context) {
		this.context = context;
	}

	/**
	 * Die Methode führt die Kontextanalyse für diesen Ausdruck durch.
	 * Dabei wird ein Zugriff über SELF in den Syntaxbaum eingefügt,
	 * wenn dieser Ausdruck ein Attribut oder eine Methode bezeichnet.
	 * Diese Methode wird niemals für Ausdrücke aufgerufen, die rechts
	 * vom Objekt-Zugriffsoperator stehen.
	 *
	 * @param declarations
	 *        Die an dieser Stelle gültigen Deklarationen.
	 * @return Dieser Ausdruck oder ein neuer Ausdruck, falls ein Boxing
	 *         oder der Zugriff über SELF eingefügt wurde.
	 * @throws CompileException
	 *         Während der Kontextanalyse wurde ein Fehler
	 *         gefunden.
	 */
	@Override
	public Expression contextAnalysis(Declarations declarations)
			throws CompileException {
		// TODO refactor
		this.contextAnalysisForMember(declarations);
		this.contextAnalysisForArguments(declarations);

		if (this.identifier.declaration instanceof MethodDeclaration
				|| this.identifier.declaration instanceof VarDeclaration
				&& ((VarDeclaration) this.identifier.declaration).declType == VarDeclaration.Type.Attribute) {
			AccessExpression a = new AccessExpression(new VarOrCall(
					new ResolvableIdentifier("_self", this.position)), this);
			a.leftOperand = a.leftOperand.contextAnalysis(declarations);
			a.leftOperand = a.leftOperand.box(declarations);
			a.type = this.type;
			a.lValue = this.lValue;
			return a;
		} else {
			return this;
		}
	}

	/**
	 * Die Methode führt die Kontextanalyse für diesen Ausdruck durch.
	 * Diese Methode wird auch für Ausdrücke aufgerufen, die rechts
	 * vom Objekt-Zugriffsoperator stehen.
	 *
	 * @param declarations
	 *        Die an dieser Stelle gültigen Deklarationen.
	 * @throws CompileException
	 *         Während der Kontextanylyse wurde ein Fehler
	 *         gefunden.
	 */
	public void contextAnalysisForMember(Declarations declarations)
			throws CompileException {
		// TODO refactor
		declarations.resolveVarOrMethod(this.identifier);

		if (this.identifier.declaration instanceof VarDeclaration) {
			VarDeclaration var = (VarDeclaration) this.identifier.declaration;
			this.type = var.getResolvedType();
			this.lValue = true;
		} else if (this.identifier.declaration instanceof MethodDeclaration) {
			MethodDeclaration method = (MethodDeclaration) this.identifier.declaration;
			this.type = method.getResolvedReturnType();
		} else {
			assert false;
		}
	}

	/**
	 * Contextual analysis for arguments.
	 *
	 * @param declarations
	 * @throws CompileException
	 */
	public void contextAnalysisForArguments(Declarations declarations)
			throws CompileException {
		if (!(this.identifier.declaration instanceof MethodDeclaration)) {
			if (this.arguments.size() != 0) {
				throw new CompileException(
						"Arguments cannot be passed to a variable.",
						this.identifier.position);
			}
		} else {
			/* Verify that the passed arguments match the expected parameters. */
			MethodDeclaration decl = (MethodDeclaration) this.identifier.declaration;

			if (this.arguments.size() != decl.parameters.size()) {
				throw new CompileException(String.format(
						"Parameter count mismatch: %d expected, %d given.",
						decl.parameters.size(), this.arguments.size()),
						this.identifier.position);
			}

			List<Expression> boxed = new LinkedList<>();

			Iterator<Expression> args = this.arguments.iterator();
			Iterator<VarDeclaration> params = decl.parameters.iterator();

			for (int num = 1; args.hasNext(); num++) {
				Expression arg = args.next();
				VarDeclaration param = params.next();

				arg = arg.contextAnalysis(declarations);

				/* Parameters expect boxed values, i.e., Integer instead of _Integer. */
				arg = arg.box(declarations);
				boxed.add(arg);

				assert (param.type.declaration instanceof ClassDeclaration);

				if (!arg.type.isA((ClassDeclaration) param.type.declaration)) {
					throw new CompileException(String.format(
							"Argument %d mismatches: %s expected, %s given.",
							num, param.type.declaration.identifier.name,
							arg.type.identifier.name), this.identifier.position);
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
		tree.println(this.identifier.name
				+ (this.type == null ? "" : " : " + (this.lValue ? "REF " : "")
						+ this.type.identifier.name));
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
		if (this.identifier.declaration instanceof VarDeclaration) {
			VarDeclaration v = (VarDeclaration) this.identifier.declaration;
			if (v.declType == VarDeclaration.Type.Attribute) {
				/* Stored in the class object. */
				code.println("; Referencing attribute " + this.identifier.name);
				code.println("MRM R5, (R2)");
				code.println("MRI R6, " + v.offset);
				code.println("ADD R5, R6");
				code.println("MMR (R2), R5");
			} else if (v.declType == VarDeclaration.Type.Local) {
				/* Stored in the stack frame. */
				code.println("; Referencing local variable "
						+ this.identifier.name);
				code.println("MRI R5, " + v.offset);
				code.println("ADD R5, R3");
				code.println("ADD R2, R1");
				code.println("MMR (R2), R5");
			}
		} else if (this.identifier.declaration instanceof MethodDeclaration) {
			String returnLabel = code.nextLabel();
			MethodDeclaration m = (MethodDeclaration) this.identifier.declaration;

			if (this.context != null) {
				code.println("; Static method call: " + this.identifier.name);

				code.println("; Arguments");
				code.println("");

				/* Push arguments on the stack. */
				int i = 1;
				for (Expression e : this.arguments) {
					code.println("; Argument " + i);
					code.println("; " + e.getClass());
					e.generateCode(code);
					i++;
				}

				/* Push return address on the stack. */
				code.println("MRI R5, " + returnLabel + " ; Return address.");
				code.println("ADD R2, R1");
				code.println("MMR (R2), R5 ; Save return address on the stack.");

				/* Jump to method by overwriting PC. */
				code.println("MRI R0, "
						+ this.context.resolveAsmMethodName(m.identifier.name));
			} else {
				code.println("; Dynamic method call: " + this.identifier.name);
				code.println("; VMT index = " + m.vmtIndex);

				code.println("; Arguments");
				code.println("");

				/* Push arguments on the stack. */
				int i = 1;
				for (Expression e : this.arguments) {
					code.println("; Argument " + i);
					code.println("; " + e.getClass());
					e.generateCode(code);
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

				code.println("MRI R5, " + m.vmtIndex);
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