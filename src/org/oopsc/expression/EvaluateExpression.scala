package org.oopsc.expression

import org.oopsc._
import org.oopsc.scope._
import org.oopsc.symbol._
import scala.collection.mutable.ArrayBuffer

/**
 * Represents a variable/attribute access or a method call.
 */
class EvaluateExpression(var ref: ResolvableSymbol) extends Expression(ref.identifier.position) {
  protected var scope: Scope = null
  protected var context: VariableSymbol = null
  protected var isStaticContext = false
  protected var generateContextCode = true

  var arguments = new ArrayBuffer[Expression]

  def generateContextCode(value: Boolean) {
    this.generateContextCode = value
  }

  def addArgument(value: Expression) {
    arguments += value
  }

  /**
   * Sets a static/dynamic context. By default, methods are called dynamically by resolving the target
   * index from the VMT to which the object points to. A static context bypasses the VMT and calls
   * the method directly. This method can be used for calling methods from the base class.
   */
  def setContext(context: VariableSymbol, isStatic: Boolean) {
    this.context = context
    this.isStaticContext = isStatic
  }

  def setScope(scope: Scope) {
    this.scope = scope
  }

  override def refPass(sem: SemanticAnalysis) {
    val resolveScope = if (this.scope == null) sem.currentScope.get else this.scope

    /* Resolve variable or method. */
    this.ref.declaration = Some(resolveScope.resolveSymbol(this.ref.identifier, Some(sem.currentClass)))

    /* Check arguments. */
    if (this.ref.declaration.get.isInstanceOf[ClassSymbol]) {
      if (this.arguments.size != 1) {
        throw new CompileException("A type cast expects exactly one argument.", this.ref.identifier.position)
      }
    } else if (!this.ref.declaration.get.isInstanceOf[MethodSymbol]) {
      if (this.arguments.size != 0) {
        throw new CompileException("Arguments cannot be passed to a variable.", this.ref.identifier.position)
      }
    }

    /* Resolve method or attribute context. */
    if (this.generateContextCode) {
      if (this.ref.declaration.get.isInstanceOf[MethodSymbol] || this.ref.declaration.get.isInstanceOf[AttributeSymbol]) {
        if (this.context == null && sem.currentMethod != null) {
          this.context = sem.currentMethod.self
        }
      }
    }

    this.ref.declaration.get match {
      case sym: VariableSymbol =>
        this.lValue = true

      case decl: MethodSymbol =>
        /* Verify that the passed arguments match the expected parameters. */
        if (this.arguments.size != decl.parameters.size) {
          throw new CompileException(s"Parameter count mismatch: ${decl.parameters.size} expected, ${this.arguments.size} given.", this.ref.identifier.position)
        }

        for (((arg, param), num) <- this.arguments.zip(decl.parameters).zipWithIndex) {
          arg.refPass(sem)

          if (!arg.resolvedType().isA(param.getResolvedType)) {
            throw new CompileException(
              s"Argument ${num + 1} mismatches: ${param.resolvedType.get.identifier.name} expected, ${arg.resolvedType().name} given.", this.ref.identifier.position)
          }
        }

      case clazz: ClassSymbol =>
        this.arguments(0).refPass(sem)
    }
  }

  /**
   * Override to propagate the resolved type.
   */
  override def resolvedType(): ClassSymbol =
    this.ref.declaration.get match {
      case v: VariableSymbol => v.getResolvedType
      case m: MethodSymbol => m.getResolvedReturnType
      case c: ClassSymbol => c
      case _ => super.resolvedType()
    }

  def print(tree: TreeStream) {
    tree.println(this.ref.identifier.name + " : " + (if (this.lValue) "REF " else "") + this.resolvedType().name)
  }

  protected def _generateContextCode(code: CodeStream) {
    if (this.context != null) {
      code.println("; Context: " + this.context.identifier.name)
      val `var` = new EvaluateExpression(new ResolvableSymbol(this.context.identifier, new Some[Symbol](this.context)))
      `var`.lValue = true
      `var`.generateCode(code, false)
      code.println("; End context.")
    } else {
      code.println("; No context.")
    }
  }

  def generateCode(code: CodeStream) {
    this.ref.declaration.get match {
      case sym: ClassSymbol =>
        code.println("; CAST")
        val arg = this.arguments(0)

        /* Push the object reference on the stack (R2). */
        arg.generateCode(code, true)

        /* TODO What is supposed to happen for casts such as Object(NULL) or Main(NULL)? */

        code.println("MRM R5, (R2)") // R5 = Evaluated value of this.oper.

        val endLabel = code.nextLabel
        TypeCheckExpression.checkType(code, sym.identifier.name, endLabel)

        /* Code to be executed upon type mismatch: *R2 = NULL */
        code.println("MRI R5, 0")
        code.println("MMR (R2), R5")

        code.println(s"$endLabel:")

        /* Code to be executed after match or mismatch. If R2 was not set to NULL (upon a mismatch),
         * it will now still contain the object reference.
         */

      case sym: AttributeSymbol =>
        /* An attribute is stored in the class object. */
        code.println("; Referencing attribute " + this.ref.identifier.name)
        this._generateContextCode(code)
        code.println("MRM R5, (R2)")
        code.println("MRI R6, " + sym.offset)
        code.println("ADD R5, R6")
        code.println("MMR (R2), R5")

      case sym: VariableSymbol =>
        /* A variable is stored in the stack frame. */
        code.println("; Referencing local variable " + this.ref.identifier.name)
        code.println("MRI R5, " + sym.offset)
        code.println("ADD R5, R3")
        code.println("ADD R2, R1")
        code.println("MMR (R2), R5")

      case m: MethodSymbol =>
        val returnLabel = code.nextLabel
        if (this.context != null && this.isStaticContext) {
          code.println("; Static method call: " + this.ref.identifier.name)
          code.println("; Arguments")
          code.println("")

          /* Push arguments on the stack. */
          for ((e, i) <- this.arguments.zipWithIndex) {
            code.println("; Argument " + i)
            code.println("; " + e.getClass)
            e.generateCode(code, true)
          }

          /* Push return address on the stack. */
          code.println("MRI R5, " + returnLabel + " ; Return address.")
          code.println("ADD R2, R1")
          code.println("MMR (R2), R5 ; Save return address on the stack.")

          /* Jump to method by overwriting PC. */
          code.println("MRI R0, " + m.getAsmMethodName)
        } else {
          this._generateContextCode(code)
          code.println("; Dynamic method call: " + this.ref.identifier.name)
          code.println("; VMT index = " + m.vmtIndex)
          code.println("; Arguments")
          code.println("")

          /* Push arguments on the stack. */
          for ((e, i) <- this.arguments.zipWithIndex) {
            code.println("; Argument " + i)
            code.println("; " + e.getClass)
            e.generateCode(code, true)
          }

          /* Push return address on the stack. */
          code.println("MRI R5, " + returnLabel + " ; Return address.")
          code.println("ADD R2, R1")
          code.println("MMR (R2), R5 ; Save return address on the stack.")

          /* Resolve function address from VMT. */
          code.println("MRR R5, R2")
          code.println("MRI R6, " + (1 + this.arguments.size))
          code.println("SUB R5, R6")

          code.println("MRM R6, (R5)") // R5 = Object address.
          code.println("MRM R6, (R6)") // R6 = VMT address.

          code.println("MRI R5, " + m.vmtIndex)
          code.println("ADD R6, R5")

          /* Jump to method by overwriting PC. */
          code.println("MRM R0, (R6)")
        }

        code.println(returnLabel + ":")
    }
  }
}