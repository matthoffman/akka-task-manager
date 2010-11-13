This is an exercise in using Scala and actors in general, and Akka in particular.  So, let's start with a caveat:  
I'm no expert in any of those things.  But as an excuse to use all three, I thought I'd try re-implementing something 
in a domain I'm familiar with. So this is a simple task management framework using Akka.

Conceptually, this is just raising the level of abstraction one tier higher. This framework deals with Tasks, which 
are discrete units of work.  Tasks can be broken down into child tasks, and those child tasks can be executed in 
parallel or in serial, at the discresion of the parent task.  Tasks then end up being a tree -- there's a root task, 
and any number of child tasks underneath it. 
Typically, worker threads (which could be on the same machine, or on separate machines) poll the available tasks 
until they find one they want to work on, and then they attempt to execute it.  In order to make sure that each task
is executed only once at any given time, worker threads attempt to "check out" the task.  The task (represented by an 
Actor) allows only one checkout at a time.

Note: there's nothing keeping us from extending this model so that a task can be executed by several threads at once -- 
a speculative execution model.  But for simplicity's sake, I've kept it one-to-one for the moment.


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
