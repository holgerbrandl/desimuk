package org.kalasim.test

import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.junit.Assert
import org.junit.Test
import org.kalasim.*
import org.kalasim.misc.asCM

class RequestTests {

    @Test
    fun `preemptive resources should bump claims`() {
        // see version 19.0.9  2019-10-08 in salabim change log for code snippets

        /**
        if the component has to start all over again (hold(1)) if it is bumped:
        def process(self):
        prio = sim.Pdf((1,2,3), 1)
        while True:
        yield self.request((preemptive_resource, 1, prio)
        yield self.hold(1)
        if self.isclaiming(preemptive_resource):
        break
        self.release(preemptive_resource)
         */


        createSimulation {
            val resource = Resource(preemptive = true)

            val prioPDF = EnumeratedDistribution(listOf(1, 2, 3).map { it to 1.0 / 3.0 }.asCM())

            object : Component() {

                override fun process() = sequence {
                    while (true) {
                        request(resource withQuantity 1 andPriority prioPDF.sample())
                        hold(1)
                        if (!isClaiming(resource)) {
                            break
                        } else {
                            release(resource)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `it should release resourced when terminating`() = createTestSimulation(true) {

        // see bank office docs: By design resources that were claimed are automatically
        // released when a process terminates

        val resource = Resource()

        object : Component("foo") {

            override fun process() = sequence {
                request(resource)
                hold(1)
                printTrace("finished process, terminating...")
            }
        }

        run(10)

        resource.claimers.size shouldBe 0
    }

    @Test
    fun `preemptive resources should bump components depending on priority`() = createTestSimulation(true) {

        // see bank office docs: By design resources that were claimed are automatically
        // released when a process terminates

        val resource = Resource("repair", preemptive = true, capacity = 2)

        class BumpComponent(
            name: String,
            val preRequestHold: Int,
            val postRequestHold: Int,
            val requestPriority: Int? = null,
            val failOnBump: Boolean = false
        ) : Component(name) {

            override fun process() = sequence {
                hold(preRequestHold)

                if (requestPriority != null) {
                    request(resource withPriority requestPriority)
                } else {
                    request(resource)
                }

                hold(postRequestHold)

                if (isBumped(resource)) {
                    printTrace("got bumped from $resource")
                    if (failOnBump) Assert.fail()
                    return@sequence
                }

                printTrace("finished process, terminating...")
            }
        }

        val tpTool = BumpComponent("top-prio tool", 0, 100, requestPriority = 100)
        val lpTool = BumpComponent("low-prio tool", 0, 5)
        val mpTool = BumpComponent("mid-prio tool", 10, 5)
        val hpTool = BumpComponent("high-prio tool", 11, 5, requestPriority = 3)
        val hpTool2 = BumpComponent("high-prio tool 2", 12, 5, requestPriority = 3)

        run(10)

        // it should have auto-release the resource by now
        resource.claimers.size shouldBe 1
        lpTool.isRequesting shouldBe false
        lpTool.isData shouldBe true
        lpTool.requests.shouldBeEmpty()

        run(5)

        // at this point hp-tool should have bumped mpTool
        mpTool.isBumped(resource) shouldBe true
        mpTool.isData shouldBe true

        hpTool.isScheduled shouldBe true
        hpTool2.isRequesting shouldBe true

        resource.requesters.contains(hpTool2) shouldBe true
        resource.claimers.contains(hpTool) shouldBe true


        run(5)
        hpTool.isData shouldBe true
        hpTool2.isScheduled shouldBe true

        resource.requesters.size shouldBe 0
        resource.claimers.contains(hpTool2) shouldBe true
        resource.claimers.contains(tpTool) shouldBe true
        resource.claimers.size shouldBe 2
    }

    @Test
    fun `null prio requests should not bump each other`() = createTestSimulation(true) {
        val r = Resource(preemptive = true)

        class BumpComponent : Component() {

            override fun process() = sequence {
                request(r)
                hold(5)
                printTrace("finished process, terminating...")
            }
        }

        val bc1 = BumpComponent()
        val bc2 = BumpComponent()

        run(3)

        bc1.isScheduled shouldBe true
        bc2.isRequesting shouldBe true
    }

    @Test
    fun `it should not bump regular non-preemptive resources`() = createTestSimulation(true) {
        val r = Resource()

        class BumpComponent : Component() {

            override fun process() = sequence {
                request(r)
                hold(5)
                printTrace("finished process, terminating...")
            }
        }

        val bc1 = BumpComponent()
        val bc2 = BumpComponent()

        run(8)

        bc1.isData shouldBe true
        bc2.isScheduled shouldBe true
    }

    @Test
    fun `it should reevaluate requests upon capacity changes`() {

        class Customer(val clerk: Resource) : Component() {

            override fun process() = sequence {
                hold(duration = 5.0)

                request(clerk)
                hold(duration = 2.0, priority = 3)
                release(clerk)

                passivate()
                hold(duration = 5.0)
            }
        }

        createSimulation {
            val clerk = Resource(capacity = 0)

            val customer = Customer(clerk)

            run(8)
            clerk.capacity = 1.0

            run(10)
            customer.activate()

//            sequence<Component>{
//                with(customer){
//                    hold(3)
//                }
//            }.toList()

            run(10)

            // how long was the component in passive state
            customer.statusMonitor.printHistogram()
//                println(customer.statusMonitor[ComponentState.PASSIVE])

            customer.statusMonitor.total(ComponentState.PASSIVE) shouldBe 8.0
        }
    }

    @Test
    fun `it should auto-release resources in builder`(){

        createSimulation(true) {
            val r = Resource()

            object : Component(){
                override fun process() = sequence {
                    hold(2)

                    request(ResourceRequest(r)){
                        hold(1)
                    }
                }
            }

            println(toString())

            run(5)

            println(toString())

            r.claimers.isEmpty() shouldBe true
            r.requesters.isEmpty() shouldBe true
        }
    }
}


