# 1.2.3

Use jawsome-dsl 1.2.1.

# 1.2.2

`def-cli-pipeline` has a new signature:

    (defmacro def-cli-pipeline [l2-form & {:keys [cli-time-thunks]
                                           :or {cli-time-thunks}}]
                                ...)

instead of just being `[l2-form & cli-time-thunks]`.

# 1.2.1

Bugfix -- changed the `field-order` semantic in jawsome-dsl so have to
call `fields` on the schema.

# 1.2.0

jawsome-cli is now extensible with arbitrary custom functions!

### The motivation

I have a bunch of json data stored in files. I want to assoc into each
jsonmap which sourcefile the jsonmap came from (i.e. the first batch
of jsonmaps came from file A, the second batch came from file B,
...). However, I only know the sourcefile at runtime. I want a way to
pass the sourcefile in through the command line invocation of the
jawsome jar.

### Usage: passing custom values in

Pass through custom arguments at the command line using `-X key:val`.

e.g. If I have a custom value I want to pass in to my denorm phase, I
would use `java -jar path-to-my-uberjar.jar denorm -X
custom-key:custom-val`. Note that `custom-key` will be parsed as
"everything before the first `:`" so `custom-key` may not contain a `:`.

### Usage: using the custom values on the other side

`def-cli-pipeline` is now variadic -- it takes an l2-form AND any
number of functions to invoke at runtime. These "cli-time-thunks"
should generally look like this:

    (defn set-custom-val! [xs]
      (let [cli-custom-val (get xs "custom-val")]
        (swap! custom-val (constantly cli-custom-val))))

That is, they take a map of all the xs that were passed in through
`-X`, get some value in the map of xs, and execute some code with
side-effects.

You can then dethunk the thunk in a pipeline like so:

    (def custom-val (atom nil))
    (defvar 'yield-custom-val-map
      (fn []
        {"custom-val" @custom-val}))

    (def-cli-pipeline
      '(pipeline
         (denorm-phase
           (xform-phase (custom :static-values (dethunk (ref yield-custom-val-map))))))
      set-custom-val!)

Verbose but it gets the job done.

# 1.1.0

Added `--field-order` option for schema-phase, `--record-terminator`
option for project-phase.

`--field-order` exists in case you want to know what the project-phase
header will look like before you actually get to the project
phase.

`--record-terminator` lets you specify the record terminator in the
projected data (defaults to newline if unspecified). You might want to
use this if e.g. there are unescaped newlines in the data.
