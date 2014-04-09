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
