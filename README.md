danger-jawsome
==============

A framework for rapidly configuring and deploying JSON Denormalization
processes

# Goals

Before we set out to build something, let's just have a nice little
brain dump- you know, for historical purposes.

# The Problem

We want to be able to easily query JSON data and couple JSON data with
other production data sources in order to gain insight into various
parts of our company. Analytics on JSON (semi-structured) data is very
challenging, and while there are many tools out there, there are a lot
of trade offs. I want speed, I want SQL, and I want consistency.

## Where does JSON data come from?

Here's a list of sources at One Kings Lane that generate JSON data

* Apache (User Interaction Data)
* Elastic Search (Search Data)

## Challenges of JSON

### Mapping it to relational databases to make it queryable

If needs to map JSON into a relationship database, we would
have to explode the data into an arbitrary number of subtables, create
join identifiers, and constantly be adding new tables for every new
variation of a particular JSON record that can occur. That's a LOT of
dynamic SQL to write, there's all sort of lock contention, DDL issues,
down stream process changes, etc that would constantly be effected,
and if you have lots of fields that only appear every now and then,
you're going to have a lot of child tables that are very empty- which
makes navigating your schemas very hard- as well as maintaining table
relationships- how do you know the order to join things if you have
JSON that's 3 layers nested deep- and how scary is that query going to
be to write!

### Deriving or maintaining schemas

What's more challenging is that since JSON is embedded with its schema
on a per record basis, there's no way to assert that any two records
share the same schema if they belong to the same set of data.  That
either needs to be done externally through validation, or internally
to the process that's entering the data into the database.  What's
worse is that since JSON is often recorded in files, there's no type
information as part of the schema.  While in a given record we can
determine whether a value should be interpreted as a string, number,
literal, array, or map, across records we can't be certain if an entry
is a number in one record that it will be a string in the next, or a
map in the next, or an array; or if an entry is an array, that it will
be always be an array of heterogeneous types, or that they'll appear
in the same location, or that they won't have other nested structure
of the same schema.

To handle this, you would have to create services to validate data,
record schemas outside of the derived data for validation, and change
code paths every time a new variation has to be introduced. There
would have to be lots of long meetings and lots of crying before a
change could be introduced because so many different tables would
suddently appear, and the join heirarchy would have to be updated, etc
etc.  Yuck. Errecting barriers only creates stability through
inability.

That's no good- and even still, with all the crazy joins,
no one is happier- least of all analysts.

# The Solution - Denormalization

Instead of working exhaustingly hard to normalize the JSON data for
space efficiency (which is often the reason we normalize data), let's
assume that space is cheap (because it often is when compared to the
cost of people), and swing way the other way. Denormalize all of the
data.

What does this mean? For any property that's an atom (number, boolean,
null, string) create a column that's named by its property name. For
any property that contains a map, insert their values into columns
identified by their property path, with a clear demarcation of where
the dot (".") is in the column name (so that `customer.id` and
`customer_id` would yield two different column names).  For any
arrays, duplicate the entire record for every particular array value,
adding a column to record the value and another column to record the
index- and yes, if there are multiple arrays or nested arrays you will
see the records balloon exponentially with respect to the number of
arrays.

# Addressing Concerns

Hmm, this sounds easy, but doesn't this just open up a whole 'nother
can of worms? Here are some concerns that may jump to the front of
your mind:

1. How does this making the querying easier that normalization?
2. Won't database performance be terrible?
2. Isn't duplicating all of that data when there are arrays a huge
   waste of space?
3. What happens when a new field or property shows up- what if our
   data is always changing?
4. How does this solve the schema problem? I could have mixed column
   types? How do I work through that?
5. How on earth could this be performant?
6. What if my data is messy? How would I clean it up? It would be hard
   to clean the data if the column names are so long and it's much
   easier to think about things as maps.
7. I want to implement type enforcement of fields.
8. How on earth does this scale?
9. My data has changed over time, I need to implement rules to
   homogenize the data.

## Columnar Database

At One Kings Lane we're using Vertica, a columnar store database.
This means that data is stored in column major order instead of row
major. One of the advantages of this is that since data is stored
column by column, values within a given column is typically similar,
the storage engine can encode and compress the data very efficiently,
both lowering the query time, and minimizing the space required to
store the data. Since Vertica is also a cluster-deployed database,
data is segmented and partitioned across several nodes which
provides for parallel execution of queries.

The use of a columnar database that is optomized for relational cubes
and fact tables is a huge boon for us since they have a number of
optimizations that take into consideration that your data has LOTS of
duplication in values in each column.


... TODO ALEX CONTINUE ADDRESSING CONCERNS ABOVE.

# Technical Overview

The purpose of Jawsome is two fold:

1. A _framework_ for constructing an engine for the above outlined
 JSON denormalization process
2. A _library_ of functions useful for JSON manipulation and analysis
   of collections of JSON documents

## Framework

As a framework, we seek to provide a declarative specification of
a JSON processing pipeline.  Each pipeline typically has the following
phases:

1. Raw text cleanup
2. Transformations and Filtering
3. Denormalization
4. Schema Generation
5. Output formatting

More over, we want these phases to be automatically adapted to
different execution platforms, namely:

* CLI
* Hadoop

Let's look at these two aspects in more detail:

### Phases

#### Raw text cleanup

While JSON can originate in many places, it often can come directly
from a file. Files, often can be very dirty. They might contain
comments, they might contain unreadable lines of JSON, they might be
filled with errors from the process that had been generating the log
files- _ultimately the JSON data may be in some such state that it
cannot be parsed because for one reason or another its not valid
JSON._

##### Nested, escaped, JSON forms

Often times, applications that are writing out JSON data may have
components that are also writing out JSON data and returning them as
strings- these applications yield JSON records where some property's
value may be an escaped JSON string.  Having to write code to
specifically remove the escaping so that the JSON is parseable with
its structure intact is very tedious. While this may result in
parseable JSON records, we really want to be able to traverse into
those nested property paths. As such assume that we would want to
unescape any inner nested JSON.


##### Handling of Unicode characters

Many systems encode unicode a number of different ways. We need to
ensure that various encodings are readable so that we don't encounter
exceptions when attempting to parse the JSON, nor lose the data
captured by the unicode characters.

##### Extensibility

We also want to make sure this phase is extensible so that users can
supply their own raw text cleanups to ensure the files are parseable
or correct errors outside of JSON.

As such, users can provide a function that takes a string and returns
a string.


#### Transformations

Once the JSON data is parsable, it's much easier to work with as an in
memory map. As such, there are many common transformations that we
would like to offer through configuration since they are common
reoccuring patterns.

##### Property Renaming, Remapping and Pruning
  Renaming keys, altering the property paths of values, or pruning
  them all together.

##### String Value Reification (Nullify, Numberify, Boolify) #####

  Parsing values to determine if they can have their type simplified
  from simply being a string.

##### Value Synonym Mapping
  Coverting synonyms for values to those literal values (e.g. `"-"` =>
  `null`, `"yes"` => `true`, `""` => `""`)

##### Null Pruning
  Removing keys whose corresponding values are `null`

##### Property path value-type filtering (i.e. type enforcement) #####

  For particular fields that we always expect to have a value in a
  particular range of values (e.g. strings, numbers, booleans), remove
  any fields that do not have an expected value.

##### Static value injection
We may want to inject static values for various reasons.


The thing about these transformations is that they are very sensitive
to the order in which they are performed. It wouldn't make sense to
remove all the values that are null, before we've applied a
transformation that maps all the synonyms for null values to
null. Similarly, it wouldn't make sense to walk each record and turn
all of the strings that appear as valid numbers or synonyms for
boolean values into numbers or booleans after
removing any key-value pairs that don't match a configured type
requirement for those fields.

This means all of these transformations themselves form their own
ordered pipeline.  As such, we propose that the most reasonable order
for these transformations to take place in is as follows:



1. String Value Reification (Nullify, Numberify, Boolify)
2. Value Synonym Mapping
3. Property Renaming, Remapping and Pruning
4. Null pruning
5. Property path value-type filtering (i.e. type enforcement)
6. Static value injection




###### Extensibility

Sandwiched around this pipeline, the end user can supply their own
transformations- enabling the user to do any preprocessing they like,
or any post processing, and by sandwiched, I do indeed mean that they
can provide a function that takes a clojure map and return a clojure
map before the 6 steps above run, and after the 6 steps above run.

It's important to note that *Every step in the pipeline accepts a map and returns a map, nil, or false*

This has two important implications:

1. If `nil` or `false` is returned, it's assumed that this element should be filtered our
and not make it into the final data set.
2. A user should be careful when supplying his own transformation
functions to ensure they do not return nils or falses when they mean
to take no action.


During this transformation phase, there are a number of places that a
user may want to inject their own custom transformations, as this is
the prime time to specify transformation functions that operate on and
return json map every step of the way.


-- Pre denorm
8. Property Name Hoisting
-- Post denorm
8. Property Name special character handling -- move out?
sh
