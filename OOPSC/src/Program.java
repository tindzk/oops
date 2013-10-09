import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 * Die Klasse repräsentiert den Syntaxbaum des gesamten Programms.
 * Sie ist der Einstiegspunkt für die Kontextanalyse und die
 * Synthese.
 */
class Program {
	/**
	 * User-defined classes.
	 */
	List<ClassDeclaration> classes = new LinkedList<>();

	/**
	 * Initialisation statements.
	 */
	List<Statement> init = new LinkedList<>();

	/**
	 * Constructor.
	 */
	public Program() {
		/* Add a statement that instantiates the class `Main' and calls its method main().
		 * Equivalent to NEW Main.main. */
		this.init.add(new CallStatement(new AccessExpression(new NewExpression(
				new ResolvableIdentifier("Main", null), null), new VarOrCall(
				new ResolvableIdentifier("main", null)))));

		/* Add predeclared classes. */
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

		// Benutzerdefinierte Klassen hinzufügen
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

		/* Resolve dependencies for startup statements. */
		for (Statement stmt : this.init) {
			stmt.contextAnalysis(declarations);
		}

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
		/* Initialise registers. */
		code.setNamespace("_init");
		code.println("; Erzeugt durch OOPS-0 compiler, Version 2012-03-15.");
		code.println("MRI R1, 1 ; R1 ist immer 1");
		code.println("MRI R2, _stack ; R2 zeigt auf Stapel");
		code.println("MRI R4, _heap ; R4 zeigt auf die nächste freie Stelle auf dem Heap");

		/* Initialise the initial exception frame, i.e., the first element is a
		 * pointer to the second one which itself points to the default exception
		 * handler (_uncaughtException). */
		code.println("MRI R5, _currentExceptionFrame");
		code.println("MRI R6, _currentExceptionFrame");
		code.println("ADD R6, R1");
		code.println("MMR (R5), R6");
		code.println("ADD R5, R1");
		code.println("MRI R6, _uncaughtException");
		code.println("MMR (R5), R6");

		Stack<Statement.Context> contexts = new Stack<>();
		contexts.add(Statement.Context.Default);

		/* Generate code for initialisation statements. */
		for (Statement stmt : this.init) {
			stmt.generateCode(code, contexts);
		}

		code.println("MRI R0, _end ; Programm beenden");

		/* Generate code for user-defined classes. */
		for (ClassDeclaration c : this.classes) {
			c.generateCode(code);
		}

		/* Allocate space for the default exception frame. */
		code.println("_currentExceptionFrame:");
		code.println("DAT 2, 0");

		/* Allocate space for the stack and the heap. */
		code.println("_stack: ; Hier fängt der Stapel an");
		code.println("DAT " + stackSize + ", 0");

		code.println("_heap: ; Hier fängt der Heap an");
		code.println("DAT " + heapSize + ", 0");

		/* Function being jumped to when an exception could not be caught. */
		code.println("_uncaughtException:");
		String s = "ABORT ";
		for (byte c : s.getBytes()) {
			code.println("MRI R5, " + (int) c);
			code.println("SYS 1, 5");
		}
		code.println("MRR R5, R7");
		code.println("SYS 1, 5");

		code.println("_end: ; Programmende");
	}
}