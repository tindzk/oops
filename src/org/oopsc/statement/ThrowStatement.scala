package org.oopsc.statement

import org.oopsc._
import org.oopsc.expression.Expression

object ThrowStatement {
  def throwException(code: CodeStream) {
    /* Load the pointer to the global variable _currentExceptionFrame into R5. */
    code.println("MRI R5, _currentExceptionFrame")

    /* Dereference the value, i.e., load the exception frame. */
    code.println("MRM R5, (R5)")

    /* Load the address of the exception handler from the current exception frame
     * and jump to it. */
    code.println("MRM R0, (R5)")
  }
}

/**
 * Statement for triggering an exception.
 */
class ThrowStatement(var value: Expression, var position: Position) extends Statement {
  override def refPass(sem: SemanticAnalysis) {
    this.value.refPass(sem)
    this.value.resolvedType.check(sem, Types.intType, this.value.position)
  }

  override def optimPass() : Statement = {
    this.value = this.value.optimPass()
    this
  }

  override def print(tree: TreeStream) {
    tree.println("THROW")
    tree.indent
    this.value.print(tree)
    tree.unindent
  }

  override def generateCode(code: CodeStream, tryContexts: Int) {
    code.println("; THROW")

    /* Push the exception code on the stack. */
    this.value.generateCode(code, false)

    /* Copy the exception code to R7. */
    code.println("MRM R7, (R2)")

    /* Pop the exception code from the stack. */
    code.println("SUB R2, R1")

    /* Pass the exception to the inner-most exception handler and propagate it if
     * necessary. */
    ThrowStatement.throwException(code)

    code.println("; END THROW")
  }
}