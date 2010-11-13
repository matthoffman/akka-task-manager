This is an exercise in using Scala, actors, and Akka in particular.  So, let's start with a caveat:  I'm no expert in any
of those things.  But as an excuse to use all three, I thought I'd try re-implementing something in a domain I'm familiar
with.

So, this is a simple task management framework using Akka.

Conceptually, this is just raising the level of abstraction another level. This framework deals with Tasks, which are
discrete units of work.  These units of work can be made to execute in serial or parallel, and can have parents and children.

Tasks, in this definition, have a worker (the thing that defines the work to be done), a state (ready, executing,
complete, failed), and an arbitrary set of properties.  At the moment, I'm defining all properties as strings, for both
keys and values, to avoid dealing with serialization.  But it seems reasonable that we could piggyback Akka's serialization
framework for properties.

The Task defines a 


So, let's take a trivial example:

Say we want to search for all the instances of the word "foo" in a large document.  And lets say, for some reason, we
wanted to split this up over multiple threads.

We can define a task, called "search for word".

, and possibly over multiple machines.


There are some key problems I'm not handling in this framework (yet).  One is distributing the data: we can move execution
to
