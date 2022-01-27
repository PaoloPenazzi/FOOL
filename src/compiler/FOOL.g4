grammar FOOL;
 
@lexer::members {
public int lexicalErrors=0;
}
   
/*------------------------------------------------------------------
 * PARSER RULES
 *
 * Il parser riceve lo stream di token, dal lexer, in input. E' corretto questo stream?
 * Sì o No? Mentre capisco se una stringa è accettabile o meno genero un
 * ALBERO SINTATTICO (un albero che nelle foglie ha simboli terminali e in ogni nodo interno ho una produzione)
 * --> PARSE TREE = SYNTAX TREE.
 * in particolare genererà un AST (Abstract Syntax Tree). Il Parser
 * considera prima la creazione di un parse tree (Syntax Tree) poi crea il AST traducendo il parse tree
 * PRIORITA'
 * ordine in cui sono scritte le produzioni di una variabile:
 * la prima produzione ha la priorità più alta
 *
 * ASSOCIATIVITA'
 * per ogni produzione (operatore) si ha associatività:
 *  - a destra, se si specifica <assoc=right> prima del corpo della produzione;
 *  - a sinistra, se non si specifica nulla (associatività a sinistra è default)
 *
 * Parse Tree: contiene i token inclusi quelli che servono solo per scoprire l'ordine
 * delle cose, il nesting delle cose. Si dice che mostri la SINTASSI CONCRETA
 *
 * Abstract Syntax Tree: prendo il ST e ne elimina le gerarchie. Si dice che mostri
 * la SINTASSI ASTRATTA.
 *
 * Ricordati che il Parser fa anche syntax checking (La sintassi di un linguaggio di programmazione è una raccolta
 * di regole per specificare la struttura o la forma del codice, mentre la
 * semantica si riferisce all'interpretazione del codice o al significato
 * associato dei simboli, dei caratteri o di qualsiasi parte di un programma.).
 *
 * Le regole grammaticali del parser genereranno un Parse Tree, l'AST dovremo farlo noi!
 *
 * Il Parser usa un automa a pila per il confronto delle cose (es: controllare
 * che tutte le parentesi siano aperte e chiuse).
 *
 * In ANTLR4, inoltre, è possibile dare un nome a ciascuna produzione di una
 * variabile (es. "exp") tramite un tag #nome.
 *
 * Sotto, la grammatica del Parser.
 *
 * SIGNIFICATO DI LET IN:
 *
 * Per semplicità abbiamo una parte con le dichiarazioni (var e fun) e un'altra con il corpo del blocco con le dichiarazioni
 * ATTENZIONE: posso avere blocchi annidati.
 *
 *
 *
 *
 *  Dichiarazioni come nel linguaggio funzionale ML (Meta Language)
 *
 *   -------------
 *
 *   {
 *   int x = 5;
 *   int y = 6;
 *
 *   codice senza dichiarazioni
 *   }
 *
 *   ------------------
 *
 *
 *   let
 *
 *   int x = 5; //notare che nei linguaggi funzionali non ho assegnamenti se non quando la dichiaro (in java posso fare (y=5)+9, qua no).
 *   int y = 6;
 *
 *   in
 *
 *   codice senza dichiarazioni
 *
 *   ;
 *
 *  Adesso abbiamo questa variabile progbody che può essere o direttamente exp SEMIC (come è sempre stato nei casi base)
 *  quindi solamente il corpo del programma senza dichiarazioni di nessun tipo oppure abbiamo LET dec+ IN exp SEMIC che è
 *  il programma con la dichiarazione globale di let (con dentro dichiarazioni globali di fun e var) e poi seguita da IN
 *  che sarà il corpo della funzione. La dichiarazione di variabile è VAR ID COLON type ASS exp SEMIC
 *        VAR ID SEMIC type  ASS   exp  SEMIC
 *   (es: var y    :   int   =     5+2    ;   )
 *  Type può essere intero o boleano.
 *
 *  La dichiarazione di funzione è invece
 *    FUN ID COLON type LPAR (ID COLON type (COMMA ID COLON type)* )? RPAR (LET dec+           IN)? exp   SEMIC
 *    fun f   :    bool (     n   :     int    ,   m   :    int         )
 *                                                                            let
 *                                                                             var x:int = m;
 *                                                                                              in g(3==5)  ;
 *
 *  le cose da tenere presente nella creazione e gestione della Symbol Table son due: dichiarazione degli identificatori
 *  uso di una funzione identificata da un id e uso degli id. Quando dichiaro un id metto nella symbol table una pallina
 *  associata al nome che ho dichiarato.
 *
 *
 *------------------------------------------------------------------*/
  
prog  : progbody EOF ;

progbody : LET ( cldec+ dec* | dec+ ) IN exp SEMIC #letInProg   //semic è il ;
         | exp SEMIC                               #noDecProg
         ;

cldec  : CLASS ID (EXTENDS ID)?
              LPAR (ID COLON type (COMMA ID COLON type)* )? RPAR
              CLPAR
                   methdec*
              CRPAR ;

methdec : FUN ID COLON type
              LPAR (ID COLON type (COMMA ID COLON type)* )? RPAR
                   (LET dec+ IN)? exp
              SEMIC ;

dec : VAR ID COLON type ASS exp SEMIC #vardec
    | FUN ID COLON type
          LPAR (ID COLON type (COMMA ID COLON type)* )? RPAR // perchè non basta una stella di klenee? Perchè se ho
                                                            // un argomento non serve la virgola dopo l'argomento se ne
                                                            // ho più di uno sì. Posso anche non avere parametri.
               (LET dec+ IN)? exp
          SEMIC #fundec
    ;


        // come facciamo a mettere allo stesso livello di priorità due simboli su ANTLR ? Ad esempio se volessimo avere in una
        // produzione o TIMES o DIV perchè sono allo stesso livello metto (prendendo la produzione sotto):
        // exp (TIMES | DIV) exp. Nel visitor cosa dobbiamo considerare? due casi uno che sia TIMES o DIV. Dobbiamo fare una
        // if per gestire i due casi (nel caso del mult, moltiplico i figli che visito oppure divido).

        // nelle produzioni di dichiarazione noi genereremo un nodo nullo anche se ci sarà un errore sintattico perchè inizialmente matcha
        // con qualcosa (ad esempio nella dichiarazione di una variabile all'inizio matcha con VAR ma poi potrebbe mancare l'id).
        // Mentre qua invece non genera nulla nel caso di un errore sintattico, nemmeno il nodo vuoto. Perchè? Perchè qui se manca,
        // ad esempio nella prima, TIMES lui non riesce proprio a matchare la produzione e quindi il problema di dover generare
        // un nodo nullo per individuare un errore sintattico non c'è proprio. Perchè queste produzioni exp sono ricorsive a sinistra
        // ma dentro vengono tradotte come ricorsive a destra. Quindi TIMES è come se fosse il primo simbolo e quindi essendo il primo token
        // lui non matcha con nulla. Sotto è scritto meglio |
        //                                                  |
        //                                                 \/
        //ATTENZIONE: gli errori sintattici fanno si' che ANTLR possa fare un match
        //parziale delle produzioni: in questo caso i figli successivi all'ultimo figlio
        //che fa match sono "null", mentre quelli precedenti sono non-"null".
        //Alcuni token quindi sono sicuramente non-"null": es. il primo token in
        //produzioni che cominciano con un token oppure, ad es., il token PLUS in #plus
        //(la ricorsione a sinistra trasformata internamente in destra lo rende primo
        //token nel ciclo interno in cui sono matchate le produzioni #plus).
exp     : exp (TIMES | DIV) exp #timesDiv
        | exp (PLUS | MINUS) exp #plusMinus
        | exp (EQ | GE | LE) exp #comp
        | exp (AND | OR) exp #andOr
        | NOT exp #not
        | LPAR exp RPAR #pars
    	| MINUS? NUM #integer // minus non ci deve sempre essere, quindi metto il ?, così gestisco il caso del - dell'operatore
    	                      // seguito da un altro - (es: 8--7, il primo - è dell'operazione, il secondo del numero)
    	                      // poi dovrò modificare il codice del visitor, perchè ora nella foglia dovrò gestire il caso
    	                      // in cui c'è un meno davanti al numero, perchè devo invertire il segno del numero. Se c'è il token
    	                      // minus nella visita della produzione allora dovrò invertire il segno. Per capire meglio, nel caso,
    	                      // riguardare lezione 11-02 (gli ultimi 40 minuti).
	    | TRUE #true     
	    | FALSE #false
	    | NULL #null
        | NEW ID LPAR (exp (COMMA exp)* )? RPAR #new
	    | IF exp THEN CLPAR exp CRPAR ELSE CLPAR exp CRPAR  #if   //CLPAR = curly left paraenthesis
	    | PRINT LPAR exp RPAR #print
	    | ID #id  //uso di una variabile identificata da un id
	    | ID LPAR (exp (COMMA exp)* )? RPAR #call //uso di una funzione identificata da un id, che potrebbe non avere parametri
	    | ID DOT ID LPAR (exp (COMMA exp)* )? RPAR #dotCall
        ;
             
type    : INT #intType
        | BOOL #boolType
        | ID #idType
 	    ;  
 	  		  
/*------------------------------------------------------------------
 * LEXER RULES
 *
 * obiettivo del lexer trovare lessemi e mapparli in token
 *
 * sono in ordine di priorità,da quello a priorità più
 * alta a quello a prorità più bassa. Quindi se nessun lessema matcha un token
 * allora prenderà il token errore.
 *
 * TOKEN : LESSEMA
 * i token WHITESP, COMMENT ed ERR non devono essere inviati al parser;
 * ed il token ERR corrisponde ad una condizione di errore (gli errori devono
 * essere contati e segnalati all'utente).
 *
 * Nel realizzare la specifica per il lexer, fare attenzione alla regola di
 * maximal-match (lessema applicato al token che più lungo si associa, es: "iffy" non
 * sarà di token IF ma di tipo ID),
 * che viene applicata per default da ANTLR.
 *
 * channel(hidden) il token verrà inviato sul canale hidden e non considerato
 * viene proprio skippato
 *
 * Lexer fatto da due parti: dichiarativo (descrive ciascun token come espressione
 * regolare/automa) e imperativa (mischia assieme tutte queste expr. reg, che fa
 * ANTLR).
 *
 * Ricordati che il lexer farà look-ahead.
 *------------------------------------------------------------------*/

PLUS  	: '+' ;
MINUS   : '-' ; //
TIMES   : '*' ;
DIV 	: '/' ; //
LPAR	: '(' ;
RPAR	: ')' ;
CLPAR	: '{' ;
CRPAR	: '}' ;
SEMIC 	: ';' ;
COLON   : ':' ;
COMMA	: ',' ;
DOT	    : '.' ;
OR	    : '||'; //
AND	    : '&&'; //
NOT	    : '!' ; //
GE	    : '>=' ; //
LE	    : '<=' ; //
EQ	    : '==' ;
ASS	    : '=' ;
TRUE	: 'true' ;
FALSE	: 'false' ;
IF	    : 'if' ;
THEN	: 'then';
ELSE	: 'else' ;
PRINT	: 'print' ;
LET     : 'let' ;
IN      : 'in' ;
VAR     : 'var' ;
FUN	    : 'fun' ;
CLASS	: 'class' ;
EXTENDS : 'extends' ;
NEW 	: 'new' ;
NULL    : 'null' ;
INT	    : 'int' ;
BOOL	: 'bool' ;
NUM     : '0' | ('1'..'9')('0'..'9')* ;

ID  	: ('a'..'z'|'A'..'Z')('a'..'z' | 'A'..'Z' | '0'..'9')* ;


WHITESP  : ( '\t' | ' ' | '\r' | '\n' )+    -> channel(HIDDEN) ;

COMMENT : '/*' .*? '*/' -> channel(HIDDEN) ;

ERR   	 : . { System.out.println("Invalid char: "+ getText() +" at line "+getLine()); lexicalErrors++; } -> channel(HIDDEN);

