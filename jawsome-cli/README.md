# jawsome-cli

A CLI wrapper for transforming JSON data as configured in pipelines in
jawsome-dsl.

## Usage

This library boils down to the `def-cli-pipeline`
macro. `def-cli-pipeline` takes a jawsome-dsl pipeline s-expression,
and defines a main function which dispatches to the appropriate pipeline
function. To make that the 'active' pipeline for a given project, you
need to do two things:
  (1) set the namespace where it's defined to be the :main namespace
      in your project.clj.
  (2) add a `(:gen-class)` directive to the namespace where it's
      defined.

For example, if your pipeline is defined in namespace
jawsome-pipeline-searchpro.core, you would add
  `:main jawsome-pipeline-searchpro.core`
to the project.clj for jawsome-pipeline-searchpro, add
  `(:gen-class)`
to the ns-macro at the top of jawsome-pipeline-searchpro.core, and run
  `lein uberjar`
to generate jawsome-pipeline-searchpro-x.y.z-standalone.jar.

Then, you could invoke individual pipeline phases with the following:
  `java -jar /path/to/jawsome-pipeline-searchpro-x.y.z-standalone.jar denorm` (*1)
  `java -jar /path/to/jawsome-pipeline-searchpro-x.y.z-standalone.jar schema` (*2)
  `java -jar /path/to/jawsome-pipeline-searchpro-x.y.z-standalone.jar project` (*3)
and you could invoke an overall pipeline with the following:
  `cat foo.js |
      *1 | tee denorm.js |
      *2 > schema.clj ;
      *3 denorm.js schema.clj > project.xsv`

## License

Copyright One Kings Lane © 2013

Distributed under the Eclipse Public License, the same as Clojure.
