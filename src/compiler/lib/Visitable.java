package compiler.lib;

public interface Visitable {

	// inizialmente accettava per PrintASTVisitor ora accetta per tutti i visitor dato che gli passo un BaseASTVisitor
	// il <S,E extends Exception> davanti al parametro di ritorno S è un binder e indica che i parametri S ed E non
	// vengono dati dalla interfaccia in maniera globale ma sono interni/locali al metodo accept o meglio da chi la chiama/
	// implementa. Anche lui può gettare una eccezione e ci dovrà essere il binder per E. 
	<S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E;

}
