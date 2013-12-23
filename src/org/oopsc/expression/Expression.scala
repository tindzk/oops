package org.oopsc.expression

import org.oopsc._
import org.oopsc.symbol._
import java.io.{ UnsupportedEncodingException, ByteArrayOutputStream }
import org.oopsc.statement.ThrowStatement

/**
 * Die abstrakte Basisklasse für alle Ausdrücke im Syntaxbaum.
 * Zusätzlich zur Standardschnittstelle für Ausdrücke definiert sie auch
 * Methoden zur Erzeugung neuer Ausdrücke für das Boxing und Unboxing von
 * Ausdrücken sowie das Dereferenzieren.
 */
abstract class Expression(var position: Position) {
  def resolvedType() : ClassSymbol = {
    throw new CompileException("Type was not resolved.", position)
  }

  /**
   * Ist dieser Ausdruck ein L-Wert, d.h. eine Referenz auf eine Variable?
   * Die meisten Ausdrücke sind keine L-Werte.
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
   * Die Methode gibt diesen Ausdruck in einer Baumstruktur aus.
   * Wenn der Typ des Ausdrucks bereits ermittelt wurde, wird er auch ausgegeben.
   *
   * @param tree
   * Der Strom, in den die Ausgabe erfolgt.
   */
  def print(tree: TreeStream)

  /**
   * Die Methode generiert den Assembler-Code für diesen Ausdruck. Sie geht
   * davon aus, dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
   *
   * @param code
   * Der Strom, in den die Ausgabe erfolgt.
   */
  def generateCode(code: CodeStream)

  protected def generateDeRefCode(code: CodeStream) {
    code.println("; DEREF")
    code.println("MRM R5, (R2)")
    code.println("MRM R5, (R5)")

    /* Throw an exception if the address is NULL. */
    val nextLabel = code.nextLabel
    code.println("JPC R5, " + nextLabel)
    new ThrowStatement(new IntegerLiteralExpression(1)).generateCode(code)

    code.println(nextLabel + ":")
    code.println("MMR (R2), R5")
  }

  protected def generateBoxCode(code: CodeStream) {
    code.println("; BOX")
    code.println("MRM R5, (R2) ; Wert vom Stapel nehmen")
    code.println("SUB R2, R1")
    code.println("MRM R6, (R2) ; Referenz auf neues Objekt holen (bleibt auf Stapel)")
    code.println("MRI R7, " + ClassSymbol.HEADERSIZE)
    code.println("ADD R6, R7 ; Speicherstelle in neuem Objekt berechnen")
    code.println("MMR (R6), R5 ; Wert in Objekt speichern")
  }

  protected def generateUnBoxCode(code: CodeStream) {
    code.println("; UNBOX type = " + this.resolvedType().identifier.name)
    code.println("MRM R5, (R2) ; Objektreferenz vom Stapel lesen")
    code.println("MRI R6, " + ClassSymbol.HEADERSIZE)
    code.println("ADD R5, R6 ; Adresse des Werts bestimmen")
    code.println("MRM R5, (R5) ; Wert auslesen")
    code.println("MMR (R2), R5 ; und auf den Stapel schreiben")
  }

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