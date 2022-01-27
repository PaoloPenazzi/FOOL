package compiler;

import compiler.AST.*;
import compiler.lib.*;

/*
 *
 * ci dovremo lavorare per il progetto.
 *
 */

public class TypeRels {

	// valuta se il tipo "a" e' <= al tipo "b", dove "a" e "b" sono tipi di base: IntTypeNode o BoolTypeNode
	public static boolean isSubtype(TypeNode a, TypeNode b) {
		return a.getClass().equals(b.getClass()) || ((a instanceof BoolTypeNode) && (b instanceof IntTypeNode));
	}

	// torna vero se tutti i TypeNode passati sono BoolTypeNode, false altrimenti
	public static boolean areBoolean(TypeNode... nodes) {
		for(TypeNode node : nodes){
			if(!(node instanceof BoolTypeNode)){
				return false;
			}
		}
		return true;
	}

	// TODO create method isSubClass
	// TODO check functioning of TypeCheckEASTVisitor in eq ge le visits

}
