/**
 * Die Basisklasse zur Repräsentation deklarierter Objekte.
 */
abstract class Declaration {
    /** Der Name der deklarierten Klasse, Methode oder Variablen. */
    Identifier identifier;
    
    /**
     * Konstruktor.
     * @param identifier Der Name der deklarierten Klasse, Methode oder Variablen.
     */
    Declaration(Identifier identifier) {
        this.identifier = identifier;
    }

    /**
     * Führt die Kontextanalyse für diese Deklaration durch.
     * @param declarations Die an dieser Stelle gültigen Deklarationen.
     * @throws CompileException Während der Kontextanylyse wurde ein Fehler
     *         gefunden.
     */
    abstract void contextAnalysis(Declarations declarations) throws CompileException;

    /**
     * Die Methode gibt diese Deklaration in einer Baumstruktur aus.
     * @param tree Der Strom, in den die Ausgabe erfolgt.
     */
    abstract void print(TreeStream tree);
}
