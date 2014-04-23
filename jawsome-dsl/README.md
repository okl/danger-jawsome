# jawsome-dsl

A DSL for declaratively creating functions to transform JSON.

Gives you access to all the transforms from jawsome-core for free :)

This README assumes that you've read the jawsome-core README first. If
you haven't done that yet, do it now!

## Motivation

Jawsome-core provides a nice library of individual functions that are
useful when transforming json... but it just ain't very usable at a
high level. Jawsome-dsl aims to address that problem, by making it
easy (and simple!) to create "pipelines" of JSON transformations.

Jawsome-dsl is a layer of abstraction higher than jawsome-core. It
lets you combine the low-level building blocks in jawsome-core to
create high-level Clojure functions that do exactly what you need.

# Usage

## Pipelines

The main construct used in jawsome-dsl is the pipeline. A pipeline may
have up to three phases:

1. *Denorm* cleanup and denormalize the input data (flattens
   hierarchical / nested structures, denormalizes collections /
   arrays)
2. *Schema* determine the schema of the denormalized data
3. *Project* project the denormalized data from free-schema
   association-lists (i.e. Clojure maps) to fixed-schema CSV rows. In
   practice, this means "rewriting the Clojure maps into CSV rows,
   leaving holes for columns/fields that didn't appear in a given
   record".

A pipeline might look like this:

    '(pipeline
       (denorm-phase (read-phase (xforms :read-json))
                     (xform-phase (custom :prune-paths ["field_we_want_to_ignore])
                                  (xforms :reify)
                                  (custom :log
                                          :my-custom-fn-which-has-been-properly-registered)
                                  (xforms :translate {"undefined" nil}
                                          :denorm)))
       (schema-phase)
       (project-phase)))

The function that turns pipelines into functions is called
`pipeline-interp`. It takes an environment, too -- usually, you can
use the `default-env`, but sometimes you might want to use a special
env like `jawsome-dsl.denorm/env-to-disable-post-denorm-cleanup`.

`pipeline-interp` returns a map with the following structure:
`{:denorm denorm-fn, :schema schema-fn, :project project-fn}`

* The denorm-fn is **one-to-many**. It takes one record (either a
  single line of text, or a single Clojure map) and returns a seq of records
  (Clojure maps). This is because it denormalizes when there are arrays
  in the field data.
* The schema-fn is a folding fxn, so it is **many-to-one**. It takes a
  seq of records and returns a schema.
* The project-fn is **one-to-one**. It takes a denormed-record, a
  schema, and a field delimiter, and it returns an
  xsv-projected-record.

### Syntax-quoted pipelines

Sometimes it's really convenient to use a syntax-quote when building a
pipeline, rather than a normal quote. The normal `pipeline-interp`
function will complain if you give it a syntax-quoted pipeline, since
all the symbols will have been namespace-resolved, but the function
`interp-namespaced-pipeline` knows how to handle that problem.

## Ordered vs unordered xforms

As described in the jawsome-core README, many of the transforms in the
denorm-phase are very sensitive to order. There's a subset of those
library functions that we believe have a single sensible order:

Read phase:

1. remove-cruft
1. recode-unicode
1. read-json

Xform phase:

1. hoist
1. remap-properties
1. reify
1. translate
1. translate-paths
1. type-enforce
1. denorm

See init_registry.clj for the current orderings.

### xforms blocks

These xforms are used in `xforms` blocks (see the sample pipeline
above). If you put them in an xforms block and you specify them in
some other order, jawsome-dsl will **REORDER** them to the above
order. If you really want to break this order, e.g. to type-enforce
before you reify, you can do that by putting them in separate xforms
blocks:

    (xform-phase (xforms :type-enforce)
                 (xforms :reify))

Just to be clear, you couldn't accomplish that behavior like this:

    (xform-phase (xforms :type-enforce :reify))

because that's equivalent to either of these:

    (xform-phase (xforms :reify :type-enforce))

    (xform-phase (xforms :reify)
                 (xforms :type-enforce))

### custom blocks

Any other transform (either a custom transform that you've defined, or
a jawsome-core transform that isn't part of the inherently sensible
ordering) is used in `custom` blocks.

See e.g. :log, :prune-paths, and
:my-custom-fn-which-has-been-properly-registered above.

## Extensibility

### Registering custom xforms (defxform)

You can register custom xforms with `defxform`. I suggest writing your
custom xforms in the following way.

First, write a function that satisfies the following contract:

* Read-phase xforms should take a string and return a string (or a seq
  of strings).
* If your data is encoded in a custom data structure which you're
  trying to define a reader for, then a read-phase xform may take a
  string and return a Clojure map. But this probably won't happen very
  often.
* Xform-phase xforms should take a Clojure map and return a Clojure
  map (or a seq of Clojure maps).

Then, write a function that takes any initalization args you need to
properly initialize/configure the first function (such as a map of
synonyms for the :synonyms xform, a list of paths to prune for the
:prune-paths xform, etc) and returns your first function (the one that
only takes a string/map as input).

Define the SECOND function under `defxform`.

E.g. if I want an xform that removes fields with a certain value:

    (defn remove-fields-with-a-certain-value [clj-map value]
      (let [removed (remove #(= value (val %)) clj-map)]
        (into {} removed)))

    (defn make-remove-fields "This returns a function!" [value]
      (fn [clj-map]
        (remove-fields-with-a-certain-value clj-map value)))

    (defxform 'remove-fields make-remove-fields)

See init_registry.clj for more defxform examples.

### Registering custom vars (defvar)

You can define your own vars (because maybe you have a big honking
configuration map and you don't want it inlined in the pipeline
definition) using `defvar`. Dereference them using `ref`, e.g. `(ref
myvar)` after having done `(defvar 'myvar 42)`

You can also define thunks (i.e. functions that take no arguments)
using defvar. Invoke them at interpret time using `dethunk`,
e.g. `(dethunk (ref mythunk))` after having done `(defvar 'mythunk )`

### Implementation notes: Interpreters!

This DSL is implemented with two layers of interpreter:

1. High-level (core.clj and denorm.clj), or L2 colloquially
2. Low-level (xform.clj), or L1 colloquially

The L1 interpreter has a symbol table where variables and functions
are defined, called the "registry". And yep, you guessed it -- defvar
and defxform modify the registry :) The registry gets initialized with
basically everything from jawsome-core. See init_registry.clj for a
complete listing.

## Xforms that are enabled by default

Jawsome-dsl aims to be pretty transparent, and not have any hidden
behaviors or gotchas. However, there were a couple of transforms that
we found ourselves using ALL the time and which we wanted to enable by
default, to reduce verbosity of pipeline definitions. They are as
follows:

Denorm phase:

1. `remove-empty-string-fields` Empty string fields are removed by
   default... if you don't want to remove empty string fields, because
   your application has different semantics around null and empty string,
   then you will have to explicitly specify that by passing in
   jawsome-dsl.denorm/env-to-disable-post-denorm-cleanup instead of the
   default-env.
2. `sanitize-field-names` (takes field names, finds characters that
   aren't alphanumeric or underscores, and replaces them with
   underscores) is also enabled by default. Disable it the same way as
   remove-empty-string-fields.

Schema phase:

N/A

Project phase:

N/A

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
