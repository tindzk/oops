package org.oopsc.symbol

import org.oopsc.scope._
import org.oopsc._

/* Variable declaration. Also used for method parameters. */
class VariableSymbol(ident: Identifier, typeName: Identifier) extends Symbol(ident) {
  var resolvedType: Option[ClassSymbol] = None

  /**
   * Die Position der Variablen im Stapelrahmen bzw. des Attributs im Objekt.
   * Dies wird während der semantischen Analyse eingetragen.
   */
  var offset: Int = 0

  var scope: Scope = null

  /**
   * Returns the resolved type. Requires prior semantic analysis (definition pass
   * is sufficient).
   */
  def getResolvedType: ClassSymbol = {
    if (this.resolvedType.isEmpty) {
      this.resolvedType = Some(this.scope.resolveClass(typeName))
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
    tree.println(this.identifier.name +  " (" + this.offset + ")" + " : " + (
      if (this.resolvedType.isEmpty) "<unresolved>" else this.resolvedType.get.name()))
  }
}