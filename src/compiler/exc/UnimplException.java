package compiler.exc;

/*
*
* Eccezione che lancio se durante lo sviluppo mi scordo di implementare qualcosa. Usata per visitNode in BaseASTVisitor.
* Utile in fase di debug, sennò difficile capire l'errore!
*
 */
public class UnimplException extends RuntimeException {

	private static final long serialVersionUID = 1L;

}
