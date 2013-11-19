package org.oopsc.scope

import org.oopsc.symbol.Symbol
import org.oopsc.{ Identifier, CompileException }
import scala.collection.mutable.LinkedHashMap
import org.oopsc.symbol.ClassSymbol

trait Scope {
  /** Only needed for compatibility with Java. */
  def getThis: Scope = this

  /** Scope in which this scope defined. For global scope, it's None */
  var enclosingScope: Option[Scope] = None

  /** Symbol table, maps identifier to symbol object. */
  protected var symbols = new LinkedHashMap[String, Symbol]()

  /**
   * The parent scope denotes where to look next for a symbol lookup, i.e.,
   * in the superclass or enclosing scope (default). This method may be overwritten
   * by a symbol class.
   */
  def getParentScope: Option[Scope] = this.enclosingScope

  /* Returns the scope name. */
  def getScopeName: String

  /** Define a symbol in the current scope. */
  def defineSymbol(sym: Symbol) = {
    if (this.symbols.contains(sym.name())) {
      val pos = this.symbols.get(sym.name()).get.identifier.position
      throw new CompileException(s"Redeclaration of symbol ${sym.name()} (declared in ${pos.line}:${pos.column}).",
        sym.identifier.position)
    }

    this.symbols += ((sym.name(), sym))
  }

  /** Look up the passed identifier in this scope, or in parent scope if not declared here. */
  def resolve(name: String): Option[Symbol] = {
    this.symbols.get(name) match {
      case Some(sym) => return Some(sym)
      case None => None
    }

    this.getParentScope match {
      case Some(s) => s.resolve(name)
      case None => None
    }
  }

  def checkedResolve(ident: Identifier): Symbol = {
    return resolve(ident.name) match {
      case Some(v) => v
      case None => throw new CompileException(s"Symbol ${ident.name} not found.", ident.position)
    }
  }

  def resolveClass(ident: Identifier): ClassSymbol = {
    return resolve(ident.name) match {
      case Some(c: ClassSymbol) => c
      case Some(c) => throw new CompileException(s"${ident.name} is not a class.", ident.position)
      case _ => throw new CompileException(s"Class symbol ${ident.name} not found.", ident.position)
    }
  }
}