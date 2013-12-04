package org.oopsc

import org.oopsc.statement._
import org.oopsc.symbol.MethodSymbol
import scala.collection.mutable.ListBuffer

/**
 * @note The term `to terminate' may be misleading. Here, it is understood in
 *       its static sense (during the compilation time). Therefore, considering a
 *       method, we are merely determining statically if it is always returning a
 *       value (a total function). However, a method identified as `terminating' by
 *       our algorithm may still never terminate during run-time.
 */
class Branch {
  var terminates = false
  var sub = new ListBuffer[Branch]
}

object BranchEvaluator {
  /**
   * Constructs a tree for the given statements, notably all branches. This
   * method is called recursively, taking into all nesting levels.
   */
  protected def constructTree(sem: SemanticAnalysis, parent: Branch, stmts: ListBuffer[Statement]) {
    for (stmt <- stmts) {
      stmt match {
        case ifStmt: IfStatement =>
          val branchIf = new Branch
          parent.sub += branchIf

          constructTree(sem, branchIf, ifStmt.thenStatements)

          if (branchIf.terminates && ifStmt.condition.isAlwaysTrue(sem)) {
            parent.terminates = true
          }

          /* Requires that elseStatements always contains an entry for the else-block
           * even if it is empty. */
          for ((expr, stmts) <- ifStmt.elseStatements) {
            val branch = new Branch
            parent.sub += branch
            constructTree(sem, branch, stmts)

            if (branch.terminates && (expr == null || expr.isAlwaysTrue(sem))) {
              parent.terminates = true
            }
          }

        case whileStmt: WhileStatement =>
          /* Only consider while statements if the condition is always true. */
          if (whileStmt.condition.isAlwaysTrue(sem)) {
            val branch = new Branch
            parent.sub += branch
            constructTree(sem, branch, whileStmt.statements)
          }

        case s: ReturnStatement =>
          parent.terminates = true

        case s: ThrowStatement =>
          parent.terminates = true

        case _ =>
      }
    }

    if (!parent.terminates && parent.sub.size != 0) {
      /* If all sub-branches terminate, the parent as well. */
      for (branch <- parent.sub) {
        if (!branch.terminates) {
          return
        }
      }

      parent.terminates = true
    }
  }

  /**
   * Internally constructs a branch tree and determines whether all branches
   * terminate.
   *
   * This is done by iterating recursively over the sub-trees and investigating
   * the existence of return and throw statements. We also consider if such a
   * statement is conditional.
   *
   * Finally, using back-propagation we determine whether each sub-tree is
   * terminating. The method returns true if the root node terminates.
   */
  def terminates(sem: SemanticAnalysis, method: MethodSymbol): Boolean = {
    val root: Branch = new Branch
    constructTree(sem, root, method.statements)
    return root.terminates
  }
}