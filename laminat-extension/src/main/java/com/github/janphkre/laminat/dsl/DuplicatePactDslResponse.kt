package com.github.janphkre.laminat.dsl

import au.com.dius.pact.consumer.dsl.PactDslRequestWithPath
import au.com.dius.pact.consumer.dsl.PactDslResponse
import au.com.dius.pact.consumer.dsl.PactDslWithState
import au.com.dius.pact.model.RequestResponsePact

internal class DuplicatePactDslResponse : PactDslResponse(null, null) {

    override fun given(state: String?): PactDslWithState {
        throw UnsupportedOperationException("Chaining interactions is not supported for duplicates.")
    }

    override fun given(state: String?, params: MutableMap<String, Any>?): PactDslWithState {
        throw UnsupportedOperationException("Chaining interactions is not supported for duplicates.")
    }

    override fun uponReceiving(description: String?): PactDslRequestWithPath {
        throw UnsupportedOperationException("Chaining interactions is not supported for duplicates.")
    }

    override fun toPact(): RequestResponsePact {
        throw UnsupportedOperationException("Duplicate pact responses may not be used in pacts directly. Use toResponse() instead.")
    }
}