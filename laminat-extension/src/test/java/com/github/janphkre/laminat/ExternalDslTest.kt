package com.github.janphkre.laminat

import au.com.dius.pact.consumer.dsl.PactDslRequestWithPath
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.external.PactJsonifier
import au.com.dius.pact.model.ProviderState
import com.github.janphkre.laminat.dsl.array
import com.github.janphkre.laminat.dsl.duplicate
import com.github.janphkre.laminat.dsl.getAllPacts
import com.github.janphkre.laminat.dsl.obj
import com.github.janphkre.laminat.dsl.pact
import com.github.janphkre.laminat.dsl.request
import com.github.janphkre.laminat.dsl.response
import com.github.janphkre.laminat.dsl.stringMatcher
import com.github.janphkre.laminat.dsl.stringType
import org.junit.Assert
import org.junit.Test
import java.io.File

class ExternalDslTest {

    object TestRequests {

        val initialRequest get() = request {
            given("SUCCESS")
                .uponReceiving("GET testRequest")
                .method("GET")
                .path("test/path")
                .headers(defaultRequestHeaders)
        }

        private val defaultRequestHeaders = hashMapOf(
            Pair("We", "will have to see about this!")
        )

        private fun request(initializer: PactDslWithProvider.() -> PactDslRequestWithPath): PactDslRequestWithPath {
            return request("testconsumer", "testproducer", initializer)
        }
    }

    object TestResponses {

        private val nullableErrorType: String? = "FATAL"
        private val nullableExampleString: String? = "NullableExampleString"

        val initialResponse by response { this }

        val errorResponse by response {
            stringMatcher("errorType", ".*",
                nullableErrorType
            )
                .array("messages") {
                    obj {
                        decimalType("opacity", 0.9)
                        stringType("message", "Error messsage.")
                    }
                        .obj {
                            decimalType("opacity", 0.3)
                            stringType("message", "Info messsage.")
                        }
                }
                .obj("exampleObj") {
                    stringType("exampleString",
                        nullableExampleString
                    )
                }
        }
    }

    object TestPacts {

        val initialPact by pact {
            TestRequests.initialRequest
                .willRespondWith()
                .status(200)
                .headers(defaultResponseHeaders)
                .body(TestResponses.initialResponse)
        }

        val errorPact by duplicate(
            ProviderState("ERROR"),
            { initialPact }) {
            status(500)
                .headers(defaultResponseHeaders)
                .body(TestResponses.errorResponse)
        }

        private val defaultResponseHeaders = hashMapOf(
            Pair("We", "will have to see about this as well.")
        )
    }

    private val expectedPact = "testconsumer:testproducer.json"

    @Test
    fun externalPact_buildJson_correctlyBuilt() {
        PactJsonifier.generateJson(listOf(TestPacts.initialPact), File("pacts"))
        val outputPactFile = File("pacts/$expectedPact")
        Assert.assertTrue("Pact was not generated!", outputPactFile.exists())

        val outputPact = readFile(outputPactFile)
        val expectedPact = readFile(File("src/test/assets/$expectedPact"))
        Assert.assertEquals("Generated pact does not match expectations!", expectedPact, outputPact)
    }

    @Test
    fun externalPact_collectAllJson_AllItemsReturned() {
        val pactList = TestPacts.getAllPacts()
        Assert.assertEquals("The reflective collect did not grab all pacts!", 2, pactList.size)
        Assert.assertTrue("", pactList.any {
            it.requestResponseInteractions.first().displayState() == "ERROR"
        })
        Assert.assertTrue("", pactList.any {
            it.requestResponseInteractions.first().displayState() == "SUCCESS"
        })
    }
}