package compiler;

import java.util.*;
import compiler.lib.*;

/*
* L'ST ce lo dà ANTLR. L'AST ce lo dobbiamo fare. Sta a noi decidere cosa astrarre e cosa no. Un albero di oggetti in Java.
*
* Questo è un file di classi in pratica. Una classe per il più (Plus che diventa PlusNode, Times che diventa TimesNode ecc.).
* Ste classi avranno due figli che potranno essere un qualsiasi Node, quindi usiamo una interfaccia Node per generalizzare
* il più possibile.
*
* La radice è ProgNode che ha un unico figlio che è di nuovo un nodo Exo che è il programma principale
*
* la classe Node ha un metodo accept che riceve come argomento l'oggetto del visitor e lo implemento per ciascuno delle
* classi Node e praticamente è una implementazione che riceve l'oggetto visitor e rimbalza la chiamata chiamando indietro sul visitor
* visitNode(). Poi in pratica nell'ASTVisitor quando dovrò fare una chiamata visitNode(..) la farò ad un metodo visit(Node e)
* generico che al suo interno richiama l'implementazione di accept () del nodo Node e corrispondente. Quindi chiamando questo visit(Node e)
* sposta dall'argomento al soggetto a quel punto Java si occuperà a compile time di identificare la classe effettiva del Node e
* e chiamare la giusta implementazione del metodo accept()
*
* Le classi in AST e metodi di visita riordinati (radici in alto foglie in basso)
*
 */
public class AST {
	
	public static class ProgLetInNode extends Node {
		final List<DecNode> declist;
		final Node exp;
		ProgLetInNode(List<DecNode> d, Node e) {
			declist = Collections.unmodifiableList(d); 
			exp = e;
		}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	public static class ProgNode extends Node {
		final Node exp;
		ProgNode(Node e) {exp = e;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}
	
	public static class FunNode extends DecNode {
		final String id;
		// tipo di ritorno
		final TypeNode retType; //tipo di ritorno della funzione!
		// lista dei parametri in input alla funzione
		final List<ParNode> parlist;
		// lista delle dichiarazione di variabile o di funzione all'interno della funzione (dichiarazioni all'interno del let)
		final List<DecNode> declist;
		// nodo del corpo della funzione
		final Node exp;
		FunNode(String i, TypeNode rt, List<ParNode> pl, List<DecNode> dl, Node e) {
	    	id=i; 
	    	retType=rt; 
	    	parlist=Collections.unmodifiableList(pl); 
	    	declist=Collections.unmodifiableList(dl); 
	    	exp=e;
	    }
		
		//void setType(TypeNode t) {type = t;}
		
		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	// nodo relativo ai parametri di funzione
	public static class ParNode extends DecNode {
		final String id;
		ParNode(String i, TypeNode t) {id = i; type = t;}


		//il metodo accept, che verrà chiamato dal printVisitor e a runtime deciderà quale implementazione chiamare, in pratica
		// rimbalza la visitNode una volta che capito qual'è il tipo di Nodo da visitare. Infatti, se si va a vedere l'implementazione di accept,
		// si vede che gli passo this ovvero un printVisitor così da poter ritornare lì. In pratica io di là nella classe printVisitor
		// non conosco il tipo, rimbalzo la chiamata qua che a runtime gestirà il tipo su cui chiamare il tutto.
		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}
	
	public static class VarNode extends DecNode {
		final String id;
		final Node exp;
		VarNode(String i, TypeNode t, Node v) {id = i; type = t; exp = v;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}
		
	public static class PrintNode extends Node {
		final Node exp;
		PrintNode(Node e) {exp = e;}

		// il metodo accept accetta un qualunque baseASTVisitor dato che tutti gli ASTVisitor estendono il base
		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}
	
	public static class IfNode extends Node {
		final Node cond;
		final Node th;
		final Node el;
		IfNode(Node c, Node t, Node e) {cond = c; th = t; el = e;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}
	
	public static class EqualNode extends Node {
		final Node left;
		final Node right;
		EqualNode(Node l, Node r) {left = l; right = r;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}
	
	public static class TimesNode extends Node {
		final Node left;
		final Node right;
		TimesNode(Node l, Node r) {left = l; right = r;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}
	
	public static class PlusNode extends Node {
		final Node left;
		final Node right;
		PlusNode(Node l, Node r) {left = l; right = r;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}
	
	public static class CallNode extends Node {
		final String id;
		final List<Node> arglist;
		STentry entry;
		int nl; //campo che tiene conto del nesting level. ci servirà sapere anche il nesting level dell'uso
		// (e non più solo della dichiarazione) per farne la differenza e trovare a chi si riferisce in fase di code generation.
		CallNode(String i, List<Node> p) {
			id = i; 
			arglist = Collections.unmodifiableList(p);
		}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}
	
	public static class IdNode extends Node {
		final String id;
		STentry entry;
		int nl; //campo che tiene conto del nesting level. ci servirà sapere anche il nesting level dell'uso
		// (e non più solo della dichiarazione) per farne la differenza e trovare a chi si riferisce in fase di code generation.
		IdNode(String i) {id = i;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}
	
	public static class BoolNode extends Node {
		final Boolean val;
		BoolNode(boolean n) {val = n;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}
	
	public static class IntNode extends Node {
		final Integer val;
		IntNode(Integer n) {val = n;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	/*
	*
	* ArrowTypeNode contiene le informazioni corrispondenti alla notazione per i tipi funzionali vista a lezione:
	* (T1,T2,...,Tn)->T . Cioè il tipo T1,T2,...,Tn dei parametri (nel campo parlist) ed il tipo T di
	* ritorno della funzione (nel campo ret). es: f( int i, int y): bool -> (int, int) -> bool.
	*
	* Notare che estende il TypeNode .. infatti questo è il TIPO (funzionale) DELLE FUNZIONI!
	 */
	public static class ArrowTypeNode extends TypeNode {
		final List<TypeNode> parlist;
		final TypeNode ret;
		ArrowTypeNode(List<TypeNode> p, TypeNode r) {
			parlist = Collections.unmodifiableList(p); 
			ret = r;
		}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}
	
	public static class BoolTypeNode extends TypeNode {

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	public static class IntTypeNode extends TypeNode {

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

}