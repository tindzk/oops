import java.util.LinkedList;

/**
 * Die Klasse repräsentiert eine Klassendeklaration im Syntaxbaum.
 * Zudem stellt sie Methoden zum Typvergleich zur Verfügung.
 */
class ClassDeclaration extends Declaration {
    /**
     * Konstante für die Größe der Verwaltungsinformation am Anfang eines jeden Objekts.
     * Bisher ist die Größe 0.
     */
    static final int HEADERSIZE = 0;

    /** Ein interner Typ für das Ergebnis von Methoden. */
    static final ClassDeclaration voidType = new ClassDeclaration(new Identifier("_Void", null));

    /** Ein interner Typ für null. Dieser Typ ist kompatibel zu allen Klassen. */
    static final ClassDeclaration nullType = new ClassDeclaration(new Identifier("_Null", null));

    /** Der interne Basisdatentyp für Zahlen. */
    static final ClassDeclaration intType = new ClassDeclaration(new Identifier("_Integer", null));

    /** Der interne Basisdatentyp für Wahrheitswerte. */
    static final ClassDeclaration boolType = new ClassDeclaration(new Identifier("_Boolean", null));

    /** Die Klasse Integer. */
    static final ClassDeclaration intClass = new ClassDeclaration(new Identifier("Integer", null));

    /** Die Attribute dieser Klasse. */
    LinkedList<VarDeclaration> attributes = new LinkedList<VarDeclaration>();

    /** Die Methoden dieser Klasse. */
    LinkedList<MethodDeclaration> methods = new LinkedList<MethodDeclaration>();

    /** Die innerhalb dieser Klasse sichtbaren Deklarationen. */
    Declarations declarations;

    /**
     * Die Größe eines Objekts dieser Klasse. Die Größe wird innerhalb von
     * {@link #contextAnalysis(Declarations) contextAnalysis} bestimmt.
     */
    int objectSize;

    /**
     * Konstruktor.
     * @param name Der Name der deklarierten Klasse.
     */
    ClassDeclaration(Identifier name) {
        super(name);
    }

    /**
     * Die Methode führt die Kontextanalyse für diese Klassen-Deklaration durch.
     * @param declarations Die an dieser Stelle gültigen Deklarationen.
     * @throws CompileException Während der Kontextanylyse wurde ein Fehler
     *         gefunden.
     */
    void contextAnalysis(Declarations declarations) throws CompileException {
        // Standardgröße für Objekte festlegen
        objectSize = HEADERSIZE;

        // Attributtypen auflösen und Indizes innerhalb des Objekts vergeben
        for (VarDeclaration a : attributes) {
            a.contextAnalysis(declarations);
            a.offset = objectSize++;
        }

        // Neuen Deklarationsraum schaffen
        declarations.enter();
        declarations.currentClass = this;

        // Attribute eintragen
        for (VarDeclaration a : attributes) {
            declarations.add(a);
        }

        // Methoden eintragen
        for (MethodDeclaration m : methods) {
            declarations.add(m);
        }

        // Wird auf ein Objekt dieser Klasse zugegriffen, werden die Deklarationen
        // in diesem Zustand benötigt. Deshalb werden sie in der Klasse gespeichert.
        this.declarations = (Declarations) declarations.clone();

        // Kontextanalyse für Methoden durchführen
        for (MethodDeclaration m : methods) {
            m.contextAnalysis(declarations);
        }

        // Deklarationsraum verlassen
        declarations.leave();
    }

    /**
     * Die Methode prüft, ob dieser Typ kompatibel mit einem anderen Typ ist.
     * @param expected Der Typ, mit dem verglichen wird.
     * @return Sind die beiden Typen sind kompatibel?
     */
    boolean isA(ClassDeclaration expected) {
        // Spezialbehandlung für null, das mit allen Klassen kompatibel ist,
        // aber nicht mit den Basisdatentypen _Integer und _Boolean sowie auch nicht
        // an Stellen erlaubt ist, wo gar kein Wert erwartet wird.
        if (this == nullType && expected != intType && expected != boolType && expected != voidType) {
            return true;
        } else {
            return this == expected;
        }
    }

    /**
     * Die Methode erzeugt eine Ausnahme für einen Typfehler. Sie wandelt dabei intern verwendete
     * Typnamen in die auch außen sichtbaren Namen um.
     * @param expected Der Typ, der nicht kompatibel ist.
     * @param position Die Stelle im Quelltext, an der der Typfehler gefunden wurde.
     * @throws CompileException Die Meldung über den Typfehler.
     */
    static void typeError(ClassDeclaration expected, Position position) throws CompileException {
        if (expected == intType) {
            throw new CompileException("Ausdruck vom Typ Integer erwartet", position);
        } else if (expected == boolType) {
            throw new CompileException("Ausdruck vom Typ Boolean erwartet", position);
        } else if (expected == ClassDeclaration.voidType) {
            throw new CompileException("Hier darf keinen Wert zurückgeliefert werden", position);
        } else {
            throw new CompileException("Ausdruck vom Typ " + expected.identifier.name + " erwartet", position);
        }
    }

    /**
     * Die Methode prüft, ob dieser Typ kompatibel mit einem anderen Typ ist.
     * Sollte das nicht der Fall sein, wird eine Ausnahme mit einer Fehlermeldung generiert.
     * @param expected Der Typ, mit dem verglichen wird.
     * @param position Die Position im Quelltext, an der diese Überprüfung
     *         relevant ist. Die Position wird in der Fehlermeldung verwendet.
     * @throws CompileException Die Typen sind nicht kompatibel.
     */
    void check(ClassDeclaration expected, Position position) throws CompileException {
        if (!isA(expected)) {
            typeError(expected, position);
        }
    }

    /**
     * Die Methode gibt diese Deklaration in einer Baumstruktur aus.
     * @param tree Der Strom, in den die Ausgabe erfolgt.
     */
    void print(TreeStream tree) {
        tree.println("CLASS " + identifier.name);
        tree.indent();
        if (!attributes.isEmpty()) {
            tree.println("ATTRIBUTES");
            tree.indent();
            for (VarDeclaration a : attributes) {
                a.print(tree);
            }
            tree.unindent();
        }
        if (!methods.isEmpty()) {
            tree.println("METHODS");
            tree.indent();
            for (MethodDeclaration m : methods) {
                m.print(tree);
            }
            tree.unindent();
        }
        tree.unindent();
    }

    /**
     * Generiert den Assembler-Code für diese Klasse. Dabei wird davon ausgegangen,
     * dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
     * @param code Der Strom, in den die Ausgabe erfolgt.
     */
    void generateCode(CodeStream code) {
        code.println("; CLASS " + identifier.name);

        // Synthese für alle Methoden
        for (MethodDeclaration m : methods) {
            m.generateCode(code);
        }
        code.println("; END CLASS " + identifier.name);
    }
}
