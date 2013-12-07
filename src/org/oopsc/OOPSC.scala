package org.oopsc

/**
 * Diese Klasse stellt die Hauptmethode des Übersetzers für OOPS
 * dar. Sie wertet die Kommandozeilen-Optionen aus und bietet
 * eine Hilfe an, falls diese falsch sind.
 */
object OOPSC {
  /**
   * Die Hauptmethode des Übersetzers.
   * Sie wertet die Kommandozeilen-Optionen aus und bietet eine Hilfe an, falls diese falsch sind.
   * Sind sie gültig, wird zuerst die Syntaxanalyse durchgeführt. Diese erzeugt den
   * Syntaxbaum des Programms, in dem dann die Kontextanalyse durchgeführt wird. Zum
   * Schluss wird dann der Code generiert.
   *
   * @param args
	 * Die Kommandozeilenargumente. Diese sind im Quelltext der Methode { @link #usage usage}
   *                                                                          nachzulesen.
   */
  // TODO refactor
  def main(args: Array[String]) {
    var inFile: String = null
    var outFile: String = null
    var showContext = false
    var showSymbols = false
    var showSyntax = false
    var debug = false
    var optimisations = false
    var heapSize = 100
    var stackSize = 100

    var i = 0
    while (i < args.length) {
      val arg: String = args(i)

      if (arg == "-c") {
        showContext = true
      } else if (arg == "-h") {
        usage
        return
      } else if (arg == "-hs") {
        i += 1
        if (i < args.length) {
          heapSize = Integer.parseInt(args(i))
        } else {
          System.out.println("Fehlendes Argument fuer " + arg)
          usage
        }
      } else if (arg == "-d") {
        debug = true
      } else if (arg == "-o") {
        optimisations = true
      } else if (arg == "-l") {
        showSymbols = true
      } else if (arg == "-s") {
        showSyntax = true
      } else if (arg == "-ss") {
        i += 1
        if (i < args.length) {
          stackSize = Integer.parseInt(args(i))
        } else {
          System.out.println("Fehlendes Argument fuer " + arg)
          usage
        }
      } else if (arg.length > 0 && arg.charAt(0) == '-') {
        System.out.println("Unbekannte Option " + arg)
        usage
        return
      } else if (outFile != null) {
        System.out.println("Nur zwei Dateinamen erlaubt")
        usage
        return
      } else if (inFile != null) {
        outFile = arg
      } else {
        inFile = arg
      }

      i += 1
    }

    if (inFile == null) {
      System.out.println("Keine Quelldatei angegeben")
      usage
      return
    }

    try {
      val p = new SyntaxAnalysis(inFile, showSymbols).parse

      if (showSyntax) {
        p.printTree
      }

      p.semanticAnalysis

      if (optimisations) {
        p.optimise
      }

      if (showContext) {
        p.printTree
      }

      val stream: CodeStream = if (outFile == null) CodeStream.apply else CodeStream.apply(outFile)

      p.generateCode(stream, stackSize, heapSize)

      if (outFile != null) {
        stream.close
      }
    } catch {
      case e: CompileException => {
        System.out.println(e.getMessage)

        if (debug) {
          e.printStackTrace
        }

        System.exit(1)
      }
    }
  }

  /**
   * Die Methode gibt eine Hilfe auf der Konsole aus.
   */
  private def usage {
    System.out.println("java -jar OOPSC.jar [-c] [-h] [-hs <n>] [-o] [-i] [-l] [-s] [-ss <n>] <quelldatei> [<ausgabedatei>]")
    System.out.println("    -c       Zeige das Ergebnis der Kontextanalyse")
    System.out.println("    -h       Zeige diese Hilfe")
    System.out.println("    -hs <n>  Reserviere <n> Worte fuer den Heap (Standard ist 100)")
    System.out.println("    -o       Perform some basic optimisations")
    System.out.println("    -l       Zeige das Ergebnis der lexikalischen Analyse")
    System.out.println("    -s       Zeige das Ergebnis der syntaktischen Analyse")
    System.out.println("    -ss <n>  Reserviere <n> Worte fuer den Stapel (Standard ist 100)")
  }
}