package org.oopsc.expression

import org.oopsc._
import org.oopsc.symbol._
import java.io.{ UnsupportedEncodingException, ByteArrayOutputStream }
import org.oopsc.statement.ThrowStatement

/**
 * Base class for all expressions. Provides methods for boxing/unboxing and
 * dereferencing.
 */
abstract class Expression(var position: Position) {
  def resolvedType() : ClassSymbol = {
    throw new CompileException("Type was not resolved.", position)
  }

  /**
   * True if the expression is a reference to a variable.
   */
  var lValue = false

  /**
   * Performs the reference pass of the semantic analysis.
   *
   * @param sem Context of the semantic analysis.
   */
  def refPass(sem: SemanticAnalysis) {

  }

  /**
   * Performs the optimisation pass.
   *
   * @return Optimised expression.
   */
  def optimPass() = this

  /**
   * Prints the expression in a tree structure.
   *
   * @param tree Output stream.
   */
  def print(tree: TreeStream)

  /**
   * Generates assembly code for the expression. Requires prior completion of the
   * contextual analsyis.
   *
   * @param code Output stream.
   */
  def generateCode(code: CodeStream)

  protected def generateDeRefCode(code: CodeStream) {
    code.println("; DEREF")
    code.println("MRM R5, (R2)")
    code.println("MRM R5, (R5)")

    /* Throw an exception if the address is NULL. */
    val nextLabel = code.nextLabel
    code.println(s"JPC R5, $nextLabel")
    new ThrowStatement(new IntegerLiteralExpression(1)).generateCode(code)

    code.println(s"$nextLabel:")
    code.println("MMR (R2), R5")
  }

  protected def generateBoxCode(code: CodeStream) {
    code.println(s"; BOX ${this.resolvedType().identifier.name}")
    code.println("MRM R5, (R2) ; Take value from the stack.")
    code.println("SUB R2, R1")
    code.println("MRM R6, (R2) ; Get reference to new object.")
    code.println(s"MRI R7, ${ClassSymbol.HEADERSIZE}")
    code.println("ADD R6, R7 ; Calculate memory position in the new object.")
    code.println("MMR (R6), R5 ; Save value in the object.")
  }

  protected def generateUnBoxCode(code: CodeStream) {
    code.println(s"; UNBOX ${this.resolvedType().identifier.name}")
    code.println("MRM R5, (R2) ; Read object reference from the stack.")
    code.println(s"MRI R6, ${ClassSymbol.HEADERSIZE}")
    code.println("ADD R5, R6 ; Calculate address of the value.")
    code.println("MRM R5, (R5) ; Read value...")
    code.println("MMR (R2), R5 ; ...and put on the stack.")
  }

  /**
   * Generates assembly code, taking into account boxing/unboxing and dereferencing.
   */
  def generateCode(code: CodeStream, box: Boolean) {
    if (box && ((this.resolvedType() eq Types.intType) || (this.resolvedType() eq Types.boolType))) {
      var newType: NewExpression = null

      if (this.resolvedType() eq Types.intType) {
        newType = new NewExpression(new ResolvableClassSymbol(Types.intClass.identifier))
        newType.newType.declaration = Some(Types.intClass)
      } else {
        newType = new NewExpression(new ResolvableClassSymbol(Types.boolClass.identifier))
        newType.newType.declaration = Some(Types.boolClass)
      }

      newType.generateCode(code)

      this.generateCode(code)
      this.generateBoxCode(code)
    } else {
      this.generateCode(code)

      if (this.lValue) {
        this.generateDeRefCode(code)
      }

      if (!box && ((this.resolvedType() eq Types.boolClass) || (this.resolvedType() eq Types.intClass))) {
        this.generateUnBoxCode(code)
      }
    }
  }

  override def toString: String = {
    val stream = new ByteArrayOutputStream
    val tree: TreeStream = new TreeStream(stream, 4)

    this.print(tree)

    try {
      return stream.toString("UTF-8")
    } catch {
      case e: UnsupportedEncodingException => {
        return null
      }
    }
  }
}