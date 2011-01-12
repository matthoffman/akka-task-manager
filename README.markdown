h1. Akka Task Framework

h2. Background

This is an exercise in using Scala, actors, and Akka.  So, let's start with a caveat:  
I'm no expert in any of those things.  But as an excuse to use all Scala, actors, and Akka, I thought I'd try 
re-implementing something in a domain I'm familiar with. So this is a simple task management framework using Akka.

Conceptually, this is just raising Akka's level of abstraction one tier higher. This framework deals with Tasks, which
are discrete units of work.  For ours purposes, Tasks are defined in the following way: 

* Each Task has a "worker", which defines the work to be done.  In this implementation, that's a block. 
* Each Task has a state, which is one of "ready", "executing", "successful", or "failed".
* Each Task can have children, which are themselves Tasks.  The parent Task specifies the following things about its children:
  ** can child tasks be executed before the parent completes? 
  ** should the children be executed in parallel (with one another), or in serial?
  ** does a failure in a child task equal a failure in this task?
* Each Task can have an arbitrary set of properties.  At the moment, I'm defining all properties as strings, for both
* keys and values, to avoid dealing with serialization.  But it seems reasonable that we could piggyback Akka's serialization
* framework for properties, so that we could attach more complex data to Task objects.

Since Tasks can have children, which can themselves have children, etc., Tasks can be visualized as a tree. In this 
implementation, I call this a Task Graph, mainly because "Task Graph" rolls off the tonge better than "Task Tree". 
However, the latter is more correct, so if pressed, I'm happy to change it. 

Clients of this task management framework (which could be within the same JVM, or in separate JVMs using Akka's Remote
Actors) poll the available tasks until they find one they want to work on, and then they attempt to execute it.  In 
order to make sure that each task is executed only once at any given time, task executor threads attempt to "check 
out" the task. When it is done working on the task, it checks it back in. When checking a task back in, an executor 
can mark the task as "successful", "failed", or "ready" (that is, not yet finished, and ready for someone else to 
check out).

In this implementation, each task is represented by an Actor. They could be represented in a persistent map instead, 
with access to the whole map guarded by an actor.  I'm open to suggestions. 

h2. Limitations
This particular implementation allows only one checkout at a time.  There's nothing keeping us from extending this 
model so that a task can be executed by several threads at once -- a speculative execution model.  But for 
simplicity's sake, I've kept it one-to-one for the moment.


h2. Example

(code example coming; for now, this is a theoretical thought example.  See the tests for where we are with the actual
code thus far)

So, let's take a trivial example:

Say we want to search for all the instances of the word "foo" in a large document.  And lets say, for some reason, we
wanted to split this up over multiple threads, and possibly over multiple machines.

We can define a task, called "search for word".  The worker for this task might do something like this: 

Count the number of lines in the document.
For each 1000 lines of the document, create a new child task.  Give the child task a property that tells it which line 
to start on, and a worker that says "start at line START\_LINE, go 1000 lines, and count all instances of the word 'foo'.
Print the results to the console." 

Then, assuming we have task executor threads running, it will find this parent task and execute it.  The parent task will 
count the lines in the task and create a set of child tasks, one per every 1000 lines in the document.  Other task 
executor threads will check out these child tasks and execute them.  We will start to see counts being printed to the 
console.  Totalling those results will give us the total number of occurrances of the word "foo".

"What?" you say.  "Totalling the results ourselves?  Doesn't the framework do that for us?"  
Er...no.  At the moment, the framework does not allow you to for pass data between tasks, with the exception of creating 
properties in new tasks or tasks that are currently checked out.  So there's no way for the children to pass their 
results back to the parent.  That's mainly because this could be a whole lot of data, and I'm not yet sure of a scalable, 
sane way of doing that.  Perhaps Akka's pluggable serialization mechanism could be brought to bear here? 

Different problem domains require different data-transfer strategies, and there's no reasonable on-size-fits-all 
option that I know of.  But ideally we could offer some pluggable options. 
In the meantime, though, it is not a full-on Map-Reduce framework. With a bit of extension, algorithms like MapReduce 
could be modeled with this framework. 

