package compiler;

import compiler.AST.*;
import compiler.lib.*;
import compiler.exc.*;

/*
 * Realizziamo una classe PrintASTVisitor (di cui viene dato un file iniziale) che effettui la visita implementando
 * un metodo "visitNode" per ciascuna classe AST.java, che riceva un oggetto di tale classe come argomento
 * (lo lanceremo sulla radice, che è di classe ProgNode).
 *
 * Usiamo la reflection di Java per fare le stampe, in modo da facilitare l'estendibilità del linguaggio. Ora in BaseASTVisitor
 * avremo due tipi di stampa una che stampa solo il nodo e uno che stampa nodo e stringa per le foglie.
 *
 *
 * Prima avevamo creato il printASTVisiotr ora lo estendiamo per stampare l"EAST quindi un AST arricchito. Ragionamento
 * analogo a prima. Facciamo una semplice visita dell'Enriched AST generato in modo da visualizzarlo stampando in modo
 * indentato, oltre ai suoi nodi (di classe che eredita da Node), anche le sue STentry. Per farlo dovremo realizzare un
 *  visitor che consenta di visitare sia Node che STentry tramite una interfaccia Visitable (contenente il metodo "accept")
 * implementata da entrambi.
 *
 */

public class PrintEASTVisitor extends BaseEASTVisitor<Void,VoidException> {

	/*
	 * printa sempre (p=true) e
	 */
	PrintEASTVisitor() { super(false,true); } 

	@Override
	public Void visitNode(ProgLetInNode n) {
		printNode(n);
		for (Node dec : n.declist) visit(dec);
		// qua avremmo dovuto fare tipo visitNode(n.exp) ma n.exp di che tipo è? quindi: Perche' Java dà errore quando
		// proviamo a invocare "visitNode" passando come argomento un nodo figlio?
		// Mentre, es., in C# esiste un cast "(dynamic)" che consente di determinare il metodo visitNode da invocare
		// a run-time in base al tipo effettivo dell'argomento passato (come dynamic binding ma fatto sull'argomento),
		// ciò non è possibile in Java: associazione tra invocazione e metodo visitNode invocato fatta a compile-time.
		// In Java dobbiamo usare il metodo classico di implementare il visitor pattern.
		// a. si crea un metodo visit che riceve un generico Node come parametro n
		// b. al fine di invocare il metodo visit specifico per il tipo effettivo di n:
		// - si dota ciascuna classe in AST di un metodo accept che invochi visit(this)
		// - si invoca n.accept( ) in modo da utilizzare il dynamic binding di Java su n
		// NOTA:
		// - devo aggiungere all'interfaccia Node il metodo accept
		// - tale metodo deve ricevere l'oggetto PrintASTVisitor (su cui invocare visitNode)
		//
		// Quando visitavamo i syntax tree utilizzavamo l'implementazione del visitor pattern generata da ANTLR4.
		// Per gli AST abbiamo dovuto implementarlo noi!
		visit(n.exp);
		return null;
	}

	/*
	*
	* Sistemiamo il PrintASTVisitor in modo che funzioni con la nuova versione parametrica di BaseASTVisitor usando Void
	* (dichiarare Void come tipo di ritorno mi costringe a mettere "return null;").
	*
	 */

	@Override
	public Void visitNode(ProgNode n) {
		printNode(n);
		visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(FunNode n) {
		printNode(n,n.id);
		visit(n.retType);
		for (ParNode par : n.parlist) visit(par);
		for (Node dec : n.declist) visit(dec);
		visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(ParNode n) {
		printNode(n,n.id);
		visit(n.getType());
		return null;
	}

	@Override
	public Void visitNode(VarNode n) {
		printNode(n,n.id);
		visit(n.getType());
		visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(PrintNode n) {
		printNode(n);
		visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(IfNode n) {
		printNode(n);
		visit(n.cond);
		visit(n.th);
		visit(n.el);
		return null;
	}

	@Override
	public Void visitNode(EqualNode n) {
		printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(TimesNode n) {
		printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(PlusNode n) {
		printNode(n); //stampa "Plus" oppure se TimesNode stampa "Times" e così via...
		// ho due argomenti nel PlusNode, cos' come in altri, allora faccio la visit due volte
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(CallNode n) {
		printNode(n,n.id+" at nestinglevel "+n.nl);
		// i nodi che hanno attaccato la pallina dovranno anche visitare la stentry
		// per poterla stampare
		visit(n.entry);
		for (Node arg : n.arglist) visit(arg);
		return null;
	}

	@Override
	public Void visitNode(IdNode n) {
		printNode(n,n.id+" at nestinglevel "+n.nl);
		// i nodi che hanno attaccato la pallina dovranno anche visitare la stentry
		// per poterla stampare
		visit(n.entry);
		return null;
	}

	@Override
	public Void visitNode(BoolNode n) {
		printNode(n,n.val.toString());
		return null;
	}

	@Override
	public Void visitNode(IntNode n) {
		printNode(n,n.val.toString()); //Stampa "Int: 3" ad esempio
		return null;
	}
	
	@Override
	public Void visitNode(ArrowTypeNode n) {
		printNode(n);
		for (Node par: n.parlist) visit(par); //visitiamo i figli
		visit(n.ret,"->"); //marks return type. per renderlo visivamente più bello attacchiamo una freccettina
		// agli spazi di indentazione. richiede una stampa speciale
		// "marcata" per il tipo di ritorno (oltre ad essere indentato, viene preceduto
		// da "->"), realizzata aggiungendo un parametro a metodo visit di BaseASTVisitor
		return null;
	}

	@Override
	public Void visitNode(BoolTypeNode n) {
		printNode(n);
		return null;
	}

	@Override
	public Void visitNode(IntTypeNode n) {
		//ho un intero quindi la foglia e stampo e basta. NOTARE IL TIPO DI RITORNO Void!
		printNode(n);
		return null;
	}

	// questa mi stampa la roba relativa alla pallina a cui una chiamata è attaccata. Tipo se in un programma uso una var
	// y verrà stampata prima la roba relativa all idNode quindi alla chiamata di variabile poi, attraverso la visit(n.entry)
	// che si usa in idNode (e callNode) verrà stampata anche la roba relativa alla dichiarazione ("guarda che la tua y
	// è stata dichiarata a nestlev 1, è di type int, ecc..."). LE PALLINE MI DICONO QUALE DICHIARAZIONE È ATTACCATA ALL'USO!
	@Override
	public Void visitSTentry(STentry entry) {
		printSTentry("nestlev "+entry.nl);
		printSTentry("type"); // il tipo è un nodo (potrebbe essere un alberello) quindi bisogna visitare anche il tipo
		visit(entry.type);
		printSTentry("offset "+entry.offset);
		return null;
	}

}
