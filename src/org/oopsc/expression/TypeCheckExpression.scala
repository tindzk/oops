package org.oopsc.expression

import org.oopsc._
import org.oopsc.symbol.{ResolvableClassSymbol}
import org.oopsc.{CodeStream, TreeStream, SemanticAnalysis}

object TypeCheckExpression {
  def checkType(code: CodeStream, className: String, endLabel: String) {
    val iterLabel = code.nextLabel

    /* Dereference the object to get the VMT (offset 0 of the object). */
    code.println("MRM R5, (R5)") // R5 = VMT of current class.

    code.println(s"MRI R7, $className")

    code.println(s"$iterLabel:")
    code.println("MRR R6, R5") // R6 = VMT of current class.
    code.println("SUB R6, R7") // R6 is 0 if the class matches.
    code.println("ISZ R6, R6") // R6 is 1 if the class matches, 0 otherwise.
    code.println(s"JPC R6, $endLabel") // Jump to $endLabel if the type matches. R6 is 1.

    /* The class does not match. */
    code.println("MRM R5, (R5)") // R5 = VMT address of the super class (offset 0 in VMT).
    code.println(s"JPC R5, $iterLabel") // Next iteration if the current class is not Object, i.e. R5 != 0.

    /* Otherwise we found a type mismatch. Stop with R6 = 0. */
  }
}

class TypeCheckExpression(var oper: Expression, className: ResolvableClassSymbol) extends Expression(oper.position) {
  override def refPass(sem: SemanticAnalysis) {
    this.oper.refPass(sem)
    this.className.declaration = Some(sem.currentScope.get.resolveClass(className.identifier))
  }

  override def optimPass(): Expression = {
    this.oper = this.oper.optimPass()
    this
  }

  override def resolvedType() = Types.boolType

  def print(tree: TreeStream) {
    tree.println("ISA " + this.className.identifier.name)
    tree.indent
    this.oper.print(tree)
    tree.unindent
  }

  def generateCode(code: CodeStream) {
    code.println("; ISA")
    this.oper.generateCode(code, false)

    val endLabel = code.nextLabel

    code.println("MRM R5, (R2)") // R5 = Evaluated value of this.oper.

    if (this.className.declaration.get == Types.objectClass) {
      /* Deal with special case: NULL ISA Object. */
      val beginLabel = code.nextLabel
      code.println(s"JPC R5, $beginLabel") // Jump to beginLabel if R5 != NULL.
      code.println("MMR (R2), R1") // Write the result on the stack.
      code.println(s"MRI R0, $endLabel")
      code.println(s"$beginLabel:")
    }

    TypeCheckExpression.checkType(code, this.className.identifier.name, endLabel)

    /* Code to be executed upon type mismatch. */
    code.println(s"$endLabel:")

    /* Code to be executed after match or mismatch: *R2 = R6 with R6 = 0 or 1. */
    code.println("MMR (R2), R6")
  }
}