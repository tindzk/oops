package org.oopsc.statement

import org.oopsc._
import org.oopsc.expression._
import scala.collection.mutable.ListBuffer

object TryStatement {
  /**
   * After the exception frame allocated by a TRY block is not used anymore, this
   * method must be called to unwind the stack to its state before.
   */
  def popException(code: CodeStream, restoreStackFp: Boolean) {
    /* Load current exception frame and dereference it. */
    code.println("MRI R6, _currentExceptionFrame")
    code.println("MRM R6, (R6)")

    /* Fix up the stack by making it point to the position right before the
     * exception frame. */
    code.println("MRR R2, R6")
    code.println("SUB R2, R1")

    if (restoreStackFp) {
      code.println("MRM R3, (R2)")
    }

    code.println("SUB R2, R1")

    /* Load the previous exception frame pointer into R6. */
    code.println("ADD R6, R1")
    code.println("MRM R6, (R6)") /* Dereference value. */

    /* Load the pointer to the global variable _currentExceptionFrame into R5. */
    code.println("MRI R5, _currentExceptionFrame")

    /* Make the global exception frame marker point to the previous exception
     * frame pointer. */
    code.println("MMR (R5), R6")
  }
}

/**
 * Implements a TRY statement which is used for exception handling.
 */
class TryStatement(tryStatements: ListBuffer[Statement], position: Position) extends Statement {
  /**
   * CATCH branches assigning a statement block to a value that needs to be caught in order for
   * the statements to be executed.
   */
  var catchStatements = Map.empty[LiteralExpression, ListBuffer[Statement]]

  override def refPass(sem: SemanticAnalysis) {
    for (s <- this.tryStatements) {
      s.refPass(sem)
    }

    for ((expr, stmts) <- this.catchStatements) {
      expr.resolvedType.check(sem, Types.intType, expr.position)

      for (s <- stmts) {
        s.refPass(sem)
      }
    }

    if (this.catchStatements.isEmpty) {
      throw new CompileException("At least one catch block is required in a TRY statement.", this.position)
    }
  }

  def addCatchBlock(condition: LiteralExpression, stmts: ListBuffer[Statement]) {
    this.catchStatements += (condition -> stmts)
  }

  override def print(tree: TreeStream) {
    tree.println("TRY")

    if (!this.tryStatements.isEmpty) {
      tree.indent

      for (s <- this.tryStatements) {
        s.print(tree)
      }

      tree.unindent
    }

    for ((expr, stmts) <- this.catchStatements) {
      tree.println("CATCH")
      tree.indent

      expr.print(tree)

      for (s <- stmts) {
        s.print(tree)
      }

      tree.unindent
    }
  }

  /**
   * We create a new exception frame on the stack. An exception frame consists of
   * two variables: the address of the last exception frame and the address where
   * to continue the execution when an exception was thrown.
   */
  override def generateCode(code: CodeStream, tryContexts: Int) {
    code.println("; TRY")

    /* Push the frame pointer on the stack as we will need to restore it later. */
    code.println("ADD R2, R1")
    code.println("MMR (R2), R3")

    var catchLabel = code.nextLabel

    /* Push address to the exception handler on the stack, denotes at the same
     * time the beginning of our new exception frame. */
    code.println("MRI R5, " + catchLabel)
    code.println("ADD R2, R1")
    code.println("MMR (R2), R5")

    /* Push the address pointing to the current exception frame on the stack. */
    code.println("MRI R5, _currentExceptionFrame")
    code.println("MRM R5, (R5)") /* Dereference the value. */
    code.println("ADD R2, R1")
    code.println("MMR (R2), R5")

    /* Overwrite the global exception frame pointer with the address of the catch label.
     * R5 = address of the global variable _currentExceptionFrame
     * R6 = address of our new current exception frame */
    code.println("MRR R6, R2")
    code.println("SUB R6, R1")
    code.println("MRI R5, _currentExceptionFrame")
    code.println("MMR (R5), R6")

    val endLabel = code.nextLabel

    for (s <- this.tryStatements) {
      s.generateCode(code, tryContexts + 1)
    }

    /* This instruction is only reached if no exception was thrown. */
    code.println("MRI R0, " + endLabel)

    for ((expr, stmts) <- this.catchStatements) {
      /* An exception was thrown. */
      code.println("; CATCH " + expr.value)
      code.println(catchLabel + ":")
      catchLabel = code.nextLabel

      /* When an exception is thrown, the associated error code is stored in R7. */
      code.println("MRI R5, " + expr.value)
      code.println("SUB R5, R7")

      /* If entry.getKey().value != error code, jump to the next `catch' branch. */
      code.println("ISZ R5, R5")
      code.println("XOR R5, R1")
      code.println("JPC R5, " + catchLabel)

      /* The exception was caught. Therefore, pop the exception off the stack
       * before executing the statements. */
      TryStatement.popException(code, true)

      for (stmt <- stmts) {
        stmt.generateCode(code, tryContexts)
      }

      /* Jump to the end of the TRY block. */
      code.println("MRI R0, " + endLabel)
      code.println("; END CATCH")
    }

    /* The exception could not be dealt with. */
    code.println(catchLabel + ":")

    /* Pop the exception off the stack, restoring the stack frame pointer. */
    TryStatement.popException(code, true)

    /* Propagate the exception to the next exception handler. */
    ThrowStatement.throwException(code)

    code.println("; END TRY")
    code.println(endLabel + ":")
  }
}