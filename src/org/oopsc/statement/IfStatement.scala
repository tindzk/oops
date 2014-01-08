package org.oopsc.statement

import org.oopsc.{CodeStream, TreeStream, SemanticAnalysis, Types}
import org.oopsc.expression.{BooleanLiteralExpression, Expression}
import scala.collection.mutable.ListBuffer

class IfStatement(_condition: Expression, _thenStatements: ListBuffer[Statement]) extends Statement {
  var branches = new ListBuffer[(Expression, ListBuffer[Statement])]
  branches += (_condition -> _thenStatements)

  var elseBranch = new ListBuffer[Statement]

  override def refPass(sem: SemanticAnalysis) {
    for ((cond, stmts) <- this.branches) {
      cond.refPass(sem)
      cond.resolvedType.check(Types.boolType, cond.position)
      stmts.foreach(_.refPass(sem))
    }

    elseBranch.foreach(_.refPass(sem))
  }

  override def optimPass() : Statement = {
    this.branches = this.branches.map(b => (b._1.optimPass(), b._2.map(_.optimPass())))

    var newBranches = new ListBuffer[(Expression, ListBuffer[Statement])]

    /* Delete all branches that always evaluate to `false'. */
    var skipRest = false
    for ((cond, stmts) <- this.branches if !skipRest) {
      cond match {
        case BooleanLiteralExpression(false, _) =>
          /* Skip branch. */
        case BooleanLiteralExpression(true, _) =>
          newBranches += (cond -> stmts)
          /* Skip all other branches. */
          skipRest = true
        case _ =>
          newBranches += (cond -> stmts)
      }
    }

    this.branches = newBranches
    this.elseBranch = this.elseBranch.map(_.optimPass())

    /* this.elseBranch may contain a NullStatement. Filter these elements out. */
    this.elseBranch.filterNot(_.isInstanceOf[NullStatement])

    /* If no branches left, return NullStatement. */
    if (this.branches.isEmpty && this.elseBranch.isEmpty) {
      return new NullStatement
    }

    this
  }

  def addIfElse(condition: Expression, stmts: ListBuffer[Statement]) {
    this.branches += (condition -> stmts)
  }

  def setElse(stmts: ListBuffer[Statement]) {
    this.elseBranch = stmts
  }

  private def print(tree: TreeStream, condition: Expression, stmts: ListBuffer[Statement]) {
    tree.println("BRANCH")
    tree.indent

    if (condition == null) {
      tree.println("DEFAULT")
    } else {
      condition.print(tree)
    }

    tree.indent
    stmts.foreach(_.print(tree))
    tree.unindent

    tree.unindent
  }

  override def print(tree: TreeStream) {
    tree.println("IF")
    tree.indent

    for ((cond, stmts) <- this.branches) {
      if (cond != null) {
        this.print(tree, cond, stmts)
      }
    }

    if (this.elseBranch.nonEmpty) {
      this.print(tree, null, this.elseBranch)
    }

    tree.unindent
  }

  private def generateCode(code: CodeStream, tryContexts: Int, condition: Expression, stmts: ListBuffer[Statement], nextLabel: String, endLabel: String) {
    condition match {
      case BooleanLiteralExpression(true, _) =>
        /* Minor optimisation: No need to generate evaluation code for the true literal. */
      case _ =>
        condition.generateCode(code, false)
        code.println("MRM R5, (R2) ; Bedingung vom Stapel nehmen")
        code.println("SUB R2, R1")
        code.println("ISZ R5, R5 ; Wenn 0, dann")
        code.println("JPC R5, " + nextLabel + " ; Sprung zu END IF bzw. nächstem ELSEIF/ELSE")
        code.println("; THEN")
    }

    stmts.foreach(_.generateCode(code, tryContexts))
    code.println("MRI R0, " + endLabel + " ; Sprung zu END IF")
  }

  override def generateCode(code: CodeStream, tryContexts: Int) {
    code.println("; IF")

    val endLabel = code.nextLabel
    var nextLabel = code.nextLabel

    for (((cond, stmts), i) <- this.branches.zipWithIndex) {
      code.println("; BRANCH #" + i)
      this.generateCode(code, tryContexts, cond, stmts, nextLabel, endLabel)
      code.println("; END BRANCH")

      code.println(nextLabel + ":")
      nextLabel = code.nextLabel
    }

    if (this.elseBranch.nonEmpty) {
      code.println("; ELSE")
      this.elseBranch.foreach(_.generateCode(code, tryContexts))
      code.println("; END ELSE")
    }

    code.println("; END IF")
    code.println(endLabel + ":")
  }
}