package compiler.exc;

/*
*
* Eccezione unchecked perchè estende da RuntimeException e questo mi libera dal fatto di dover mettere tutte le varie
* "throws E" dove uso questa eccezione (la uso nel print visitor).
*
 */
public class VoidException extends RuntimeException {

	private static final long serialVersionUID = 1L;

}
