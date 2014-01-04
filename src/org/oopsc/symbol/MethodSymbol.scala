package org.oopsc.symbol

import org.oopsc._
import org.oopsc.statement._
import scala.collection.mutable.ListBuffer
import java.util

class MethodSymbol(ident: Identifier) extends ScopedSymbol(ident) {
  /** Local variable SELF. */
  var self: VariableSymbol = null

  /** Local variable BASE. */
  var base: VariableSymbol = null

  /** List of all parameters. */
  var parameters = new ListBuffer[VariableSymbol]()

  /** The method's local variables. */
  var locals = new ListBuffer[VariableSymbol]

  /** The method body, i.e. its statements. */
  var statements = new ListBuffer[Statement]

  /** Return type. Default type corresponds to Types.voidType. */
  var retType: Identifier = null
  var resolvedRetType: Option[ClassSymbol] = None

  var vmtIndex = -1
  var overrides: Option[MethodSymbol] = None

  /**
   * Returns method name in an assembly string. By accessing the declaring class,
   * it takes into account if a method was inherited.
   */
  def getAsmMethodName =
    this.declaringClass.get.identifier.name + "_" + this.identifier.name

  def getResolvedReturnType: ClassSymbol = {
    /* Resolve return type if necessary. */
    if (this.resolvedRetType.isEmpty) {
      this.resolvedRetType = Some(this.resolveClass(this.retType))
    }

    this.resolvedRetType.get
  }

  def resolveParameter(name: String): Option[Symbol] =
    parameters.find(_.name() == name)

  override protected def resolve(ident: Identifier, requestingClass: Option[ClassSymbol]): Option[Symbol] =
    resolveParameter(ident.name) match {
      case Some(m) => Some(m)
      case None => super.resolve(ident, requestingClass)
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

    /* Register variable SELF pointing to this class. */
    this.self = new VariableSymbol(new Identifier("SELF"), sem.currentClass)
    this.self.accessLevel = AccessLevel.Private
    this.self.declaringClass = Some(sem.currentClass)
    this.defineSymbol(this.self)

    /* BASE represents the inherited class. Define symbol for all classes except for Object. */
    sem.currentClass.getSuperClass() match {
      case Some(superClass) =>
        this.base = new VariableSymbol(new Identifier("BASE"), superClass)
        this.base.accessLevel = AccessLevel.Private
        this.base.declaringClass = Some(sem.currentClass)
        this.defineSymbol(this.base)

      case None =>
    }

    /* Register all parameters. */
    this.parameters.foreach(_.defPass(sem))

    /* Register all local variables. */
    this.locals.foreach(_.defPass(sem))

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
    if (this.base != null) {
      this.base.offset = offset
    }

    /* Skip return address and old frame pointer. */
    offset = 1

    /* Set offsets for local variables. */
    for (v <- this.locals) {
      v.offset = offset
      offset += 1
    }

    sem.leave()
  }

  var terminates = false

  override def refPass(sem: SemanticAnalysis) {
    sem.enter(this)

    this.overrides match {
      case Some(m) =>
        /* TODO Can private methods be overwritten by subclasses? */
        if (m.accessLevel != this.accessLevel) {
          throw new CompileException(
            s"${this.identifier.name} overwrites method in superclass ${sem.currentClass.identifier.name} with different access level.",
            this.identifier.position)
        }

      case _ =>
    }

    /* Resolve types of all parameters and variables. */
    this.parameters.foreach(_.refPass(sem))
    this.locals.foreach(_.refPass(sem))

    val hasReturnValue = this.getResolvedReturnType ne Types.voidType
    this.terminates = BranchEvaluator.terminates(sem, this)

    if (hasReturnValue && !this.terminates) {
      throw new CompileException("Method needs a return or throw statement that is always reachable.", this.retType.position)
    }

    /* Reference pass for all statements. */
    this.statements.foreach(_.refPass(sem))

    sem.leave()
  }

  def optimPass() {
    this.statements = this.statements.map(_.optimPass())
  }

  def print(tree: TreeStream) {
    tree.println(s"${this.accessLevel} METHOD ${this.identifier.name} (${this.vmtIndex}): " +
      this.resolvedRetType.map(_.name()).getOrElse("<unresolved>"))
    tree.indent

    if (!this.parameters.isEmpty) {
      tree.println("PARAMETERS")
      tree.indent
      this.parameters.foreach(_.print(tree))
      tree.unindent
    }

    if (!this.locals.isEmpty) {
      tree.println("VARIABLES")
      tree.indent
      this.locals.foreach(_.print(tree))
      tree.unindent
    }

    if (!this.statements.isEmpty) {
      tree.println("BEGIN")
      tree.indent
      this.statements.foreach(_.print(tree))
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
    val size = this.locals.size + this.parameters.size + 2

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

  def generateCode(code: CodeStream, tryContexts: Int) {
    code.println("; METHOD " + this.identifier.name)
    this.generateMethodPrologue(code)

    code.println("")
    code.println("; Statements")
    code.println("")

    for (s <- this.statements) {
      code.println("; Statement: " + s.getClass.getName)
      s.generateCode(code, tryContexts)
      code.println("")
    }

    code.println("; END METHOD " + this.identifier.name)

    /* If we encounter a `return' or `throw' in the normal control flow of the
     * method, we do not need to generate the epilogue twice.
     */
    if (!terminates) {
      this.generateMethodEpilogue(code, "")
    }
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