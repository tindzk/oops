package org.oopsc.symbol

import org.oopsc.scope._
import org.oopsc._

/* Variable declaration. Also used for method parameters. */
class VariableSymbol(ident: Identifier) extends Symbol(ident) {
  var typeIdent: Identifier = null
  var resolvedType: Option[ClassSymbol] = None

  def this(ident: Identifier, typeSymbol: ClassSymbol) {
    this(ident)
    this.resolvedType = Some(typeSymbol)
    this.typeIdent = typeSymbol.identifier
  }

  def this(ident: Identifier, typeIdent: Identifier) {
    this(ident)
    this.typeIdent = typeIdent
  }

  /**
   * Die Position der Variablen im Stapelrahmen bzw. des Attributs im Objekt.
   * Dies wird während der semantischen Analyse eingetragen.
   */
  var offset = 0

  var scope: Scope = null

  /**
   * Returns the resolved type. Requires prior semantic analysis (definition pass
   * is sufficient).
   */
  def getResolvedType: ClassSymbol = {
    if (this.resolvedType.isEmpty) {
      this.resolvedType = Some(this.scope.resolveClass(typeIdent))
    }

    resolvedType.get
  }

  def defPass(sem: SemanticAnalysis) {
    sem.defineSymbol(this)
    this.scope = sem.currentScope.get
  }

  def refPass(sem: SemanticAnalysis) {
    getResolvedType
  }

  def print(tree: TreeStream) {
    tree.println(s"${this.identifier.name} (${this.offset}): " +
      this.resolvedType.map(_.name()).getOrElse("<unresolved>"))
  }
}