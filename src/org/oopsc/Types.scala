package org.oopsc

import org.oopsc.symbol._

object Types {
  /** Internal type for methods without result. */
  final val voidType = new ClassSymbol(new Identifier("_Void"))

  /** Internal type for null. Compatible to all classes. */
  final val nullType = new ClassSymbol(new Identifier("_Null"))

  /** Internal base type for numbers. */
  final val intType = new ClassSymbol(new Identifier("_Integer"))

  /** Internal base type for boolean values. */
  final val boolType = new ClassSymbol(new Identifier("_Boolean"))

  /** Internal base type for strings. */
  final val stringType = new ClassSymbol(new Identifier("_String"))

  /** Class `Object'. */
  final val objectClass = new ClassSymbol(new Identifier("Object"))

  /** Class `Integer'. */
  final val intClass = new ClassSymbol(new Identifier("Integer"), objectClass)

  /** Class `Boolean'. */
  final val boolClass = new ClassSymbol(new Identifier("Boolean"), objectClass)

  /* Do not set ClassDeclaration.(int|bool)Class.objectSize manually as this
   * value is going to be overwritten during the contextual analysis. The
   * attribute is required for boxing as it holds the actual value.
   */
  intClass.attributes += AttributeSymbol.apply(new Identifier("_value"), intType)
  boolClass.attributes += AttributeSymbol.apply(new Identifier("_value"), boolType)
}