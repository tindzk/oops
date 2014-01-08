package org.oopsc.statement

import org.oopsc.CodeStream
import org.oopsc.CompileException
import org.oopsc.SemanticAnalysis
import org.oopsc.TreeStream
import org.oopsc.expression.{EvaluateExpression, Expression}
import org.oopsc.symbol.VariableSymbol

class Assignment(var leftOperand: Expression, var rightOperand: Expression) extends Statement {
  override def refPass(sem: SemanticAnalysis) {
    this.leftOperand.refPass(sem)
    this.rightOperand.refPass(sem)

    if (!this.leftOperand.lValue) {
      throw new CompileException("Variable reference expected.", this.leftOperand.position)
    } else {
      this.leftOperand match {
        case left: EvaluateExpression =>
          if (left.ref.declaration.get.identifier.name == "BASE" ||
              left.ref.declaration.get.identifier.name == "SELF")
          {
            throw new CompileException("Cannot assign to BASE or SELF.", this.leftOperand.position)
          }

        case _ =>
      }
    }

    this.rightOperand.resolvedType.check(this.leftOperand.resolvedType, this.rightOperand.position)
  }

  override def optimPass(): Statement = {
    this.leftOperand = this.leftOperand.optimPass()
    this.rightOperand = this.rightOperand.optimPass()
    this
  }

  override def print(tree: TreeStream) {
    tree.println("ASSIGNMENT")
    tree.indent
    this.leftOperand.print(tree)
    this.rightOperand.print(tree)
    tree.unindent
  }

  override def generateCode(code: CodeStream, tryContexts: Int) {
    code.println("; ASSIGNMENT")
    code.println("; Left operand.")
    this.leftOperand.generateCode(code)
    code.println("; Right operand.")
    this.rightOperand.generateCode(code, true)
    code.println("MRM R5, (R2) ; Take right value from the stack.")
    code.println("SUB R2, R1")
    code.println("MRM R6, (R2) ; Take reference to the left value from the stack.")
    code.println("SUB R2, R1")
    code.println("MMR (R6), R5 ; Assign.")
    code.println("; END ASSIGNMENT")
  }
}