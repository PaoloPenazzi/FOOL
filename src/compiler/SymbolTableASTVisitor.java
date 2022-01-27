package compiler;

import java.util.*;
import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;


/*
*
* Realizziamo la prima fase della semantic analysis vista a lezione: associare
* usi di identificatori (variabili o funzioni) a dichiarazioni tramite symbol
* table, usando le regole di scoping statico
* - a use of an identifier x matches the declaration in the most closely enclosing
* scope (such that the declaration precedes the use)
* - inner scope identifier x declaration hides x declared in an outer scope
* In particolare scegliamo la realizzazione della symbol table come lista (ArrayList) di hashtable (HashMap).
*
* costruiamo una classe SymbolTableASTVisitor
* che associ usi a dichiarazioni tramite la symbol table
* - dando errori in caso di multiple dichiarazioni e identificatori non dichiarati
* (la notifica di errore deve mostrare il numero di linea nel sorgente)
* - attaccando alla foglia dell'AST che rappresenta l'uso di un identificatore x
* la symbol table entry (oggetto di classe STentry) che contiene le informazioni
* prese dalla dichiarazione di x
* L'effetto della visita è che l'AST si trasforma in un Enriched Abstract Syntax Tree (EAST), dove ad alcuni
* nodi dell'AST sono attaccate STentry. E' stato possibile stabilire tale collegamento
*  semantico grazie ai nomi degli identificatori, che da ora in poi non verranno più usati.
*
* Per gestire gli offset relativi al layotu degli AR modifichiamo la classe SymbolTableASTVisitor aggiungendo il calcolo e
* l'inserimento dell'offset nelle STentry (campo offset) per i VarNode.
*
 */
public class SymbolTableASTVisitor extends BaseASTVisitor<Void,VoidException> {

	// la symobl table è una lista di mappe. Una mappa per ogni scope. Nella mappa avrò come chiave la stringa della var
	// o funzione e come valore la STEntry (palline del nostro albero) ovvero SymbolTableEntry un oggetto che conterrà
	// le info necessarie a tener traccia
	// delle palline del nostro albero. Controllo quindi che le dichiarazioni ci siano (check) e poi arrichisce l'albero.
	// alla fine di sta roba la symtable scompare, rimane solo l'ast arricchito.
	// ATTENZIONE: il FRONTE della symTable è sempre a livello nestinglevel. Quindi se per esempio nl=2 allora il fronte
	// della SymTable sarà symtable.get(2)  (la prima mappa che incontriamo). symtable.get(0) è la globalità.
	private List<Map<String, STentry>> symTable = new ArrayList<>();
	private int nestingLevel=0; // current nesting level
	private int decOffset=-2; // counter for offset of local declarations at current nesting level. Gli offset delle dichiarazioni
	// partono da -2.
	// contiamo gli errori.
	int stErrors=0;

	SymbolTableASTVisitor() {}
	SymbolTableASTVisitor(boolean debug) {super(debug);} // enables print for debugging

	// funzione che va a cercare in tutte le tabelle (quindi in tutti gli scope) la variabile/funzone che stiamo usando
	// attraverso l'id. Appena lo trova si ferma.
	private STentry stLookup(String id) {
		int j = nestingLevel;
		STentry entry = null;
		while (j >= 0 && entry == null) 
			entry = symTable.get(j--).get(id);	
		return entry;
	}

	// visito l'ambiente globale in cui ho le dichiarazioni in let, quindi oltre a visitare il corpo dovrò anche aggiungere
	// roba alla symtable. Ma prima di tutto devo aggiungere una mappa (quindi un livello di scope, aumento del nesting level
	// ) dove andrò a mettere le dichiarazioni del livello globale (livello 0)
	@Override
	public Void visitNode(ProgLetInNode n) {
		if (print) printNode(n);
		//creo mappa vuota da usare per l'ambeinte globale e l'aggiungo alla symboltable
		Map<String, STentry> hm = new HashMap<>();
		symTable.add(hm);
		//come si aggiungo le dichiarazioni a questo livello? beh visitando le dichiarazioni ( visit(dec) iterativamente
		// le dichiarazioni scoperte verranno aggiunte alla symtable. Vedi nelle visite sotto che lo fa.
	    for (Node dec : n.declist) visit(dec);
		//visito poi il corpo
		visit(n.exp);
		//una volta visitato il corpo, sono arrivato alla chiusura delle parentesi graffe dell'ambiente globale, rimuovo
		// il livello 0 (globale) della symtable e ora questa è vuota, perchè abbiamo finito. Finita la visita la symtable
		// non c'è più e l'AST è arricchito con tutti i riferimenti delle variabili/funzioni.
		symTable.remove(0);
		return null;
	}

	// progNode di partenza, classico senza nessun tipo di dichiarazione (detto 200 volte: le possibili radici sono due:
	// se ho let/in o se ho direttamente il corpo. Nel caso avessi direttamente il corpo (progNode) allora non faccio
	// nulla perchè non ho dichiarazioni e quindi estendo semplicemente la visita!
	@Override
	public Void visitNode(ProgNode n) {
		if (print) printNode(n);
		visit(n.exp);
		return null;
	}
	
	@Override
	public Void visitNode(FunNode n) {
		if (print) printNode(n);
		//prendo symtable al livello attuale come per la VarNode.
		Map<String, STentry> hm = symTable.get(nestingLevel);

		List<TypeNode> parTypes = new ArrayList<>();  
		for (ParNode par : n.parlist) parTypes.add(par.getType());

		//creo la pallina. Gestisco la dichiarazione di funzione: quindi metto il nestin level e creo l'arrowtype node
		// (tipo funzionale) della funzione (parTypes -> retType). I tipi dei parametri finisco in due posti: qua nella dichiaraizone
		// del tipo della funzione e nella dichiarazione proprio dei parametri stessi di cui dichiariamo anche il tipo.
		STentry entry = new STentry(nestingLevel, new ArrowTypeNode(parTypes,n.retType),decOffset--);
		//inserimento di ID nella symtable. RICORDA CHE IL NOME DELLA FUNZIONE E QUINDI LA FUNZIONE, VIENE INSERITA
		// NELLO SCOPE ESTERNO NON IN QUELLO INTERNO, QUINDI METTO L'ID NELLO SCOPE ESTERNO E POI NE CREO UN ALTRO
		// DOVE AVVERRÀ IL RESTO DELLA ROBA.
		if (hm.put(n.id, entry) != null) {
			System.out.println("Fun id " + n.id + " at line "+ n.getLine() +" already declared");
			stErrors++;
		}

		//creare una nuova hashmap per la symTable. entro in un nuovo scope
		nestingLevel++;
		//aggiungo nuova mappa.
		Map<String, STentry> hmn = new HashMap<>();
		symTable.add(hmn);

		//entro in un nuovo scope, creo un nuovo AR e quindi devo ripartire da -2, salvandomi l'offset da cui son partito.
		int prevNLDecOffset=decOffset; // stores counter for offset of declarations at previous nesting level 
		decOffset=-2;

		/*
		*
		* Notare che non facciamo la visitParNode perchè è da fare qua! E' implicita nella visita della dichiarazione di funzione.
		* In pratca il pezzo di codice qui sotto è la visitParNode.
		*
		* parOffset è il contatore dell'offset dei parametri
		*
		 */
		int parOffset=1;
		for (ParNode par : n.parlist)
			if (hmn.put(par.id, new STentry(nestingLevel,par.getType(),parOffset++)) != null) {
				System.out.println("Par id " + par.id + " at line "+ n.getLine() +" already declared");
				stErrors++;
			}

		// visito le dichiarazione all'interno della funzione. Quando visito queste dichiarazioni posso incontrare di nuovo
		// anche dei funNode quindi si richiama questo metodo.
		for (Node dec : n.declist) visit(dec); // qui ripartono da -2 gli offset
		// visito il corpo. Questa volta è importnate che sia qua perhcè devo considerare tutto ciò che è stato dichiarato
		// o dai parametri o dalle dichiarazioni locali.
		visit(n.exp);

		//ho visitato tutto il corpo e allora rimuovo la hashmap corrente poiche' esco dallo scope.
		symTable.remove(nestingLevel--);
		decOffset=prevNLDecOffset; // restores counter for offset of declarations at previous nesting level . Ripristino l'offset in cui ero.
		return null;
	}
	
	@Override
	public Void visitNode(VarNode n) {
		if (print) printNode(n);
		// la visita di exp la devo fare prima perchè se ho degli usi di variabili devono poi essere già stati dichiarati.
		// si pensi a x = x+1. Se non chiaro riguardare video 2021-11-15 al minuto 3:00:00.
		// IMPORTANTE CHE STIA QUI!
		visit(n.exp);
		// ok sono arrivato a visitare una dichiarazione di una variabile. Che succ? Prendo la tabella che c'è nel fronte
		// quindi al livello di nestinglevel, cioè nel livello in cui mi trovo. Aggiungo quindi a questa tabella questa
		// dichiarazione che sto visitando.
		Map<String, STentry> hm = symTable.get(nestingLevel);
		// mi creo una pallina dove mettere le informazioni di questa dichiarazione. Info necessarie alle varie visite:
		//nesitnglevel per la symtab, type per la typecheck e offset per la codegen (che postdecremento appena aggiungo
		// una nuova variabile)
		STentry entry = new STentry(nestingLevel,n.getType(),decOffset--);
		// inserimento di ID nella symtable
		if (hm.put(n.id, entry) != null) {
			System.out.println("Var id " + n.id + " at line "+ n.getLine() +" already declared");
			stErrors++;
		}
		return null;
	}

	@Override
	public Void visitNode(PrintNode n) {
		if (print) printNode(n);
		visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(IfNode n) {
		if (print) printNode(n);
		visit(n.cond);
		visit(n.th);
		visit(n.el);
		return null;
	}
	
	@Override
	public Void visitNode(EqualNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}
	
	@Override
	public Void visitNode(TimesNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}
	
	@Override
	public Void visitNode(PlusNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}


	// visita di una chiamata di funzione, quindi un uso di funzione quindi devo vedere se è dichiarata.
	// La code generation di usi di identificatori (usi di variabili IdNode o chiamate
	// di funzioni CallNode) richiede di conoscere la differenza di nesting level tra
	// l'uso (IdNode/CallNode) e la relativa dichiarazione (campo "nl" della STentry).
	// Dobbiamo quindi dotare anche IdNode e CallNode di un campo "nl".
	@Override
	public Void visitNode(CallNode n) {
		if (print) printNode(n);
		//cerca la dichiarazione della funzione.
		STentry entry = stLookup(n.id);
		// se la entry è null, non ho trovato la dichiarazione di funzione.
		if (entry == null) {
			System.out.println("Fun id " + n.id + " at line "+ n.getLine() + " not declared");
			stErrors++;
		} else {
			// se la trovo attacco la funNode all' callNode.
			n.entry = entry;
			n.nl = nestingLevel;
		}
		// visito gli argomenti per controllare la giusta dichiarazione. Tutte le volte che visito io posso incontrare cose
		// di ogni tipo, ci potrebbe essere anche un albero complicatissimo!
		for (Node arg : n.arglist) visit(arg);
		return null;
	}


	// visita di un uso di variabile quindi devo vedere se è dichiarata.
	// La code generation di usi di identificatori (usi di variabili IdNode o chiamate
	// di funzioni CallNode) richiede di conoscere la differenza di nesting level tra
	// l'uso (IdNode/CallNode) e la relativa dichiarazione (campo "nl" della STentry).
	// Dobbiamo quindi dotare anche IdNode e CallNode di un campo "nl".
	@Override
	public Void visitNode(IdNode n) {
		if (print) printNode(n);
		//cerca la dichiarazione della variabile.
		STentry entry = stLookup(n.id);
		// se la entry è null, non ho trovato la variabile
		if (entry == null) {
			System.out.println("Var or Par id " + n.id + " at line "+ n.getLine() + " not declared");
			stErrors++;
		} else {
			// se la trovo attacco la varNode all' idNode.
			n.entry = entry;
			n.nl = nestingLevel;
		}
		return null;
	}

	@Override
	public Void visitNode(BoolNode n) {
		if (print) printNode(n, n.val.toString());
		return null;
	}

	@Override
	public Void visitNode(IntNode n) {
		if (print) printNode(n, n.val.toString());
		return null;
	}
}
