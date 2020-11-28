# Component

See https://www.salabim.org/manual/Component.html

## Process Control

### Wait

This functionality is similar but not equal to the waitevent and queueevent
methods in SimPy2.
The method allows a process to wait for a any if all of (number of) certain
value(s) of a so called state.
A state has a value and each time this value change, all components waiting for
this state are checked.
The class State has a number of methods, which allow to control the state:
  set(value) sets the value. Normally used without argument, in which
          case True will be used.
  reset() resets the value to False=
  trigger(value) sets the value (default True), triggers any components waiting,
    and then immediately resets to a given value (default False).
    optionally, the number of components to be honored with the trigger
    value may be limited (if used, most like 1).
The current value of a state can be retrieved with
  get() or by directly calling the state.
  So, e.g. dooropen.get() or dooropen()
On top of that, the queue of waiters may be accessed with
  State.waiters()
And there is a monitor to register the the value over time, called State.value .
.
The waiters queue and value will be monitored by default.

Components can wait for a certain value of a state (or states) by
  yield self.wait(dooropen)
or
  yield self.wait((dooropen,False))
And for several states at one time:
  yield self.wait(frontdooropen,backdooropen) to test for fontdooropen OR backdooropen
or
  yield self.wait(dooropen,lighton,all=True) to test for dooropen AND lighton
It is also possible to check for several values of one state:
  yield self.wait((light,'green'),(light,'yellow')) tests for lighth is green of yellow

The method wait can have an optional timeout parameter.
If they are timed out, Component.fail() is True.

If a component is in a wait state, the status waiting will be returned.
In order to test for this state, use either
  c.state() == waiting
or
  c.iswaiting()
See the example script demo wait.py for a demonstration of the trigger and time out functionality.


## Execution Order

Order is defined by scheduled time.  To avoid race conditions exeution order be fintuned usin `priority` and `urgent` which are supported for all methods that result in a rescheduling of a component, namely  `wait`, `request`,  `activate` and `reschedule`

## Generator

Def
> A `Component` that contains at least one yield.