package org.oopsc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.oopsc.expression.AccessExpression;
import org.oopsc.expression.NewExpression;
import org.oopsc.statement.CallStatement;
import org.oopsc.statement.Statement;

class Vertex {
	/** Visited state. */
	public enum Visited {
		NotVisited, BeingVisited, DoneVisited
	}

	/**
	 * Class declaration associated to the vertex.
	 */
	public ClassDeclaration classDeclaration = null;

	/**
	 * By default, all vertexes were not visited.
	 */
	public Visited visited = Visited.NotVisited;

	/**
	 * The linked vertexes.
	 */
	public List<Vertex> edges = new ArrayList<>();

	/**
	 * Default constructor.
	 */
	public Vertex() {

	}

	/**
	 * Constructor
	 *
	 * @param c
	 *        Class declaration.
	 */
	public Vertex(ClassDeclaration c) {
		this.classDeclaration = c;
	}

	/**
	 * Adds edge to vertex.
	 *
	 * @param v
	 *        Vertex
	 */
	public void addEdge(Vertex v) {
		this.edges.add(v);
	}

	/**
	 * Recursively checks for cycles applying a depth-first search (DFS).
	 *
	 * @return true if vertex or its links contains cycles, false otherwise.
	 */
	public boolean hasCycles() {
		if (this.visited == Visited.BeingVisited) {
			return true;
		} else if (this.visited == Visited.DoneVisited) {
			return false;
		}

		this.visited = Visited.BeingVisited;

		for (Vertex v : this.edges) {
			if (v.hasCycles()) {
				return true;
			}
		}

		this.visited = Visited.DoneVisited;
		return false;
	}

	/**
	 * Recursively converts all sub-nodes of the current vertex to a list.
	 *
	 * @param res
	 *        Result list.
	 */
	public void toList(List<ClassDeclaration> res) {
		for (Vertex v : this.edges) {
			res.add(v.classDeclaration);
			v.toList(res);
		}
	}
}

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
		this.classes.add(ClassDeclaration.objectClass);
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
	 * Construct acyclic graph.
	 *
	 * @param declarations
	 * @param classes
	 * @return Class dependencies in the resolved order.
	 * @throws CompileException
	 */
	public List<ClassDeclaration> resolveClassDeps(Declarations declarations,
			List<ClassDeclaration> classes) throws CompileException {
		Map<ClassDeclaration, Vertex> mapping = new HashMap<>();

		for (ClassDeclaration cls : classes) {
			mapping.put(cls, new Vertex(cls));
		}

		/* Dummy node facilitating traversal. */
		Vertex root = new Vertex();

		for (ClassDeclaration cls : classes) {
			if (cls.baseType == null) {
				/* Connect all classes without parents to the root node. */
				root.addEdge(mapping.get(cls));
			} else {
				declarations.resolveType(cls.baseType);
				mapping.get(cls.baseType.declaration).addEdge(mapping.get(cls));
			}
		}

		if (root.hasCycles()) {
			throw new CompileException(
					"Class hierarchy is not devoid of cycles.", null);
		}

		/* Some classes may not be connected to the root node. Iterate over all
		 * vertexes that were not yet visited and check for cycles.
		 */
		for (Vertex v : mapping.values()) {
			if (v.visited == Vertex.Visited.NotVisited && v.hasCycles()) {
				throw new CompileException(
						"Class hierarchy is not devoid of cycles.", null);
			}
		}

		List<ClassDeclaration> res = new LinkedList<>();
		root.toList(res);

		return res;
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

		this.classes = this.resolveClassDeps(declarations, this.classes);

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

		/* Generate VMT for each class. */
		for (ClassDeclaration c : this.classes) {
			MethodDeclaration methods[] = c.generateVMT();

			code.println(c.identifier.name + ":");

			for (MethodDeclaration m : methods) {
				code.println("DAT 1, "
						+ c.resolveAsmMethodName(m.identifier.name));
			}
		}

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