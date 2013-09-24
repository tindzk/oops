/**
 * Die Klasse repräsentiert den Syntaxbaum des gesamten Programms.
 * Sie ist der Einstiegspunkt für die Kontextanalyse und die
 * Synthese.
 */
class Program {
    /** Die benutzerdefinierte Klasse. */
    ClassDeclaration theClass;

    /**
     * Eine Ausdruck, der ein Objekt der Klasse Main erzeugt und dann darin die
     * Methode main aufruft. Entspricht NEW Main.main.
     */
    private Expression main = new AccessExpression(
            new NewExpression(new ResolvableIdentifier("Main", null), null),
            new VarOrCall(new ResolvableIdentifier("main", null)));

    /**
     * Konstruktor.
     * @param theClass Die benutzerdefinierte Klasse.
     */
    Program(ClassDeclaration theClass) {
        this.theClass = theClass;
    }

    /**
     * Die Methode führt die Kontextanalyse für das Programm durch.
     * @throws CompileException Während der Kontextanylyse wurde ein Fehler
     *         gefunden.
     */
    void contextAnalysis() throws CompileException {
        Declarations declarations = new Declarations();

        // Integer enthält ein Element
        ClassDeclaration.intClass.objectSize = ClassDeclaration.HEADERSIZE + 1;

        // Neuen Deklarationsraum schaffen
        declarations.enter();

        // Vordefinierte Klasse hinzufügen
        declarations.add(ClassDeclaration.intClass);

        // Benutzerdefinierte Klasse hinzufügen
        declarations.add(theClass);

        // Kontextanalyse für die Methoden der Klasse durchführen
        theClass.contextAnalysis(declarations);

        // Abhängigkeiten für Startup-Code auflösen
        main = main.contextAnalysis(declarations);

        // Deklarationsraum verlassen
        declarations.leave();
    }

    /**
     * Die Methode gibt den Syntaxbaum des Programms aus.
     */
    void printTree() {
        TreeStream tree = new TreeStream(System.out, 4);
        theClass.print(tree);
    }

    /**
     * Die Methode generiert den Assembler-Code für das Programm. Sie geht
     * davon aus, dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
     * @param code Der Strom, in den die Ausgabe erfolgt.
     */
    void generateCode(CodeStream code, int stackSize, int heapSize) {
        // Start-Code: Register initialisieren
        code.setNamespace("_init");
        code.println("; Erzeugt durch OOPS-0 compiler, Version 2012-03-15.");
        code.println("MRI R1, 1 ; R1 ist immer 1");
        code.println("MRI R2, _stack ; R2 zeigt auf Stapel");
        code.println("MRI R4, _heap ; R4 zeigt auf die nächste freie Stelle auf dem Heap");

        // Ein Objekt der Klasse Main konstruieren und die Methode main aufrufen.
        main.generateCode(code);
        code.println("MRI R0, _end ; Programm beenden");

        // Generiere Code für benutzerdefinierte Klasse
        theClass.generateCode(code);

        // Speicher für Stapel und Heap reservieren
        code.println("_stack: ; Hier fängt der Stapel an");
        code.println("DAT " + stackSize + ", 0");
        code.println("_heap: ; Hier fängt der Heap an");
        code.println("DAT " + heapSize + ", 0");
        code.println("_end: ; Programmende");
    }
}
