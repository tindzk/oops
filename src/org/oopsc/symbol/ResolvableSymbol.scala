package org.oopsc.symbol

import org.oopsc.Identifier

/* Represents a reference to a symbol that is resolved during the reference phase. */
class ResolvableSymbol(var identifier: Identifier, var declaration: Option[Symbol] = None) {

}

class ResolvableClassSymbol(var identifier: Identifier, var declaration: Option[ClassSymbol] = None) {

}