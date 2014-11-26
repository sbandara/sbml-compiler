SbmlCompiler
============

<h2>Overview</h2>
This Java package can generate <a href="http://www.mcs.anl.gov/adifor">ADIFOR</a>
-compliant DAE code in FORTRAN from any <a href="http://www.sbml.org/">SBML</a>
level 2 version 2 model (except those with event rules). SbmlCompiler
analyzes evaluation dependencies of the SBML model and generates minimally a
function <code>FFCN</code> of differential equations and <code>GFCN</code> of algebraic equations in
FORTRAN with decorated symbols. SbmlCompiler can be instructed to also generate
a <code>PLOT</code> function, or measurement functions <code>MFCN</code> for parameter estimation problems.

<h2>Notable Features</h2>
Elements of the dependency graph can be replaced programmatically to manipulate
the model in accordance with experimental setups. For example, a <code>DiffStateCoder</code>
object for a ligand concentration in the bindings returned by <code>getInVivoBindings()</code>
can be replaced by a <code>ControlCoder</code> object if the ligand concentration can be
dosed experimentally. This makes SbmlCompiler useful for supporting parameter
estimation campaigns from cell based or in vitro assays. Any FORTRAN code
produced SbmlCompiler is suitable for automatic differentiation by
<a href="http://www.mcs.anl.gov/adifor">ADIFOR</a>.

<h2>Validation</h2>
SbmlCompiler has been validated against all 17 models of the initial release of
the <a href="http://www.ebi.ac.uk/biomodels/">BioModels</a> database.

<h2>Acknowledgements</h2>
I thank my internship mentors Ivayla Vacheva and Roland Eils at the German
Cancer Research Center (dkfz.) and Johannes Schlöder, Stefan Körkel, and Hans Georg Bock
at the Interdisciplinary Center for Scientific Computing (IWR). I developed this
package as part of the project "Computational platform for modeling of signal transduction"
funded by Microsoft Research.