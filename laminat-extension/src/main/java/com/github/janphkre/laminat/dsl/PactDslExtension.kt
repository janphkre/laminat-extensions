package com.github.janphkre.laminat.dsl

import au.com.dius.pact.consumer.ConsumerPactBuilder
import au.com.dius.pact.consumer.dsl.DslPart
import au.com.dius.pact.consumer.dsl.PactDslJsonArray
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.consumer.dsl.PactDslRequestWithPath
import au.com.dius.pact.consumer.dsl.PactDslResponse
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.external.PactBuildException
import au.com.dius.pact.model.ProviderState
import au.com.dius.pact.model.RequestResponseInteraction
import au.com.dius.pact.model.RequestResponsePact
import kotlin.reflect.full.memberProperties

fun pact(initializer: () -> PactDslResponse): Lazy<RequestResponsePact> {
    return lazy { initializer.invoke().toPact() }
}

fun duplicate(
    state: ProviderState,
    baseInitializer: () -> RequestResponsePact,
    responseInitializer: PactDslResponse.() -> PactDslResponse
): Lazy<RequestResponsePact> {
    return lazy {
        internalDuplicate(
            state,
            baseInitializer(),
            responseInitializer
        )
    }
}

fun duplicateFromRequest(
    state: ProviderState,
    baseInitializer: () -> PactDslRequestWithPath,
    responseInitializer: PactDslResponse.() -> PactDslResponse
): Lazy<RequestResponsePact> {
    return lazy {
        internalDuplicate(
            state,
            baseInitializer().willRespondWith().toPact(),
            responseInitializer
        )
    }
}

fun Any.getAllPacts(): List<RequestResponsePact> {
    return this::class.memberProperties.filter {
        it.returnType.classifier == RequestResponsePact::class
    }.mapNotNull { it.getter.call(this) as? RequestResponsePact }
}

private fun internalDuplicate(state: ProviderState, basePact: RequestResponsePact, responseInitializer: PactDslResponse.() -> PactDslResponse): RequestResponsePact {
    val responseDsl = DuplicatePactDslResponse()
    responseDsl.responseInitializer()
    return RequestResponsePact(
        basePact.provider,
        basePact.consumer,
        basePact.requestResponseInteractions.map { baseInteraction ->
            RequestResponseInteraction(
                "${baseInteraction.description} ${state.name}",
                listOf(state),
                baseInteraction.request,
                responseDsl.toResponse()
            )
        }
    )
}

fun request(consumerName: String, providerName: String, initializer: PactDslWithProvider.() -> PactDslRequestWithPath): PactDslRequestWithPath {
    return initializer.invoke(ConsumerPactBuilder(consumerName).hasPactWith(providerName))
}

fun response(initializer: PactDslJsonBody.() -> DslPart?): Lazy<DslPart?> {
    return lazy {
        initializer.invoke(PactDslJsonBody())
    }
}

fun PactDslJsonBody.stringMatcher(name: String, regex: String, value: String?): PactDslJsonBody {
    if (value != null) {
        return stringMatcher(name, regex, value)
    } else {
        throw PactBuildException("Expected field $name to be set!")
    }
}

fun PactDslJsonBody.stringType(name: String, value: String?): PactDslJsonBody {
    if (value != null) {
        return stringType(name, value)
    } else {
        throw PactBuildException("Expected field $name to be set!")
    }
}

fun PactDslJsonBody.obj(name: String, initializer: PactDslJsonBody.() -> DslPart?): PactDslJsonBody {
    val result = this.`object`(name)
    initializer.invoke(result)
    result.closeObject()
    return this
}

fun PactDslJsonBody.array(name: String, initializer: PactDslJsonArray.() -> Unit): PactDslJsonBody {
    val result = this.array(name)
    initializer.invoke(result)
    result.closeArray()
    return this
}

fun PactDslJsonBody.minArrayLike(name: String, count: Int, exampleInitializer: PactDslJsonBody.() -> Unit): PactDslJsonBody {
    val result = this.minArrayLike(name, count)
    exampleInitializer.invoke(result)
    result.closeObject()!!.closeArray()
    return this
}

fun PactDslJsonArray.obj(initializer: PactDslJsonBody.() -> Unit): PactDslJsonArray {
    val result = this.`object`()
    initializer.invoke(result)
    result.closeObject()
    return this
}