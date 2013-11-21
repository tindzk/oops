package org.oopsc.symbol

import org.oopsc._
import org.oopsc.statement._
import scala.collection.mutable.ListBuffer
import java.util

class MethodSymbol(ident: Identifier) extends ScopedSymbol(ident) {
  /** Local variable SELF. */
  var self = new VariableSymbol(new Identifier("_self"), null)

  /** Local variable BASE. */
  var base = new VariableSymbol(new Identifier("_base"), null)

  /** List of all parameters. */
  var parameters = new ListBuffer[VariableSymbol]()

  /** The method's local variables. */
  var locals = new ListBuffer[VariableSymbol]

  /** The method body, i.e. its statements. */
  var statements = new ListBuffer[Statement]

  /** Return type. Default type corresponds to Types.voidType. */
  var retType: Identifier = null
  var resolvedRetType: Option[ClassSymbol] = None

  var vmtIndex: Int = -1

  def getResolvedReturnType: ClassSymbol = {
    /* Resolve return type if necessary. */
    if (this.resolvedRetType.isEmpty) {
      this.resolvedRetType = Some(this.resolveClass(this.retType))
    }

    this.resolvedRetType.get
  }

  def resolveParameter(name: String): Option[Symbol] =
    parameters.find(_.name() == name)

  override def resolve(name: String): Option[Symbol] =
    resolveParameter(name) match {
      case Some(m) => Some(m)
      case None => super.resolve(name)
    }

  def defPass(sem: SemanticAnalysis) {
    sem.defineSymbol(this)
    sem.enter(this)

    if (this.retType == null) {
      this.resolvedRetType = new Some(Types.voidType)
    }

    if ((sem.currentClass.name == "Main") && (this.identifier.name == "main")) {
      if (this.parameters.size != 0) {
        throw new CompileException("Main.main() must not have any parameters.", this.identifier.position)
      } else if (this.getResolvedReturnType ne Types.voidType) {
        throw new CompileException("Main.main() must not have a non-void return type.", this.identifier.position)
      }
    }

    /* SELF points to this class. */
    this.self.resolvedType = new Some(sem.currentClass)

    /* BASE represents the inherited class, may be None. */
    this.base.resolvedType = sem.currentClass.getSuperClass()

    /* Insert BASE if it is typed. */
    if (this.base.resolvedType.isDefined) {
      this.defineSymbol(this.base)
    }

    /* Register SELF. */
    this.defineSymbol(this.self)

    /* Register all parameters. */
    for (v <- this.parameters) {
      v.defPass(sem)
    }

    /* Register all local variables. */
    for (v <- this.locals) {
      v.defPass(sem)
    }

    /* Perform the definition pass for statements. */
    for (s <- this.statements) {
      s.defPass(sem)
    }

    /* Set offsets for parameters. They are right before the return address (-1) on the stack. */
    var offset = -2
    for (v <- this.parameters.reverse) {
      v.offset = offset
      offset -= 1
    }

    /* SELF is right before the parameters on the stack. */
    this.self.offset = offset

    /* BASE has the same address on the stack as SELF, however the type of BASE
     * corresponds to the base type.
     */
    this.base.offset = offset

    /* Skip return address and old frame pointer. */
    offset = 1

    /* Set offsets for local variables. */
    for (v <- this.locals) {
      v.offset = offset
      offset += 1
    }

    sem.leave()
  }

  var hasReturnValue = false
  var terminates = false

  override def refPass(sem: SemanticAnalysis) {
    sem.enter(this)

    /* Resolve types of all parameters. */
    for (v <- this.parameters) {
      v.refPass(sem)
    }

    /* Resolve types of all variables. */
    for (v <- this.locals) {
      v.refPass(sem)
    }

    this.hasReturnValue = this.getResolvedReturnType ne Types.voidType
    this.terminates = BranchEvaluator.terminates(sem, this)

    if (this.hasReturnValue && !this.terminates) {
      throw new CompileException("Method needs a return statement that is always reachable.", this.retType.position)
    }

    /* Reference pass for all statements. */
    for (s <- this.statements) {
      s.refPass(sem)
    }

    sem.leave()
  }

  def print(tree: TreeStream) {
    /* TODO Print parameters and return type. */
    tree.println("METHOD " + this.identifier.name)
    tree.indent

    if (!this.locals.isEmpty) {
      tree.println("VARIABLES")
      tree.indent

      for (v <- this.locals) {
        v.print(tree)
      }

      tree.unindent
    }

    if (!this.statements.isEmpty) {
      tree.println("BEGIN")
      tree.indent

      for (s <- this.statements) {
        s.print(tree)
      }

      tree.unindent
    }

    tree.unindent
  }

  protected def generateMethodPrologue(code: CodeStream) {
    val ns = this.self.getResolvedType.name() + "_" + this.identifier.name
    code.setNamespace(ns)

    code.println(ns + ":")
    code.println("ADD R2, R1")
    code.println("MMR (R2), R3 ; Save current stack frame in R2.")
    code.println("MRR R3, R2 ; Save current stack position in the new stack frame.")

    if (!this.locals.isEmpty) {
      code.println("MRI R5, " + this.locals.size)
      code.println("ADD R2, R5 ; Allocate space for local variables.")
    }
  }

  /**
   * @param customInstruction
   * Will be inserted after fixing up the stack.
   */
  def generateMethodEpilogue(code: CodeStream, customInstruction: String) {
    /* Calculate size of stack space occupied by this method and its call, +2 for old stack frame and
     * return address.
     */
    val size: Int = this.locals.size + this.parameters.size + 2

    /* Make R2 point to the same address as before the method was called. */
    code.println("MRI R5, " + (size + 1))
    code.println("SUB R2, R5 ; Free the stack space.")

    if (customInstruction.length != 0) {
      code.println(customInstruction)
    }

    /* Load the return address (R3 - 1) into R5, so that we can later jump to it. */
    code.println("SUB R3, R1")
    code.println("MRM R5, (R3) ; Get old return address.")
    code.println("ADD R3, R1")

    /* Make R3 point to the previous stack frame. */
    code.println("MRM R3, (R3)")

    /* Jump to the return address (R5). */
    code.println("MRR R0, R5 ; Jump back.")
    code.println("")
  }

  /**
   * Generiert den Assembler-Code für diese Methode. Dabei wird davon ausgegangen,
   * dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
   *
   * @param code
   * Der Strom, in den die Ausgabe erfolgt.
   * @param contexts
   * Current stack of contexts, may be used to inject instructions for
   * unwinding the stack (as needed for RETURN statements in TRY blocks).
   */
  def generateCode(code: CodeStream, contexts: util.Stack[Statement.Context]) {
    code.println("; METHOD " + this.identifier.name)
    this.generateMethodPrologue(code)

    code.println("")
    code.println("; Statements")
    code.println("")

    for (s <- this.statements) {
      code.println("; Statement: " + s.getClass.getName)
      s.generateCode(code, contexts)
      code.println("")
    }

    code.println("; END METHOD " + this.identifier.name)

    if (this.hasReturnValue && terminates) {
      /* If we encounter a `return' or `throw' in the normal control flow of the
       * method, we do not need to generate the epilogue twice.
       */
      return
    }

    this.generateMethodEpilogue(code, "")
  }

  /**
   * Compares the signature for equality.
   *
   * @param m Comparison method.
   * @return true if signatures are equal, false otherwise.
   */
  def signatureEquals(m: MethodSymbol): Boolean = {
    if (this.getResolvedReturnType ne m.getResolvedReturnType) {
      return false
    }

    if (m.parameters.size != this.parameters.size) {
      return false
    }

    for ((left, right) <- this.parameters.zip(m.parameters)) {
      if (left.getResolvedType ne right.getResolvedType) {
        return false
      }
    }

    true
  }
}