package org.oopsc

import org.oopsc.symbol._

object Types {
  /** Ein interner Typ für das Ergebnis von Methoden. */
  final val voidType = new ClassSymbol(new Identifier("_Void"))

  /** Ein interner Typ für null. Dieser Typ ist kompatibel zu allen Klassen. */
  final val nullType = new ClassSymbol(new Identifier("_Null"))

  /** Der interne Basisdatentyp für Zahlen. */
  final val intType = new ClassSymbol(new Identifier("_Integer"))

  /** Der interne Basisdatentyp für Wahrheitswerte. */
  final val boolType = new ClassSymbol(new Identifier("_Boolean"))

  /** Die Klasse Object. */
  final val objectClass = new ClassSymbol(new Identifier("Object"))

  /** Die Klasse Integer. */
  final val intClass = new ClassSymbol(new Identifier("Integer"), objectClass)

  /** Die Klasse Boolean. */
  final val boolClass = new ClassSymbol(new Identifier("Boolean"), objectClass)

  /* Do not set ClassDeclaration.(int|bool)Class.objectSize manually as this
   * value is going to be overwritten during the contextual analysis. The
   * attribute is required for boxing as it holds the actual value.
   */
  intClass.attributes += AttributeSymbol.apply(new Identifier("_value"), intType)
  boolClass.attributes += AttributeSymbol.apply(new Identifier("_value"), boolType)
}