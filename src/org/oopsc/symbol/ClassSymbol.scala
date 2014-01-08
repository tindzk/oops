package org.oopsc.symbol

import scala.collection.mutable.{ ListBuffer, LinkedHashMap }

import org.oopsc._
import org.oopsc.statement._
import org.oopsc.scope.Scope

object ClassSymbol {
  /**
   * Constant for the size of the header at the beginning of each object.
   * As of now, the header only contains an address to the object's VMT.
   */
  final val HEADERSIZE = 1

  /**
   * Throws an exception for a type mismatch, converting the type names into a string.
   *
   * @param expected Expected type.
   * @param position Position in the source code.
   */
  def typeError(expected: ClassSymbol, given: ClassSymbol, position: Position) {
    throw new CompileException(s"Type mismatch: ${expected.identifier.name} expected, ${given.identifier.name} given.", position)
  }
}

class ClassSymbol(ident: Identifier) extends ScopedSymbol(ident) {
  /** List of all fields and methods. */
  var members = new LinkedHashMap[String, Symbol]()

  /** Attributes declared in this class. */
  var attributes = new ListBuffer[VariableSymbol]

  /** Methods declared in this class. */
  var methods = new ListBuffer[MethodSymbol]

  /** Parent class. */
  var superClass: Option[ResolvableClassSymbol] = None

  /**
   * The size of an object in this class. The exact size will be determined
   * during the referential pass. The minimum size for all objects is the header size.
   */
  var objectSize = ClassSymbol.HEADERSIZE

  def this(ident: Identifier, superClass: ClassSymbol) {
    this(ident)
    this.superClass = Some(new ResolvableClassSymbol(superClass.identifier))
  }

  def this(ident: Identifier, superClass: ResolvableClassSymbol) {
    this(ident)
    this.superClass = Some(superClass)
  }

  /**
   * Recursively collect all methods in the inheritance chain. Filter out
   * overridden methods.
   */
  private def collectMethods(overridden: List[MethodSymbol] = List.empty): ListBuffer[MethodSymbol] = {
    val _overridden = overridden ++ this.methods.filter(_.overrides.isDefined).map(_.overrides.get)
    this.methods ++ (this.superClass match {
      case Some(c) => c.declaration.get.collectMethods(_overridden).diff(_overridden)
      case None => ListBuffer[MethodSymbol]()
    })
  }

  /**
   * Generates a VMT for the current class, including its sub-classes. Requires
   * that the contextual analysis was previously performed.
   */
  def generateVMT = this.collectMethods().sortBy(_.vmtIndex)

  /* Needed so that the definition pass can be performed multiple times for built-in classes. */
  // TODO find a better solution
  var first = true

  override def defPass(sem: SemanticAnalysis) {
    /* Reset object size as defPass() and refPass() may be called multiple times on
     * internal types.
     */
    this.objectSize = ClassSymbol.HEADERSIZE

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
      this.attributes.foreach(_.defPass(sem))
      this.methods.foreach(_.defPass(sem))
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

  /**
   * Check whether the class dependencies represent an acyclic graph. This is
   * done by traversing the class hierarchy recursively. If a class occurs more
   * than once, a cycle was found.
   */
  def checkForCycles(encounteredClasses: List[ClassSymbol] = List.empty) {
    this.superClass match {
      case Some(superClass) =>
        val base = getSuperClass().get

        if (encounteredClasses.contains(base)) {
          throw new CompileException("Class hierarchy is not devoid of cycles.", this.identifier.position)
        }

        base.checkForCycles(this :: encounteredClasses)

      case None =>
    }
  }

  override def refPass(sem: SemanticAnalysis) {
    sem.enter(this)

    this.checkForCycles()

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

          if (base.getAttribute(m.identifier.name).isDefined) {
            throw new CompileException(
              s"The method ${this.identifier.name}.${m.identifier.name}() is overriding a method of its base class ${base.identifier.name}.",
              identifier.position)
          }
        }

        for (v <- this.attributes) {
          if (base.getMethod(v.identifier.name).isDefined) {
            throw new CompileException(
              s"The attribute ${v.identifier.name} in ${this.identifier.name} is overriding a method of its base class ${base.identifier.name}.",
              identifier.position)
          }
        }

        /* The VMT attributes start with the offset 1. The first entry in the VMT is reserved for the base class. */
        val vmtAttrsOffset = 1

        /* Set the VMT index for each method. */
        var vmtIndex = if (base.methods.isEmpty) vmtAttrsOffset else base.methods.last.vmtIndex + 1

        for (m <- this.methods) {
          /* If the method is overridden, take the VMT index from its parent method. */
          base.getMethod(m.identifier.name) match {
            case Some(baseMethod) =>
              m.vmtIndex = baseMethod.vmtIndex
              m.overrides = Some(baseMethod)

            case None =>
              m.vmtIndex = vmtIndex
              vmtIndex += 1
          }
        }

      case None =>
        /* Set the VMT index for each method. */
        var vmtIndex = 0
        for (m <- this.methods) {
          m.vmtIndex = vmtIndex
          vmtIndex += 1
        }
    }

    /* Resolve attribute types and assign indices. */
    for (a <- this.attributes) {
      a.refPass(sem)
      a.offset = this.objectSize
      this.objectSize += 1
    }

    this.methods.foreach(_.refPass(sem))

    sem.leave
  }

  def optimPass() {
    this.methods.foreach(_.optimPass())
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
   * Generates assembly code for this class. Requires prior completion of the
   * contextual analysis.
   *
   * @param code Output stream.
   */
  def generateCode(code: CodeStream) {
    code.println(s"; CLASS ${this.identifier.name}")

    for (m <- this.methods) {
      m.generateCode(code, 0)
    }

    code.println("; END CLASS")
  }

  override def print(tree: TreeStream) {
    tree.println("CLASS " + this.identifier.name)
    tree.indent

    if (!this.attributes.isEmpty) {
      tree.println("ATTRIBUTES")
      tree.indent
      this.attributes.foreach(_.print(tree))
      tree.unindent
    }

    if (!this.methods.isEmpty) {
      tree.println("METHODS")
      tree.indent
      this.methods.foreach(_.print(tree))
      tree.unindent
    }

    tree.unindent
  }

  /**
   * Checks the compatibility to the given type. Throws an exception upon type mismatch.
   *
   * @param expected Expected type.
   * @param position Position in the source code.
   */
  def check(expected: ClassSymbol, position: Position) {
    if (!this.isA(expected)) {
      ClassSymbol.typeError(expected, this, position)
    }
  }

  /**
   * Checks the compatibility to the given type.
   *
   * @param expected Expected type.
   */
  def isA(expected: ClassSymbol): Boolean = {
    /* Special handling for NULL which is compatible to all classes except for
     * built-in types such as Integer, Boolean and Void.
     */
    if ((this eq Types.nullType) &&
      (expected ne Types.intType) &&
      (expected ne Types.boolType) &&
      (expected ne Types.voidType)) {
      return true
    }

    /* Type promotions for built-in types Integer and Boolean. */
    if ((this eq Types.intType) && (expected eq Types.intClass)) {
      return true
    }

    if ((this eq Types.intClass) && (expected eq Types.intType)) {
      return true
    }

    if ((this eq Types.boolType) && (expected eq Types.boolClass)) {
      return true
    }

    if ((this eq Types.boolClass) && (expected eq Types.boolType)) {
      return true
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
          case Some(s) => Some(s)
          case None => enclosingScope
        }

      case None => enclosingScope
    }

  /** For access such as a.b, only look in a's class hierarchy to resolve b. */
  def resolveMember(name: String): Option[Symbol] = {
    members.get(name) match {
      case Some(m) => return Some(m)
      case None => None
    }

    /* If not in this class, check the superclass chain. */
    superClass match {
      case Some(c) =>
        c.declaration match {
          case Some(s) => s.resolveMember(name)
          case None => None
        }

      case None => None
    }
  }

  override protected def resolve(ident: Identifier, requestingClass: Option[ClassSymbol]): Option[Symbol] =
    resolveMember(ident.name) match {
      case Some(m) => Some(m)
      case None => super.resolve(ident, requestingClass)
    }
}