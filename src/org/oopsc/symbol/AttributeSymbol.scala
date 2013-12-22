package org.oopsc.symbol

import org.oopsc._

trait AttributeSymbol extends VariableSymbol {
  override def print(tree: TreeStream) {
    tree.println(s"${this.accessLevel} ${this.identifier.name} (${this.offset}): " +
      this.resolvedType.map(_.name()).getOrElse("<unresolved>"))
  }
}

object AttributeSymbol {
  def apply(ident: Identifier, typeSymbol: ClassSymbol) = new VariableSymbol(ident, typeSymbol) with AttributeSymbol
  def apply(ident: Identifier, typeIdent: Identifier) = new VariableSymbol(ident, typeIdent) with AttributeSymbol
}