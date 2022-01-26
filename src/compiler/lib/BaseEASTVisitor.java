package compiler.lib;

import compiler.*;
import compiler.exc.UnimplException;

/*
*
* Versione estesa di BaseASTVisitor ma che può visitare anche le STEntry . Per quanto riguarda il typechecking è stato
* aggiunto parametro E (e passato) e throws E in visitSTentry.
*
 */
public class BaseEASTVisitor<S,E extends Exception> extends BaseASTVisitor<S,E>  {
	
	protected BaseEASTVisitor() {}
	protected BaseEASTVisitor(boolean ie) { super(ie); } 
	protected BaseEASTVisitor(boolean ie, boolean p) { super(ie,p); } 
     
    protected void printSTentry(String s) {
    	System.out.println(indent+"STentry: "+s);
	}
	
	public S visitSTentry(STentry s) throws E {throw new UnimplException();}
}
