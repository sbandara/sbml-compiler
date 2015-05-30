<h2>de.dkfz.tbi.sbmlcompiler</h2>

<h3>Overview</h3>
This Java package generates DAE code in FORTRAN from any continuous <a href="http://www.sbml.org/">SBML</a>
level 2 version 1 model. SbmlCompiler analyzes evaluation dependencies of the SBML model and generates
minimally a function <code>FFCN</code> of differential equations and <code>GFCN</code> of algebraic equations
in FORTRAN with decorated symbols for linkage into numerical optimization software. SbmlCompiler can be
instructed to also generate a <code>PLOT</code> function, or measurement functions <code>MFCN</code>.

<h3>Notable Features</h3>
Elements of the dependency graph can be replaced programmatically to manipulate the model in accordance with
experimental setups. For example, a <code>DiffStateCoder</code> object for a ligand concentration in the
bindings returned by <code>getDefaultBindings()</code> can be replaced by a <code>ControlCoder</code> object
if the ligand concentration can be dosed experimentally. This makes SbmlCompiler useful for supporting
parameter estimation campaigns from cell-based or <i>in vitro</i> assays. Any FORTRAN code produced
SbmlCompiler is suitable for automatic differentiation by <a href="http://www.mcs.anl.gov/adifor">ADIFOR</a>,
a feature that can be exploited by certain applications of numerical optimization.

<h3>Validation</h3>
After initial development in 2005, SbmlCompiler was validated against all 17 models of the initial release
of the <a href="http://www.ebi.ac.uk/biomodels/">BioModels</a> database. Concentration trajectories of each
model were compared to the results of the respective group of authors. SbmlCompiler is now supported by 8
unit tests.

<h3>Development Goals</h3>
Compared to the original package developed in 2005, the following modifications have been completed:

* The SbmlCompiler library no longer requires libSBML. Our own SBML reader in <code>de.dkfz.tbi.sbmlcompiler.sbml</code>
  provides more convenient APIs for the specific problem at hand.
* The supported SBML version has regressed to Level 2 Version 1, so we can focus on testing with SBML
  that was exported from PySB.

The following features would be nice to have in the near future:

* Support for different simulation clients in addition to packages that link FORTRAN code. We can do this
  by abstracting code generators from the entities in the dependency graph.
* Add support for more recent SBML revisions up to Level 2 version 5.
* Automatic differentiation of model equations, directly from the graph of dependencies and AST nodes.

<h3>Acknowledgements</h3>
This package was initially developed as part of a "Computational platform for modeling of signal transduction",
with a grant from Microsoft Research to my mentors Ivayla Vacheva and Roland Eils at the German Cancer Research
Center (dkfz.), and Johannes Schlöder, Stefan Körkel, and Hans Georg Bock at the Interfisciplinary Center for
Scientific Computing (IWR).
