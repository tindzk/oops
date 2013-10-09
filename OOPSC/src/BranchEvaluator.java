import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

/**
 * @note The term `to terminate' may be misleading. Here, it is understood in
 * its static sense (during the compilation time). Therefore, considering a
 * method, we are merely determining statically if it is always returning a
 * value (a total function). However, a method identified as `terminating' by
 * our algorithm may still never terminate during run-time.
 */
class Branch {
	boolean terminates = false;
	List<Branch> sub = new LinkedList<>();
}

public class BranchEvaluator {

	/**
	 * Constructs a tree for the given statements, notably all branches. This
	 * method is called recursively, taking into all nesting levels.
	 */
	protected static void constructTree(Branch parent, List<Statement> stmts) {
		for (Statement stmt : stmts) {
			if (stmt instanceof IfStatement) {
				IfStatement ifStmt = (IfStatement) stmt;

				Branch branchIf = new Branch();
				parent.sub.add(branchIf);
				constructTree(branchIf, ifStmt.thenStatements);

				if (branchIf.terminates && ifStmt.condition.isAlwaysTrue()) {
					parent.terminates = true;
				}

				/* Requires that elseStatements always contains an entry for the else-block
				 * even if it is empty. */
				for (Entry<Expression, List<Statement>> entry : ifStmt.elseStatements
						.entrySet()) {
					Branch branch = new Branch();
					parent.sub.add(branch);
					constructTree(branch, entry.getValue());

					if (branch.terminates
							&& (entry.getKey() == null || entry.getKey()
									.isAlwaysTrue())) {
						parent.terminates = true;
					}
				}
			} else if (stmt instanceof WhileStatement) {
				/* Only consider while statements if the condition is always true. */
				WhileStatement whileStmt = (WhileStatement) stmt;

				if (whileStmt.condition.isAlwaysTrue()) {
					Branch branch = new Branch();
					parent.sub.add(branch);
					constructTree(branch, whileStmt.statements);
				}
			} else if (stmt instanceof ReturnStatement || stmt instanceof ThrowStatement) {
				parent.terminates = true;
			}
		}

		if (!parent.terminates && parent.sub.size() != 0) {
			/* If all sub-branches terminate, the parent as well. */
			for (Branch branch : parent.sub) {
				if (!branch.terminates) {
					return;
				}
			}

			parent.terminates = true;
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
	public static boolean terminates(MethodDeclaration method) {
		Branch root = new Branch();
		constructTree(root, method.statements);
		return root.terminates;
	}

}