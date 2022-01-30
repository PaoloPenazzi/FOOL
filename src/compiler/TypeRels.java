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
		return a.getClass().equals(b.getClass()) || // caso in cui a e b sono dello stesso tipo
				((a instanceof BoolTypeNode) && (b instanceof IntTypeNode)) || // caso in cui a è bool e b int (quindi bool sottotipo int)
				(a instanceof RefTypeNode) && (b instanceof BoolTypeNode || b instanceof IntTypeNode) || // caso in cui a è id
																										// e b bool/int (ref sottotipo di bool/int)
				(a instanceof EmptyTypeNode) && (b instanceof RefTypeNode || b instanceof BoolTypeNode || b instanceof IntTypeNode) ;
				// caso in cui a è null e b è id/int/bool (quindi b sottotipo di tutti)
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
