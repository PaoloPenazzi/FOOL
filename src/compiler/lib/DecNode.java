package compiler.lib;

/*
 *
 * classe che contiene type e gettype è la classe madre di tutti i nodi che sono dichiarazione (varNode, funNode, parNode)
 * tutti questi sono caratterizzati da un tipo.
 *
 */
public abstract class DecNode extends Node {
	
	protected TypeNode type;
		
	public TypeNode getType() {return type;}

}
