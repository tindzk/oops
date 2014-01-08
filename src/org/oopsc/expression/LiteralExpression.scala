package org.oopsc.expression

import org.oopsc._
import org.oopsc.symbol._

abstract class LiteralExpression(`type`: ClassSymbol, position: Position = new Position()) extends Expression(position) {
  override def resolvedType() : ClassSymbol = `type`

  def _generateIntCode(value: Int, code: CodeStream) {
    /* Load value into R5. */
    code.println(s"MRI R5, $value")

    /* Allocate space on the stack. */
    code.println("ADD R2, R1")

    /* Copy value from R5 to the newly allocated space on the stack. */
    code.println("MMR (R2), R5")
  }

  /* For compatibility purposes only. Needed as exceptions can be thrown with integers or characters. */
  @deprecated
  def intValue: Int = throw new CompileException("Literal does not have a compatible value.")
}

case class BooleanLiteralExpression(value: Boolean, var _position: Position = new Position()) extends LiteralExpression(Types.boolType, _position) {
  def print(tree: TreeStream) {
    tree.println(this.value)
  }

  def generateCode(code: CodeStream) {
    code.println(s"; ${this.value}")
    _generateIntCode(if (value) 1 else 0, code)
  }
}

case class IntegerLiteralExpression(var value: Int, var _position: Position = new Position()) extends LiteralExpression(Types.intType, _position) {
  def print(tree: TreeStream) {
    tree.println(this.value)
  }

  def generateCode(code: CodeStream) {
    code.println(s"; ${this.value}")
    _generateIntCode(value, code)
  }

  override def intValue: Int = value
}

/* TODO For compatibility purposes, Types.intType is used. In fact, a separate type
 * should be introduced for characters. */
case class CharacterLiteralExpression(value: Char, var _position: Position = new Position()) extends LiteralExpression(Types.intType, _position) {
  def print(tree: TreeStream) {
    tree.println(this.value)
  }

  def generateCode(code: CodeStream) {
    code.println(s"; ${this.value}")
    _generateIntCode(value, code)
  }

  override def intValue: Int = value.asInstanceOf[Int]
}

case class StringLiteralExpression(value: String, var _position: Position = new Position()) extends LiteralExpression(Types.stringType, _position) {
  private var offset = -1

  override def refPass(sem: SemanticAnalysis) {
    this.offset = sem.getRodataOffset(value)
  }

  def print(tree: TreeStream) {
    tree.println(this.value)
  }

  def generateCode(code: CodeStream) {
    code.println(s"; '${this.value}'")
    code.println(s"MRI R5, _rodata_${this.offset}")
    code.println("ADD R2, R1")
    code.println("MMR (R2), R5")
  }
}

case class NullLiteralExpression(var _position: Position = new Position()) extends LiteralExpression(Types.nullType, _position) {
  def print(tree: TreeStream) {
    tree.println("NULL")
  }

  def generateCode(code: CodeStream) {
    code.println("; NULL")
    _generateIntCode(0, code)
  }
}