SbmlCompiler
============

This Java package can generate <a href="http://www.mcs.anl.gov/adifor">ADIFOR</a>
-compliant DAE code in FORTRAN from any <a href="http://www.sbml.org/">SBML</a>
level 2 version 2 model (except those with event rules). SbmlCompiler
analyzes evaluation dependencies of the SBML model and generates minimally a
function <pre>FFCN</pre> of differential equations and <pre>GFCN</pre> of algebraic equations in
FORTRAN with decorated symbols. SbmlCompiler can be instructed to also generate
a <pre>PLOT</pre> function or measurement functions <pre>MFCN</pre> for parameter estimation problems.

Elements of the dependency graph can be replaced programmatically to manipulate
the model in accordance with experimental setups. For example, a <pre>DiffStateCoder</pre>
object for a ligand concentration in the bindings returned by <pre>getInVivoBindings()</pre>
can be replaced by a <pre>ControlCoder</pre> object if the ligand concentration can be
dosed experimentally. This makes SbmlCompiler useful for supporting parameter
estimation campaigns from cell based or in vitro assays. Any FORTRAN code
produced SbmlCompiler is suitable for automatic differentiation by
<a href="http://www.mcs.anl.gov/adifor">ADIFOR</a>.

SbmlCompiler has been validated against all 17 models of the initial release of
the <a href="http://www.ebi.ac.uk/biomodels/">BioModels</a> database.
