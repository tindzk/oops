package org.oopsc.symbol

import org.oopsc._

/* TODO Print access level in print(). */
trait AttributeSymbol extends VariableSymbol

object AttributeSymbol {
  def apply(ident: Identifier, typeSymbol: ClassSymbol) = new VariableSymbol(ident, typeSymbol) with AttributeSymbol
  def apply(ident: Identifier, typeIdent: Identifier) = new VariableSymbol(ident, typeIdent) with AttributeSymbol
}