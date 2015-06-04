package de.dkfz.tbi.sbmlcompiler;

/**
 * Class of coders that are responsible for a state variable of the FFCN or
 * the GFCN. These coders are not only involved in the calculation of the
 * new state, i.e. by evaluating the differential equation or calculating
 * the algebraic residual, but also need to make the current value of the
 * state available within one or more FORTRAN functions. By convention, a
 * <code>StateVariable</code> generates the output of a model function when
 * {@link SbmlCompiler.FortranCoder#putFortranCode} is called, which is
 * referred to as the entry of dependencies, and rescales the input current
 * value of the state upon invocation of {@link SbmlCompiler.StateVariable
 * #putHeader}. Thus, a <code>StateVariable</code> closes the loop of
 * dependencies by interfacing to the integrator. Note that <code>
 * putFortranCode</code> is executed only once during compilation, whereas
 * <code>putHeader</code> may be called up to once for each model function.
 * @author Samuel Bandara
 */
abstract class StateVariable extends FortranCoder {
	
	StateVariable(SbmlCompiler compiler, Problem.Role role) {
		super(compiler, role);
	}

	void initialize(Bindings bindings) throws SbmlCompilerException {
	}
	
	/**
	 * Puts the header assignment of this state variable to the target
	 * function <code>target</code>. <code>SbmlCompiler</code> guarantees
	 * that this method is invoked only once per target function.
	 * @param target target function
	 * @param bindings dependency model of the experiment
	 */
	abstract void putHeader(TargetFunction target, Bindings bindings);
	
	/**
	 * Returns whether this state variable is differential or algebraic.
	 * @return FFCN if it is differential, or GFCN if it is algebraic
	 */
	abstract int getTarget();
}
