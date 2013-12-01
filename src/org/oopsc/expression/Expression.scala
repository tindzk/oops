package org.oopsc.expression

import org.oopsc._
import org.oopsc.symbol._
import java.io.{ UnsupportedEncodingException, ByteArrayOutputStream }

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
  var lValue: Boolean = false

  /**
   * Die Methode führt die Kontextanalyse für diesen Ausdruck durch.
   * Sie ist nicht abstrakt, da es einige abgeleitete Klassen gibt,
   * die sie nicht implementieren, weil sie dort nicht benötigt wird.
   * Da im Rahmen der Kontextanalyse auch neue Ausdrücke erzeugt werden
   * können, sollte diese Methode immer in der Form "a = a.contextAnalysis(...)"
   * aufgerufen werden, damit ein neuer Ausdruck auch im Baum gespeichert wird.
   *
   * @param sem
   * Die an dieser Stelle gültigen Deklarationen.
   * @return Dieser Ausdruck oder ein neuer Ausdruck, falls ein Boxing,
   *         Unboxing oder eine Dereferenzierung in den Baum eingefügt
   *         wurden.
   * @throws CompileException
   * Während der Kontextanylyse wurde ein Fehler
   * gefunden.
   */
  def refPass(sem: SemanticAnalysis) {

  }

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

  /**
   * If the condition is static, evaluate it to true or false. If the condition
   * is not static, always return false.
   *
   * TODO Must also evaluate more sophisticated expression such as `x == x' to
   * true.
   */
  def isAlwaysTrue(sem: SemanticAnalysis): Boolean = {
    return false
  }
}