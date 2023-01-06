package com.linecorp.kotlinjdsl.query.clause.hint

/**
 * Internal Only
 * Don't use this directly because it's an **INTERNAL**.
 * It does not support backward compatibility.
 */
interface SqlQueryHintClause<Q> {
    fun apply(query: Q)
}
