package org.oopsc.statement

import org.oopsc.{CodeStream, TreeStream, SemanticAnalysis, Types}
import org.oopsc.expression.Expression
import scala.collection.mutable.{ListBuffer, HashMap}

class IfStatement(var condition: Expression, var thenStatements: ListBuffer[Statement]) extends Statement {
  /** Contains ELSE-IF blocks and ELSE block. */
  var elseStatements = new HashMap[Expression, ListBuffer[Statement]]
  this.elseStatements.put(null, new ListBuffer[Statement])

  override def refPass(sem: SemanticAnalysis) {
    this.condition.refPass(sem)
    this.condition.resolvedType.check(sem, Types.boolType, this.condition.position)

    this.thenStatements.foreach(_.refPass(sem))

    for ((cond, stmts) <- this.elseStatements) {
      if (cond != null) {
        cond.refPass(sem)
        cond.resolvedType.check(sem, Types.boolType, cond.position)
      }

      stmts.foreach(_.refPass(sem))
    }
  }

  override def optimPass() : Statement = {
    /* TODO Delete all branches that always evaluate to `false'.
     * If no branches left, return NullStatement. */
    this.condition = this.condition.optimPass()
    //this.elseStatements.map(p => (if (p._1 != null) p._1.optimPass else null, p._1.optimPass))
    this.thenStatements = this.thenStatements.map(_.optimPass())
    this
  }

  def addIfElse(condition: Expression, stmts: ListBuffer[Statement]) {
    this.elseStatements.put(condition, stmts)
  }

  def setElse(stmts: ListBuffer[Statement]) {
    this.elseStatements.put(null, stmts)
  }

  private def print(tree: TreeStream, condition: Expression, stmts: ListBuffer[Statement]) {
    tree.indent

    if (condition != null) {
      condition.print(tree)
    } else {
      tree.println("ELSE")
    }

    if (!stmts.isEmpty) {
      if (condition != null) {
        tree.println("THEN")
      }

      tree.indent

      for (s <- stmts) {
        s.print(tree)
      }

      tree.unindent
    }

    tree.unindent
  }

  override def print(tree: TreeStream) {
    tree.println("IF")
    this.print(tree, this.condition, this.thenStatements)

    for ((cond, stmts) <- this.elseStatements) {
      if (cond != null) {
        this.print(tree, cond, stmts)
      }
    }

    val elseStmts = this.elseStatements.get(null).get

    if (elseStmts.size != 0) {
      this.print(tree, null, elseStmts)
    }
  }

  private def generateCode(code: CodeStream, tryContexts: Int, condition: Expression, stmts: ListBuffer[Statement], nextLabel: String, endLabel: String) {
    condition.generateCode(code, false)

    code.println("MRM R5, (R2) ; Bedingung vom Stapel nehmen")
    code.println("SUB R2, R1")
    code.println("ISZ R5, R5 ; Wenn 0, dann")
    code.println("JPC R5, " + nextLabel + " ; Sprung zu END IF bzw. nächstem ELSEIF/ELSE")
    code.println("; THEN")

    for (s <- stmts) {
      s.generateCode(code, tryContexts)
    }

    code.println("MRI R0, " + endLabel + " ; Sprung zu END IF")
  }

  override def generateCode(code: CodeStream, tryContexts: Int) {
    code.println("; IF")

    val endLabel = code.nextLabel
    var nextLabel = code.nextLabel

    this.generateCode(code, tryContexts, this.condition, this.thenStatements, nextLabel, endLabel)

    for ((cond, stmts) <- this.elseStatements) {
      /* Deal with ELSE block separately. */
      if (cond != null) {
        code.println("; ELSEIF")
        code.println(nextLabel + ":")

        nextLabel = code.nextLabel

        this.generateCode(code, tryContexts, cond, stmts, nextLabel, endLabel)

        code.println("; END ELSEIF")
      }
    }

    code.println(nextLabel + ":")

    val elseStmts = this.elseStatements.get(null).get

    if (elseStmts.size != 0) {
      code.println("; ELSE")

      for (s <- elseStmts) {
        s.generateCode(code, tryContexts)
      }

      code.println("; END ELSE")
    }

    code.println("; END IF")
    code.println(endLabel + ":")
  }
}