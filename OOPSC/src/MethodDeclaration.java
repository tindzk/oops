import java.util.LinkedList;

/**
 * Die Klasse repräsentiert eine Methode im Syntaxbaum.
 */
class MethodDeclaration extends Declaration {
    /** Die lokale Variable SELF. */
    VarDeclaration self = new VarDeclaration(new Identifier("_self", null), false);

    /** Die lokalen Variablen der Methode. */
    LinkedList<VarDeclaration> vars = new LinkedList<VarDeclaration>();

    /** Die Anweisungen der Methode, d.h. der Methodenrumpf. */
    LinkedList<Statement> statements = new LinkedList<Statement>();

    /**
     * Konstruktor.
     * @param name Der Name der deklarierten Methode.
     */
    MethodDeclaration(Identifier name) {
        super(name);
    }

    /**
     * Führt die Kontextanalyse für diese Methoden-Deklaration durch.
     * @param declarations Die an dieser Stelle gültigen Deklarationen.
     * @throws CompileException Während der Kontextanylyse wurde ein Fehler
     *         gefunden.
     */
    void contextAnalysis(Declarations declarations) throws CompileException {
        // SELF ist Variable vom Typ dieser Klasse
        self.type = new ResolvableIdentifier(declarations.currentClass.identifier.name, null);
        self.type.declaration = declarations.currentClass;

        // Löse Typen aller Variablen auf
        for (VarDeclaration v : vars) {
            v.contextAnalysis(declarations);
        }

        // Neuen Deklarationsraum schaffen
        declarations.enter();

        // SELF eintragen
        declarations.add(self);

        // SELF liegt vor der Rücksprungadresse auf dem Stapel
        self.offset = -2;

        // Rücksprungadresse und alten Rahmenzeiger überspringen
        int offset = 1;

        // Lokale Variablen eintragen
        for (VarDeclaration v : vars) {
            declarations.add(v);
            v.offset = offset++;
        }

        // Kontextanalyse aller Anweisungen durchführen
        for (Statement s : statements) {
            s.contextAnalysis(declarations);
        }

        // Alten Deklarationsraum wiederherstellen
        declarations.leave();
    }

    /**
     * Die Methode gibt diese Deklaration in einer Baumstruktur aus.
     * @param tree Der Strom, in den die Ausgabe erfolgt.
     */
    void print(TreeStream tree) {
        tree.println("METHOD " + identifier.name);
        tree.indent();
        if (!vars.isEmpty()) {
            tree.println("VARIABLES");
            tree.indent();
            for (VarDeclaration v : vars) {
                v.print(tree);
            }
            tree.unindent();
        }
        if (!statements.isEmpty()) {
            tree.println("BEGIN");
            tree.indent();
            for (Statement s : statements) {
                s.print(tree);
            }
            tree.unindent();
        }
        tree.unindent();
    }

    /**
     * Generiert den Assembler-Code für diese Methode. Dabei wird davon ausgegangen,
     * dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
     * @param code Der Strom, in den die Ausgabe erfolgt.
     */
    void generateCode(CodeStream code) {
        code.setNamespace(self.type.name + "_" + identifier.name);
        code.println("; METHOD " + identifier.name);
        code.println(self.type.name + "_" + identifier.name + ":");
        code.println("ADD R2, R1");
        code.println("MMR (R2), R3 ; Alten Stapelrahmen sichern");
        code.println("MRR R3, R2 ; Aktuelle Stapelposition ist neuer Rahmen");
        if (!vars.isEmpty()) {
            code.println("MRI R5, " + vars.size());
            code.println("ADD R2, R5 ; Platz für lokale Variablen schaffen");
        }
        for (Statement s : statements) {
            s.generateCode(code);
        }
        code.println("; END METHOD " + identifier.name);
        code.println("MRI R5, " + (vars.size() + 3));
        code.println("SUB R2, R5 ; Stack korrigieren");
        code.println("SUB R3, R1");
        code.println("MRM R5, (R3) ; Rücksprungadresse holen");
        code.println("ADD R3, R1");
        code.println("MRM R3, (R3) ; Alten Stapelrahmen holen");
        code.println("MRR R0, R5 ; Rücksprung");
        code.println("");
    }
}
