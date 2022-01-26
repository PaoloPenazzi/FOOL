package svm;
public class ExecuteVM {

    /*
     * File che fa l'effettiva esecuzione della VM. In pratica simuliamo un processore reale.
     *
     *
     *
     *
     *
     */
    
    public static final int CODESIZE = 10000;
    public static final int MEMSIZE = 10000;

    // area di memoria del codice oggetto
    private int[] code;
    // area di memoria dove mettiamo le strutture dati da usare durante l'esecuzione
    // nei programmi Java cosa c'è in memory? Lo Heap (dove vengono memorizzate gli oggetti, area libera, un po come la
    // malloc in C: tutte le volte che serve memoria per un oggetto si trova un buco lì) e lo Stack (per le variabili, serve
    // per la ricorsione!). Nel multi-thread ho strutture dati per ogni thread.
    private int[] memory = new int[MEMSIZE];
    
    private int ip = 0; // istruction pointer, che ci serve per accedere all'array code. Questo, ovviamente, non andrà
    //sequenzialmente, quando c'è un salto ad esempio salterà all'indirizzo della istruzione. Nel ciclo fetch-execuite delle istruzioni
    // dei processori. Fetch --> recupero le istruzioni all'indirizzo ed Execute --> la eseuge, sempre così.
    //
    private int sp = MEMSIZE; //punta al top dello stack. Lo stack viene sempre messo in alto e quindi quando lo stack è
    // vuoto questo punta all'indirizzo più alto esistente della memoria, quando invece facciamo una push lo stack pointer
    // decrementa. Mentre lo heap parte dal basso e quando la memoria finisce si incontrano. Quando lo heap pointer e lo
    // stack pointer si incontrano la memoria è esaurita. Sia fp che sp puntano alla base dello stack.
    
    private int hp = 0; // (heap pointer)
    private int fp = MEMSIZE; // (frame pointer)
    private int ra; // (return address)
    private int tm; // (temporary storage)

    // quando facciamo javac e java da riga di comando che succede? Questo! Creiamo una VM a cui passiamo i file java ..
    // qui il codice oggetto viene direttamente caricato da costruttore e non dal file.
    public ExecuteVM(int[] code) {
      this.code = code;
    }

    // in pratica questo è il famoso ciclo fetch-execute!
    public void cpu() {
      while ( true ) {
        int bytecode = code[ip++]; // fetch. Post-incremento ip perhcè nel caso normale vado avanti con l'istruzione.
        int v1,v2;
        int address;
        switch ( bytecode ) { //execute.
          case SVMParser.PUSH:
            // noi, per come abbiamo definito le cose in SVM.g4 sappiamo che sotto la push ci sarà il numero che dobbiamo pushare.
            // quindi AVANZIAMO di una istruzione, prendiamo il numero che si trova nella cella di code a indirizzo ip++
            // e pushiamo il numero (ovvero ci mettiamo in memoria (nella struttura dati array memory[]) il numero)
            push( code[ip++] );
            break;
          case SVMParser.POP:
            // butta via letteralmente quello che c'è nel top dello stack
            // in teoria potremmo salvarci anche il numero di ritorno (come nel caso della add)
            // ma in questa istruzione POP non ci interessa.
            pop();
            break;
          case SVMParser.ADD :
            v1=pop();
            v2=pop();
            // perche push v2 + v1? invece di v1 + v2?
            // perchè se ho un codice tipo
            // push 5
            // push 7
            // sub
            // cosa voglio che faccia? 5 -7! Quindi prima poppa il 7 (v2) e poi il 5 (v1)
            push(v2 + v1);
            break;
          case SVMParser.MULT :
            v1=pop();
            v2=pop();
            push(v2 * v1);
            break;
          case SVMParser.DIV :
            v1=pop();
            v2=pop();
            push(v2 / v1); //in caso di divisone per 0 caccia eccezione Java (la VM di Java).
            break;
          case SVMParser.SUB :
            v1=pop();
            v2=pop();
            push(v2 - v1);
            break;
          case SVMParser.STOREW : //
            address = pop();
            memory[address] = pop();    
            break;
          case SVMParser.LOADW : //
            push(memory[pop()]);
            break;
          case SVMParser.BRANCH :
            // lui inizialmente trova branch poi, dato che all'inizio post-incrementiamo, avremo ora in code[ip] l'indirizzo di
            // memoria del codice che dovrà essere eseguito. A questo punto noi saltiamo mettendo in ip (ovvero la prossima
            // istruzione) l'indirizzo del codice da eseguire.
            address = code[ip];
            ip = address; //salto!
            break;
          case SVMParser.BRANCHEQ :
            address = code[ip++];
            v1=pop();
            v2=pop();
            if (v2 == v1) ip = address;
            break;
          case SVMParser.BRANCHLESSEQ :
            address = code[ip++];
            v1=pop();
            v2=pop();
            if (v2 <= v1) ip = address;
            break;
          case SVMParser.JS : //
            address = pop();
            ra = ip;
            ip = address;
            break;
         case SVMParser.STORERA : //
            ra=pop();
            break;
         case SVMParser.LOADRA : //
            push(ra);
            break;
         case SVMParser.STORETM : 
            tm=pop();
            break;
         case SVMParser.LOADTM : 
            push(tm);
            break;
         case SVMParser.LOADFP : //
            push(fp);
            break;
         case SVMParser.STOREFP : //
            fp=pop();
            break;
         case SVMParser.COPYFP : //
            fp=sp;
            break;
         case SVMParser.STOREHP : //
            hp=pop();
            break;
         case SVMParser.LOADHP : //
            push(hp);
            break;
         case SVMParser.PRINT :
            System.out.println((sp<MEMSIZE)?memory[sp]:"Empty stack!");
            break;
         case SVMParser.HALT :
            return;
        }
      }
    } 

    // ragionamento analogo ma post incremento perchè cosi sp punta sempre al top! Ricordati che lo stack cresce vero il basso
    // quindi dovrò aumentare.
    private int pop() {
      return memory[sp++];
    }

    // se devo fare la push di un valore sullo stack che faccio? pre-decremento lo stackpointer e poi prendo memory della posizione
    // pre-decrementata e ci metto v. In questa maniera quando lo stack è vuoto sp punta a memsize. Cosa succede se faccio il push di 5?
    // che in memory di memsize-1 ci mette 5. E si vede quindi che lo sp punta a memsize-1. Quindi sp punta sempre al top
    // dello stack così! Se voglio leggere il top dello stack quindi dovrò fare banalmente memory[sp]!
    private void push(int v) {
      memory[--sp] = v;
    }
    
}