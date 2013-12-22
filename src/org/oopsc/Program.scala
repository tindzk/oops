package org.oopsc

import org.oopsc.expression._
import org.oopsc.scope._
import org.oopsc.symbol._
import org.oopsc.statement._
import scala.collection.mutable.{ListBuffer, HashMap}

object Vertex {
  /** Visited state. */
  object Visited extends Enumeration {
    type Visited = Value
    val NotVisited, BeingVisited, DoneVisited = Value
  }
}

class Vertex(var classDeclaration: ClassSymbol = null) {
  import Vertex._

  /**
   * By default, all vertexes are not visited.
   */
  var visited = Visited.NotVisited

  /**
   * The linked vertexes.
   */
  var edges = new ListBuffer[Vertex]

  /**
   * Adds edge to vertex.
   *
   * @param v Vertex
   */
  def addEdge(v: Vertex) {
    this.edges += v
  }

  /**
   * Recursively checks for cycles applying a depth-first search (DFS).
   *
   * @return true if vertex or its links contains cycles, false otherwise.
   */
  def hasCycles: Boolean = {
    if (this.visited eq Visited.BeingVisited) {
      return true
    } else if (this.visited eq Visited.DoneVisited) {
      return false
    }

    this.visited = Visited.BeingVisited

    for (v <- this.edges) {
      if (v.hasCycles) {
        return true
      }
    }

    this.visited = Visited.DoneVisited
    return false
  }
}

/**
 * Die Klasse repräsentiert den Syntaxbaum des gesamten Programms.
 * Sie ist der Einstiegspunkt für die Kontextanalyse und die
 * Synthese.
 */
class Program {
  var sem = new SemanticAnalysis

  /**
   * User-defined classes.
   */
  var classes = new ListBuffer[ClassSymbol]
  /**
   * Initialisation statements.
   */
  var init = new ListBuffer[Statement]

  /* Add a statement that instantiates the class `Main' and calls its method main().
   * Equivalent to NEW Main.main. */
  this.init += new CallStatement(new AccessExpression(new NewExpression(new ResolvableClassSymbol(new Identifier("Main"))), new VarOrCall(new ResolvableSymbol(new Identifier("main")))))

  /**
   * Definiere Klasse.
   *
   * @param clazz
	 * Die benutzerdefinierte Klasse.
   */
  def addClass(clazz: ClassSymbol) {
    this.classes += clazz
  }

  /**
   * Check whether the class dependencies are an acyclic graph.
   *
   * @param classes
   * @throws CompileException
   */
  def checkCycles(classes: ListBuffer[ClassSymbol]) {
    /* TODO perform cycle checking directly in ClassSymbol */
    val mapping = new HashMap[ClassSymbol, Vertex]

    for (cls <- classes) {
      mapping.put(cls, new Vertex(cls))
    }

    /* Dummy node facilitating traversal. */
    val root = new Vertex
    for (cls <- classes) {
      cls.superClass match {
        case Some(superCls) =>
          mapping.get(superCls.declaration.get).get.addEdge(mapping.get(cls).get)

        case None =>
          /* Connect all classes without parents to the root node, effectively this is only
           * the `Object' class. */
          root.addEdge(mapping.get(cls).get)
      }
    }

    if (root.hasCycles) {
      throw new CompileException("Class hierarchy is not devoid of cycles.", root.classDeclaration.identifier.position)
    }

    /* Some classes may not be connected to the root node. Iterate over all
     * vertexes that were not yet visited and check for cycles. */
    for (v <- mapping.values) {
      if ((v.visited eq Vertex.Visited.NotVisited) && v.hasCycles) {
        throw new CompileException("Class hierarchy is not devoid of cycles.", v.classDeclaration.identifier.position)
      }
    }
  }

  /**
   * Performs the semantic analysis for the whole program.
   */
  def semanticAnalysis {
    /* Add predeclared classes. */
    this.classes += Types.objectClass
    this.classes += Types.intClass
    this.classes += Types.boolClass

    val scope = new GlobalScope
    this.sem.enter(scope)

    for (c <- this.classes) {
      c.defPass(this.sem)
    }

    for (c <- this.classes) {
      c.refPass(this.sem)
    }

    /* Resolve dependencies for startup statements. */
    for (stmt <- this.init) {
      stmt.refPass(this.sem)
    }

    this.sem.leave

    if (this.sem.currentScope.isDefined) {
      throw new CompileException("Current scope must be None after semantic analysis.")
    }

    this.checkCycles(this.classes)
  }

  def optimise {
    this.classes.foreach(_.optimPass())
  }

  /**
   * Die Methode gibt den Syntaxbaum des Programms aus.
   */
  def printTree {
    val tree = new TreeStream(System.out, 4)

    for (c <- this.classes) {
      c.print(tree)
    }
  }

  /**
   * Die Methode generiert den Assembler-Code für das Programm. Sie geht
   * davon aus, dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
   *
   * @param code
	 * Der Strom, in den die Ausgabe erfolgt.
   */
  def generateCode(code: CodeStream, stackSize: Int, heapSize: Int) {
    /* Initialise registers. */
    code.setNamespace("_init")
    code.println("; Erzeugt durch OOPS-0 compiler, Version 2012-03-15.")
    code.println("MRI R1, 1 ; R1 ist immer 1")
    code.println("MRI R2, _stack ; R2 zeigt auf Stapel")
    code.println("MRI R4, _heap ; R4 zeigt auf die nächste freie Stelle auf dem Heap")

    /* Initialise the initial exception frame, i.e., the first element is a
     * pointer to the second one which itself points to the default exception
     * handler (_uncaughtException). */
    code.println("MRI R5, _currentExceptionFrame")
    code.println("MRI R6, _currentExceptionFrame")
    code.println("ADD R6, R1")
    code.println("MMR (R5), R6")
    code.println("ADD R5, R1")
    code.println("MRI R6, _uncaughtException")
    code.println("MMR (R5), R6")

    /* Generate code for initialisation statements. */
    for (stmt <- this.init) {
      stmt.generateCode(code, 0)
    }

    code.println("MRI R0, _end ; Programm beenden")

    /* Generate code for user-defined classes. */
    for (c <- this.classes) {
      c.generateCode(code)
    }

    /* Allocate space for the default exception frame. */
    code.println("_currentExceptionFrame:")
    code.println("DAT 2, 0")

    /* Generate VMT for each class. */
    for (c <- this.classes) {
      code.println(c.identifier.name + ":")

      for (m <- c.generateVMT) {
        code.println("DAT 1, " + m.getAsmMethodName)
      }
    }

    /* Allocate space for the stack and the heap. */
    code.println("_stack: ; Hier fängt der Stapel an")
    code.println("DAT " + stackSize + ", 0")

    code.println("_heap: ; Hier fängt der Heap an")
    code.println("DAT " + heapSize + ", 0")

    /* Function being jumped to when an exception could not be caught. */
    code.println("_uncaughtException:")
    val s = "ABORT "
    for (c <- s.getBytes) {
      code.println("MRI R5, " + c.asInstanceOf[Int])
      code.println("SYS 1, 5")
    }
    code.println("MRR R5, R7")
    code.println("SYS 1, 5")

    code.println("_end: ; Programmende")
  }
}