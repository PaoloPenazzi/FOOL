package compiler.lib;

public abstract class Node implements Visitable {

	// è comodo che ogni nodo abbia il campo line perchè memorizza nel ST il numero di linea nel sorgente dove compare quel token
	int line=-1;  // line -1 means unset
	
	public void setLine(int l) { line=l; }

	public int getLine() { return line; }

}

	  