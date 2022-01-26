package compiler.lib;

import compiler.AST.*;
import compiler.exc.*;

import static compiler.lib.FOOLlib.*;


/*
* Al fine di poter realizzare altri tipi di visitor oltre al PrintASTVisitor
* utilizziamo un BaseASTVisitor da cui ereditare. Scheletro di visitor dove possiamo realizzare diverse tipe di visita. Tipo:
* con il printASTVisitor visitiamo e printiamo, con il vecchio calcASTVisitor visitiamo e calcoliamo ma il concetto di base
* è identico per tutti i casi, cambia l'azione finale. Ad esempio visiteremo l'ast e genereremo il codice o la Symbol Table
* e così via.
*
* Scgliamo una classe concreta così abbiamo una classe prototipo da non dovere implementare diverse volte cose varie ed avere costruttori che
* torneranno utili.
*
* BaseASTVisitor contiene: il codice di visit(Node n) e una implementazione vuota per tutti i metodi visitNode.
* Ora le classi in AST devono invocare visitNode(this) su un generico BaseASTVisitor: devo modificare il metodo accept
* in modo che riceva un generico BaseASTVisitor sia nell'interfaccia Node (in realtà nell'interfaccia Visitable estesa da Node)
* che nelle classi in AST
*
* Il nostro typechecker userà il baseASTVisitor in questo modo. S sarà il tipo che ritorna e E sarà che le ecceszioni gettabili
* sono TypeExcception. Per quanto riguarda il typechecking è stato aggiunto parametro E extends Exception, poi è stato
* aggiunto throws E in TUTTE le visit/visitNode e aggiunta try-finally in visit che fa indentazione, per ripristinarla comunque
*
*
* La gestione degli (Enriched) AST incompleti dipende dallo specifico visitor:
* - per alcuni visitor è sufficiente lo stesso approccio usato per i Syntax Tree
* (es. SymbolTableASTVisitor e PrintEASTVisitor)
* - per altri visitor è necessario gettare un'eccezione unchecked IncomplException
* (es. TypeCheckEASTVisitor).
* Gestiamo i due casi sopra introducendo un parametro booleano "incomplExc" aggiuntivo
* al BaseASTVisitor e al BaseEASTVisitor che indichi se si vuole che
* venga, o meno, gettata una IncomplException in caso di albero incompleto.
*
* - in BaseASTVisitor, quando si effettua una qualsiasi visit con argomento null, torniamo null o lanciamo l'eccezione
*  sulla base di tale paramentro
* - settiamo appropriatamente tale nuovo parametro in ciascun visitor e in TypeCheckEASTVisitor aggiungiamo
* la cattura di IncomplException quando si visitano dichiarazioni e in Test.java
* (possiamo ora togliere il controllo di STentry a null nel PrintEASTVisitor, controllo che facevamo ma ora, dato che lo
* gestiamo alla base non serve più).

*
 */
public class BaseASTVisitor<S,E extends Exception> {

	private boolean incomplExc; // enables throwing IncomplException
	protected boolean print;    // enables printing
	protected String indent;

	protected BaseASTVisitor() {}
	protected BaseASTVisitor(boolean ie) { incomplExc = ie; } 
	protected BaseASTVisitor(boolean ie, boolean p) { incomplExc = ie; print = p; } 

	/*
	*
	* Quando ci genera una eccezione noi non sappiamo a che punto dell'albero si blocchi. A meno che non siamo nel printAST
	* che stampa man mano che visita l'ST noi non sapremo mai dove si blocca. E' utile allora avere una opzione di debug che ti stampa
	* l'albero AST mentre lo visiti così sai dove si blocca. Dato che sarà comune a tutti i visitor allora lo mettiamo in base.
	*
	* Prendo tutto il codice che c'era in printASTVisitor per la stampa e lo mettiamo qua.
	* PrintNode accessibile da sotto quindi li definiamo protetti.
	 */
	protected void printNode(Node n) {
		System.out.println(indent+extractNodeName(n.getClass().getName()));
	}

	protected void printNode(Node n, String s) {
		System.out.println(indent+extractNodeName(n.getClass().getName())+": "+s);
	}

	//tutti i metodi di visita possono cacciare un'eccezione
	public S visit(Visitable v) throws E {
		// visita classica che richiama la visita con due parametri ma ha la marca vuota quindi in pratica è la visita
		// normale senza ulteriori stampe.
		return visit(v, "");                //performs unmarked visit
	}

	/*
	*
	* Torna un S e poi avremo due modalità se siamo in stampa allora fai tutta sta roba qua, sennò non lo facciamo.
	* Usiamo un campo booleano print.
	*
	* mark serve a metterci qualcosa prima della indentazione. Usata nella visita dell'arrowtypenode per capire meglio come
	* sono divisi i parametri tra paramettri di ingresso e di ritorno.
	*
	 */
	public S visit(Visitable v, String mark) throws E {   //when printing marks this visit with string mark

		//come per il visit in ASTGenerationSTVisitor anche qui devo controllare che il nodo che devo visitare non sia null.
		// se null perchè incompleto, genero l'eccezione di incompletezza altrimenti restituisco null. Genera l'ecezione di incompletezza
		// se l'AST è incompleto. La IncomplException verrà lanciata solamemte da alcuni tipi di visitor non tutti (tipo il
		// il print no). Quindi aggiungiamo un booleano che ci dice se il tipo di visitor che stiamo usando vuole generare
		// la incomplException.
		if (v==null)                                      
			if (incomplExc) throw new IncomplException(); 
			else                                         
				return null; 
		if (print) {									// se devo printare allora faccio l'indentazione classica
			String temp = indent;
			indent = (indent == null) ? "" : indent + "  ";
			indent+=mark; //inserts mark
			// mettiamo un try-catch per via del fatto che possiamo lanciare una eccezione
			try {
				S result = visitByAcc(v);
				return result;
			} finally { indent = temp; } // garantito che verrà fatto quindi la indentazione viene ristabilita.
			                             // Viene ripristinata l'indentazione. Non c'è la catch, quindi cosa succede?
										 // CHe l'eccezione viene riportata sù al chiamante (che ristabilisce la sua indentazione)
										 // poi al chiamante del chiamante ( che ristabilisce la sua indentazione) e così via...
		} else 
			return visitByAcc(v);
	}

	// Modifichiamo inoltre il metodo accept nell'interfaccia Node (Visitable in realtà) e nelle classi dentro AST.java
	// facendo sì che, anch'esso, torni S. Il metodo visitByAcc(ept) si occupa di chiamare solamente il metodo accept sul
	// visitable.
	S visitByAcc(Visitable v) throws E {
		return v.accept(this);
	}

	/*
	* di base c'è una implementazione di visitNode per tutti i tipi di Nodi. Se non implementata, infatti, tira una eccezione
	* che ci ricorda di implementarla! Per generalizzare il più possibile i metodi di visita (nel caso della print non
	* tornano nulla, nel caso del calcolo un intero e così via..) applichiamo i generici.
	*
	* Modifichiamo la classe BaseASTVisitor introducendo, tramite i generics di Java, un tipo parametrico S da usare
	* come tipo di ritorno per i metodi visit (ad esempio tale parametro sarà Integer nel CalcASTVisitor e Void nel PrintASTVisitor).
	 *
	*
	 */
	public S visitNode(ProgLetInNode n) throws E {throw new UnimplException();}
	public S visitNode(ProgNode n) throws E {throw new UnimplException();}
	public S visitNode(FunNode n) throws E {throw new UnimplException();}
	public S visitNode(ParNode n) throws E {throw new UnimplException();}
	public S visitNode(VarNode n) throws E {throw new UnimplException();}
	public S visitNode(PrintNode n) throws E {throw new UnimplException();}
	public S visitNode(IfNode n) throws E {throw new UnimplException();}
	public S visitNode(EqualNode n) throws E {throw new UnimplException();}
	public S visitNode(TimesNode n) throws E {throw new UnimplException();}
	public S visitNode(PlusNode n) throws E {throw new UnimplException();}
	public S visitNode(CallNode n) throws E {throw new UnimplException();}
	public S visitNode(IdNode n) throws E {throw new UnimplException();}
	public S visitNode(BoolNode n) throws E {throw new UnimplException();}
	public S visitNode(IntNode n) throws E {throw new UnimplException();}	

	public S visitNode(ArrowTypeNode n) throws E {throw new UnimplException();}
	public S visitNode(BoolTypeNode n) throws E {throw new UnimplException();}
	public S visitNode(IntTypeNode n) throws E {throw new UnimplException();}
}
