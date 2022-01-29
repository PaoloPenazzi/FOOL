package compiler;

import java.lang.reflect.Field;
import java.util.*;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import compiler.AST.*;
import compiler.FOOLParser.*;
import compiler.lib.*;
import static compiler.lib.FOOLlib.*;

// In FOOLBaseVisitor ho messo Node quindi ritornerò sempre dei Node quando visito
/*
* Nell'AST, a differenza del ST, nei nodi interni abbiamo simboli terminali e non variabili. Oltre che non abbiamo quello
* zucchero sintattico che ci serve nella costruzione del ST (parentesi, punti e virgole ecc.). L'idea è semplificare l'albero al massimo
*
* Questa classe quindi cosa fa? Visito l'ST per generare l'AST!
*
* Nei linguaggi funzionali tutto è un espressione (if-then-else funzionale in Java è quello con il ? e torna qualcosa, il
* if-then-else non funzionale in Java è il classico con gli statement). Qua tutto è un espressione quindi, ad esempio,
* negli if-then-else torna sempre il valore di then o else.
*
* Estende la classe di base FOOLBaseVisitor che mi da il SYntax Tree e la parametrizziamo con Node
*
* Gestiamo i Syntax Tree incompleti tornando null quando si effettua una qualsiasi visit con argomento null
* nell'ASTGenerationSTVisitor (questo risolve il problema della gestione di ST con errori ma causa
* la generazione di AST incompleti)
 */
public class ASTGenerationSTVisitor extends FOOLBaseVisitor<Node> {

	String indent;
    public boolean print;
	
    ASTGenerationSTVisitor() {}    
    ASTGenerationSTVisitor(boolean debug) { print=debug; }

	// stampa automatizzata con la Reflcetion che praticamente va a pigliare la variabile e poi non gli associa tutta
	// la produzione completa ma solo il nome della produzione. Come fa a capire tutte le info? dalla classe madre
	// ParserRuleContext che genera il *Context (progContext, letInContext, ecc.).
    private void printVarAndProdName(ParserRuleContext ctx) {
        String prefix="";        
    	Class<?> ctxClass=ctx.getClass(), parentClass=ctxClass.getSuperclass();
        if (!parentClass.equals(ParserRuleContext.class)) // parentClass is the var context (and not ctxClass itself)
        	prefix=lowerizeFirstChar(extractCtxName(parentClass.getName()))+": production #";
    	System.out.println(indent+prefix+lowerizeFirstChar(extractCtxName(ctxClass.getName()))); //stampa fatta per il ProgContext.
	//extractCtxName funzione fatta da noi per levare la parte "Context" dal nome
    }
        
    @Override
	public Node visit(ParseTree t) {
		// Intercettiamo la chiamata al visit per fare l'indentazione
		// aumento un livello di indentazione
		//se ci sono errori sintattici le visite potrebbero essere chiamate con parametri null. Questo non deve mandare in
		// pappa il compilatore, ovvero non deve generare nullPointerExc. Per eviatre sta cosa controlliamo che non sia null
		// e quindi ritorna null. Però in pratica sposto il problema: non vado in eccezione quando c'è un errore sintattico
		// e manca qualcosa ma però mi genero un ast-incompoleto. Quindi quando andrò a viistare l'ast allora vedrò dei figli
		// null. Quindi dovrò fare un controllo simile anche nel BaseASTVisitor, ovvero la base di tutti i visitatori dell'ast.
		// sul visit di quella classe dovrò controllare che t non sia null.
    	if (t==null) return null;
        String temp=indent;
        indent=(indent==null)?"":indent+"  ";
		// chiamo il metodo visit giusto e andrà a fare l'indagine e chiamerà il giusto
		// metodo di visita
        Node result = super.visit(t);
		// rimuovo il livello di indentazione
		// ripristino il livello di indentazione a quello che era prima della chiamata
		// al sotto albero
        indent=temp;
        return result; 
	}

	@Override
	public Node visitProg(ProgContext c) {
		if (print) printVarAndProdName(c);
		return visit(c.progbody());
	}


	@Override
	public Node visitLetInProg(LetInProgContext c) {
		if (print) printVarAndProdName(c);

		// visito i figli dichiarati, itero i vari dec (come al solito, si capisce tutto dalla produzione che genera il tree
		// in fool.g4, infatti LET dec+ IN exp SEMIC, presenta dec+ quindi almeno un figlio ma ne posso avere n. Quindi itero
		// su tutti
		List<DecNode> declist = new ArrayList<>();
		for (DecContext dec : c.dec()) declist.add((DecNode) visit(dec));

		return new ProgLetInNode(declist, visit(c.exp()));
	}

	// classico caso in cui ho solo il corpo, non ho let/in globale quindi faccio solo la visita del corpo
	@Override
	public Node visitNoDecProg(NoDecProgContext c) {
		if (print) printVarAndProdName(c);
		return new ProgNode(visit(c.exp()));
	}

	@Override
	public Node visitTimesDiv(TimesDivContext c) {
		if (print) printVarAndProdName(c);
		// so che sto visitando un Times (lo so dal SyntaxTree) e quindi dovrò creare un TimesNode. In visitTimes cosa facevamo?
		// inizialmente noi calcolavamo direttamente l'espressione dal ST. Mentre ora non dobbiamo calcolare nulla! Dobbiamo
		// creare un AST composto da Node. Il node corrispondente alla visita di un Times è TimesNode che prende, ovviamente,
		// due argomenti. Quindi avrà due sotto-alberi da scoprire, di conseguenza facciamo la visita dei suoi due figli
		// che, una volta arrivati in fondo a loro volta della loro visita, restituiranno un valore che sarà inserito nel TimesNode.
		// analogo discorso per visitPlus ecc. Il concetto generale è questo con 1, 2 o n argomenti nel costruttore.
		Node n = null;
		if ( c.DIV() != null ) {
			n = new DivNode(visit(c.exp(0)), visit(c.exp(1)));
			n.setLine(c.DIV().getSymbol().getLine());
		} else {
			n = new TimesNode(visit(c.exp(0)), visit(c.exp(1)));
			n.setLine(c.TIMES().getSymbol().getLine());
		}
        return n;		
	}

	@Override
	public Node visitPlusMinus(PlusMinusContext c) {
		if (print) printVarAndProdName(c);

		Node n = null;
		if ( c.PLUS() != null ) {
			n = new PlusNode(visit(c.exp(0)), visit(c.exp(1)));
			n.setLine(c.PLUS().getSymbol().getLine());
		} else {
			n = new MinusNode(visit(c.exp(0)), visit(c.exp(1)));
			n.setLine(c.MINUS().getSymbol().getLine());
		}
		return n;
	}

	@Override
	public Node visitComp(CompContext c) {
		if (print) printVarAndProdName(c);
		Node n = null;

		if ( c.EQ() != null ) {
			n = new EqualNode(visit(c.exp(0)), visit(c.exp(1)));
			n.setLine(c.EQ().getSymbol().getLine());
		} else if( c.GE() != null ){
			n = new GreaterEqualNode(visit(c.exp(0)), visit(c.exp(1)));
			n.setLine(c.GE().getSymbol().getLine());
		} else {
			n = new LessEqualNode(visit(c.exp(0)), visit(c.exp(1)));
			n.setLine(c.LE().getSymbol().getLine());
		}
		return n;
	}

	@Override
	public Node visitAndOr(AndOrContext c) {
		if (print) printVarAndProdName(c);

		Node n = null;
		if ( c.AND() != null ) {
			n = new AndNode(visit(c.exp(0)), visit(c.exp(1)));
			n.setLine(c.AND().getSymbol().getLine());
		} else {
			n = new OrNode(visit(c.exp(0)), visit(c.exp(1)));
			n.setLine(c.OR().getSymbol().getLine());
		}
		return n;
	}

	@Override
	public Node visitNot(NotContext c) {
		if (print) printVarAndProdName(c);
		Node n = new NotNode(visit(c.exp()));
		n.setLine(c.NOT().getSymbol().getLine());
		return n;
	}

	//dichiarazione di classe
	@Override
	public Node visitCldec(CldecContext c) {
		if (print) printVarAndProdName(c);

		List<FieldNode> fieldList = new ArrayList<>();
		// per ogni campo prenderci id del campo e tipo
		for (int i = 1; i < c.ID().size(); i++) {
			FieldNode p = new FieldNode(c.ID(i).getText(), (TypeNode) visit(c.type(i)));
			p.setLine(c.ID(i).getSymbol().getLine());
			fieldList.add(p);
		}

		List<MethodNode> methodNodeList = new ArrayList<>();
		//for (DecContext dec : c.dec()) decList.add((DecNode) visit(dec));
		for (MethdecContext dec : c.methdec()) {
			methodNodeList.add((MethodNode) visit(dec));
		}

		Node n = null;
		if (c.ID().size()>0) { //non-incomplete ST
			// abbiamo piu' id e più type (infatti gli diamo il numero, 0 o più)
			// il corpo si scopre facendo la visita dell'unico figlio exp.
			// se qualcosa non ti torna guarda sempre la produzione
			n = new ClassNode(c.ID(0).getText(), fieldList, methodNodeList);
			n.setLine(c.CLASS().getSymbol().getLine());
		}

		return n;
	}

	@Override
	public Node visitMethdec(MethdecContext c) {
		if (print) printVarAndProdName(c);

		List<ParNode> parList = new ArrayList<>();
		for (int i = 1; i < c.ID().size(); i++) {
			// prendiamo gli id ed i type da 1, perchè id(0) è l'id della funzione e type(0) pure.
			// il type può essere inttype o booltype
			ParNode p = new ParNode(c.ID(i).getText(),(TypeNode) visit(c.type(i)));
			p.setLine(c.ID(i).getSymbol().getLine());
			parList.add(p);
		}

		// itero sul figlio dec della funzione e scopro tutte le dichiarazione dei figli (var o funzione).
		// infatti, dalla sua produzione, si capisce che posso averne 0, 1 o più di dichiarazione, quindi itero.
		List<DecNode> decList = new ArrayList<>();
		for (DecContext dec : c.dec()) decList.add((DecNode) visit(dec));

		// discorso analogo al visitVarDec: se manca qualcosa torno un nodo null e avrò un errore sintattico.
		Node n = null;
		if (c.ID().size()>0) { //non-incomplete ST
			// abbiamo piu' id e più type (infatti gli diamo il numero, 0 o più)
			// il corpo si scopre facendo la visita dell'unico figlio exp.
			// se qualcosa non ti torna guarda sempre la produzione
			n = new MethodNode(c.ID(0).getText(),(TypeNode)visit(c.type(0)),parList,decList,visit(c.exp()));
			n.setLine(c.FUN().getSymbol().getLine());
		}

		return n;
	}

	@Override
	public Node visitDotCall(DotCallContext c) {
		if (print) printVarAndProdName(c);

		//gestiamo gli argomenti, dobbiamo scoprire il loro nodo terminale quindi classica visita
		List<Node> arglist = new ArrayList<>();
		for (ExpContext arg : c.exp()) arglist.add(visit(arg));

		Node n = new ClassCallNode(c.ID(0).getText(), c.ID(1).getText(), arglist);
		n.setLine(c.ID(0).getSymbol().getLine());
		return n;
	}

	@Override
	public Node visitNew(NewContext c) {
		if (print) printVarAndProdName(c);

		//gestiamo gli argomenti, dobbiamo scoprire il loro nodo terminale quindi classica visita
		List<Node> arglist = new ArrayList<>();
		for (ExpContext arg : c.exp()) arglist.add(visit(arg));

		Node n = new NewNode(c.ID().getText(), arglist);
		n.setLine(c.ID().getSymbol().getLine());
		return n;
	}








	/*
	* Generazione nodo AST per la dichiarazione di variabili. L'id del nodo da dove lo prendiamo? Dal syntax tree. Il tipo
	* dove lo prendiamo? Beh per scoprire il tipo dovremo fare la visita del nodo nell'ST che ci ritornerà un tipo (che potrà
	* essere (per ora) IntTypeNode (il TIPO int) o BoolTypeNode. Attenzione a non confoderli con IntNode o BoolNode che sono le foglie
	* con interi o booleani. Mentre l'exp cioè l'espressione associata alla variabile? anche quello andrà esplorato e scoperto
	* attraverso il ST. Da cosa si intuisce poi che qualcosa andrà esplorato? Guardando la produzione si notato delle variabili
	* quelle andranno esplorate fino a che non si arriva ad una foglia. es: VAR ID COLON type ASS exp SEMIC --> type e exp
	* sono le due variabili che esploreremo, di cui faremo la visita in visitVarDec(..).
	 */
	@Override
	public Node visitVardec(VardecContext c) {
		if (print) printVarAndProdName(c);
		// creiamo la variabile node che eventualmente è nulla
		Node n = null;
		if (c.ID()!=null) { // non-incomplete ST. Se id è null non c'è allora non è corretto sintatticamente e quindi la visita
			// è nulla e ritorno null.  cioè controllo che il syntax tree non sia incompleto. in questo caso non devo andare in palla
			// devo gestire l'errore. ATTENZIONE: antlr genera comunque il syntax tree ma non dovrà runnare il codice. solo alla fine
			// dopo aver fatto la sym table e tutto, allora farò vedere al programmatore TUTTI gli errori che ci sono stati.
			n = new VarNode(c.ID().getText(), (TypeNode) visit(c.type()), visit(c.exp()));
			//memorizza nel campo line del nodo n la linea dove vedo quel token nel codice sorgente scritto dall'utente
			n.setLine(c.VAR().getSymbol().getLine());
		}
        return n;
	}

	@Override
	public Node visitFundec(FundecContext c) {
		if (print) printVarAndProdName(c);

		List<ParNode> parList = new ArrayList<>();
		for (int i = 1; i < c.ID().size(); i++) {
			// prendiamo gli id ed i type da 1, perchè id(0) è l'id della funzione e type(0) pure.
			// il type può essere inttype o booltype
			ParNode p = new ParNode(c.ID(i).getText(),(TypeNode) visit(c.type(i)));
			p.setLine(c.ID(i).getSymbol().getLine());
			parList.add(p);
		}

		// itero sul figlio dec della funzione e scopro tutte le dichiarazione dei figli (var o funzione).
		// infatti, dalla sua produzione, si capisce che posso averne 0, 1 o più di dichiarazione, quindi itero.
		List<DecNode> decList = new ArrayList<>();
		for (DecContext dec : c.dec()) decList.add((DecNode) visit(dec));

		// discorso analogo al visitVarDec: se manca qualcosa torno un nodo null e avrò un errore sintattico.
		Node n = null;
		if (c.ID().size()>0) { //non-incomplete ST
			// abbiamo piu' id e più type (infatti gli diamo il numero, 0 o più)
			// il corpo si scopre facendo la visita dell'unico figlio exp.
			// se qualcosa non ti torna guarda sempre la produzione
			n = new FunNode(c.ID(0).getText(),(TypeNode)visit(c.type(0)),parList,decList,visit(c.exp()));
			n.setLine(c.FUN().getSymbol().getLine());
		}

        return n;
	}

	@Override
	public Node visitIntType(IntTypeContext c) {
		if (print) printVarAndProdName(c);
		return new IntTypeNode();
	}

	@Override
	public Node visitBoolType(BoolTypeContext c) {
		if (print) printVarAndProdName(c);
		return new BoolTypeNode();
	}

	// Indica la classe (Account, Cane, ecc)
	@Override
	public Node visitIdType(IdTypeContext c) {
		if (print) printVarAndProdName(c);
		return new RefTypeNode(c.ID().getText());
	}

	@Override
	public Node visitInteger(IntegerContext c) {
		if (print) printVarAndProdName(c);
		int v = Integer.parseInt(c.NUM().getText());
		// intero che tiene conto della presenza del minus
		return new IntNode(c.MINUS()==null?v:-v);
	}

	@Override
	public Node visitTrue(TrueContext c) {
		if (print) printVarAndProdName(c);
		return new BoolNode(true);
	}

	@Override
	public Node visitFalse(FalseContext c) {
		if (print) printVarAndProdName(c);
		return new BoolNode(false);
	}

	@Override
	public Node visitNull(NullContext c) {
		if (print) printVarAndProdName(c);
		return new EmptyNode();
	}

	@Override
	public Node visitIf(IfContext c) {
		if (print) printVarAndProdName(c);
		Node ifNode = visit(c.exp(0));
		Node thenNode = visit(c.exp(1));
		Node elseNode = visit(c.exp(2));
		Node n = new IfNode(ifNode, thenNode, elseNode);
		n.setLine(c.IF().getSymbol().getLine());			
        return n;		
	}

	@Override
	public Node visitPrint(PrintContext c) {
		if (print) printVarAndProdName(c);
		return new PrintNode(visit(c.exp()));
	}

	@Override
	public Node visitPars(ParsContext c) {
		if (print) printVarAndProdName(c);
		// le parentesi hanno un solo figlio, quindi esploro il figlio tra le parentesi (che sono simboli terminali e quindi
		// ci sono già, diciamo così.
		return visit(c.exp());
	}

	// uso di una variabile, visita dell'id
	@Override
	public Node visitId(IdContext c) {
		if (print) printVarAndProdName(c);
		Node n = new IdNode(c.ID().getText());
		n.setLine(c.ID().getSymbol().getLine());
		return n;
	}

	// uso di una funzione
	@Override
	public Node visitCall(CallContext c) {
		if (print) printVarAndProdName(c);

		//gestiamo gli argomenti, dobbiamo scoprire il loro nodo terminale quindi classica visita
		List<Node> arglist = new ArrayList<>();
		for (ExpContext arg : c.exp()) arglist.add(visit(arg));

		Node n = new CallNode(c.ID().getText(), arglist);
		n.setLine(c.ID().getSymbol().getLine());
		return n;
	}


}
