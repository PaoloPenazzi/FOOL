grammar SVM;

@parser::header {
import java.util.*;
}

//diventano campi nei lexer e parser nelle classi Java e lo possiamo usare nel codice del
//parser/lexer

@lexer::members {
public int lexicalErrors=0;
}

/*---------------------------------------------------------------------------------
 * l'array code è quello che conterrà il nostro codice oggetto vero e grezzo.
 * il parser parserà le istruzioni una alla volta e di volta in volta riempirà questo array
 * es: legge la prima istruzione e mette la prima istruzione tradotta nella prima posizione dell'array code che
 * rappresenta il codice oggetto. la variabile i quindi verrà aumenta ogni volta che traduciamo sequenzialmente
 * il nostro .asm.
 *
 * Ricordati che quando generiamo i file per SVM.g4 con ANTLR, di disattivare la generazione del parse tree listener e
 * visitor perchè ovviamente non ci serve creare un albero ma dobbiamo solo tradurre un testo, non eseguirlo.
 *
 *
 * labelRef è una struttura dati che associa ai nomi delle etichette a dove sono usate (posizione del "buco" di codice
 * nell'array code). In realtà alla fine è più comodo fare il contrario dalla posizione (integer) alla stringa, nome
 * della etrichetta.
 *
 * labelDef è una struttura dati che associa ad ogni etichetta l'indirizzo di definizione
 *
 * QUESTO E' L'ASSEMBLATOREEEEEEEEEEE
 *------------------------------------------------------------------------------------*/
@parser::members {
public int[] code = new int[ExecuteVM.CODESIZE];
private int i = 0;


private Map<String,Integer> labelDef = new HashMap<>();
private Map<Integer,String> labelRef = new HashMap<>();
}

/*------------------------------------------------------------------
 *              compilazione                       assembly
 *  quicksort.fool --------> quicksort.fool.asm    ----------> codice oggetto
 *                           (assembler della SVM)                    |
 *                                                                    |
 *                                                                    V
 *                                                             Stack Virtual Machine
 *
 * Noi dato un file .fool in ingresso lo compileremo e ne trarremo un file .asm
 * questo file avrà prima tutte le righe del main (fatte da push, pop, ecc.) e sotto
 * le label delle funzioni e chiamate a funzione.
 *
 * Ruolo dell'assemblatore: convertire .asm in codice oggetto. Sarà poi la VM che riceve il codice
 * oggetto e quindi invece di ricevere per esempio add riceve il numero corrispondente ad add.
 * Associeremo ad ogni istruzione un numero (es: add identificato dal numero 72. Quando
 * la VM vede il numero 72 fà la somma). ATTENZIONE: dove vengono dichiarate queste costanti per
 * per ogni istruzione? Non lo facciamo noi ma ANTLR in automatico! QUando generiamo la grammatica con tasto destro -->
 * "Generate ..." nel file SVMParser.java (nel caso della grammatica generale in FOOLParser.java) troveremo tutte le
 * costanti associate ad ogni istruzione!
 *
 * es: (esempio banale, prime righe (fino a push 10) abbiamo il main e sotto
 *      le funzioni con le label (l1, la loro chiamata) e il corpo di esse (print, push 0, ecc.)
 *      push 0
 *      push 1
 *      push 2
 *      push 3
 *      push 4
 *      push 5
 *      push 6
 *      push 7
 *      push 8
 *      push 9
 *      push 10
 *
 *  l1: print
 *      push 0
 *      bleq l2
 *      b l1
 *
 *  l2: halt
 *
 * Il fatto di avere ANTLR ci aiuta un botto. Perchè in pratica sto file di testo .asm
 * va trasformato in codice oggetto e quindi che bisogna fargli? Usiamo ANLTR perchè facciamo
 * parsing di sto cavolo di file di testo. Quindi anche in questo caso dovremo fare un'analisi lessicale
 * e un'analisi sintattica. A livello di sintassi il .asm è banale. Nel lessico
 * ci sarà ad esempio il token dei numeri interi (push 59), un token per ciascuna istruzione
 * (un token per la push). La struttura sintattica è piatta, è banale non come in C o Python
 * ogni tanto potremmo avere tra ste istruzioni delle label ma per il resto
 * è banale è piatta. A livello sinattico nelle label avremo nome : istruzione..
 *
 * Nel lexer avremo un token per ogni parola, stringa che si troverà nel .asm
 *      STRINGA --> TOKEN
 *   (es: pop ----> POP)
 * E poi che succede? come per il file FOOL.g4 anche qui avremo un parser che, dato
 * in input una serie di token ci dirà se va bene o meno e genererà qualcosa.
 *
 *
 *------------------------------------------------------------------*/


/*------------------------------------------------------------------
 * PARSER RULES
 *
 *
 * anche il parser è roba già vista. Variabile iniziale è sempre la prima che appare.
 * prima l'avevamo chiamato prog ora assembly.
 *
 * avremo un numero minimo di registri.
 *
 * le etichette saranno tradotte in indirizzi. per noi gli indirizzi saranno
 * saranno salvati in una memoria (array) e quindi per noi gli indirizzi saranno
 * posizioni in questo array di interi. Quello che c'è dentro all'array in posizione n sarà quello che stiamo eseguendo.
 * es: se in posizione 5 c'è 72 che vuol dire add, quando l'array raggiunge posizione 5
 * farà una add.
 *
 *
 *
 *------------------------------------------------------------------*/

// riempiamo i buchi --> il codice Java viene eseguito dopo aver letto la produzione, quindi dopo aver visto tutte le istruzioni
// ed essere arrivato a EOF allora riempo i buchi!
assembly: instruction* EOF 	{ for (Integer j: labelRef.keySet()) 
								code[j]=labelDef.get(labelRef.get(j)); 
							} ;

instruction : 
        PUSH n=INTEGER   {code[i++] = PUSH; 
			              code[i++] = Integer.parseInt($n.text);} //push INTEGER on the stack, ricordati che stiamo
			              // mettendo in code[i++] la costante numerica che corrisponde a PUSH (definita in SVMParser.java). Il processore
			              // capisce in automatico che, dato che c'è una push, deve prendere anche il pezzo di codice dopo che sarà
			              // un numero intero.
	  | PUSH l=LABEL    {code[i++] = PUSH;
	    		         labelRef.put(i++,$l.text);}  //push the location address pointed by LABEL on the stack, metto
	    		             //nello stack un'etichetta. Che nel codice oggetto diventa l'indirizzo!
	    		             //qunado vediamo "salta a pippo" nel codice oggetto divetnerà l'indirizzo della memoria
	    		             // dell'istruzione successiva a "pippo" quindi chi salta andrà all'indirizzo specifico.
	    		             //ma perchè mettiamo un indirizzo nello stack? percheè dopo aver pushato l'indirizzo
	    		             //qualcuno lo popperà e ci salterà
	    		             // lui lascia un buco che significa che lì utilizza/chiama una label
	  | POP		    {code[i++] = POP;}	//pop the top of the stack
	  | ADD		    {code[i++] = ADD;} //replace the two values on top of the stack with their sum
	  | SUB		    {code[i++] = SUB;} //pop the two values v1 and v2 (respectively) and push v2-v1
	  | MULT	    {code[i++] = MULT;} //replace the two values on top of the stack with their product
	  | DIV		    {code[i++] = DIV;} //pop the two values v1 and v2 (respectively) and push v2/v1
	  | STOREW	  {code[i++] = STOREW;} //pop two values:
                                        //the second one is written at the memory address pointed by the first one
	  | LOADW           {code[i++] = LOADW;} //read the content of the memory cell pointed by the top of the stack
	                                         //and replace the top of the stack with such value
	  | l=LABEL COL     {labelDef.put($l.text,i);} //LABEL points at the location of the subsequent instruction
	                                               //defionizione di label quindi aggiorniamo la mappa labelDef
	                                               // e la posizione in cui siamo ma stavolta (unica volta) non dobbiamo
	                                               // post incrementare nè perchè dobbiamo lasciare un buco nè perchè
	                                               // dobbiamo inserire del codice nell'array code. RICORDATI CHE IL
	                                               // LABEL: NON DIVENTA CODICE OGGETTO, SERVE SOLO A DIRE CHE QUELLA
	                                               // FUNZIONE SI TROVA A QUELL'INDIRIZZO LI' E BASTA. SERVE PER SOSTITUIRE
	                                               // IN UN SECONDO MOMENTO A SOSTITUIRE LA LABEL CON IL SUO INDIRIZZO.
	                                               // Alla fine, dopo aver parsato tutto il .asm, dovremmo riguardare tutto
	                                               // il codice oggetto generato e dove c'è un buco andargli a mettere
	                                               // l'indirizzo della label corrispondente. In pratica confronto le due mappe.
	  | BRANCH l=LABEL  {code[i++] = BRANCH;
                       labelRef.put(i++,$l.text);} //jump at the instruction pointed by LABEL
	  | BRANCHEQ l=LABEL {code[i++] = BRANCHEQ;
                        labelRef.put(i++,$l.text);} //pop two values and jump if they are equal
	  | BRANCHLESSEQ l=LABEL {code[i++] = BRANCHLESSEQ;
                          labelRef.put(i++,$l.text);} //pop two values and jump if the second one is less or equal to the first one
	  | JS              {code[i++] = JS;}		     //pop one value from the stack:
                                                     //copy the instruction pointer in the RA register and jump to the popped value
	  | LOADRA          {code[i++] = LOADRA;}    //push in the stack the content of the RA register
	  | STORERA         {code[i++] = STORERA;}   //pop the top of the stack and copy it in the RA register
	  | LOADTM          {code[i++] = LOADTM;}   //push in the stack the content of the TM register. TM è Temporary Memory, un registro
	                                            //temporaneo.
	  | STORETM         {code[i++] = STORETM;}   //pop the top of the stack and copy it in the TM register
	  | LOADFP          {code[i++] = LOADFP;}   //push in the stack the content of the FP register
	  | STOREFP         {code[i++] = STOREFP;}   //pop the top of the stack and copy it in the FP register
	  | COPYFP          {code[i++] = COPYFP;}   //copy in the FP register the currest stack pointer
	  | LOADHP          {code[i++] = LOADHP;}   //push in the stack the content of the HP register
	  | STOREHP         {code[i++] = STOREHP;}   //pop the top of the stack and copy it in the HP register
	  | PRINT           {code[i++] = PRINT;} //visualize the top of the stack without removing it
	  | HALT            {code[i++] = HALT;} //terminate the execution
	  ;
	  
/*------------------------------------------------------------------
 * LEXER RULES
 *
 * dopo il lexer avrò una sequenza di token per il parser --> già visto
 *------------------------------------------------------------------*/

PUSH	 : 'push' ; 	
POP	 : 'pop' ; 	
ADD	 : 'add' ;  	
SUB	 : 'sub' ;	
MULT	 : 'mult' ;  	
DIV	 : 'div' ;	
STOREW	 : 'sw' ; 	
LOADW	 : 'lw' ;	
BRANCH	 : 'b' ;	
BRANCHEQ : 'beq' ;	
BRANCHLESSEQ:'bleq' ;	
JS	 : 'js' ;	
LOADRA	 : 'lra' ;	
STORERA  : 'sra' ;	 
LOADTM	 : 'ltm' ;	
STORETM  : 'stm' ;	
LOADFP	 : 'lfp' ;	
STOREFP	 : 'sfp' ;	
COPYFP   : 'cfp' ;      
LOADHP	 : 'lhp' ;	
STOREHP	 : 'shp' ;	
PRINT	 : 'print' ;	
HALT	 : 'halt' ;	
 
COL	 : ':' ;
LABEL	 : ('a'..'z'|'A'..'Z')('a'..'z' | 'A'..'Z' | '0'..'9')* ;
INTEGER	 : '0' | ('-')?(('1'..'9')('0'..'9')*) ;

COMMENT : '/*' .*? '*/' -> channel(HIDDEN) ;

WHITESP  : (' '|'\t'|'\n'|'\r')+ -> channel(HIDDEN) ;

ERR	     : . { System.out.println("Invalid char: "+getText()+" at line "+getLine()); lexicalErrors++; } -> channel(HIDDEN); 

