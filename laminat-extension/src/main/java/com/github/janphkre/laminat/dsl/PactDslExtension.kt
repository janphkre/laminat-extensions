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

/**
 * Lazily define a pact with the dsl steps reduced by the toPact() method.
 * Pacts should be created lazily to reduce redundancy und unnecessary pact creations.
 *
 * @see RequestResponsePact
 * @author Jan Phillip Kretzschmar
 */
fun pact(initializer: () -> PactDslResponse): Lazy<RequestResponsePact> {
    return lazy { initializer.invoke().toPact() }
}

/**
 * Duplicates a different pact lazily to define a
 * different state and response for it.
 * Both the underlying pact and response are passed in as lambdas to still maintain laziness.
 *
 * @see RequestResponsePact
 * @author Jan Phillip Kretzschmar
 */
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

/**
 * Duplicates a request lazily for a pact to be able to apply minor changes
 * to the request without changing the underlying request and not defining a completely new request.
 * Both the underlying pact and response are passed in as lambdas to still maintain laziness.
 *
 * @see RequestResponsePact
 * @author Jan Phillip Kretzschmar
 */
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

/**
 * Returns the list of pacts that are accessible through getters on this object.
 * This is useful when you define a singleton (kotlin) object which holds all interactions in single
 * pacts as lazy fields. Each interaction can be used individually in tests but still generate
 * tue resulting pact with all interactions.
 *
 * @see RequestResponsePact
 * @author Jan Phillip Kretzschmar
 */
fun Any.getAllPacts(): List<RequestResponsePact> {
    return this::class.memberProperties.filter {
        it.returnType.classifier == RequestResponsePact::class
    }.mapNotNull { it.getter.call(this) as? RequestResponsePact }
}

/**
 * Lazily define a request through a lambda.
 * It may make sense to wrap this in a method of your own inside an superclass to only pass in the
 * lambda and hold the consumerName as well as the providerName in the superclass instead.
 * @see PactDslRqeuestWithPath
 * @author Jan Phillip Kretzschmar
 */
fun request(consumerName: String, providerName: String, initializer: PactDslWithProvider.() -> PactDslRequestWithPath): PactDslRequestWithPath {
    return initializer.invoke(ConsumerPactBuilder(consumerName).hasPactWith(providerName))
}

/**
 * Lazily define a json response with a lambda on a {@see PactDslJsonBody}.
 *
 * @author Jan Phillip Kretzschmar
 */
fun response(initializer: PactDslJsonBody.() -> DslPart?): Lazy<DslPart?> {
    return lazy {
        initializer.invoke(PactDslJsonBody())
    }
}

/**
 * Extension to the default string matcher of PactDslJsonBody to do a NonNull verification
 * for nullable values. It rejects null values by throwing a PactBuildException.
 *
 * @see PactDslJsonBody
 * @author Jan Phillip Kretzschmar
 */
@Throws(PactBuildException::class)
fun PactDslJsonBody.stringMatcher(name: String, regex: String, value: String?): PactDslJsonBody {
    if (value != null) {
        return stringMatcher(name, regex, value)
    } else {
        throw PactBuildException("Expected field $name to be set!")
    }
}

/**
 * Extension to the default string type of PactDslJsonBody to do a NonNull verification
 * for nullable values. It rejects null values by throwing a PactBuildException.
 *
 * @see PactDslJsonBody
 * @author Jan Phillip Kretzschmar
 */
@Throws(PactBuildException::class)
fun PactDslJsonBody.stringType(name: String, value: String?): PactDslJsonBody {
    if (value != null) {
        return stringType(name, value)
    } else {
        throw PactBuildException("Expected field $name to be set!")
    }
}

/**
 * Extension to the default object method of PactDslJsonBody to allow an object to be build
 * in a kotlin dsl style instead.
 *
 * @see PactDslJsonBody
 * @author Jan Phillip Kretzschmar
 */
fun PactDslJsonBody.obj(name: String, initializer: PactDslJsonBody.() -> DslPart?): PactDslJsonBody {
    val result = this.`object`(name)
    initializer.invoke(result)
    result.closeObject()
    return this
}

/**
 * Extension to the default array method of PactDslJsonBody to allow an array to be build
 * in a kotlin dsl style instead.
 *
 * @see PactDslJsonBody
 * @author Jan Phillip Kretzschmar
 */
fun PactDslJsonBody.array(name: String, initializer: PactDslJsonArray.() -> Unit): PactDslJsonBody {
    val result = this.array(name)
    initializer.invoke(result)
    result.closeArray()
    return this
}

/**
 * Extension to the default minArrayLike method of PactDslJsonBody to allow an array to be build
 * in a kotlin dsl style instead.
 *
 * @see PactDslJsonBody
 * @author Jan Phillip Kretzschmar
 */
fun PactDslJsonBody.minArrayLike(name: String, count: Int, exampleInitializer: PactDslJsonBody.() -> Unit): PactDslJsonBody {
    val result = this.minArrayLike(name, count)
    exampleInitializer.invoke(result)
    result.closeObject()!!.closeArray()
    return this
}

/**
 * Extension to the default maxArrayLike method of PactDslJsonBody to allow an array to be build
 * in a kotlin dsl style instead.
 *
 * @see PactDslJsonBody
 * @author Jan Phillip Kretzschmar
 */
fun PactDslJsonBody.maxArrayLike(name: String, count: Int, exampleInitializer: PactDslJsonBody.() -> Unit): PactDslJsonBody {
    val result = this.maxArrayLike(name, count)
    exampleInitializer.invoke(result)
    result.closeObject()!!.closeArray()
    return this
}

/**
 * Extension to the default array method of PactDslJsonArray to allow an object to be build
 * in a kotlin dsl style instead.
 *
 * @see PactDslJsonArray
 * @author Jan Phillip Kretzschmar
 */
fun PactDslJsonArray.obj(initializer: PactDslJsonBody.() -> Unit): PactDslJsonArray {
    val result = this.`object`()
    initializer.invoke(result)
    result.closeObject()
    return this
}