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

	// nodo creazione istanza di classe
	public static class NewNode extends Node {
		final String id;
		final List<Node> argList;

		STentry entry;

		public NewNode(String id, List<Node> argList) {
			this.id = id;
			this.argList = argList;
		}


		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}


	public static class ClassNode extends DecNode {
		final String id;
		final List<FieldNode> fieldList;
		final List<MethodNode> methodList;

		ClassNode(String id, List<FieldNode> fields, List<MethodNode> methods) {
			this.id = id;
			fieldList = Collections.unmodifiableList(fields);
			methodList = Collections.unmodifiableList(methods);
		}

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	// nodo dichiarazione del campo
	public static class FieldNode extends DecNode {
		final String id;
		final TypeNode type;

		FieldNode(String id, TypeNode type) {
			this.id = id;
			this.type = type;
		}

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	// nodo dichiarazione di un metodo
	public static class MethodNode extends DecNode {
		final String id;
		final TypeNode retType;
		final List<ParNode> parlist;
		final List<DecNode> declist;
		final Node exp;

		public MethodNode(String id, TypeNode retType, List<ParNode> paramList, List<DecNode> decList, Node e) {
			this.id = id;
			this.retType = retType;
			this.parlist = paramList;
			this.declist = decList;
			this.exp = e;
		}

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	// Chiamata ad un metodo della classe da fuori.
	// var value = C.getValue();
	public static class ClassCallNode extends Node {
		final String classID;
		final String methodID;
		final List<Node> argList;

		STentry entry; //id1 cercata come per ID in IdNode e CallNode (discesa livelli)
		STentry methodEntry; // id2 cercata nella Virtual Table (raggiunta tramite la Class Table)
		                           // della classe del tipo RefTypeNode di ID1 (se ID1 non ha tale tipo si ha una notifica di errore)

		public ClassCallNode(String classID, String methodID, List<Node> args) {
			this.classID = classID;
			this.methodID = methodID;
			this.argList = args;
		}

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

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

	public static class DivNode extends Node {
		final Node left;
		final Node right;
		DivNode(Node l, Node r) {left = l; right = r;}

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

	public static class MinusNode extends Node {
		final Node left;
		final Node right;

		public MinusNode(Node left, Node right) {
			this.left = left;
			this.right = right;
		}

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	public static class OrNode extends Node {
		final Node left;
		final Node right;
		OrNode(Node l, Node r) {left = l; right = r;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	public static class AndNode extends Node {
		final Node left;
		final Node right;
		AndNode(Node l, Node r) {left = l; right = r;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	public static class GreaterEqualNode extends Node {
		final Node left;
		final Node right;
		GreaterEqualNode(Node l, Node r) {left = l; right = r;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	public static class LessEqualNode extends Node {
		final Node left;
		final Node right;
		LessEqualNode(Node l, Node r) {left = l; right = r;}

		@Override
		public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {return visitor.visitNode(this);}
	}

	public static class NotNode extends Node {
		final Node exp;
		NotNode(Node e) {exp = e;}

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

	// nodo per il null c= null
	public static class EmptyNode extends Node {

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	//--------------------- TYPE -----------------------------------------

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

	public static class ClassTypeNode extends TypeNode {
		final List<TypeNode> allFields;
		final List<ArrowTypeNode> allMethods;

		public ClassTypeNode(List<TypeNode> allFields, List<ArrowTypeNode> allMethods) {
			this.allFields = allFields;
			this.allMethods = allMethods;
		}

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	public static class MethodTypeNode extends TypeNode {
		final ArrowTypeNode fun;

		public MethodTypeNode(ArrowTypeNode fun) {
			this.fun = fun;
		}

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	public static class RefTypeNode extends TypeNode {
		final String id;

		public RefTypeNode(String id) {
			this.id = id;
		}

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}

	public static class EmptyTypeNode extends TypeNode {

		@Override
		public <S, E extends Exception> S accept(BaseASTVisitor<S, E> visitor) throws E {
			return visitor.visitNode(this);
		}
	}




}