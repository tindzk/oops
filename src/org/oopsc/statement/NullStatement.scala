package org.oopsc.statement

import org.oopsc.{CodeStream, TreeStream}

class NullStatement extends Statement {
  def print(tree: TreeStream) {
    tree.println("NOP")
  }

  override def generateCode(code: CodeStream, tryContexts: Int) {
    code.println("; NOP")
  }
}