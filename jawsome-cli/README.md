# jawsome-cli

A CLI wrapper for transforming JSON data as configured in jawsome-dsl pipelines.

This README assumes that you have read the jawsome-dsl README. If you
haven't done that yet, do it now!

## How to require it

Add these dependencies to your `project.clj`:

```clj
[com.onekingslane.danger/jawsome-cli "x.y.z"]
[com.onekingslane.danger/jawsome-dsl "x.y.z"]
```

These will allow you to make the following requires:

```clj
(:require [jawsome-cli.core :refer [def-cli-pipeline def-multi-cli-pipeline]])
(:require [jawsome-dsl.xform :refer [defxform defvar]])
```

## Usage: def-cli-pipeline

This library boils down to the `def-cli-pipeline`
macro. `def-cli-pipeline` takes a jawsome-dsl pipeline s-expression,
and turns it into a `(defn -main ...)` which dispatches to the
appropriate pipeline function. To make that the 'active' pipeline for
a given project, you only have to set the namespace where it's defined
to be the `:main` namespace in your project.clj.

For example, if your def-cli-pipeline is in namespace
my-jawsome-pipeline.core, you would add `:main
my-jawsome-pipeline.core` to my-jawsome-pipeline's project.clj.

(The `def-cli-pipeline` macro also includes a gen-class statement, so
you do NOT need to add a `(:gen-class)` directive to your ns macro at
the top of your module.)

Then you could run

    $ lein uberjar

to generate my-jawsome-pipeline-x.y.z-standalone.jar, which
would let you invoke individual pipeline phases with the following:

  `java -jar /path/to/my-jawsome-pipeline-x.y.z-standalone.jar denorm` (*1)
  `java -jar /path/to/my-jawsome-pipeline-x.y.z-standalone.jar schema` (*2)
  `java -jar /path/to/my-jawsome-pipeline-x.y.z-standalone.jar project` (*3)

or you could invoke an overall pipeline with something like the following:

    $  cat foo.js |
    $      *1 | tee denorm.js |
    $      *2 > schema.clj ;
    $      *3 denorm.js schema.clj > project.xsv

## Advanced usage: def-multi-cli-pipeline

Jawsome-cli allows you to have multiple pipeline variants in the same
jar, switching between them at the command line invocation. The
`def-multi-cli-pipeline` macro is how you do it. Example:

    (def-multi-cli-pipeline
      ("VARIANT1" '(pipeline ...) :cli-time-thunks [set-s3url!])
      ("VARIANT2" '(pipeline ...)))

You may have as many variants as you like. (Notice that each variant
must be named.)

You still have to set the namespace of the def-multi-cli-pipeline to
be the :main namespace for your project, and you still build it with
"lein uberjar". The only usage difference is that you have to specify
the desired variant in the cli-invocation. E.g. instead of `java -jar
myjar.jar denorm`, you use `java -jar myjar.jar VARIANT1 denorm`.

### Why def-multi-cli-pipeline?

* Maybe you have multiple pipeline variants that you want to switch
  between depending on the source of the raw data -- maybe there's a
  server that's configured differently from all the others, for
  whatever reason.
* Maybe you find your raw data has really big arrays, so your
  target data sink is ballooning quickly in data volume. You want to
  have two pipelines with two separate data sinks -- one that has all
  the columns except for the array data, and one that has JUST the
  array data and a foreign key. You'll want the two pipelines to be
  defined next to each other because they share the same read phase.

## License

Copyright One Kings Lane © 2013

Distributed under the Eclipse Public License, the same as Clojure.
