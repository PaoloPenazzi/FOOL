package compiler;

import compiler.AST.*;
import compiler.lib.*;
import compiler.exc.*;
import static compiler.lib.FOOLlib.*;

/*
*
* Ultima parte del compilatore: genera codice assembly da dare in pasto all'assemblatore e alla SVM.
*
* Quindi genererà codice sotto forma di una stringa, quindi il BaseVisitor torna String e non dovrà gestire eccezioni
* (VoidExc.).
*
* Realizziamo l'ultima fase del compilatore: la code generation, che viene effettuato tramite visita
* dell'(Enriched) Abstract Syntax Tree determinando il codice SVM da generare (String) in modo bottom-up. Perchè il file
* si chiama CodeGenerationASTVisitor ma uso l'EAST? Questo non vuol dire che non uso le palline (le uso eccome)
* però tecnicamente io non entro mai dentro le STEntry e quindi non visitandole non si puo definire EASTVisitor. Uso le
* info dentro le palline ma non le visito, perchè non devo far controlli. Spiegato bene qui sotto:
* "Tecnicamente la code generation non richiederà di proseguire la visita dell'AST visitando anche le STentry.
* Quindi, pur accedendo alle STentry attaccate ai Node, sarà sufficiente un CodeGenerationASTVisitor che genera
* il codice sotto forma di una stringa Java."
*
* In generale il codice che vorremo creare sarà di questo tipo:
*
* 				codice
* 				...
* 				halt
*
* 				label 0:
* 				...
* 				label n:
*
* Se noi non abbiamo dichiarazione (e quindi usi di funzione) ci sarà solo l'ambiente globale e non avremo nesting di
* nessun tipo. Quindi programmi semplici o con un solo let/in o direttamente il probody. Come sarà quindi la struttura
*  di questo AR (Activation Record)? Cosa ci andrà dentro questa struttura dati? Tipicamente, dopo una chiamata di funzione,
* ci sarà il Control Link, gli argomenti passati ecc. ma qui (in questa prima fase in cui non abbiamo funzioni) non avremo
* nulla di tutto questo. Noi quindi ora stiamo facendo prima l'AR dell'ambiente globale (senza nested) poi avremo anche
* le funzioni (quindi nested). Sullo stack si ha quindi il solo AR dell'ambiente globale (in cui vengono
* allocate le variabili dichiarate quando inizia l'esecuzione del programma): utilizziamo come layout per tale AR questo
* sotto.
*
* SOLO AMB GLOBALE, LAYOUT SUO AR: (STACK CRESCE VERSO IL BASSO!)
*
* [BASE DELLO STACK E' QUI SOTTO]  <- $fp (settato a valore iniziale di $sp)
* valore prima var dichiarata      [offset -1]
* valore seconda var               [offset -2]
* .
* .
* valore ultima (n-esima) var      [offset -n]
*
*
* Gli offset a lezione andavano di 4 in 4 qui invece di 1 in 1 perchè la nostra memoria occupa un posto dell'array da noi fatto.
* Le variabili vengono allocate verso il basso, ogni volta si scala di 1 l'offset. E' importante fare sto discorso perchè
* dovremo sapere esattamente dove metteremo la variabile e quindi il suo offset, per quando USIAMO queste variabili. Dove
* mettiamo questa informazione di offset per ogni variabile? NELLA FOTTUTA PALLINA, NELLA STENTRY! perchè in questa maniera
* so l'offset e so dove andare a leggere il valore della variabile. La prima cosa da fare è associare questa informazione
* di offset alla pallina.
*
* Come faremo a ricondurci alla var dichiarata che in un certo momento ci interessa? prendiamo l'fp e gli sottraiamo l'offset
* della variabile usata, offset che si prenderà dalla dichiarazione della variabile e quindi dalla pallina.
*
* Ricordarsi che tutta la generazione di codice e la stackVM funziona seguenda la INVARIANTE: la generazione di codice lascia
* lo stack come l'abbiamo trovato e il risultato della funzione è in cima.
*
* Fino ad ora abbiamo gestito solo le robe a livello globale. Ora ci addentriamo con dei livelli nested, quindi dovremmo
* gestire più AR e più generazioni di AR. Rispetto a quello visto a lezione abbiamo gli scope annidati. Quando chiamiamo una
* funzione dovremo anche gestire gli AL (Access Link). I nostri AR avranno oltre al CL (Control link, che restituisce il controllo
* al chiamante) anche l'AL (se io sto chiamando una certa funzione f (quindi sto creando l'AR di f) a cosa punta l'AL di
* questo nuovo AR di f? punterà allo scope che staticamente racchiude il corpo di f. Quindi a quale AR deve puntare ?
* Al AR dello scope in cui f viene dichiarata! Quindi quando io setto l'AL dentro un AR di una chiamata di f devo fare in modo
* che punti all'AR dello scope in cui f viene dichiarata! Quindi ci serviranno, per raggiungere questo AR che è l'AR dello scope
* in cui f viene dichiarata, della differenza di nesting level tra dove sta avvenendo la chiamata di f (chi chiama f,
* in pratica e la dichiarazione di f. Quindi avrò bisogno dei nl. Come faccio a sapere il nesting level della dichiarazione di f
* per calcolare questa differenza? E' il solito discorso: quando ho bisogno di info relative alla dichiarazione di una fun/var
* le prendo dalla pallina (STEntry). Il primo progetto sopra era solo per l'ambiente globale, questo x entrambi.
*
* Progettiamo il layout degli AR (quello finale per ambiente globale e chiamate di funzioni). Sul progetto del layout
* degli AR sarà basata la generazione di codice.
*
*
*
*
* LAYOUT AR DI UNA FUNZIONE (STACK CRESCE VERSO IL BASSO!)
*
* CL:address (fp) di AR chiamante
* valore ultimo (m-esimo) parametro         [offset m]  //parametri con offset positivi
* .
* .
* valore primo parametro                    [offset 1]
* AL:address (fp) di AR dichiarazione       <- $fp in codice body della funz //(posizione di riferimento sull'Access Link perchè è il punto più usato
* Return Address													// RA comodo averlo qui in mezzo. ma cosa succede agli offset? bisogna partire da -2.
* valore/addr prima var/funz dichiarata     [offset -2]
* valore/addr seconda var/funz              [offset -3]
* .
* .
* valore/addr ultima (n-esima) var/funz     [offset -(n+1)]
*
* --------------------------------------------------------------
*
* Cerchiamo di mantenere l'uniformità tra layout dell'ar globale e della funzione. Infatti in ambo i layout le var dichiarate
* crescono verso il basso.
*
* LAYOUT AR DELL'AMBIENTE GLOBALE
*
* [BASE DELLO STACK E' QUI SOTTO]           <- $fp in codice "main" (posizione di riferimento)
* Return Address fittizio 0 (si va in halt) //RA fittizio che non uso ma per avere layout uniformi in modo tale che le dich vanno sempre da -2 in poi
* valore/addr prima var/funz dichiarata     [offset -2]
* valore/addr seconda var/funz              [offset -3]
* .
* .
* valore/addr ultima (n-esima) var/funz     [offset -(n+1)]
*
*
*
*
*
*
*
*
 */
public class CodeGenerationASTVisitor extends BaseASTVisitor<String, VoidException> {

  CodeGenerationASTVisitor() {}
  CodeGenerationASTVisitor(boolean debug) {super(false,debug);} //enables print for debugging

	@Override
	public String visitNode(ProgLetInNode n) {
		if (print) printNode(n);
		String declCode = null;
		for (Node dec : n.declist) declCode=nlJoin(declCode,visit(dec)); //visita che ci da la stringa delle dichiarazione del primo let
																		//globale. Visitiamo tutte le dichiarazione una dopo
																		//l'altra (in ordine). visitiamo il codice della prima var/fun e
																		//allochiamo spazio per questa, poi la seconda e così via, generando il
																		// codice per ognuna di queste.
																		// es: let
																		// 		var x: int = 1+5;
																		// 		var b: bool = true;
																		// diventa "push 5\n push 1\n ..."
		return nlJoin(
			"push 0",	//RA (return address) fittizio a 0 che non usiamo ma è per uniformare gli offset.
			declCode, // generate code for declarations (allocation)			
			visit(n.exp), // visitiamo il corpo globale del programma
			"halt",
			getCode()
		);
	}

	// caso semplice senza dichiarazione
	@Override
	public String visitNode(ProgNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.exp), // visito semplicemente l'intera espressione e poi blocco il processore.
			"halt"
		);
	}

	@Override
	public String visitNode(FunNode n) {
		if (print) printNode(n,n.id);
		String declCode = null, popDecl = null, popParl = null;
		for (Node dec : n.declist) {
			declCode = nlJoin(declCode,visit(dec));
			popDecl = nlJoin(popDecl,"pop");
		}
		for (int i=0;i<n.parlist.size();i++) popParl = nlJoin(popParl,"pop");
		String funl = freshFunLabel(); // mi da un'etichetta nuova per la funzione ogni volta che la chiamo
		// ATTENZIONE! Non devo ritornare il corpo della funzione! Ma devo metterlo in fondo prima di halt. Quindi me lo salvo
		// in una variabile in FOOLLib attraverso putCode() e poi con getCode() nella visita del progLetInNode lo metto in fondo.
		// io dovrò ritornare solo la dichiarazione della label della funzione, quindi "push nomefunz"
		putCode(
			nlJoin(
				funl+":", // etichetta/indirizzo del corpo della funzione
				// codice del corpo della funzione
				// quando viene chiamata una funzione è il momento giusto per settare fp perchè l'AL è proprio sullo
				// stack, in particolare è sulla cima dello stack e quindi è puntato dallo stack pointer sp
				// (si vedano i commenti in callNode). quindi settiamo fp a sp
				"cfp", // set $fp to $sp value
				// inoltre quando viene invocata una funzione cosa succede alla fine? Si salta con js e viene messo in return address
				// l'istruzione successiva. Quindi dobbiamo caricare il contenuto del registro RA per sapere dove tornare
				// dopo la fine dell'esecuzione della funzione.
				"lra", // load $ra value
				declCode, // generate code for local declarations (they use the new $fp!!!)
				visit(n.exp), // generate code for function body expression
				// qui abbiamo finito di creare l'AR. Perchè sono state messe in fondo le dichiarazioni scritte dentro
				// il corpo della funzione

				// ma dobbiamo "ripulire" la stanza. Ricordiamo l'invariante (stack inalterato) e quindi dobbiamo lasciare
				// in cima allo stack solo il risultato della chiamata/
				// per ripulire tutto in maniera safe ci salviamo nel registro temporaneo il risultato della funzione
				"stm", // set $tm to popped value (function result)
				// ripuliamo la casa, facciamo la pop per ogni dichiarazione locale
				popDecl, // remove local declarations from stack
				// poppo ra e lo salvo in ra
				"sra", // set $ra to popped value
				// poppo via l'AL
				"pop", // remove Access Link from stack
				// rimuovo tutti i parametri dallo stack (poppo via tutti)
				popParl, // remove parameters from stack
				// a questo punto dobbiamo, prima di aver ripulito completamente lo stack dall'AR, dobbiamo ripristinare
				// il controllo alla funzione chiamante tramite il Control Link. Ci serve a ripristinare il valore del fp
				// del chiamante. quindi useremo il valore che è rimasto sullo stack (il control link come abbiamo detto)
				// lo poppiamo e lo mettiamo in fp.
				"sfp", // set $fp to popped value (Control Link)

				// casa ripulita, lasciamo il regalo. Quindi rimettiamo il risultato di funzione. E ritorniamo al chiamante
				// mettendo sulla cima dello stack ra e poi saltiamo con js.
				"ltm", // load $tm value (function result)
				"lra", // load $ra value
				"js"  // jump to to popped address
			)
		);
		// ritorno solo l'indirizzo del corpo della funzione (alloca semplicemente l'indirizzo, non tutto il corpo, perchè
		// per il momento è salvato in una variabile a parte con putCode(). Una semplice push dell'indirizzo.
		return "push "+funl;		
	}

	// deve fare in modo di calcolare il valore
	@Override
	public String visitNode(VarNode n) {
		if (print) printNode(n,n.id);
		// con la visita calcolo il valore dell'espressione e ritorno solo l'allocazione del valore della variabile (es: push 5)
		return visit(n.exp);
	}

	@Override
	public String visitNode(PrintNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.exp), //eseguo l'espressione
			"print" // faccio il comando print che stampa il risultato dell'espressione lasciato sulla cima dello stack
					// (il valore dell'argomento print viene lasciata sulla cima dello stack).
					// ricordati che il comando print ha il side-effect di stampare ma ritorna quello che riceve in argomento!
					// es: print(5) + 7 dovrà stampare 5 ma ritornare anche 5, in modo tale da sommarlo a 7 e ritornare alla fine 12
		);
	}


	@Override
	public String visitNode(IfNode n) {
		if (print) printNode(n);
	 	String l1 = freshLabel();
	 	String l2 = freshLabel();		
		return nlJoin(
			visit(n.cond), //alla fine lascia sullo stack un booleano, quindi 0 o 1.
			"push 1",  //metto un 1
			"beq "+l1, //se è uguale allora visito il ramo then (perchè confronto il push della n.cond con quello pushato)
			visit(n.el), // se arrivo qui vuol dire che era false e quindi faccio il codice dell'else
			"b "+l2,  //salto al proseguimento di codice (senno come sotto farei anche il branch then...)
			l1+":",  // codice del ramo then
			visit(n.th), //visito then
			l2+":" // proseguo con il codice...
		);
	}

	// abbiamo una espressione a sx e una di dx e l'== che deve fare? guardare se sono uguali i valori ritornati dalle
	// due espressioni e alla fine nello stack lasciare 1 se sono uguali e 0 se non lo sono. Con la nostra StackVirtualMachine
	// come facciamo? Abbiamo delle istruzioni apposta: beq e b. Se sono uguali salto ad un qualcosa che metterà push 1,
	// se non sono uguali metto 0 e vado oltre. C'è un problema però: io dentro un espressione potrei avere un altro ==
	// generando label con lo stesso nome. Va bene? Ovviamente no, quindi dobbiamo trovare un metodo per generare in maniera
	// facile e "fresca" nomi di label sempre nuovi. Questi nomi vengono generati automaticamente da freshLabel() che ogni volta
	// assegnerà alle label nomi diversi.
	@Override
	public String visitNode(EqualNode n) {
		if (print) printNode(n);
	 	String l1 = freshLabel(); //genero nome label nuovo
	 	String l2 = freshLabel(); //genero nome label nuovo
		return nlJoin(				// esempio di funzionamento sotto
			visit(n.left), // push 5
			visit(n.right),     // push 4
			"beq "+l1,     // sono uguali? se si salto a l1
			"push 0",     // non sono uguali quindi metto 0
			"b "+l2,  //e faccio un salto incondizionato a l2 che mi fa proseguire il codice (sennò proseguirei e metterei 1 sullo stack..)
			l1+":",    // sono l1 e metto push 1
			"push 1", //pusho 1
			l2+":"  // continuo il mio codice..
		);
	}

	@Override
	public String visitNode(GreaterEqualNode n) {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		// right - left
		// se il risultato è <= di 0 allora vuol dire che left è >= a right
		return nlJoin(				// esempio di funzionamento sotto
				visit(n.right),
				visit(n.left),
				"sub",
				"push 0",
				"bleq "+l1,     // (right - left) è minore o uguale di 0? se si salto a l1
				"push 0",     // non sono uguali quindi metto 0
				"b "+l2,  //e faccio un salto incondizionato a l2 che mi fa proseguire il codice (sennò proseguirei e metterei 1 sullo stack..)
				l1+":",    // sono l1 e metto push 1
				"push 1", //pusho 1
				l2+":"  // continuo il mio codice..
		);
	}

	@Override
	public String visitNode(LessEqualNode n) {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(	// esempio di funzionamento sotto
				visit(n.left),
				visit(n.right),
				"bleq "+l1,     // left è minore o uguale di right? se si salto a l1
				"push 0",     // non sono uguali quindi metto 0
				"b "+l2,  //e faccio un salto incondizionato a l2 che mi fa proseguire il codice (sennò proseguirei e metterei 1 sullo stack..)
				l1+":",    // sono l1 e metto push 1
				"push 1", //pusho 1
				l2+":"  // continuo il mio codice..
		);
	}

	@Override
	public String visitNode(AndNode n) {
		if (print) printNode(n);
		String l1 = freshLabel(); //genero nome label nuovo
		String l2 = freshLabel(); //genero nome label nuovo
		// left + right
		// && ritorna 1 se sia left che right sono 1, quindi la somma deve essere 2
		return nlJoin(				// esempio di funzionamento sotto
				visit(n.left),
				visit(n.right),
				"add",
				"push 2",
				"beq "+l1,     // la somma di left e right fa 2? se si salto a l1
				"push 0",     // la somma è diversa da 2 (0 o 1) quindi pusho 0 (and non soddisfatto)
				"b "+l2,  //e faccio un salto incondizionato a l2 che mi fa proseguire il codice (sennò proseguirei e metterei 1 sullo stack..)
				l1+":",    // sono l1 e metto push 1
				"push 1", //la somma è 2 quindi pusho 1 (and soddisfatto)
				l2+":"  // continuo il mio codice..
		);
	}

	@Override
	public String visitNode(OrNode n) {
		if (print) printNode(n);
		String l1 = freshLabel(); //genero nome label nuovo
		String l2 = freshLabel(); //genero nome label nuovo
		// left + right
		// || ritorna 1 se almeno uno tra left e right è 1, quindi la somma deve essere 1 o 2, ma non 0
		// la somma tra left e right, essendo booleani, può essere solo 0,1,2 quindi basta controllare che non sia 0
		return nlJoin(				// esempio di funzionamento sotto
				visit(n.left),
				visit(n.right),
				"add",
				"push 0",
				"beq "+l1,     // la somma di left e right fa 0? se si salto a l1
				"push 1",     // la somma è diversa da 0 (1 o 2) quindi pusho 1 (or soddisfatto)
				"b "+l2,  //e faccio un salto incondizionato a l2 che mi fa proseguire il codice (sennò proseguirei e metterei 0 sullo stack..)
				l1+":",    // sono l1 e metto push 1
				"push 0", // la somma è 0 quindi pusho 0 (or non soddisfatto)
				l2+":"  // continuo il mio codice..
		);
	}

	@Override
	public String visitNode(NotNode n) {
		if (print) printNode(n);
		// exp può essere 0 o 1 (boolean type)
		// Facendo 1 - exp, otteniamo 0 se exp è 1, e 1 se exp è 0
		return nlJoin(				// esempio di funzionamento sotto
				"push 1",
				visit(n.exp),
				"sub"
		);
	}


	// ragionamento identico al PlusNode
	@Override
	public String visitNode(TimesNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.left),
			visit(n.right),
			"mult"
		);	
	}

	@Override
	public String visitNode(DivNode n) {
		if (print) printNode(n);
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"div"
		);
	}

	// il plusNode cos'è? l'espressione risultante della somma tra il figlio di sx e quello di dx. Entrambi i figli seguono, come
	// tutti, l'invariante. Quindi le cose da fare sono: valutare l'espressione del figlio di sx (che ritorna qualcosa),
	// valutare l'espressione del figlio di dx (che ritorna qualcos'altro) e poi sommare i due valori ritornati assieme, aggiungendo
	// il risultato finale alla cima dello stack e basta (non resta altro! sempre per rispettare l'invariante). Cioè
	// la stringa che ritorna una visita di un PlusNode sarebbe visit(n.left) + visit(n.right) + "add", questa è l'idea.
	// c'è però il problema dell'andata a capo. COme faccio a capire quando andare a capo per bene? Usiamo una nostra funzioncina
	// nlJoin() che mette a capo per bene gli argomenti che non sono null, ma li mette in mezzo non solo in fondo.
	@Override
	public String visitNode(PlusNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.left),
			visit(n.right),
			"add"				
		); //genera ad esempio "push 5\n push 9\n add\n"
	}

	@Override
	public String visitNode(MinusNode n) {
		if (print) printNode(n);
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"sub"
		);
	}

	// creiamo la prima parte del layout dell'AR della funzione (cioè dall'AL in sù perhè la parte prima è gestita in fase di dichiarazione
	// di funzione).
	@Override
	public String visitNode(CallNode n) {
		if (print) printNode(n,n.id);
		String argCode = null, getAR = null;
		// codice degli argomenti in ORDINE ROVESCIATO
		// devo anche visitarli ovviamente perchè potrebbero essere espressioni complicate
		for (int i=n.arglist.size()-1;i>=0;i--) argCode=nlJoin(argCode,visit(n.arglist.get(i)));
		// come faccio a sapere dove sta la dichiarazione di g? devo risalire la catena statica degli AL (come al solito)
		// il codice è lo stesso delle chiamate delle variabili.
		for (int i = 0;i<n.nl-n.entry.nl;i++) getAR=nlJoin(getAR,"lw");
		return nlJoin(
			"lfp", // load Control Link (pointer to frame of function "id" caller). Il control link a chi punta? a me, chiamante
				//della funzione. Quindi il valore del puntatore al mio frame (che è la posizione di riferimento)
			argCode, // generate code for argument expressions in reversed order
			"lfp", getAR, // retrieve address of frame containing "id" declaration by following the static chain (of Access Links)
				// a questo punto so dove si trova la mia funzione dichiarata. cosa manca? Io non devo solo saltare e via.
				// quindi? beh guardando il layout manca l'access link. Quindi prima di saltare devo pushare sullo stack
				// l'AL. Come alloco l'AL? L'AL deve puntare all'indirizzo dell'AR della dichiarazione della funzione (detto
				// 500 volte). Dove ce l'ho qua il frame dove è dichiarata la funzione? DENTRO getAR! Quindi basta lasciarlo
				// sullo stack (perchè poi lo dovremo anche sottrare all'offset dell'entry (n.entry.offset). Come faccio
				// a lasciarlo sullo stack e contemporaneamente usarlo? Ne faccio una copia. Come faccio a farne una copia?
				// uso il registro temporaneo.
            "stm", // set $tm to popped value (with the aim of duplicating top of stack). Setto tm al top dello stack
				// che è il fp di questo AR cioè l'AL che ci serve usare e lasciare sullo stack (che dovremo quindi duplicare).
            "ltm", // load Access Link (pointer to frame of function "id" declaration). Mi carica l'access Link
            "ltm", // duplicate top of stack. Questo mi duplica la cima dello stack. Così: uno mi verrà mangiatro nella somma
				// ma l'altro mi rimarrà sulla cima dello stack come AL (e anche fp), come si vede dal layout.
            "push "+n.entry.offset, "add", // compute address of "id" declaration. Sottraggo l'offset della funzione dichiarata
				// all'fp per trovare la sua dichiarazione (l'address) del corpo della fun.
			"lw", // load address of "id" function. carico l'indirizzo a cui saltare in cima allo stack.
            "js"  // jump to popped address (saving address of subsequent instruction in $ra). salto all'indirizzo indicato
				// alla cima dello stack. ha il side effect che mette nell'indirizzo RA l'indirizzo dell'istruzione successiva.
		);
	}

	@Override
	public String visitNode(IdNode n) {
		if (print) printNode(n,n.id);
		String getAR = null;
		// prendo la differenza di nesting level e faccio tante volte quanti sono gli scope di differenza "lw".
		// risalendo così di AR in AR
		for (int i = 0;i<n.nl-n.entry.nl;i++) getAR=nlJoin(getAR,"lw");
		return nlJoin(
			"lfp", //carico l'fp (che poi andremo a sottrare per trovare l'offset che corrisponde alla dichiarazione
						//della variabile che si sta usando).
				getAR, // retrieve address of frame containing "id" declaration
			              // by following the static chain (of Access Links). Qui avrò tanti lw così risalgo la catena statica
				         // tante volte quanto è la differenza tra questo posto dove uso la variabile e dove la dichiaro.
			"push "+n.entry.offset, "add", // compute address of "id" declaration. sommo (sottraggo in realtà)
				// quindi l'offset al fp per trovare dove si trova la dichiarazione della variabile usata e quindi il suo valore.
				//quindi ora sullo stack avremo l'indirizzo del valore della variabile da caricare
			"lw" // load value of "id" variable. Piglia l'indirizzo dallo stack e mette il valore. in pratica risalgo.
		);
	}


	@Override
	public String visitNode(BoolNode n) {
		if (print) printNode(n,n.val.toString());
		// ricordarsi che true=1 e false=0
		return "push "+(n.val?1:0);
	}

	// cosa dovrà tornare la foglia int? semplicemente pushiamo 5. Ricordi 5+9? push 5 push 9 add (che rispetta l'invariante!
	// ricordati che la code gen torna una mega stringa quindi torna una stringa anche la singola visita.
	@Override
	public String visitNode(IntNode n) {
		if (print) printNode(n,n.val.toString());
		return "push "+n.val;
	}
}