package org.oopsc.expression

import org.oopsc.symbol.VariableSymbol
import org.oopsc.{ CodeStream, TreeStream, SemanticAnalysis }

/**
 * Represents an expression with an attribute or method access
 * operator (i.e. dot) in the syntax tree.
 */
class AccessExpression(var leftOperand: Expression, rightOperand: EvaluateExpression) extends Expression(leftOperand.position) {
  override def refPass(sem: SemanticAnalysis) {
    this.leftOperand.refPass(sem)

    /* The left operand denotes the context. The right operand therefore does not
     * need to resolve the context. */
    this.rightOperand.generateContextCode(false)

    /* Deal with accesses to methods or attributes in the base class. */
    this.leftOperand match {
      case call: EvaluateExpression =>
        if (call.ref.identifier.name == "BASE") {
          this.rightOperand.generateContextCode(true)
          this.rightOperand.setContext(call.ref.declaration.get.asInstanceOf[VariableSymbol], true)
        }
      case _ =>
    }

    /* The scope of the right operand consists of the result type of the left
     * operand. */
    this.rightOperand.scope = this.leftOperand.resolvedType()
    this.rightOperand.refPass(sem)

    /* The type of this expression is always the type of the right operand. */
    this.lValue = this.rightOperand.lValue
  }

  override def optimPass() : Expression = {
    this.leftOperand = this.leftOperand.optimPass()
    this
  }

  override def resolvedType() =
    this.rightOperand.resolvedType()

  def print(tree: TreeStream) {
    tree.println("PERIOD" + (if (this.lValue) "REF " else "") + this.resolvedType().name)
    tree.indent
    this.leftOperand.print(tree)
    this.rightOperand.print(tree)
    tree.unindent
  }

  def generateCode(code: CodeStream) {
    this.leftOperand.generateCode(code, true)
    this.rightOperand.generateCode(code)
  }
}