package compiler;

import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

import javax.swing.plaf.metal.MetalToggleButtonUI;

import static compiler.TypeRels.*;

//visitNode(n) fa il type checking di un Node n e ritorna:
//- per una espressione, il suo tipo (oggetto BoolTypeNode o IntTypeNode)
//- per una dichiarazione, "null"; controlla la correttezza interna della dichiarazione
//(- per un tipo: "null"; controlla che il tipo non sia incompleto) 
//
//visitSTentry(s) ritorna, per una STentry s, il tipo contenuto al suo interno

/*
*
* In certi casi, per gestire errori rilevati durante una visita, è necessario
interrompere la visita lanciando una eccezione (es. perche' è impossibile
determinare un valore di ritorno consistente per visitNode in caso di errore).
Ciò sarà necessario per il TypeCheckEASTVisitor che realizzeremo oggi.
*
* Faremo delle visiite in cui nelle visite verrà lanciata una eccezione se ci fosse un errore di tipo.
*
* Obbiettivo del typechecker è controllare in tutti i possibili casi di uso di tipi che siano giusti.
* Ricordati che la visita del typechecker viene fatta bottom-up: dalle foglie alla radice.
*
* Realizziamo la seconda fase della semantic analysis vista a lezione: il type checking, che viene effettuato tramite
* visita dell'enriched abstract syntax tree determinando i tipi delle espressioni (TypeNode) in modo bottom-up.
*
* Costruiamo una classe TypeCheckEASTVisitor che realizzi il type checking dei programmi FOOL la cui sintassi è quella
*  di FOOL.g4:
* - la relazione di subtyping è definita tramite il metodo isSubtype di FOOLlib
* - consideriamo i booleani essere sottotipo degli interi con l'interpretazione:
* true vale 1 e false vale 0
* In caso il visitor rilevi un errore di tipo deve lanciare una eccezione TypeException contenente il messaggio
*  di errore ed il numero di linea: ciò automaticamente incrementa il contatore "typeErrors" della classe FOOLlib.
*
*
* Per poter rilevare multipli errori di tipo introduciamo la cattura di TypeException durante la visita,
* in caso di type checking di dichiarazioni. La visita di dichiarazioni non torna un oggetto TypeNode (semplicemente torna
* null) che serva al chiamante: possiamo quindi accettare a questo livello un errore di tipo avvenuto dentro la
* dichiarazione senza propagare l'eccezione.
* - introduciamo la cattura e stampa di eccezioni quando si visitano dichiarazioni
* - facciamo lo stesso in Test per le eccezioni nella main program expression
*
* Il compilatore deve completare tutte le fasi del front-end anche in presenza di errori collezionando più errori
* possibili (anche relativi alle fasi precedenti al type checking) in modo che il programmatore possa correggerli insieme.
*
* Il problema però è che:
* - errori lessicali/sintattici possono portare a creazione da parte di ANTLR4 di Syntax Tree incompleti
*  (in cui le variabili figlie di un nodo sono "null")
* - tali errori (per un effetto a catena) ed errori semantici rilevati via symbol table possono portare
*  alla creazione di EAST che contengono anch'essi figli null
* Ciò tipicamente genera null pointer exceptions durante l'esecuzione e impedisce che si continui a collezionare
*  errori per le "parti buone" del programma.
*
*
* Si noti che la visita dei TypeNode (che ritorna null) è importante, non solo
* per stamparli in caso di debug, ma anche per controllare che non siano incompleti
* prima di utilizzarli!
*
*
*
 */
public class TypeCheckEASTVisitor extends BaseEASTVisitor<TypeNode,TypeException> {

	TypeCheckEASTVisitor() { super(true); } // enables incomplete tree exceptions 
	TypeCheckEASTVisitor(boolean debug) { super(true,debug); } // enables print for debugging

	// checks that a type object is visitable (not incomplete)
	// metodo ausiliario ckvisit(t) da utilizzare quando si leggono tipi t in
	// campi dell'EAST, che lancia la visita su t (per controllarne la completezza e
	// stamparlo in caso di debug) e torna t stesso.
	// * Si noti che la visita dei TypeNode (che ritorna null) è importante, non solo
	// * per stamparli in caso di debug, ma anche per controllare che non siano incompleti
	// * prima di utilizzarli (si ricorda che se io li usassi e fossero vuoti, questi genererebbero nullPointerExc.,
	// cosa non gradita!) ! (Ti ricordi che nelle vecchie versioni del compilatore avevo dei gran visit() in giro?
	// Lo facevamo per i motivi scritti sopra e ora lo facciamo in ckvisit()
	private TypeNode ckvisit(TypeNode t) throws TypeException {
		//visito
		visit(t);
		//ritorno (se tutto bene torno un TypeNode pieno e non nullo)
		return t;
	} 
	
	@Override
	public TypeNode visitNode(ProgLetInNode n) throws TypeException {
		if (print) printNode(n);
		for (Node dec : n.declist)
			try {
				// Si noti che la visita dei TypeNode (che ritorna null) è importante, non solo
				// per stamparli in caso di debug, ma anche per controllare che non siano incompleti
				// prima di utilizzarli!
				// visit(dec);
				visit(dec);
			} catch (IncomplException e) { //propagazione di un errore sintattico che già sappiamo. In fase di debug potrebbe tornare utile
				//scrivere qualcosa.
			} catch (TypeException e) {
				// per non bloccare tutto alla dichiarazione
				System.out.println("Type checking error in a declaration: " + e.text);
			}
		return visit(n.exp);
	}

	// come print, valuto solo il tipo del programma, faccio partire il tutto in pratica...
	// caso in cui non ho dichiarazione, ritorno solo il tipo del corpo di programma che è l'unica cosa che ho.
	@Override
	public TypeNode visitNode(ProgNode n) throws TypeException {
		if (print) printNode(n);
		return visit(n.exp);
	}

	@Override
	public TypeNode visitNode(FunNode n) throws TypeException {
		if (print) printNode(n,n.id);
		for (Node dec : n.declist)
			try {
				visit(dec);
			} catch (IncomplException e) { 
			} catch (TypeException e) {
				// per non bloccare tutto alla dichiarazione
				System.out.println("Type checking error in a declaration: " + e.text);
			}
		// anche qua come nella visita della varNode, controlliamo che il corpo della funzioni ritorni un tipo correlato
		// (sottotipo di...) con quello dichiarato (es: se una funzione dichiarata torna int e gli facciamo tornare una stringa
		// dal corpo dell funzione allora non andrà bene!).
		if ( !isSubtype(visit(n.exp),ckvisit(n.retType)) ) 
			throw new TypeException("Wrong return type for function " + n.id,n.getLine());
		return null;
	}

	// la dichiarazione di una variabile fa parte di un let/in. Cosa dobbiamo controllare in una dichiarazione di variabile?
	// che il tipo della espressione rispecchi il tipo dichiarato (es: int x = 3 OK!, int x = "cazzo" NO!).
	@Override
	public TypeNode visitNode(VarNode n) throws TypeException {
		if (print) printNode(n,n.id);
		if ( !isSubtype(visit(n.exp),ckvisit(n.getType())) )
			throw new TypeException("Incompatible value for variable " + n.id,n.getLine());
		return null;
	}

	// non fa null'altro...
	@Override
	public TypeNode visitNode(PrintNode n) throws TypeException {
		if (print) printNode(n);
		return visit(n.exp);
	}

	// l'if ha 3 figli tutti e tre potrebbero essere super complicati. Ricordati che l'ifNode per noi è un tipo funzionale
	// quindi torna qualcosa e in particolare noi qua dobbiamo capire il tipo del ritorno!
	@Override
	public TypeNode visitNode(IfNode n) throws TypeException {
		if (print) printNode(n);
		// per prima cosa controlliamo se la condizione dell'if è booleana: se non lo ho caccio errore. Ha senso dire booleano
		// o sottotipo? In realtà non troppo ma se in futuro esistesse un sottotipo di bool allora è gia a posto. (in Java
		// esiste, perchè tutti estendono da Object).
		if ( !(isSubtype(visit(n.cond), new BoolTypeNode())) )
			throw new TypeException("Non boolean condition in if",n.getLine());
		// se la condizione è giusta proseguo con lo statement
		TypeNode t = visit(n.th);
		TypeNode e = visit(n.el);
		// come devono essere relazionati il tipo dell'else e del then? Anche in questo caso cerchiamo di generalizzare perchè
		//nel nostro caso è facile (abbiamo solo int e bool) ma in Java ad esempio? Nel nostro caso (con int e bool)
		// i tipi di ritorno del then-else dovranno essere uno sottotipo dell'altro e tornare il sovra-tipo, quello più generico!
		// Mentre nel caso generico è importnate che abbiamo un antenato in comune e prendiamo l'antenato in comune più prossimo
		// (minimal common anchestor). In Java si ha sempre un antenato in comune che è Object.
		if (isSubtype(t, e)) return e;
		if (isSubtype(e, t)) return t;
		throw new TypeException("Incompatible types in then-else branches",n.getLine());
	}

	@Override
	public TypeNode visitNode(EqualNode n) throws TypeException {
		if (print) printNode(n);
		TypeNode l = visit(n.left);
		TypeNode r = visit(n.right);
		// cosa dobbiamo gestire come tipo tra questi argomenti dell'equal? Nel caso di int e bool è facile, perchè le
		// visite torneranno sempre int o bool. Ma pensiamo a Java dove abbiamo le classi. E' necessario che tra il figlio
		// di sx e dx ci sia una relazione, in particolare vogliamo che abbiano un figlio in comune,
		// quindi un sottotipo in comune (in questo caso particolare ci accontentiamo che uno sia sottotipo dell'altro)
		// per permettere di equiparare queste due cose. Quindi il concetto è questo: controllare che i tipi che
		// confrontiamo siano sottotipo dell'altro, così da avere una sorta di relazione.
		if ( !(isSubtype(l, r) || isSubtype(r, l)) )
			throw new TypeException("Incompatible types in equal ",n.getLine());
		// è un == quindi cosa torniamo? Un booleano!
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(GreaterEqualNode n) throws TypeException {
		if (print) printNode(n);
		TypeNode l = visit(n.left);
		TypeNode r = visit(n.right);
		if ( !(isSubtype(l, r) || isSubtype(r, l)) )
			throw new TypeException("Incompatible types in greater equal ",n.getLine());
		// è un >= quindi cosa torniamo? Un booleano!
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(LessEqualNode n) throws TypeException {
		if (print) printNode(n);
		TypeNode l = visit(n.left);
		TypeNode r = visit(n.right);
		if ( !(isSubtype(l, r) || isSubtype(r, l)) )
			throw new TypeException("Incompatible types in less equal ",n.getLine());
		// è un <= quindi cosa torniamo? Un booleano!
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(NotNode n) throws TypeException {
		if (print) printNode(n);
		TypeNode e = visit(n.exp);
		if ( !areBoolean(e) )
			throw new TypeException("Not operator applied to non-boolean exp ",n.getLine());
		// è un ! quindi cosa torniamo? Un booleano!
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(AndNode n) throws TypeException {
		if (print) printNode(n);
		TypeNode l = visit(n.left);
		TypeNode r = visit(n.right);
		if ( areBoolean(l,r) )
			throw new TypeException("And operator applied to non-boolean exps ",n.getLine());
		// è un && quindi cosa torniamo? Un booleano!
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(OrNode n) throws TypeException {
		if (print) printNode(n);
		TypeNode l = visit(n.left);
		TypeNode r = visit(n.right);
		if ( areBoolean(l,r) )
			throw new TypeException("Or operator applied to non-boolean exps ",n.getLine());
		// è un && quindi cosa torniamo? Un booleano!
		return new BoolTypeNode();
	}

	@Override
	public TypeNode visitNode(TimesNode n) throws TypeException {
		if (print) printNode(n);
		// concetto identico al PlusNode, vedere commenti sotto.
		if ( !(isSubtype(visit(n.left), new IntTypeNode())
				&& isSubtype(visit(n.right), new IntTypeNode())) )
			throw new TypeException("Non integers in multiplication ",n.getLine());
		return new IntTypeNode();
	}

	@Override
	public TypeNode visitNode(DivNode n) throws TypeException {
		if (print) printNode(n);
		if ( !(isSubtype(visit(n.left), new IntTypeNode())
				&& isSubtype(visit(n.right), new IntTypeNode())) )
			throw new TypeException("Non integers in division ",n.getLine());
		return new IntTypeNode();
	}

	@Override
	public TypeNode visitNode(PlusNode n) throws TypeException {
		if (print) printNode(n);
		// nell'idNode si controllava che non chiamassimo una funzione (male) al posto di una variabile. Qui cosa dobbiamo fare?
		// dobbiamo controllare che le somme non vengano fatte tra tipi diversi! Posso sommare stringhe (che
		// non ho, ma è per fare un esempio) e interi? Ovviamente no! Quindi devo fare questo controllo! Controllo che sia il figlio
		// di sx sia quello di dx della somma siano di tipo int o sottotipo (booleano, che vale 0/1, quindi posso sommarlo
		// ad intero).
		if ( !(isSubtype(visit(n.left), new IntTypeNode())
				&& isSubtype(visit(n.right), new IntTypeNode())) )
			throw new TypeException("Non integers in sum ",n.getLine());
		// la somma dovrà tornare un intero, OVVIAMENTE
		return new IntTypeNode();
	}

	@Override
	public TypeNode visitNode(MinusNode n) throws TypeException {
		if (print) printNode(n);
		if ( !(isSubtype(visit(n.left), new IntTypeNode())
				&& isSubtype(visit(n.right), new IntTypeNode())) )
			throw new TypeException("Non integers in subtraction ",n.getLine());
		return new IntTypeNode();
	}


	@Override
	public TypeNode visitNode(CallNode n) throws TypeException {
		if (print) printNode(n,n.id);
		ArrowTypeNode at;

		// recupero tipo (che mi aspetto essere ArrowTypeNode) da STentry
		TypeNode t = visit(n.entry); 
		if ( !(t instanceof ArrowTypeNode) && !(t instanceof MethodTypeNode) ) {
			throw new TypeException("Invocation of a non-function " + n.id, n.getLine());
		}
		// Se t è un methodtype node lo casto ad arrowtype node con il metodo fun()
		if (t instanceof MethodTypeNode) {
			at = ((MethodTypeNode) t).fun;
		} else {
			at = (ArrowTypeNode) t;
		}

		// errori possibili (che indicano, in ordine, i controlli da fare):
		// Invocation of a non-function [id del CallNode]
		// Wrong number of parameters in the invocation of [id del CallNode]
		// Wrong type for ...-th parameter in the invocation of [id del CallNode]
		if ( !(at.parlist.size() == n.arglist.size()) )
			// caso in cui nella dichiarazione ho un certo numero di argomenti e nell'uso ne ho un numero diverso
			throw new TypeException("Wrong number of parameters in the invocation of "+n.id,n.getLine());
		for (int i = 0; i < n.arglist.size(); i++)
			if ( !(isSubtype(visit(n.arglist.get(i)),at.parlist.get(i))) )
				// caso in cui nella dichiarazione ho certi tipi negli argomenti e nell'uso ne ho tipi diversi
				throw new TypeException("Wrong type for "+(i+1)+"-th parameter in the invocation of "+n.id,n.getLine());

		// dopo i check restituisco il tipo di ritorno della funzione, perchè vuol dire che tutto è andato bene.
		return at.ret;
	}

	// ci domandiamo: di che tipo è un id node (ovvero l'uso di una varibaile o parametro) ?
	// sarà il tipo con cui è stato dichiarato. Da dove lo prendiamo? Dalla pallina! QUindi dobbiamo visitare la pallina
	// (STEntry) e fare in modo che in questa visita (si intende nella TypeCheck visita non la visita dell'idNode e basta)
	// la visita ritorni il tipo della pallina/dichiarazione. Ricapitolando: la visita della STEntry cambia a seconda del tipo
	// di visita generica stiamo facendo. Nella visita del type checking noi visitiamo il tipo della pallina mentre nel
	// print visitor noi guardiamo il nesting level e le altre info perchè ci interessa sapere (o meglio stampare) quelle!
	@Override
	public TypeNode visitNode(IdNode n) throws TypeException {
		if (print) printNode(n,n.id);
		TypeNode t = visit(n.entry);
		// ci va bene se usiamo il nome di una funzione come uso di variabile? ovviamente no! Quindi dobbiamo controllare
		// che questo nome non sia di tipo funzionale! Perchè useremo una funzione in modo sbagliato e non va bene
		if (t instanceof ArrowTypeNode || t instanceof ClassTypeNode || t instanceof MethodTypeNode) {
			throw new TypeException("Wrong usage of function identifier " + n.id, n.getLine());
		}
		return t;
	}

	@Override
	public TypeNode visitNode(BoolNode n) {
		if (print) printNode(n,n.val.toString());
		return new BoolTypeNode();
	}

	// ci domandiamo: di che tipo è un intero? ovviamente un intero! Quindi ritorniamo intTypeNode
	@Override
	public TypeNode visitNode(IntNode n) {
		if (print) printNode(n,n.val.toString());
		return new IntTypeNode();
	}

	
	@Override
	public TypeNode visitNode(ArrowTypeNode n) throws TypeException {
		if (print) printNode(n);
		for (Node par: n.parlist) visit(par);
		visit(n.ret,"->"); //marks return type
		return null;
	}

	// ci domandiamo: di che tipo è un bool? ovviamente un bool! Quindi ritorniamo boolTypeNode
	@Override
	public TypeNode visitNode(BoolTypeNode n) {
		if (print) printNode(n);
		return null;
	}

	@Override
	public TypeNode visitNode(ClassTypeNode n) {
		if (print) printNode(n);
		return null;
	}

	@Override
	public TypeNode visitNode(RefTypeNode n) {
		if (print) printNode(n);
		return null;
	}

	@Override
	public TypeNode visitNode(MethodTypeNode n) {
		if (print) printNode(n);
		return null;
	}

	@Override
	public TypeNode visitNode(EmptyTypeNode n) {
		if (print) printNode(n);
		return null;
	}

	@Override
	public TypeNode visitNode(IntTypeNode n) {
		if (print) printNode(n);
		return null;
	}

	// STentry (ritorna campo type). Verrà chiamata quando c'è da sapere il tipo all'interno di una pallina e allora tornerà
	// il tipo della dichiarazione.
	@Override
	public TypeNode visitSTentry(STentry entry) throws TypeException {
		if (print) printSTentry("type");
		// visita comoda in fase di debug/stampa
		return ckvisit(entry.type); 
	}

	// non usato (come ParNode)
	//	@Override
	//	public TypeNode visitNode(FieldNode node) throws TypeException {
	//		return super.visitNode(node);
	//	}


	@Override
	public TypeNode visitNode(MethodNode n) throws TypeException {
		if (print) printNode(n,n.id);

		for (Node dec : n.declist)
			try {
				visit(dec);
			} catch (IncomplException e) {
			} catch (TypeException e) {
				// per non bloccare tutto alla dichiarazione
				System.out.println("Type checking error in a method declaration: " + e.text);
			}
		// anche qua come nella visita della varNode, controlliamo che il corpo del metodo ritorni un tipo correlato
		// (sottotipo di...) con quello dichiarato (es: se un metodo dichiarato torna int e gli facciamo tornare una stringa
		// dal corpo della funzione allora non andrà bene!).
		if ( !isSubtype(visit(n.exp), ckvisit(n.retType)) )
			throw new TypeException("Wrong return type for method " + n.id,n.getLine());
		return null;
	}

	@Override
	public TypeNode visitNode(ClassNode n) throws TypeException {
		if (print) printNode(n,n.id);

		for (MethodNode methodNode : n.methodList) {
			try {
				visit(methodNode);
			} catch (IncomplException e) {
			} catch (TypeException e) {
				// per non bloccare tutto alla dichiarazione
				System.out.println("Type checking error in a method declaration: " + e.text);
			}
		}

		return null;
	}

	@Override
	public TypeNode visitNode(ClassCallNode n) throws TypeException {
		if (print) printNode(n,n.classID.id);
		ArrowTypeNode at;

		// recupero tipo (che mi aspetto essere MethodTypeNode) da STentry. In teoria non sarò sempre e solo un
		// methodTypeNode?
		TypeNode t = visit(n.methodEntry);

		if ( !(t instanceof ArrowTypeNode) && !(t instanceof MethodTypeNode) ) {
			throw new TypeException("Invocation of a non-method " + n.methodID, n.getLine());
		}

		if (t instanceof MethodTypeNode) {
			at = ((MethodTypeNode) t).fun;
		} else {
			System.out.println("Sono un arrowtype node");
			at = (ArrowTypeNode) t;
		}

		// errori possibili (che indicano, in ordine, i controlli da fare):
		// Invocation of a non-function [id del CallNode]
		// Wrong number of parameters in the invocation of [id del CallNode]
		// Wrong type for ...-th parameter in the invocation of [id del CallNode]
		if ( !(at.parlist.size() == n.argList.size()) )
			// caso in cui nella dichiarazione ho un certo numero di argomenti e nell'uso ne ho un numero diverso
			throw new TypeException("Wrong number of parameters in the invocation of "+n.methodID,n.getLine());
		for (int i = 0; i < n.argList.size(); i++)
			if ( !(isSubtype(visit(n.argList.get(i)),at.parlist.get(i))) )
				// caso in cui nella dichiarazione ho certi tipi negli argomenti e nell'uso ne ho tipi diversi
				throw new TypeException("Wrong type for "+(i+1)+"-th parameter in the invocation of "+n.methodID,n.getLine());

		// dopo i check restituisco il tipo di ritorno della funzione, perchè vuol dire che tutto è andato bene.
		return at.ret;
	}

	@Override
	public TypeNode visitNode(NewNode n) throws TypeException {

		// recupero tipo e mi aspetto sia RefTypeNode
		TypeNode t = visit(n.entry);
		if ( !(t instanceof ClassTypeNode) ) {
			throw new TypeException("Invocation of a new non-class " + n.id, n.getLine());
		}

		ClassTypeNode at = (ClassTypeNode) t;

		// errori possibili (che indicano, in ordine, i controlli da fare):
		// Invocation of a new non-class
		// Wrong number of parameters in the invocation of new non-class
		// Wrong type for ...-th parameter in the invocation of new non-class
		if ( !(at.allFields.size() == n.argList.size()) )
			// caso in cui nella dichiarazione ho un certo numero di argomenti e nell'uso ne ho un numero diverso
			throw new TypeException("Wrong number of parameters in the invocation of "+n.id,n.getLine());
		for (int i = 0; i < n.argList.size(); i++)
			if ( !(isSubtype(visit(n.argList.get(i)),at.allFields.get(i))) )
				// caso in cui nella dichiarazione ho certi tipi negli argomenti e nell'uso ne ho tipi diversi
				throw new TypeException("Wrong type for "+(i+1)+"-th parameter in the invocation of "+n.id,n.getLine());

		return new RefTypeNode(n.id);
	}

	@Override
	public TypeNode visitNode(EmptyNode n) throws TypeException {
		return new EmptyTypeNode();
	}
}