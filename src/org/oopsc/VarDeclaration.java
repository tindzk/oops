package org.oopsc;

public class VarDeclaration extends Declaration {
	public enum Type {
		Attribute,
		Local
	}

	/** Der Typ der Variablen bzw. des Attributs. */
	public ResolvableIdentifier type;

	public Type declType;

	/**
	 * Die Position der Variablen im Stapelrahmen bzw. des Attributs im Objekt.
	 * Dies wird während der Kontextanalyse eingetragen.
	 */
	public int offset;

	/**
	 * Konstruktor.
	 *
	 * @param name
	 *        Der Name der deklarierten Variablen bzw. des Attributs.
	 * @param isAttribute
	 *        Wird hier ein Attribut deklariert (statt einer lokalen
	 *        Variablen)?
	 */
	public VarDeclaration(Identifier name, Type declType) {
		super(name);
		this.declType = declType;
	}

	/**
	 * Returns the resolved type. Requires prior contextual analysis.
	 */
	public ClassDeclaration getResolvedType() {
		return (ClassDeclaration) this.type.declaration;
	}

	/**
	 * Führt die Kontextanalyse für diese Variablen-Deklaration durch.
	 *
	 * @param declarations
	 *        Die an dieser Stelle gültigen Deklarationen.
	 * @throws CompileException
	 *         Während der Kontextanylyse wurde ein Fehler
	 *         gefunden.
	 */
	@Override
	public void contextAnalysis(Declarations declarations, boolean initialPass)
			throws CompileException {
		declarations.resolveType(this.type);
	}

	/**
	 * Die Methode gibt diese Deklaration in einer Baumstruktur aus.
	 *
	 * @param tree
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	@Override
	public void print(TreeStream tree) {
		tree.println(this.identifier.name
				+ (this.type.declaration == null ? "" : " (" + this.offset
						+ ")") + " : " + this.type.name);
	}
}