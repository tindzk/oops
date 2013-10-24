package org.oopsc;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.oopsc.statement.Statement;

/**
 * Die Klasse repräsentiert eine Klassendeklaration im Syntaxbaum.
 * Zudem stellt sie Methoden zum Typvergleich zur Verfügung.
 */
public class ClassDeclaration extends Declaration {
	/**
	 * Konstante für die Größe der Verwaltungsinformation am Anfang eines jeden Objekts.
	 * As of now, the header only contains an address to the VMT of the object.
	 */
	static final public int HEADERSIZE = 1;

	/** Ein interner Typ für das Ergebnis von Methoden. */
	static final public ClassDeclaration voidType = new ClassDeclaration(
			new Identifier("_Void", null), null);

	/** Ein interner Typ für null. Dieser Typ ist kompatibel zu allen Klassen. */
	static final public ClassDeclaration nullType = new ClassDeclaration(
			new Identifier("_Null", null), null);

	/** Der interne Basisdatentyp für Zahlen. */
	static final public ClassDeclaration intType = new ClassDeclaration(
			new Identifier("_Integer", null), null);

	/** Der interne Basisdatentyp für Wahrheitswerte. */
	static final public ClassDeclaration boolType = new ClassDeclaration(
			new Identifier("_Boolean", null), null);

	/** Die Klasse Object. */
	static final public ClassDeclaration objectClass = new ClassDeclaration(
			new Identifier("Object", null), null);

	/** Die Klasse Integer. */
	static final public ClassDeclaration intClass = new ClassDeclaration(
			new Identifier("Integer", null), new ResolvableIdentifier("Object",
					null));

	/** Die Klasse Boolean. */
	static final public ClassDeclaration boolClass = new ClassDeclaration(
			new Identifier("Boolean", null), new ResolvableIdentifier("Object",
					null));

	static {
		/* Do not set ClassDeclaration.(int|bool)Class.objectSize manually as this
		 * value is going to be overwritten during the contextual analysis. The
		 * attribute is required for boxing as it holds the actual value. */
		VarDeclaration dummyIntegerValue = new VarDeclaration(new Identifier(
				"_value", null), VarDeclaration.Type.Attribute);
		dummyIntegerValue.type = new ResolvableIdentifier("_Integer", null);
		dummyIntegerValue.type.declaration = ClassDeclaration.intType;
		ClassDeclaration.intClass.attributes.add(dummyIntegerValue);

		VarDeclaration dummyBooleanValue = new VarDeclaration(new Identifier(
				"_value", null), VarDeclaration.Type.Attribute);
		dummyBooleanValue.type = new ResolvableIdentifier("_Boolean", null);
		dummyBooleanValue.type.declaration = ClassDeclaration.boolType;
		ClassDeclaration.boolClass.attributes.add(dummyBooleanValue);
	}

	/** Die Attribute dieser Klasse. */
	public List<VarDeclaration> attributes = new LinkedList<>();

	/** Die Methoden dieser Klasse. */
	public List<MethodDeclaration> methods = new LinkedList<>();

	/** Die innerhalb dieser Klasse sichtbaren Deklarationen. */
	public Declarations declarations;

	/**
	 * Die Größe eines Objekts dieser Klasse. Die Größe wird innerhalb von
	 * {@link #contextAnalysis(Declarations) contextAnalysis} bestimmt.
	 */
	public int objectSize;

	public ResolvableIdentifier baseType;

	/**
	 * Konstruktor.
	 *
	 * @param name
	 *        Der Name der deklarierten Klasse.
	 * @param baseType
	 *        The base class to extend from.
	 */
	ClassDeclaration(Identifier name, ResolvableIdentifier baseType) {
		super(name);
		this.baseType = baseType;
	}

	/**
	 * Recursively fill the VMT with the method declarations. Take into account
	 * overridden methods.
	 *
	 * @param res
	 *        Result array.
	 */
	protected void fillVMT(MethodDeclaration res[]) {
		if (this.baseType != null) {
			ClassDeclaration base = (ClassDeclaration) this.baseType.declaration;
			base.fillVMT(res);
		}

		for (MethodDeclaration m : this.methods) {
			res[m.vmtIndex] = m;
		}
	}

	/**
	 * @param cur
	 *        Current index.
	 * @return Highest VMT index.
	 */
	protected int getLastVmtIndex(int cur) {
		if (this.baseType != null) {
			ClassDeclaration base = (ClassDeclaration) this.baseType.declaration;

			int tmp = base.getLastVmtIndex(cur);
			if (tmp > cur) {
				cur = tmp;
			}
		}

		if (this.methods.size() != 0) {
			MethodDeclaration last = this.methods.get(this.methods.size() - 1);

			if (last.vmtIndex > cur) {
				cur = last.vmtIndex;
			}
		}

		return cur;
	}

	/**
	 * Generates a VMT for the current class, including its sub-classes. Requires
	 * that the contextual analysis was performed before.
	 *
	 * @return
	 */
	public MethodDeclaration[] generateVMT() {
		MethodDeclaration res[] = new MethodDeclaration[this
				.getLastVmtIndex(-1) + 1];
		this.fillVMT(res);
		return res;
	}

	/**
	 * Finds the declaration of the given method and return it in an assembly string.
	 * Takes into account if a method was inherited.
	 *
	 * @param name
	 *        Method name.
	 * @return null if not found, <class>_<method> otherwise.
	 */
	public String resolveAsmMethodName(String name) {
		for (MethodDeclaration m : this.methods) {
			if (m.identifier.name.equals(name)) {
				return this.identifier.name + "_" + name;
			}
		}

		if (this.baseType == null) {
			return null;
		}

		ClassDeclaration base = (ClassDeclaration) this.baseType.declaration;
		return base.resolveAsmMethodName(name);
	}

	/**
	 * Die Methode führt die Kontextanalyse für diese Klassen-Deklaration durch.
	 *
	 * @param declarations
	 *        Die an dieser Stelle gültigen Deklarationen.
	 * @param initialPass
	 *        Eigenschaften der Klasse initialisieren.
	 * @throws CompileException
	 *         Während der Kontextanylyse wurde ein Fehler
	 *         gefunden.
	 */
	@Override
	public void contextAnalysis(Declarations declarations, boolean initialPass)
			throws CompileException {
		// Standardgröße für Objekte festlegen
		this.objectSize = HEADERSIZE;

		if (this.baseType != null) {
			declarations.resolveType(this.baseType);
			ClassDeclaration base = (ClassDeclaration) this.baseType.declaration;

			/* Inherit attributes from the parent object. */
			this.objectSize += base.objectSize;

			declarations = (Declarations) base.declarations.clone();

			/* Verify that all overridden methods have the same signature as its parent. */
			for (MethodDeclaration m : this.methods) {
				MethodDeclaration baseMethod = base
						.getMethod(m.identifier.name);

				if (baseMethod != null) {
					/* This method overrides a parent method. */
					if (!baseMethod.signatureEquals(m)) {
						throw new CompileException(
								String.format(
										"The overridden signature of %s.%s() does not match its parent method in %s.",
										this.identifier.name,
										m.identifier.name, base.identifier.name),
								null);
					}
				}

				if (base.getAttribute(m.identifier.name) != null) {
					throw new CompileException(
							String.format(
									"The method %s.%s() is overriding a method of its base class %s.",
									this.identifier.name, m.identifier.name,
									base.identifier.name), null);
				}
			}

			for (VarDeclaration var : this.attributes) {
				if (base.getMethod(var.identifier.name) != null) {
					throw new CompileException(
							String.format(
									"The attribute %s in %s is overriding a method of its base class %s.",
									var.identifier.name, this.identifier.name,
									base.identifier.name), null);
				}
			}

			/* Set the VMT index for each method. */
			int vmtIndex = base.methods.isEmpty() ? 0 : base.methods
					.get(base.methods.size() - 1).vmtIndex + 1;
			for (MethodDeclaration m : this.methods) {
				/* If the method is overriden, take the VMT index from its parent method. */
				MethodDeclaration baseMethod = base
						.getMethod(m.identifier.name);

				if (baseMethod != null) {
					m.vmtIndex = baseMethod.vmtIndex;
				} else {
					m.vmtIndex = vmtIndex;
					vmtIndex++;
				}
			}
		} else {
			/* Set the VMT index for each method. */
			int vmtIndex = 0;
			for (MethodDeclaration m : this.methods) {
				m.vmtIndex = vmtIndex;
				vmtIndex++;
			}
		}

		// Attributtypen auflösen und Indizes innerhalb des Objekts vergeben
		for (VarDeclaration a : this.attributes) {
			a.contextAnalysis(declarations, initialPass);
			a.offset = this.objectSize++;
		}

		// Neuen Deklarationsraum schaffen
		declarations.enter();
		declarations.currentClass = this;

		// Attribute eintragen
		for (VarDeclaration a : this.attributes) {
			declarations.add(a);
		}

		// Methoden eintragen
		for (MethodDeclaration m : this.methods) {
			declarations.add(m);
		}

		if (initialPass) {
			// Wird auf ein Objekt dieser Klasse zugegriffen, werden die Deklarationen
			// in diesem Zustand benötigt. Deshalb werden sie in der Klasse gespeichert.
			this.declarations = (Declarations) declarations.clone();
		}

		// Kontextanalyse für Methoden durchführen
		for (MethodDeclaration m : this.methods) {
			declarations.currentMethod = m;
			m.contextAnalysis(declarations, initialPass);
		}

		if (!initialPass) {
			// Deklarationsraum verlassen
			declarations.leave();
		}
	}

	/**
	 * Finds the declaration for the given attribute name.
	 *
	 * @param name
	 *        Attribute name.
	 * @return null if not found, declaration otherwise.
	 */
	private VarDeclaration getAttribute(String name) {
		for (VarDeclaration var : this.attributes) {
			if (var.identifier.name.equals(name)) {
				return var;
			}
		}

		return null;
	}

	/**
	 * Finds the declaration for the given method name.
	 *
	 * @param name
	 *        Method name.
	 * @return null if not found, declaration otherwise.
	 */
	private MethodDeclaration getMethod(String name) {
		for (MethodDeclaration m : this.methods) {
			if (m.identifier.name.equals(name)) {
				return m;
			}
		}

		return null;
	}

	/**
	 * Die Methode prüft, ob dieser Typ kompatibel mit einem anderen Typ ist.
	 *
	 * @param expected
	 *        Der Typ, mit dem verglichen wird.
	 * @return Sind die beiden Typen sind kompatibel?
	 */
	public boolean isA(ClassDeclaration expected) {
		// Spezialbehandlung für null, das mit allen Klassen kompatibel ist,
		// aber nicht mit den Basisdatentypen _Integer und _Boolean sowie auch nicht
		// an Stellen erlaubt ist, wo gar kein Wert erwartet wird.
		if (this == nullType && expected != intType && expected != boolType
				&& expected != voidType) {
			return true;
		}

		/* Compare wrt. base type. */
		for (ClassDeclaration cmp = this;;) {
			if (cmp == expected) {
				return true;
			}

			if (cmp.baseType == null) {
				break;
			}

			cmp = (ClassDeclaration) cmp.baseType.declaration;
		}

		return false;
	}

	/**
	 * Die Methode erzeugt eine Ausnahme für einen Typfehler. Sie wandelt dabei intern verwendete
	 * Typnamen in die auch außen sichtbaren Namen um.
	 *
	 * @param expected
	 *        Der Typ, der nicht kompatibel ist.
	 * @param position
	 *        Die Stelle im Quelltext, an der der Typfehler gefunden wurde.
	 * @throws CompileException
	 *         Die Meldung über den Typfehler.
	 */
	static public void typeError(ClassDeclaration expected, ClassDeclaration given,
			Position position) throws CompileException {
		throw new CompileException(String.format(
				"Type mismatch: %s expected, %s given.",
				expected.identifier.name, given.identifier.name), position);
	}

	/**
	 * Die Methode prüft, ob dieser Typ kompatibel mit einem anderen Typ ist.
	 * Sollte das nicht der Fall sein, wird eine Ausnahme mit einer Fehlermeldung generiert.
	 *
	 * @param expected
	 *        Der Typ, mit dem verglichen wird.
	 * @param position
	 *        Die Position im Quelltext, an der diese Überprüfung
	 *        relevant ist. Die Position wird in der Fehlermeldung verwendet.
	 * @throws CompileException
	 *         Die Typen sind nicht kompatibel.
	 */
	public void check(ClassDeclaration expected, Position position)
			throws CompileException {
		if (!this.isA(expected)) {
			typeError(expected, this, position);
		}
	}

	/**
	 * Die Methode gibt diese Deklaration in einer Baumstruktur aus.
	 *
	 * @param tree
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	@Override
	public void print(TreeStream tree) {
		tree.println("CLASS " + this.identifier.name);
		tree.indent();

		if (!this.attributes.isEmpty()) {
			tree.println("ATTRIBUTES");
			tree.indent();
			for (VarDeclaration a : this.attributes) {
				a.print(tree);
			}
			tree.unindent();
		}

		if (!this.methods.isEmpty()) {
			tree.println("METHODS");
			tree.indent();
			for (MethodDeclaration m : this.methods) {
				m.print(tree);
			}
			tree.unindent();
		}

		tree.unindent();
	}

	/**
	 * Generiert den Assembler-Code für diese Klasse. Dabei wird davon ausgegangen,
	 * dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
	 *
	 * @param code
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	void generateCode(CodeStream code) {
		code.println("; CLASS " + this.identifier.name);

		// Synthese für alle Methoden
		for (MethodDeclaration m : this.methods) {
			Stack<Statement.Context> contexts = new Stack<>();
			contexts.add(Statement.Context.Default);
			m.generateCode(code, contexts);
		}

		code.println("; END CLASS " + this.identifier.name);
	}
}