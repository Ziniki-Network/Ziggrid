# Ziggrid

Ziggrid is a [Functional Reactive Programming](http://en.wikipedia.org/wiki/Functional_reactive_programming) Paradigm for Big Data.

Ziggrid is essentially a veneer on top of [Couchbase](http://www.couchbase.com/) that interprets a Ziggurat model definition and monitors and updates the key value store.

This repository reflects the state of Ziggrid demonstrated at the Couchbase Conference in San Francisco, 2013-09-13.

## Ziggrid Processing Model

Ziggrid uses a Functional Reactive Programming model to define an incremental, multi-level data analysis "Ziggurat" that builds from simple _analytic events_ up to more complex observations.

At each level, the model specifies a transformation from a set of input objects (either primitive events or intermediate results) to produce a new object.

Each such specification is written in a functional fashion that is independent of the observed current state of the system and merely considers what would happen
if the specified objects were the only ones in the system.  Internally, Ziggrid manages all the remaining dependencies and ensures that they are correctly handled.

The design of Ziggrid is intentionally to support _incremental_ updating of the entire Ziggurat upon the arrival of new data, without the need to repeat previous computations.
This is obviously a good fit for the semantics of Couchbase _views_ which use incremental map/reduce over the set of available objects.

Ziggrid uses a _functional_ programming model because the semantics of functional programming languages make it inherently easy to transform them into other programming
languages and do complex metaprogramming.  This is not, for example, possible with javascript or ruby programs, which must be executed in order to understand the complexity
of what they are trying to express.  The Ziggrid progamming "language" is deliberately simple and consists of discrete, powerful, complex operations that enable the engine
to make intelligent decisions about how to implement them.

## Run the Baseball Sample Demo

Everything needed to run the demo presented at the Couchbase Conference in San Francisco (except Couchbase itself) has been open sourced and is available from GitHub.
You can download and run this code for yourself.

### Setting up

In order to set up the Ziggrid back end, you need to clone this repo:

```sh
% git clone git@github.com:Ziniki-Network/Ziggrid.git
```

and move into it.

```sh
% cd Ziggrid
```

You need to build the Ziggrid back end, which is written in Java.  You will need to have a Java 1.6 or Java 1.7 JDK installed.
Then build it:

```sh
% scripts/build.sh
```

Obviously, you need to have an instance of Couchbase running somewhere, and you need to know what its "admin" port is, i.e. "http://host:8091/".
We use this to configure Couchbase and run the demo, but we assume that the system is set up in the "standard" way.  If this is not the case, you may encounter issues.
You will also need the administrator username and password for your instance.

We then need to create a bucket for the demo.  This is called "ziggrid-baseball" and should not clash with anything you already have.
NOTE that in order to run the demo multiple times, we _delete_ this bucket as well as creating it.
This script is named to remind you of this fact.

```sh
% scripts/clean-baseball.sh <couchUrl> <admin> <password>
```

The `couchUrl` here is just the host and port, for example, `localhost:8091`.
By default, this script is set up to use an unreplicated bucket of size 1301MB, which has been seen to be enough here, although your mileage may vary.  You may specify two
additional parameters to set the RAM size and number of replicas for the bucket if desired.
You will need to ensure that your Couchbase cluster has enough available RAM.

The next step is to start the baseball generator running.  You can either do this in the background or use two shell windows to allow you to also run the Ziggrid node.

```sh
% scripts/baseball_generator.sh <couch url> &
```

This is set to a default delay per game of 25ms, which we have determined to be optimal for the demo we gave at the Couchbase conference. Again, depending on the hardware you
are using, and the speed of the visualization you want to see, you may choose a different delay (in ms) by specifying it as the second argument on the command line.
Because this is a demo, the script assumes that you are running on the local machine and expects everything to be "localhost".  If this is not correct, you can provide
an additional argument which is the IP address or DNS name by which the generator should describe itself.

Finally, start up the ziggrid node itself:

```sh
% scripts/baseball_ziggrid.sh <couch url>
```

Again, there is the option to provide an additional argument which is the IP address at which the service should be discoverable.

### The actual demo

Once you have everything installed, you should be able to access the demo by entering the following url in your browser:

```uri
http://localhost:10051/
```

To control the generator, use the "play" and pause buttons at the bottom of the screen (in the progress bar).  Data for seven seasons (2006-2012) has been provided, and,
depending on the delay you set and the hardware you have at your disposal will take anywhere from 20 minutes to a few hours to install.  You can stop the flow of new events at any time
by pushing the "stop" button, although the Ziggurat will continue to process higher levels for some time after new events have stopped coming in.

The majority of raw events generated are "plate appearances"; that is, the outcome of one particular batter appearing at the plate with a defined outcome.  There are also
events recorded for the result of each game and some other events may be generated from time to time.

The first page of the demo shows various aggregated statistics calculated from this input data.

The left hand panel aggregates the game results to show the Win-Loss records for all the teams.  These are then sorted in the browser to display the tables.

The middle panel shows three player leaderboards across all teams for the selected season.  The first table is batting average, the second is production (rbis divided by
rbi opportunities) and the third is home runs.  For all three of these, the sorting and filtering is done on the back end, and the front end merely formats and displays
the results.

The right hand panel shows the averages for all the players selected for the all star team for the given season.  Again, the sorting for this table is done on the front end.

Mousing over the name of a player in any of these tables gives a player profile with more detailed information about their statistics for the season.  While open, this
profile is "live" and will continue to update with each plate appearance.  Note that for any given player the updates will only occur when one of their teams games is being played,
which is only 1 in 15, so they will be relatively static most of the time - you can expect about one update every 10s or so.

### Configuration

You can run the demo with everything on a single machine, but the performance will probably not be great.
The most distracting part of poor performance is that different levels of the Ziggurat become out of sync.

For the Couchbase conference, we ran everything on Amazon EC2.
We used a four-node Couchbase cluster, all running on cc2.8xlarge boxes.
The Ziggrid processor was run standalone on a similar cc2.8xlarge box.
The Baseball data generator was run on an m1.small box; it doesn't really do any significant amount of work and doesn't even exercise this small box.

Note: the cc2.8xlarge boxes are not available in all regions.

A Chef cookbook with recipes for the Ziggrid processor and the Baseball data generator is available at http://community.opscode.com/cookbooks/ziggrid. 

## Ziggrid Operations

A Ziggrid definition consists of a network (technically, it must be an upward-directed, acyclic graph) of operations, each of which combines multiple input objects into
a single result object.

All objects in a Ziggrid database must be typed, and the type definition must be provided in some form along with the objects.

The Ziggrid model definition must include descriptions of all the object types, including field names and their types.

The Ziggrid operations are all very high-level, describing how the output object is to be constructed.

### Enhancement

The simplest operation form is a mapping from a single input object to a single output object of a different form.
This is primarily needed in order to "clean up" incoming data events.
Each input event can be any valid JSON object, but these are often not very amenable to data analysis and summary, and it is easier to initially map the object into a
more amenable form before processing it.

An enhancement operation specifies the input and output types and then specifies for each (or a subset of) the fields in the output object an expression indicating how it
can be uniquely inferred from the input object.

### Summary

The main operation when traversing the levels of the Ziggurat is the _reduction_ or _summary_ operation.
Basically, a _summary_ operation is responsible for taking many lower-tier objects and combining them into a single result object.
Note that it is possible to summarize into a single object definition from multiple event definitions, and it is also possible to specify multiple rules for reducing
from a single event type into a summary object - usually because they will apply to different matching conditions.
Note, however, that all such reduction rules must be congruent in order for the overall model to be accepted.

Each summary rule specifies two distinct clauses.  The first clause _matches_ the incoming event object against a (potentially existing) summary object.
If no such summary object exists, one is created with the appropriate fields to ensure it matches and the corresponding "null" value fields for the operations to be performed.

Note that because the transformation is agnostic to the order in which the events are processed, all the nested reduction operations in the Ziggurat definition must be
both associative and commutative in terms of their implied implementation.  This is discussed at greater length in the white paper.

Summary objects are automatically rolled up based on the matched fields.  These rolled up versions are given the names "-key0", "-key1", etc. based on the number
of matched items that remain in the key.

### Sorting

Given a set of summary definition objects, it may be desirable to sort them according to some criteria.
The leaderboard definition enables a Ziggrid summary object to be sorted by an expression over a given set of fields, and to filter out results based on other fields.

### Snapshots and Decay

In general, the incremental accumulating nature of Ziggurats is to be desired.  However, there are times when it is interesting to preserve history: what, for example,
did a particular player's profile look like on a given day?
In order to support this, Ziggrid provides the notion of a "Snapshot" object which can provide a limited roll-up of a specified group of summary objects.
In the baseball example, this is used to calculate the "clutchness" of a player over the course of the season.
The summary objects are grouped by season, player and day-of-year; the snapshot rolls up all of these objects based on a final "day-of-year" (the one being currently
updated) and then stores the result object.

Snapshots also permit a "decay" function to be applied to the older results in a set, thus making more recent activity more interesting.
This is used to define the player "hotness".

### Machine Learning

There are a class of algorithms optimistically referred to as "machine learning" algorithms.
Even the most fancy of these, such as neural networks, do not do any actual "learning" but simply operate over very large assembled data sets.
Generally, the implementations of these algorithms use a "training set" followed by a "live set" of data which is then processed.

Ziggrid, being incremental, uses the same set of data for both purposes, but instead tries to distinguish between "outcome data" and "transient data".
Basically, the "outcome data" is data for which a result is known, while "transient data" is data about which an outcome should be predicted.

Ultimately, a number of machine learning algorithms are proposed, but for now, the only implemented one is correlation.

#### Correlation

The correlation operation uses a naive Bayes engine to correlate a "complete" set of results against a subset to recognize a correlation.
In the example, we divide all plate appearances into "situations" using an enhancement operation.
The situation attempts to identify the salient features of the at-bat: the inning, the score, the runners on base and what happened.
It then correlates this against the ultimate outcome of the game to identify whether this action in this situation is generally good, or generally bad.
Once the correlation from situation to outcome has been established _globally_ it may then be considered for any given player: how often did they find
themselves in a particular situation, and did they more often correlate with the "good" outcome or the "bad"?
This can then be reduced down to a single number representing their overall "clutchness" from 0 to 1.

## Developing and Defining Ziggurats

The Ziggurat definition for the baseball sample can be found under `SampleData/buckets/ziggrid-baseball`.
Because of the way in which Couchbase views are defined, the model here is broken up into a number of pieces in different directories, but essentially this is a single
list of definitions across all the directories and can be broken up in any manner within a single bucket.

The definitions here are all specified in JSON.  There is nothing particularly significant about this; the choice was made because it is simple to parse and use in
both JavaScript and Java code, it is easily serializable and readable, and it has very, very clear semantics.  In the demo, the model is only read and interpreted in Java,
but the medium-term intention is to have a version of Ziggrid which runs entirely in the browser and makes it very easy to develop Ziggrid models, and in the longer
term there is a desire to build a JavaScript UI which constructs Ziggrid models (and tests them) visually.

## Lessons Learned

The original design (and still the preferred design) for Ziggrid is as a very thin layer on top of Couchbase.
This thin layer would simply interpret the FRP model definitions and built an appropriate set of interconnected views, with the minimal amount
of additional infrastructure needed to propagate updates between views (see [Cloudant's Chained Map Reduce](http://examples.cloudant.com/sales/_design/sales/index.html)
for example).

Unfortunately, at the time of the demo, it was impossible to get this to perform adequately, because the underlying view technology in Couchbase is neither
sufficiently performant nor scalable to support the workload we were putting through it.  Consequently, all the operations other than the sorting in the leaderboard
were rewritten natively in Java using the Key/Value store as an intermediate persistent store, with temporary results being kept in memory and using the KV store as a
write-thru cache for persistence.

At the time of writing, this approach is also not-quite-scalable, because in order to scale horizontally, it requires multiple Ziggrid nodes.  This would further
require that the TAP messages coming from Couchbase server were "fairly" distributed across them.  While it is relatively easy to distribute the messages
randomly, this does not help with scaling very much since there can be a considerable duplication of effort in this case.  However, it is our intent to add this level
of scaling in the near future.

## Alternative Implementations

Although neither described nor planned, it is possible to use any large data store as a backend underneath Ziggrid.
For example, most large data systems use a version of a SQL database to do large data analysis.
It is possible and reasonable to define a SQL Star Schema from a Ziggurat definition and to automatically generate queries to do the roll ups.
One of the problems with this approach is that the same subqueries are performed over and over, leading to the use of materialized views to improve performance.
This approach, while usually considered a hack, can be used consistently through Ziggrid and thus made consistent.

In the NoSQL world, the most popular approach to data processing is Hadoop.
It would likewise be possible to generate appropriate Hadoop programming code from a Ziggurat definition.
Again, the problem with doing this is that Hadoop runs throw away all previous results and are thus inherently wasteful.

The Storm Analytics Package, originally part of Twitter, is another mechanism for doing data analysis and, it would seem, could be configured using Ziggrid.

## Validity

Because this is currently essentially "demo" code, there is no automated testing for validity of outputs compared to inputs and, because of the issues raised
above with not using Couchbase views, there are in fact some known bugs around deleting and resending events (they will be double counted).  It is our intent to
add automated simple case testing over the coming months.

## License

Ziggrid is Open Source under an MIT license.
