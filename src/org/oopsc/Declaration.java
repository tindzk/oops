package org.oopsc;

/**
 * Die Basisklasse zur Repräsentation deklarierter Objekte.
 */
abstract class Declaration {
	/** Der Name der deklarierten Klasse, Methode oder Variablen. */
	public Identifier identifier;

	/**
	 * Konstruktor.
	 *
	 * @param identifier
	 *        Der Name der deklarierten Klasse, Methode oder Variablen.
	 */
	public Declaration(Identifier identifier) {
		this.identifier = identifier;
	}

	/**
	 * Führt die Kontextanalyse für diese Deklaration durch.
	 *
	 * @param declarations
	 *        Die an dieser Stelle gültigen Deklarationen.
	 * @throws CompileException
	 *         Während der Kontextanylyse wurde ein Fehler
	 *         gefunden.
	 */
	abstract public void contextAnalysis(Declarations declarations, boolean initialPass)
			throws CompileException;

	/**
	 * Die Methode gibt diese Deklaration in einer Baumstruktur aus.
	 *
	 * @param tree
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	abstract public void print(TreeStream tree);
}