package org.oopsc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.oopsc.expression.*;
import org.oopsc.scope.*;
import org.oopsc.symbol.*;
import org.oopsc.statement.*;

class Vertex {
	/** Visited state. */
	public enum Visited {
		NotVisited, BeingVisited, DoneVisited
	}

	/**
	 * Class declaration associated to the vertex.
	 */
	public ClassSymbol classDeclaration = null;

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
	 *        Class symbol.
	 */
	public Vertex(ClassSymbol c) {
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
}

/**
 * Die Klasse repräsentiert den Syntaxbaum des gesamten Programms.
 * Sie ist der Einstiegspunkt für die Kontextanalyse und die
 * Synthese.
 */
class Program {
	SemanticAnalysis sem = new SemanticAnalysis();

	/**
	 * User-defined classes.
	 */
	List<ClassSymbol> classes = new LinkedList<>();

	/**
	 * Initialisation statements.
	 */
	List<Statement> init = new LinkedList<>();

	final scala.Option<ClassSymbol> x = scala.Option.apply(null);
	final scala.Option<Symbol> x2 = scala.Option.apply(null);

	/**
	 * Constructor.
	 */
	public Program() {
		/* Add a statement that instantiates the class `Main' and calls its method main().
		 * Equivalent to NEW Main.main. */
		this.init.add(new CallStatement(new AccessExpression(new NewExpression(
				new ResolvableClassSymbol(
						new Identifier("Main", new Position(0, 0)), this.x)),
				new VarOrCall(new ResolvableSymbol(new Identifier("main",
						new Position(0, 0)), this.x2)))));
	}

	/**
	 * Definiere Klasse.
	 *
	 * @param clazz
	 *        Die benutzerdefinierte Klasse.
	 */
	public void addClass(ClassSymbol clazz) {
		this.classes.add(clazz);
	}

	/**
	 * Check whether the class dependencies are an acyclic graph.
	 *
	 * @param classes
	 * @throws CompileException
	 */
	public void checkCycles(List<ClassSymbol> classes)
			throws CompileException {
		/* TODO perform cycle checking directly in ClassSymbol */
		Map<ClassSymbol, Vertex> mapping = new HashMap<>();

		for (ClassSymbol cls : classes) {
			mapping.put(cls, new Vertex(cls));
		}

		/* Dummy node facilitating traversal. */
		Vertex root = new Vertex();

		for (ClassSymbol cls : classes) {
			if (cls.superClass().isDefined()) {
				mapping.get(cls.superClass().get().declaration().get())
						.addEdge(mapping.get(cls));
			} else {
				/* Connect all classes without parents to the root node, effectively this is only
				 * the `Object' class. */
				root.addEdge(mapping.get(cls));
			}
		}

		if (root.hasCycles()) {
			throw new CompileException(
					"Class hierarchy is not devoid of cycles.",
					root.classDeclaration.identifier().position());
		}

		/* Some classes may not be connected to the root node. Iterate over all
		 * vertexes that were not yet visited and check for cycles. */
		for (Vertex v : mapping.values()) {
			if (v.visited == Vertex.Visited.NotVisited && v.hasCycles()) {
				throw new CompileException(
						"Class hierarchy is not devoid of cycles.",
						v.classDeclaration.identifier().position());
			}
		}
	}

	/**
	 * Die Methode führt die semantische Analyse für das Programm durch.
	 *
	 * @throws CompileException
	 *         Während der Kontextanylyse wurde ein Fehler
	 *         gefunden.
	 */
	public void semanticAnalysis() throws CompileException {
		/* Add predeclared classes. */
		this.classes.add(this.sem.types().objectClass());
		this.classes.add(this.sem.types().intClass());
		this.classes.add(this.sem.types().boolClass());

		GlobalScope scope = new GlobalScope();
		this.sem.enter(scope);

		for (ClassSymbol c : this.classes) {
			c.defPass(this.sem);
		}

		for (Statement stmt : this.init) {
			stmt.defPass(this.sem);
		}

		for (ClassSymbol c : this.classes) {
			c.refPass(this.sem);
		}

		/* Resolve dependencies for startup statements. */
		for (Statement stmt : this.init) {
			stmt.refPass(this.sem);
		}

		this.sem.leave();

		if (this.sem.currentScope().isDefined()) {
			throw new CompileException(
					"Current scope must be None after semantic analysis.",
					new Position(0, 0));
		}

		this.checkCycles(this.classes);
	}

	/**
	 * Die Methode gibt den Syntaxbaum des Programms aus.
	 */
	void printTree() {
		TreeStream tree = new TreeStream(System.out, 4);

		for (ClassSymbol c : this.classes) {
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
		for (ClassSymbol c : this.classes) {
			c.generateCode(code);
		}

		/* Allocate space for the default exception frame. */
		code.println("_currentExceptionFrame:");
		code.println("DAT 2, 0");

		/* Generate VMT for each class. */
		for (ClassSymbol c : this.classes) {
			code.println(c.identifier().name() + ":");

			for (MethodSymbol m : scala.collection.JavaConversions.asJavaList(c
					.generateVMT())) {
				code.println("DAT 1, "
						+ c.resolveAsmMethodName(m.identifier().name()));
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