package compiler;

import compiler.AST.*;
import compiler.lib.*;

public class TypeRels {

	// 				gerarchia dei tipi:
	// 			int			|	id (riferimento di classe)
	// 			 |          |			|
	// 			 |          |			|
	//		    bool				empty (null)

	// valuta se il tipo "a" e' <= al tipo "b", dove "a" e "b" sono tipi di base: IntTypeNode o BoolTypeNode
	public static boolean isSubtype(TypeNode a, TypeNode b) {

		// controlliamo che se a è una classe allora anche b lo dovrebbe essere sennò torno false
		if ( a instanceof RefTypeNode )  {
			if (b instanceof RefTypeNode){
				return ((RefTypeNode) a).id.equals(((RefTypeNode) b).id);
			} else {
				return false;
			}
		}

		// se a è emptyTypeNode allora b deve essere sovraclasse ovvero RefTypeNode o EmptyTypeNode
		if (a instanceof EmptyTypeNode) {
			return ( (b instanceof EmptyTypeNode) || (b instanceof RefTypeNode) );
		}

		if ( (a instanceof IntTypeNode) || (a instanceof BoolTypeNode) ) {
			return a.getClass().equals(b.getClass()) || // caso in cui a e b sono dello stesso tipo
					((a instanceof BoolTypeNode) && (b instanceof IntTypeNode));
		}

		System.out.println("Type error");
		return false;
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

}
