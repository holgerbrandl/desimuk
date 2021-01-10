package org.kalasim.examples.analysis

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.kalasim.*


// https://proandroiddev.com/kotlin-coroutines-channels-csp-android-db441400965f
// * You can think of a channel as a pipe between two coroutines. T


class MyEventConsumer : EventConsumer {
    // think of it as a  Non Blocking Queue
    val ordersChannel = Channel<Event>()

    override fun consume(event: Event) {
        GlobalScope.launch {
            ordersChannel.offer(event)
        }
    }
}


val tl = MyEventConsumer()

// start a log consumer
GlobalScope.launch {
    tl.ordersChannel.receiveAsFlow().filter {
        it.curComponent?.name == "ComponentGenerator.1"
    }.collect {
        println(it)
    }
}

// create simulation with no default logging
val sim = createSimulation {
    ComponentGenerator(iat = 1.asDist()) { Component("Car.${it}") }
}

// add custom log consumer
sim.addEventConsumer(tl)

// run the simulation
sim.run(100)
