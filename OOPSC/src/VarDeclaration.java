class VarDeclaration extends Declaration {
	/** Der Typ der Variablen bzw. des Attributs. */
	ResolvableIdentifier type;

	/** Wird hier ein Attribut deklariert (statt einer lokalen Variablen)? */
	boolean isAttribute;

	/**
	 * Die Position der Variablen im Stapelrahmen bzw. des Attributs im Objekt.
	 * Dies wird während der Kontextanalyse eingetragen.
	 */
	int offset;

	/**
	 * Konstruktor.
	 *
	 * @param name
	 *        Der Name der deklarierten Variablen bzw. des Attributs.
	 * @param isAttribute
	 *        Wird hier ein Attribut deklariert (statt einer lokalen
	 *        Variablen)?
	 */
	VarDeclaration(Identifier name, boolean isAttribute) {
		super(name);
		this.isAttribute = isAttribute;
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
	void contextAnalysis(Declarations declarations) throws CompileException {
		declarations.resolveType(this.type);
	}

	/**
	 * Die Methode gibt diese Deklaration in einer Baumstruktur aus.
	 *
	 * @param tree
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	@Override
	void print(TreeStream tree) {
		tree.println(this.identifier.name
				+ (this.type.declaration == null ? "" : " (" + this.offset
						+ ")") + " : " + this.type.name);
	}
}
