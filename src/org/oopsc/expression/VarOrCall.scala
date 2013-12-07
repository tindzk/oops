package org.oopsc.expression

import org.oopsc._
import org.oopsc.scope._
import org.oopsc.symbol._
import scala.collection.mutable.ArrayBuffer

/**
 * Represents a variable/attribute access or method call expression in the syntax tree.
 */
class VarOrCall(var ref: ResolvableSymbol) extends Expression(ref.identifier.position) {
  var scope: Scope = null

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
   * Sets a (static) context. By default, methods are called dynamically by resolving the target
   * index from the VMT to which the object counts to. A static context bypasses the VMT and calls
   * the method directly. This method can be used for calling methods in the base class.
   *
   * @param context
   */
  def setContext(context: VariableSymbol, isStatic: Boolean) {
    this.context = context
    this.isStaticContext = isStatic
  }

  override def refPass(sem: SemanticAnalysis) {
    val resolveScope = if (this.scope == null) sem.currentScope.get else this.scope

    /* Resolve variable or method. */
    this.ref.declaration = Some(resolveScope.checkedResolve(this.ref.identifier))

    /* Check arguments. */
    if (!this.ref.declaration.get.isInstanceOf[MethodSymbol]) {
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

    if (this.ref.declaration.get.isInstanceOf[VariableSymbol]) {
      this.lValue = true
    } else if (this.ref.declaration.get.isInstanceOf[MethodSymbol]) {
      /* Verify that the passed arguments match the expected parameters. */
      val decl = this.ref.declaration.get.asInstanceOf[MethodSymbol]

      if (this.arguments.size != decl.parameters.size) {
        throw new CompileException(s"Parameter count mismatch: ${decl.parameters.size} expected, ${this.arguments.size} given.", this.ref.identifier.position)
      }

      for (((arg, param), num) <- this.arguments.zip(decl.parameters).zipWithIndex) {
        arg.refPass(sem)

        if (!arg.resolvedType().isA(sem, param.getResolvedType)) {
          throw new CompileException(
            s"Argument ${num + 1} mismatches: ${param.resolvedType.get.identifier.name} expected, ${arg.resolvedType().name} given.", this.ref.identifier.position)
        }
      }
    }
  }

  override def resolvedType() : ClassSymbol = {
    /* Propagate resolved type. */
    if (this.ref.declaration.get.isInstanceOf[VariableSymbol]) {
      val `var` = this.ref.declaration.get.asInstanceOf[VariableSymbol]
      return `var`.getResolvedType
    } else if (this.ref.declaration.get.isInstanceOf[MethodSymbol]) {
      val method = this.ref.declaration.get.asInstanceOf[MethodSymbol]
      return method.getResolvedReturnType
    }

    return super.resolvedType()
  }

  def print(tree: TreeStream) {
    tree.println(this.ref.identifier.name + " : " + (if (this.lValue) "REF " else "") + this.resolvedType().name)
  }

  protected def _generateContextCode(code: CodeStream) {
    if (this.context != null) {
      code.println("; Context: " + this.context.identifier.name)
      val `var` = new VarOrCall(new ResolvableSymbol(this.context.identifier, new Some[Symbol](this.context)))
      `var`.lValue = true
      `var`.generateCode(code, false)
      code.println("; End context.")
    } else {
      code.println("; No context.")
    }
  }

  def generateCode(code: CodeStream) {
    this.ref.declaration.get match {
      case sym: AttributeSymbol =>
        this._generateContextCode(code)

        /* Stored in the class object. */
        code.println("; Referencing attribute " + this.ref.identifier.name)
        code.println("MRM R5, (R2)")
        code.println("MRI R6, " + sym.offset)
        code.println("ADD R5, R6")
        code.println("MMR (R2), R5")

      case sym: VariableSymbol =>
        /* Stored in the stack frame. */
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
          code.println("MRI R0, " + this.context.resolvedType.get.resolveAsmMethodName(m.identifier.name))
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