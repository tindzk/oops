package org.oopsc.statement

import org.oopsc._
import org.oopsc.expression.{StringLiteralExpression, Expression}

/**
 * Statement printing the given operand.
 */
class WriteStatement(var operand: Expression) extends Statement {
  override def refPass(sem: SemanticAnalysis) {
    this.operand.refPass(sem)

    if (!this.operand.resolvedType().isA(Types.intType) &&
       !this.operand.resolvedType().isA(Types.stringType)) {
      throw new CompileException(s"Type mismatch: Integer or string expected, ${this.operand.resolvedType().identifier.name} given.", this.operand.position)
    }
  }

  override def optimPass() : Statement = {
    this.operand = this.operand.optimPass()
    this
  }

  override def print(tree: TreeStream) {
    tree.println("WRITE")
    tree.indent
    this.operand.print(tree)
    tree.unindent
  }

  override def generateCode(code: CodeStream, tryContexts: Int) {
    code.println("; WRITE")

    code.println("; Operand code.")
    this.operand.generateCode(code, false)

    if (this.operand.isInstanceOf[StringLiteralExpression]) {
      code.println("; String literal.")
      code.println("MRM R5, (R2)") /* R5 contains the address pointing to the string length. */
      code.println("SUB R2, R1") /* Clean stack. */
      code.println("MRM R6, (R5)") /* R6 is our counter that will be decreased when a character was printed. */
      code.println("ADD R5, R1") /* From now on, R5 contains the address pointing to the current character. */

      val iterLabel = code.nextLabel
      val endLabel = code.nextLabel

      code.println(s"$iterLabel:")
      code.println("ISZ R7, R6") /* If the length is 0, then... */
      code.println(s"JPC R7, $endLabel") /* If the length is not 0, leave the loop. */
      code.println("MRM R7, (R5)") /* Dereference current character. */
      code.println("SYS 1, 7") /* Print current character. */
      code.println("ADD R5, R1") /* Move to next character. */
      code.println("SUB R6, R1") /* Decrease counter. */
      code.println(s"MRI R0, $iterLabel") /* Next iteration. */

      code.println(s"$endLabel:")
    } else {
      code.println("MRM R5, (R2)")
      code.println("SUB R2, R1")
      code.println("SYS 1, 5")
    }

    code.println("; END WRITE")
  }
}