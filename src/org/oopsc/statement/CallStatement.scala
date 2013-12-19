package org.oopsc.statement

import org.oopsc.{CodeStream, TreeStream, SemanticAnalysis, Types}
import org.oopsc.expression.Expression

class CallStatement(var call: Expression) extends Statement {
  override def refPass(sem: SemanticAnalysis) {
    this.call.refPass(sem)
    this.call.resolvedType.check(Types.voidType, this.call.position)
  }

  override def print(tree: TreeStream) {
    tree.println("CALL")
    tree.indent
    this.call.print(tree)
    tree.unindent
  }

  override def generateCode(code: CodeStream, tryContexts: Int) {
    code.println("; CALL")
    this.call.generateCode(code)
  }
}