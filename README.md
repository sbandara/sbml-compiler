<h2>de.dkfz.tbi.sbmlcompiler</h2>

<h3>Overview</h3>
This Java package generates <a href="http://www.mcs.anl.gov/adifor">ADIFOR</a>-processable
DAE code in FORTRAN from any <a href="http://www.sbml.org/">SBML</a> level 2 version 2 model
(except those with event rules). SbmlCompiler analyzes evaluation dependencies of the SBML
model and generates minimally a function <code>FFCN</code> of differential equations and
<code>GFCN</code> of algebraic equations in FORTRAN with decorated symbols for linkage into
numerical optimization software. SbmlCompiler can be instructed to also generate a
<code>PLOT</code> function, or measurement functions <code>MFCN</code>.

<h3>Notable Features</h3>
Elements of the dependency graph can be replaced programmatically to manipulate the model in
accordance with experimental setups. For example, a <code>DiffStateCoder</code> object for a
ligand concentration in the bindings returned by <code>getInVivoBindings()</code> can be
replaced by a <code>ControlCoder</code> object if the ligand concentration can be dosed
experimentally. This makes SbmlCompiler useful for supporting parameter estimation campaigns
from cell-based or <i>in vitro</i> assays. Any FORTRAN code produced SbmlCompiler is suitable for
automatic differentiation by <a href="http://www.mcs.anl.gov/adifor">ADIFOR</a>.

<h3>Validation</h3>
SbmlCompiler has been validated against all 17 models of the initial release of the
<a href="http://www.ebi.ac.uk/biomodels/">BioModels</a> database.

<h3>Acknowledgements</h3>
I thank my mentors Ivayla Vacheva and Roland Eils at the German Cancer Research Center
(dkfz.) and Johannes Schlöder, Stefan Körkel, and Hans Georg Bock at the Interdisciplinary
Center for Scientific Computing (IWR). I developed this package as part of a "Computational
platform for modeling of signal transduction", for which Microsoft Research provided funding.
