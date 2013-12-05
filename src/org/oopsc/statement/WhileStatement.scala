package org.oopsc.statement

import org.oopsc._
import org.oopsc.expression.Expression
import scala.collection.mutable.ListBuffer

class WhileStatement(var condition: Expression, var statements: ListBuffer[Statement]) extends Statement {
  override def refPass(sem: SemanticAnalysis) {
    this.condition.refPass(sem)
    this.condition.resolvedType.check(sem, Types.boolType, this.condition.position)

    for (s <- this.statements) {
      s.refPass(sem)
    }
  }

  override def print(tree: TreeStream) {
    tree.println("WHILE")
    tree.indent
    this.condition.print(tree)

    if (!this.statements.isEmpty) {
      tree.println("DO")
      tree.indent

      for (s <- this.statements) {
        s.print(tree)
      }

      tree.unindent
    }

    tree.unindent
  }

  override def generateCode(code: CodeStream, tryContexts: Int) {
    val whileLabel = code.nextLabel
    val endLabel = code.nextLabel

    code.println("; WHILE")
    code.println(whileLabel + ":")
    this.condition.generateCode(code, false)

    code.println("MRM R5, (R2) ; Bedingung vom Stapel nehmen")
    code.println("SUB R2, R1")
    code.println("ISZ R5, R5 ; Wenn 0, dann")
    code.println("JPC R5, " + endLabel + " ; Schleife verlassen")
    code.println("; DO")

    for (s <- this.statements) {
      s.generateCode(code, tryContexts)
    }

    code.println("; END WHILE")
    code.println("MRI R0, " + whileLabel)
    code.println(endLabel + ":")
  }
}