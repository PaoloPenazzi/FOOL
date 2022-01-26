package compiler.lib;


/*
*
* Il nostro visitor del type checking torna un typeNode. Questa classe è importante perchè è una via di mezzo tra i vari
* nodi ed è comoda per identificare i TIPI dei nodi.
*
* introdotta la classe abstract TypeNode extends Node per i tipi: TypeNode è ora classe genitore di IntTypeNode e
* BoolTypeNode, e i campi type (il tipo dei parametri/variabili) di ParNode/VarNode e retType (il tipo di ritorno della
* funzione) di FunNode sono ora dei TypeNode.
*
* Estendiamo le informazioni, prese dalla dichiarazione degli identificatori, contenute nelle symbol table entry
* (classe STentry) introducendo il tipo:
* - un tipo BoolTypeNode oppure IntTypeNode per le variabili o per i parametri
* - un tipo funzionale ArrowTypeNode (extends TypeNode) per le funzioni.
*
 */
public abstract class TypeNode extends Node {

}
