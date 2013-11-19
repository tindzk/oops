package org.oopsc.symbol

import org.oopsc._
import org.oopsc.scope._

abstract class ScopedSymbol(ident: Identifier) extends Symbol(ident) with Scope {
  override def getScopeName(): String = ident.name
}