package org.oopsc.symbol

import scala.collection.mutable.{ ListBuffer, LinkedHashMap }

import org.oopsc._
import org.oopsc.statement._
import org.oopsc.scope.Scope

object ClassSymbol {
  /**
   * Konstante für die Größe der Verwaltungsinformation am Anfang eines jeden Objekts.
   * As of now, the header only contains an address to the VMT of the object.
   */
  final val HEADERSIZE: Int = 1

  /**
   * Die Methode erzeugt eine Ausnahme für einen Typfehler. Sie wandelt dabei intern verwendete
   * Typnamen in die auch außen sichtbaren Namen um.
   *
   * @param expected
   * Der Typ, der nicht kompatibel ist.
   * @param position
   * Die Stelle im Quelltext, an der der Typfehler gefunden wurde.
   * @throws CompileException
   * Die Meldung über den Typfehler.
   */
  def typeError(expected: ClassSymbol, given: ClassSymbol, position: Position) {
    throw new CompileException(s"Type mismatch: ${expected.identifier.name} expected, ${given.identifier.name} given.", position)
  }
}

/**
 * superClass - This is the superclass not enclosingScope field. We still record
 *  the enclosing scope so we can push in and pop out of class defs.
 */
class ClassSymbol(ident: Identifier, var superClass: Option[ResolvableClassSymbol] = None) extends ScopedSymbol(ident) {
  /** List of all fields and methods. */
  var members = new LinkedHashMap[String, Symbol]()

  /** Die Attribute dieser Klasse. */
  var attributes = new ListBuffer[VariableSymbol]

  /** Die Methoden dieser Klasse. */
  var methods = new ListBuffer[MethodSymbol]

  /**
   * Die Größe eines Objekts dieser Klasse. Die Größe wird später bestimmt.
   * Default size for all objects.
   */
  var objectSize: Int = ClassSymbol.HEADERSIZE

  /**
   * Recursively fill the VMT with the method declarations. Take into account
   * overridden methods.
   *
   * @param res
   * Result array.
   */
  protected def fillVMT(res: ListBuffer[MethodSymbol]) {
    if (this.superClass.isDefined) {
      val base = this.superClass.get.declaration.get
      base.fillVMT(res)
    }
    for (m <- this.methods) {
      res.update(m.vmtIndex, m)
    }
  }

  /**
   * @param cur
   * Current index.
   * @return Highest VMT index.
   */
  protected def getLastVmtIndex(cur: Int): Int = {
    var ccur = cur
    if (this.superClass.isDefined) {
      val base = this.superClass.get.declaration.get
      val tmp: Int = base.getLastVmtIndex(cur)
      if (tmp > ccur) {
        ccur = tmp
      }
    }
    if (this.methods.size != 0) {
      val last = this.methods.last
      if (last.vmtIndex > cur) {
        ccur = last.vmtIndex
      }
    }

    ccur
  }

  /**
   * Generates a VMT for the current class, including its sub-classes. Requires
   * that the contextual analysis was performed before.
   *
   * @return
   */
  def generateVMT: ListBuffer[MethodSymbol] = {
    val res = new ListBuffer[MethodSymbol]()

    for (i <- 0 to this.getLastVmtIndex(-1)) {
      res.append(null)
    }

    this.fillVMT(res)

    res
  }

  /**
   * Finds the declaration of the given method and return it in an assembly string.
   * Takes into account if a method was inherited.
   *
   * @param name
   * Method name.
   * @return null if not found, <class>_<method> otherwise.
   */
  def resolveAsmMethodName(name: String): String = {
    for (m <- this.methods) {
      if (m.identifier.name == name) {
        return this.identifier.name + "_" + name
      }
    }
    if (this.superClass.isEmpty) {
      return null
    }
    val base: ClassSymbol = this.superClass.get.declaration.get

    base.resolveAsmMethodName(name)
  }

  /* Needed so that the definition pass can be performed multiple times for built-in classes. */
  // TODO find a better solution
  var first = true

  override def defPass(sem: SemanticAnalysis) {
    sem.defineSymbol(this)
    sem.enter(this)

    if (this.superClass.isEmpty && !(this eq Types.objectClass)) {
      /* Object is the only class without a super class. */
      this.superClass = {
        val c = new ResolvableClassSymbol(Types.objectClass.identifier)
        c.declaration = Some(Types.objectClass)
        Some(c)
      }
    }

    if (first) {
      for (a <- this.attributes) {
        a.defPass(sem)
      }

      for (m <- this.methods) {
        m.defPass(sem)
      }
    }

    sem.leave

    first = false
  }

  def getSuperClass(): Option[ClassSymbol] = {
    this.superClass match {
      case Some(superClass) =>
        if (superClass.declaration.isEmpty) {
          val base = this.resolveClass(superClass.identifier)
          superClass.declaration = Some(base)
        }

        superClass.declaration

      case None => None
    }
  }

  override def refPass(sem: SemanticAnalysis) {
    sem.enter(this)

    this.superClass match {
      case Some(superClass) =>
        val base = getSuperClass().get

        /* Inherit attributes from the parent object. */
        this.objectSize += base.objectSize

        /* Verify that all overridden methods have the same signature as its parent. */
        for (m <- this.methods) {
          base.getMethod(m.identifier.name) match {
            case Some(baseMethod) =>
              /* This method overrides a parent method. */
              if (!baseMethod.signatureEquals(m)) {
                throw new CompileException(
                  s"The overridden signature of ${this.identifier.name}.${m.identifier.name}() does not match its parent method in ${base.identifier.name}.",
                  identifier.position)
              }

            case None =>
          }

          base.getAttribute(m.identifier.name) match {
            case Some(baseAttribute) =>
              throw new CompileException(
                s"The method ${this.identifier.name}.${m.identifier.name}() is overriding a method of its base class ${base.identifier.name}.",
                identifier.position)

            case None =>
          }
        }

        for (v <- this.attributes) {
          if (base.getMethod(v.identifier.name).isDefined) {
            throw new CompileException(
              s"The attribute ${v.identifier.name} in ${this.identifier.name} is overriding a method of its base class ${base.identifier.name}.",
              identifier.position)
          }
        }

        /* Set the VMT index for each method. */
        var vmtIndex: Int = if (base.methods.isEmpty) 0 else base.methods.last.vmtIndex + 1

        for (m <- this.methods) {
          /* If the method is overridden, take the VMT index from its parent method. */
          base.getMethod(m.identifier.name) match {
            case Some(baseMethod) =>
              m.vmtIndex = baseMethod.vmtIndex

            case None =>
              m.vmtIndex = vmtIndex
              vmtIndex += 1
          }
        }

      case None =>
        /* Set the VMT index for each method. */
        var vmtIndex: Int = 0
        for (m <- this.methods) {
          m.vmtIndex = vmtIndex
          vmtIndex += 1
        }
    }

    // Attributtypen auflösen und Indizes innerhalb des Objekts vergeben.
    for (a <- this.attributes) {
      a.refPass(sem)
      a.offset = this.objectSize
      this.objectSize += 1
    }

    for (m <- this.methods) {
      m.refPass(sem)
    }

    sem.leave
  }

  /**
   * Finds the declaration for the given attribute name.
   */
  private def getAttribute(name: String) =
    this.attributes.find(_.name() == name)

  /**
   * Finds the declaration for the given method name.
   */
  private def getMethod(name: String) =
    this.methods.find(_.name() == name)

  /**
   * Generiert den Assembler-Code für diese Klasse. Dabei wird davon ausgegangen,
   * dass die Kontextanalyse vorher erfolgreich abgeschlossen wurde.
   *
   * @param code
   * Der Strom, in den die Ausgabe erfolgt.
   */
  def generateCode(code: CodeStream) {
    code.println("; CLASS " + this.identifier.name)

    for (m <- this.methods) {
      val contexts = new java.util.Stack[Statement.Context]
      contexts.add(Statement.Context.Default)
      m.generateCode(code, contexts)
    }

    code.println("; END CLASS " + this.identifier.name)
  }

  /**
   * Die Methode gibt diese Deklaration in einer Baumstruktur aus.
   *
   * @param tree
   * Der Strom, in den die Ausgabe erfolgt.
   */
  def print(tree: TreeStream) {
    tree.println("CLASS " + this.identifier.name)
    tree.indent

    if (!this.attributes.isEmpty) {
      tree.println("ATTRIBUTES")
      tree.indent

      for (a <- this.attributes) {
        a.print(tree)
      }

      tree.unindent
    }

    if (!this.methods.isEmpty) {
      tree.println("METHODS")
      tree.indent

      for (m <- this.methods) {
        m.print(tree)
      }

      tree.unindent
    }

    tree.unindent
  }

  /**
   * Die Methode prüft, ob dieser Typ kompatibel mit einem anderen Typ ist.
   * Sollte das nicht der Fall sein, wird eine Ausnahme mit einer Fehlermeldung generiert.
   *
   * @param expected
   * Der Typ, mit dem verglichen wird.
   * @param position
   * Die Position im Quelltext, an der diese Überprüfung
   * relevant ist. Die Position wird in der Fehlermeldung verwendet.
   * @throws CompileException
   * Die Typen sind nicht kompatibel.
   */
  def check(sem: SemanticAnalysis, expected: ClassSymbol, position: Position) {
    if (!this.isA(sem, expected)) {
      ClassSymbol.typeError(expected, this, position)
    }
  }

  /**
   * Die Methode prüft, ob dieser Typ kompatibel mit einem anderen Typ ist.
   *
   * @param expected
   * Der Typ, mit dem verglichen wird.
   * @return Sind die beiden Typen sind kompatibel?
   */
  def isA(sem: SemanticAnalysis, expected: ClassSymbol): Boolean = {
    // Spezialbehandlung für null, das mit allen Klassen kompatibel ist,
    // aber nicht mit den Basisdatentypen _Integer und _Boolean sowie auch nicht
    // an Stellen erlaubt ist, wo gar kein Wert erwartet wird.
    if ((this eq Types.nullType) &&
      (expected ne Types.intType) &&
      (expected ne Types.boolType) &&
      (expected ne Types.voidType)) {
      return true
    }

    /* Type promotions for built-in types integer and boolean. */
    if ((this eq Types.intType) && (expected eq Types.intClass)) {
      return true;
    }
    if ((this eq Types.intClass) && (expected eq Types.intType)) {
      return true;
    }

    if ((this eq Types.boolType) && (expected eq Types.boolClass)) {
      return true;
    }
    if ((this eq Types.boolClass) && (expected eq Types.boolType)) {
      return true;
    }

    /* Compare wrt. base type. */
    var cmp = this

    while (cmp ne expected) {
      cmp.getSuperClass() match {
        case Some(c) => cmp = c
        case None => return false
      }
    }

    true
  }

  override def getParentScope(): Option[Scope] =
    superClass match {
      case Some(c) =>
        c.declaration match {
          case Some(s) => Some(s) // if not root object, return super
          case None => enclosingScope
        }
      case None => enclosingScope // globals
    }

  /** For a.b, only look in a's class hierarchy to resolve b, not globals */
  def resolveMember(name: String): Option[Symbol] = {
    members.get(name) match {
      case Some(m) => return Some(m)
      case None => None
    }

    // if not in this class, check just the superclass chain
    superClass match {
      case Some(c) =>
        println(c.identifier.name)
        c.declaration match {
          case Some(s) => s.resolveMember(name)
          case None => None
        }

      case None => None
    }
  }

  override def resolve(name: String): Option[Symbol] =
    resolveMember(name) match {
      case Some(m) => Some(m)
      case None => super.resolve(name)
    }
}