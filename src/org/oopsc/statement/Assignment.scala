package org.oopsc.statement

import org.oopsc.CodeStream
import org.oopsc.CompileException
import org.oopsc.SemanticAnalysis
import org.oopsc.TreeStream
import org.oopsc.expression.Expression

class Assignment(var leftOperand: Expression, var rightOperand: Expression) extends Statement {
  override def refPass(sem: SemanticAnalysis) {
    this.leftOperand.refPass(sem)
    this.rightOperand.refPass(sem)

    if (!this.leftOperand.lValue) {
      throw new CompileException("Lvalue expected", this.leftOperand.position)
    }

    this.rightOperand.resolvedType.check(sem, this.leftOperand.resolvedType, this.rightOperand.position)
  }

  override def print(tree: TreeStream) {
    tree.println("ASSIGNMENT")
    tree.indent
    this.leftOperand.print(tree)
    this.rightOperand.print(tree)
    tree.unindent
  }

  override def generateCode(code: CodeStream, tryContexts: Int) {
    code.println("; ASSIGNMENT code for left operand")
    this.leftOperand.generateCode(code)
    code.println("; ASSIGNMENT code for right operand")
    this.rightOperand.generateCode(code, true)
    code.println("; ASSIGNMENT")
    code.println("MRM R5, (R2) ; Rechten Wert vom Stapel nehmen")
    code.println("SUB R2, R1")
    code.println("MRM R6, (R2) ; Referenz auf linken Wert vom Stapel nehmen")
    code.println("SUB R2, R1")
    code.println("MMR (R6), R5 ; Zuweisen")
  }
}