import java.util.LinkedList;
import java.util.List;

/**
 * Die Klasse repräsentiert den Syntaxbaum des gesamten Programms.
 * Sie ist der Einstiegspunkt für die Kontextanalyse und die
 * Synthese.
 */
class Program {
	/** Die benutzerdefinierten Klassen. */
	List<ClassDeclaration> classes = new LinkedList<>();

	/**
	 * Ein Ausdruck, der ein Objekt der Klasse Main erzeugt und dann darin die
	 * Methode main aufruft. Entspricht NEW Main.main.
	 */
	private Expression main = new AccessExpression(new NewExpression(
			new ResolvableIdentifier("Main", null), null), new VarOrCall(
			new ResolvableIdentifier("main", null)));

	/**
	 * Konstruktor.
	 */
	public Program() {
		// Integer und Boolean enthalten jeweils ein Element

		// TODO
		// Statt ClassDeclaration.(int|bool)Class.objectSize manuell zu
		// setzen, fügt man z.B. manuell ein Attribut _value : _Integer oder
		// _value : _Boolean hinzu
		ClassDeclaration.intClass.objectSize = ClassDeclaration.HEADERSIZE + 1;
		ClassDeclaration.boolClass.objectSize = ClassDeclaration.HEADERSIZE + 1;

		// Vordefinierte Klassen hinzufügen
		this.classes.add(ClassDeclaration.intClass);
		this.classes.add(ClassDeclaration.boolClass);
	}

	/**
	 * Definiere Klasse.
	 *
	 * @param clazz
	 *        Die benutzerdefinierte Klasse.
	 */
	public void addClass(ClassDeclaration clazz) {
		this.classes.add(clazz);
	}

	/**
	 * Die Methode führt die Kontextanalyse für das Programm durch.
	 *
	 * @throws CompileException
	 *         Während der Kontextanylyse wurde ein Fehler
	 *         gefunden.
	 */
	void contextAnalysis() throws CompileException {
		Declarations declarations = new Declarations();

		// Neuen Deklarationsraum schaffen
		declarations.enter();

		// Benutzerdefinierten Klassen hinzufügen
		for (ClassDeclaration c : this.classes) {
			declarations.add(c);
		}

		// Alle Klassendeklarationen initialisieren
		for (ClassDeclaration c : this.classes) {
			c.contextAnalysis(declarations, true);
		}

		// Kontextanalyse für die Methoden aller Klassen durchführen
		for (ClassDeclaration c : this.classes) {
			c.contextAnalysis(declarations, false);
		}

		// Abhängigkeiten für Startup-Code auflösen
		this.main = this.main.contextAnalysis(declarations);

		// Deklarationsraum verlassen
		declarations.leave();
	}

	/**
	 * Die Methode gibt den Syntaxbaum des Programms aus.
	 */
	void printTree() {
		TreeStream tree = new TreeStream(System.out, 4);

		for (ClassDeclaration c : this.classes) {
			c.print(tree);
		}
	}

	/**
	 * Die Methode generiert den Assembler-Code für das Programm. Sie geht
	 * davon aus, dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
	 *
	 * @param code
	 *        Der Strom, in den die Ausgabe erfolgt.
	 */
	void generateCode(CodeStream code, int stackSize, int heapSize) {
		// Start-Code: Register initialisieren
		code.setNamespace("_init");
		code.println("; Erzeugt durch OOPS-0 compiler, Version 2012-03-15.");
		code.println("MRI R1, 1 ; R1 ist immer 1");
		code.println("MRI R2, _stack ; R2 zeigt auf Stapel");
		code.println("MRI R4, _heap ; R4 zeigt auf die nächste freie Stelle auf dem Heap");

		// Ein Objekt der Klasse Main konstruieren und die Methode main aufrufen.
		this.main.generateCode(code);
		code.println("MRI R0, _end ; Programm beenden");

		// Generiere Code für benutzerdefinierte Klassen
		for (ClassDeclaration c : this.classes) {
			c.generateCode(code);
		}

		// Speicher für Stapel und Heap reservieren
		code.println("_stack: ; Hier fängt der Stapel an");
		code.println("DAT " + stackSize + ", 0");
		code.println("_heap: ; Hier fängt der Heap an");
		code.println("DAT " + heapSize + ", 0");
		code.println("_end: ; Programmende");
	}
}
