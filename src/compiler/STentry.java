package compiler;

import compiler.lib.*;

// classe che gestisce le info riguardo le dichiarazioni delle fun/var/par! LA FAMOSA PALLINA!
public class STentry implements Visitable {
	final int nl; //nesting level della pallina
	final TypeNode type; //tipo della pallina
	final int offset; // campo offset che tiene conto dell'offset di una var/fun all'interno di un AR
	public STentry(int n, TypeNode t, int o) { nl = n; type = t; offset=o; }

	@Override
	public <S,E extends Exception> S accept(BaseASTVisitor<S,E> visitor) throws E {
		// son sicuro che mi passe un BaseEASTVisitor e qwuindi casto
		return ((BaseEASTVisitor<S,E>) visitor).visitSTentry(this);
	}
}
