package org.oopsc.expression

import org.oopsc._
import org.oopsc.symbol._
import scala.Some

/**
 * Represents an expression in the syntax tree that instantiates a class.
 */
class NewExpression(var newType: ResolvableClassSymbol) extends Expression(newType.identifier.position) {
  override def refPass(sem: SemanticAnalysis) {
    this.newType.declaration = Some(sem.currentScope.get.resolveClass(this.newType.identifier))
    this.`type` = this.newType.declaration.get
  }

  def print(tree: TreeStream) {
    tree.println("NEW " + this.newType.identifier.name + (if (this.`type` == null) "" else " : " + this.`type`.name))
  }

  def generateCode(code: CodeStream) {
    code.println("; NEW " + this.newType.identifier.name)
    code.println("ADD R2, R1")
    code.println("MMR (R2), R4 ; Referenz auf neues Objekt auf den Stapel legen")
    code.println("MRI R5, " + this.newType.declaration.get.objectSize)

    /* Insert the address pointing to the VMT at the relative position 0 of the
     * object. The offsets 1.. denote the attributes. */
    code.println("MRI R6, " + this.newType.identifier.name)
    code.println("MMR (R4), R6")

    code.println("ADD R4, R5 ; Heap weiter zählen")
  }
}