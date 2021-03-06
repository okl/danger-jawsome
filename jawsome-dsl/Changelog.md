# 1.3.0

Add `only` and `keep-paths` (which are synonyms of each other) to the
registry.

Add `remove` (synonym of `prune-paths`) to the registry.

Add `interp-namespaced-pipeline` to help out those syntax-quote lovers.

# 1.2.1

Use jawsome-core 1.1.0.

# 1.2.0

Got rid of `wrap-field-names-with-a-faux-schema`. Now, the
project-phase function takes either a schema OR a set of fields as its
second argument.

Also, you can specify the field-order for the projected xsv data now,
instead of being forced into using alphabetical order.

# 1.1.0

Added `wrap-field-names-with-a-faux-schema`, which allows you to
invoke the project-phase function using a set of fields, rather than a
proper full-on jsonschema.

You may find yourself wanting to do this in 2 situations:

1. You have strong guarantees about the schema of the source data so
   you don't want to spend the time running the schema phase.

2. You only ever want to project the same set of fields, even if new
   fields do show up, so you don't bother running the schema phase to
   find out if there are new ones. (Note that, here, you are
   implicitly hoping that truncation will never happen, i.e. you have
   a strong guarantee about field width within the data, even if you
   don't have a strong guarantee about new fields appearing.)
