package org.oopsc

import com.typesafe.scalalogging.slf4j.Logging
import org.rogach.scallop._

class Conf(args : Seq[String]) extends ScallopConf(args) {
  version(s"oopsc ${OOPSC.Version} (c) 2013-2014 Tim Nieradzik")
  banner("""Usage: java -jar oopsc.jar [OPTION]... [input] [<output>]
           |oopsc is an OOPS compiler.
           |
           |Options:
           |""".stripMargin)

  val symbols = opt[Boolean](descr = "show symbols from the syntax analysis")
  val ast = opt[Boolean](descr = "print AST after contextual analysis")
  val debug = opt[Boolean](descr = "enable debug mode")
  val generateCode = toggle(name = "code", descrYes = "enable code generation (default)", descrNo = "disable code generation", default = Some(true))
  val help = opt[Boolean](descr = "print help")
  val optimisations = opt[Boolean]("optim", descr = "enable optimisations")
  val heapSize = opt[Int](descr = "heap size", default = Some(100))
  val stackSize = opt[Int](descr = "stack size", default = Some(100))
  val inputFile = trailArg[String]("input", descr = "input file")
  val outputFile = trailArg[String]("output", descr = "output file (default: stdout)", required = false)
}

object OOPSC extends Logging {
  val Version = "0.1"

  def main(args: Array[String]) {
    val conf = new Conf(args)

    if (conf.help.apply()) {
      conf.printHelp()
      return
    }

    try {
      val p = new SyntaxAnalysis(conf.inputFile.apply(), conf.symbols.apply()).parse

      p.semanticAnalysis

      if (conf.optimisations.apply()) {
        p.optimise
      }

      if (conf.ast.apply()) {
        p.printTree
      }

      val stream = conf.outputFile.get match {
        case Some(out) => CodeStream.apply(out)
        case None => CodeStream.apply()
      }

      if (conf.generateCode.apply()) {
        p.generateCode(stream, conf.stackSize.apply(), conf.heapSize.apply())
      }

      if (conf.outputFile.isDefined) {
        stream.close
      }
    } catch {
      case e: CompileException => {
        logger.error(e.getMessage)

        if (conf.debug.apply()) {
          e.printStackTrace
        }

        System.exit(1)
      }
    }
  }
}