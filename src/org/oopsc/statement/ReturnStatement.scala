package org.oopsc.statement

import org.oopsc._
import org.oopsc.symbol._
import org.oopsc.expression._

class ReturnStatement(var position: Position, var value: Expression = null) extends Statement {
  protected var method: MethodSymbol = null

  override def refPass(sem: SemanticAnalysis) {
    this.method = sem.currentMethod
    val retType = sem.currentMethod.getResolvedReturnType

    if (this.value == null) {
      if (retType ne Types.voidType) {
        throw new CompileException(s"Return value of type ${retType.name} expected.", this.position)
      }
    } else {
      this.value.refPass(sem)

      if (retType eq Types.voidType) {
        throw new CompileException("No return value expected.", this.value.position)
      }

      this.value.resolvedType.check(sem, retType, this.value.position)
    }
  }

  override def print(tree: TreeStream) {
    tree.println("RETURN")

    if (this.value != null) {
      tree.indent
      this.value.print(tree)
      tree.unindent
    }
  }

  override def generateCode(code: CodeStream, tryContexts: Int) {
    code.println("; RETURN")

    if (this.value == null) {
      /* For each RETURN statement within a TRY block, we need to unwind the stack
       * accordingly. */
      for (c <- 1 to tryContexts) {
        TryStatement.popException(code, false)
      }

      this.method.generateMethodEpilogue(code, "")
    } else {
      this.value.generateCode(code, true)

      /* Back up the value R2 points to by copying it to the register R7. R2 points
       * to the result of this.value. */
      code.println("MRM R7, (R2)")

      /* For each RETURN statement within a TRY block, we need to unwind the stack
       * accordingly. */
      for (c <- 1 to tryContexts) {
        TryStatement.popException(code, false)
      }

      /* The epilogue modifies R2 by making it point to its original value before
       * the method call. Inject the following instruction to restore our copy of
       * the return value in the register R7. */
      val customInstruction = "MMR (R2), R7"
      this.method.generateMethodEpilogue(code, customInstruction)
    }

    code.println("; END RETURN")
  }
}