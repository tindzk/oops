package org.oopsc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.Comparator
import java.util.List
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import org.oopsvm.AsmException
import org.oopsvm.Assembler
import org.oopsvm.VMException
import org.oopsvm.VirtualMachine

@RunWith(classOf[Parameterized]) object TestSuite {
  def readFile(path: String, encoding: Charset): String = {
    val encoded: Array[Byte] = Files.readAllBytes(Paths.get(path))
    return encoding.decode(ByteBuffer.wrap(encoded)).toString
  }

  @Parameters(name = "{0}")
  def data: Collection[Array[AnyRef]] = {
    val data: List[Array[AnyRef]] = new ArrayList[Array[AnyRef]]

    try {
      Files.walkFileTree(Paths.get("tests/"), new SimpleFileVisitor[Path] {
        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          if (file.toString.endsWith(".oops")) {
            data.add(Array[AnyRef](file.toString))
          }

          return FileVisitResult.CONTINUE
        }

        override def visitFileFailed(file: Path, exc: IOException): FileVisitResult = {
          return FileVisitResult.CONTINUE
        }
      })
    } catch {
      case e: IOException => {
        e.printStackTrace
      }
    }

    Collections.sort(data, new Comparator[Array[AnyRef]] {
      def compare(o: Array[AnyRef], o2: Array[AnyRef]): Int = {
        return (o(0).asInstanceOf[String]).compareTo(o2(0).asInstanceOf[String])
      }
    })

    return data
  }
}

@RunWith(classOf[Parameterized])
class TestSuite(var path: String) {
  private var p: Program = null

  def runVM(asm: String, input: String): String = {
    val asmStream: InputStream = new ByteArrayInputStream(asm.getBytes("UTF-8"))

    val vm: VirtualMachine = new VirtualMachine(new Assembler(false, false).assemble(asmStream), new Array[Int](8), false, false, false, false, false, false, false)

    val output: ByteArrayOutputStream = new ByteArrayOutputStream

    vm.setStreams(new ByteArrayInputStream(input.getBytes), output)
    vm.run

    return output.toString("UTF-8")
  }

  @Test
  def testGrammar {
    val stream: FileInputStream = new FileInputStream(this.path)
    val input: ANTLRInputStream = new ANTLRInputStream(stream)
    val lexer: GrammarLexer = new GrammarLexer(input)
    val tokens: CommonTokenStream = new CommonTokenStream(lexer)
    val parser: GrammarParser = new GrammarParser(tokens)

    parser.program

    val supposedToFail: Boolean = this.path.contains("_se")

    if (supposedToFail) {
      assertNotNull(parser.getNumberOfSyntaxErrors)
    } else {
      assertEquals(0, parser.getNumberOfSyntaxErrors)
    }
  }

  @Test def testVisitor {
    val supposedToFail: Boolean = this.path.contains("_se")

    if (supposedToFail) {
      /* Skip as we cannot use the visitor on malformed code samples. */
      return
    }

    val stream: FileInputStream = new FileInputStream(this.path)
    val input: ANTLRInputStream = new ANTLRInputStream(stream)
    val lexer: GrammarLexer = new GrammarLexer(input)
    val tokens: CommonTokenStream = new CommonTokenStream(lexer)
    val parser: GrammarParser = new GrammarParser(tokens)

    val tree: ParseTree = parser.program

    val visitor: ProgramVisitor = new ProgramVisitor(new Program)
    visitor.visit(tree)
  }

  /**
   * Performs syntax and context analysis. Also tests the code generation.
   *
   * @throws Exception
   */
  @Test def testFile {
    val supposedToFail: Boolean = this.path.contains("_se")
    val pathExpected = this.path.substring(0, this.path.length - 5) + ".out"
    var expected = ""

    try {
      expected = TestSuite.readFile(pathExpected, StandardCharsets.UTF_8)
    } catch {
      case e: NoSuchFileException => { }
    }

    try {
      this.p = new SyntaxAnalysis(this.path, false).parse
      this.p.semanticAnalysis
      this.p.printTree

      /* Test code generation. */
      val stream: ByteArrayOutputStream = new ByteArrayOutputStream
      val code: CodeStream = CodeStream.apply(stream)
      this.p.generateCode(code, 1000, 1000)
      val asm = stream.toString("UTF-8")

      System.err.println(asm)

      /* Run the VM twice with different inputs. */
      val output: String = this.runVM(asm, "abc") + this.runVM(asm, "xyz")
      assertEquals(expected, output)
    } catch {
      case e: UnsupportedEncodingException => {
        fail
      }

      case e: CompileException => {
        if (supposedToFail) {
          System.err.println(e.getMessage)
          return
        }

        throw e
      }
    }

    if (supposedToFail) {
      fail
    }
  }
}