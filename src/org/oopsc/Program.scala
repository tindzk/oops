package org.oopsc

import org.oopsc.expression._
import org.oopsc.scope._
import org.oopsc.symbol._
import org.oopsc.statement._
import scala.collection.mutable.ListBuffer

/**
 * Represents the syntax tree of the entire program. Entry point for contextual
 * analysis and code generation.
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
  this.init += new CallStatement(new AccessExpression(new NewExpression(new ResolvableClassSymbol(new Identifier("Main"))), new EvaluateExpression(new ResolvableSymbol(new Identifier("main")))))

  /**
   * Defines custom class.
   */
  def addClass(clazz: ClassSymbol) {
    this.classes += clazz
  }

  /**
   * Performs the semantic analysis for the entire program.
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
  }

  /**
   * Optimisation pass for all classes.
   */
  def optimise {
    this.classes.foreach(_.optimPass())
  }

  /**
   * Prints syntax tree.
   */
  def printTree {
    val tree = new TreeStream(System.out, 4)

    for (c <- this.classes) {
      c.print(tree)
    }
  }

  /**
   * Generates assembly code.
   *
   * @param code Output stream.
   */
  def generateCode(code: CodeStream, stackSize: Int, heapSize: Int) {
    /* Initialise registers. */
    code.setNamespace("_init")
    code.println(s"; Generated by OOPS-0 compiler, v${OOPSC.Version}.")
    code.println("MRI R1, 1 ; R1 is always 1.")
    code.println("MRI R2, _stack ; R2 points to the stack.")
    code.println("MRI R4, _heap ; R4 points to the next free slot on the heap.")

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

    code.println("MRI R0, _end ; Stop program.")

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

      /* Add an entry for the super class. */
      c.getSuperClass() match {
        case Some(p) =>
          code.println(s"DAT 1, ${p.identifier.name}")
        case None =>
          /* c is Object. */
          code.println(s"DAT 1, 0")
      }

      for (m <- c.generateVMT) {
        code.println("DAT 1, " + m.getAsmMethodName)
      }
    }

    /* Allocate space for the stack and the heap. */
    code.println("_stack: ; Beginning of the stack.")
    code.println(s"DAT $stackSize, 0")

    code.println("_heap: ; Beginning of the heap.")
    code.println(s"DAT $heapSize, 0")

    /* Print read-only data segment. */
    for ((s, i) <- this.sem.strings.zipWithIndex) {
      code.println(s"_rodata_$i: ; $s")
      code.println(s"DAT 1, ${s.length}")
      s.foreach(c => code.println(s"DAT 1, ${c.asInstanceOf[Int]}"))
    }

    /* Function being jumped to when an exception could not be caught. */
    code.println("_uncaughtException:")
    val s = "ABORT "
    for (c <- s.getBytes) {
      code.println("MRI R5, " + c.asInstanceOf[Int])
      code.println("SYS 1, 5")
    }
    code.println("MRR R5, R7")
    code.println("SYS 1, 5")

    code.println("_end: ; End of the program.")
  }
}