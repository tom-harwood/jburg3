# jburg3
JBurg3 is  extended form of the algorithm outlined in Dr. Todd Proebsting's 1992 paper, [Simple and Efficient BURS Table Generation](http://research.cs.wisc.edu/techreports/1991/TR1065.pdf). JBurg3 extends Proebsting's BURG's capabilities in several respects which have proven valuable in practice:
* The pattern matching productions accept arbitrary numbers of children, both fixed arity and variadic.
* Specialized "pattern matching" rules to handle null nodes.
* Pattern matching rules can be guarded by semantic predicates.
* Nonterminal-to-nonterminal rules can execute reduction actions.

JBurg3 shares most of these capabilities with JBurg2, which uses an ["iburg"](http://drhanson.s3.amazonaws.com/storage/documents/iburg.pdf) type algorithm that generates rewrite machines that defer dynamic programming to compile time. JBurg3, however, does its dynamic programming at BURG initialization time. JBurg3 can generate its tables at compiler-compile time and serialize the tables into an XML or host language source file.

JBurg3 differs from most other BURGs in that it does not have a specification language; the benefits of a bespoke language have, in many deployment scenarios, been outweighed by the overhead of educating developers in the tool and integrating the tool into build processes.

Work outstanding before beta:
* A good deal more documentation, in particular presentations of the reasoning behind variadic productions and nonterminal-to-nonterminal productions with logic.

The author also wishes to thank former and present employers who have supported the JBurg project:

* Allaire Corporation
* Macromedia
* [Adobe Systems](http://www.adobe.com)
* [NuoDB](http://www.nuodb.com)
* [Veracode](http://www.veracode.com)
