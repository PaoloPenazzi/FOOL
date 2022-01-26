package compiler.exc;

import compiler.lib.*;

/*
* Eccezione che diamo quando visitiamo i tipi e ce un errore di tipo. Eredita da Exeption quindi è checked.
 */
public class TypeException extends Exception {

	private static final long serialVersionUID = 1L;

	public String text;

	public TypeException(String t, int line) {
		FOOLlib.typeErrors++;
		text = t + " at line "+ line;
	}

}
