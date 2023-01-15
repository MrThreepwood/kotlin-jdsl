package com.linecorp.kotlinjdsl.test.reactive.querydsl.predicate

import com.linecorp.kotlinjdsl.listQuery
import com.linecorp.kotlinjdsl.query.spec.expression.EntitySpec
import com.linecorp.kotlinjdsl.querydsl.expression.col
import com.linecorp.kotlinjdsl.singleQuery
import com.linecorp.kotlinjdsl.subquery
import com.linecorp.kotlinjdsl.test.entity.order.Order
import com.linecorp.kotlinjdsl.test.entity.order.OrderGroup
import com.linecorp.kotlinjdsl.test.entity.order.OrderItem
import com.linecorp.kotlinjdsl.test.reactive.CriteriaQueryDslIntegrationTest
import com.linecorp.kotlinjdsl.test.reactive.blockingDetect
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

abstract class AbstractCriteriaQueryDslPredicateIntegrationTest<S> : CriteriaQueryDslIntegrationTest<S> {
    private val orderItem1 = orderItem { productName = "test1"; productImage = null; price = 10; claimed = true }
    private val orderItem2 = orderItem { productName = "test1"; productImage = null; price = 20; claimed = false }
    private val orderItem3 = orderItem { productName = "test2"; productImage = null; price = 30; claimed = false }
    private val orderItem4 = orderItem { productName = "test3"; productImage = "image"; price = 50; claimed = false }

    private val order1 = order {
        purchaserId = 1000
        groups = hashSetOf(orderGroup { items = hashSetOf(orderItem1, orderItem2); orderGroupName = "orderGroup1" })
    }
    private val order2 = order {
        purchaserId = 1000
        groups = hashSetOf(orderGroup { items = hashSetOf(orderItem3); orderGroupName = "orderGroup2" })
    }
    private val order3 = order {
        purchaserId = 2000
        groups = hashSetOf(orderGroup { items = hashSetOf(orderItem4); orderGroupName = "orderGroup3" })
    }

    private val orders = listOf(order1, order2, order3)

    @BeforeEach
    fun setUp(): Unit = blockingDetect {
        persistAll(order1, order2, order3)

    }

    @Test
    fun not(): Unit = blockingDetect {
        // when
        val orderIds = withFactory { queryFactory ->
            queryFactory.listQuery {
                select(col(Order::id))
                from(entity(Order::class))
                where(not(col(Order::purchaserId).equal(1000)))
                orderBy(col(Order::id).asc())
            }
        }

        // then
        assertThat(orderIds).isEqualTo(listOf(order3.id))
    }

    @Test
    fun and(): Unit = blockingDetect {
        // when
        val orderItemId = withFactory { queryFactory ->
            queryFactory.singleQuery {
                select(col(OrderItem::id))
                from(entity(OrderItem::class))
                where(
                    and(
                        col(OrderItem::productName).equal("test1"),
                        col(OrderItem::price).equal(10.toBigDecimal())
                    )
                )
            }
        }

        // then
        assertThat(orderItemId).isEqualTo(orderItem1.id)
    }

    @Test
    fun or(): Unit = blockingDetect {
        // when
        val orderItemIds = withFactory { queryFactory ->
            queryFactory.listQuery {
                select(col(OrderItem::id))
                from(entity(OrderItem::class))
                where(
                    or(
                        col(OrderItem::price).equal(10.toBigDecimal()),
                        col(OrderItem::price).equal(20.toBigDecimal())
                    )
                )
            }
        }

        // then
        assertThat(orderItemIds).containsOnly(orderItem1.id, orderItem2.id)
    }

    @Test
    fun equal(): Unit = blockingDetect {
        // when
        val orderItemId = withFactory { queryFactory ->
            queryFactory.singleQuery {
                select(col(OrderItem::id))
                from(entity(OrderItem::class))
                where(col(OrderItem::id).equal(orderItem1.id))
            }
        }

        // then
        assertThat(orderItemId).isEqualTo(orderItem1.id)
    }

    @Test
    fun notEqual(): Unit = blockingDetect {
        // when
        val orderItemId = withFactory { queryFactory ->
            queryFactory.singleQuery {
                select(col(Order::id))
                from(entity(Order::class))
                where(col(Order::purchaserId).notEqual(1000))
            }
        }

        // then
        assertThat(orderItemId).isEqualTo(order3.id)
    }

    @Test
    fun `in`(): Unit = blockingDetect {
        // when
        val orderItemIds = withFactory { queryFactory ->
            queryFactory.listQuery {
                select(col(OrderItem::id))
                from(entity(OrderItem::class))
                where(col(OrderItem::id).`in`(orderItem1.id, orderItem2.id))
            }
        }

        // then
        assertThat(orderItemIds).containsOnly(orderItem1.id, orderItem2.id)
    }

    @Test
    fun lessThanOrEqualTo(): Unit = blockingDetect {
        // when
        val orderItemIds = withFactory { queryFactory ->
            queryFactory.listQuery {
                select(col(OrderItem::id))
                from(entity(OrderItem::class))
                where(col(OrderItem::price).lessThanOrEqualTo(20.toBigDecimal()))
            }
        }

        // then
        assertThat(orderItemIds).containsOnly(orderItem1.id, orderItem2.id)
    }

    @Test
    fun lessThan(): Unit = blockingDetect {
        // when
        val orderItemIds = withFactory { queryFactory ->
            queryFactory.listQuery {
                select(col(OrderItem::id))
                from(entity(OrderItem::class))
                where(col(OrderItem::price).lessThan(20.toBigDecimal()))
            }
        }

        // then
        assertThat(orderItemIds).containsOnly(orderItem1.id)
    }

    @Test
    fun greaterThanOrEqualTo(): Unit = blockingDetect {
        // when
        val orderItemIds = withFactory { queryFactory ->
            queryFactory.listQuery {
                select(col(OrderItem::id))
                from(entity(OrderItem::class))
                where(col(OrderItem::price).greaterThanOrEqualTo(20.toBigDecimal()))
            }
        }

        // then
        assertThat(orderItemIds).containsOnly(orderItem2.id, orderItem3.id, orderItem4.id)
    }

    @Test
    fun greaterThan(): Unit = blockingDetect {
        // when
        val orderItemIds = withFactory { queryFactory ->
            queryFactory.listQuery {
                select(col(OrderItem::id))
                from(entity(OrderItem::class))
                where(col(OrderItem::price).greaterThan(20.toBigDecimal()))
            }
        }

        // then
        assertThat(orderItemIds).containsOnly(orderItem3.id, orderItem4.id)
    }

    @Test
    fun between(): Unit = blockingDetect {
        // when
        val orderItemIds = withFactory { queryFactory ->
            queryFactory.listQuery {
                select(col(OrderItem::id))
                from(entity(OrderItem::class))
                where(col(OrderItem::price).between(20.toBigDecimal(), 40.toBigDecimal()))
            }
        }

        // then
        assertThat(orderItemIds).containsOnly(orderItem2.id, orderItem3.id)
    }

    @Test
    fun isTrue(): Unit = blockingDetect {
        // when
        val orderItemIds = withFactory { queryFactory ->
            queryFactory.listQuery {
                select(col(OrderItem::id))
                from(entity(OrderItem::class))
                where(col(OrderItem::claimed).isTrue())
            }
        }

        // then
        assertThat(orderItemIds).containsOnly(orderItem1.id)
    }

    @Test
    fun isFalse(): Unit = blockingDetect {
        // when
        val orderItemIds = withFactory { queryFactory ->
            queryFactory.listQuery {
                select(col(OrderItem::id))
                from(entity(OrderItem::class))
                where(col(OrderItem::claimed).isFalse())
            }
        }

        // then
        assertThat(orderItemIds).containsOnly(orderItem2.id, orderItem3.id, orderItem4.id)
    }

    @Test
    fun isNull(): Unit = blockingDetect {
        // when
        val orderItemIds = withFactory { queryFactory ->
            queryFactory.listQuery {
                select(col(OrderItem::id))
                from(entity(OrderItem::class))
                where(col(OrderItem::productImage).isNull())
            }
        }

        // then
        assertThat(orderItemIds).containsOnly(orderItem1.id, orderItem2.id, orderItem3.id)
    }

    @Test
    fun isNotNull(): Unit = blockingDetect {
        // when
        val orderItemIds = withFactory { queryFactory ->
            queryFactory.listQuery {
                select(col(OrderItem::id))
                from(entity(OrderItem::class))
                where(col(OrderItem::productImage).isNotNull())
            }
        }

        // then
        assertThat(orderItemIds).containsOnly(orderItem4.id)
    }

    @Test
    fun like(): Unit = blockingDetect {
        // when
        val orderItemIds = withFactory { queryFactory ->
            queryFactory.listQuery {
                select(col(OrderItem::id))
                from(entity(OrderItem::class))
                where(col(OrderItem::productName).like("test%"))
            }
        }

        // then
        assertThat(orderItemIds).containsOnly(orderItem1.id, orderItem2.id, orderItem3.id, orderItem4.id)
    }

    @Test
    fun notLike(): Unit = blockingDetect {
        // when
        val orderItemIds = withFactory { queryFactory ->
            queryFactory.listQuery {
                select(col(OrderItem::id))
                from(entity(OrderItem::class))
                where(col(OrderItem::productName).notLike("test%"))
            }
        }

        // then
        assertThat(orderItemIds).isEmpty()
    }

    @Test
    fun exists() = blockingDetect {
        // when
        val existFoundOrders = withFactory { queryFactory ->
            queryFactory.listQuery {
                val entity: EntitySpec<Order> = entity(Order::class)
                select(entity)
                from(entity)
                where(
                    exists(queryFactory.subquery<Long> {
                        val orderGroupEntity = entity(OrderGroup::class)
                        select(literal(1))
                        from(orderGroupEntity)
                        where(
                            and(
                                col(OrderGroup::orderGroupName).equal("orderGroup1"),
                                col(OrderGroup::order).equal(entity)
                            )
                        )
                    })
                )
            }
        }

        // then
        assertThat(existFoundOrders.map { it.id }).isEqualTo(listOf(order1.id))
    }

    @Test
    fun notExists() = blockingDetect {
        // when
        val existFoundOrders = withFactory { queryFactory ->
            queryFactory.listQuery {
                val entity: EntitySpec<Order> = entity(Order::class)
                select(entity)
                from(entity)
                where(
                    notExists(queryFactory.subquery<Long> {
                        val orderGroupEntity = entity(OrderGroup::class)
                        select(literal(1))
                        from(orderGroupEntity)
                        where(
                            and(
                                col(OrderGroup::orderGroupName).equal("orderGroup1"),
                                col(OrderGroup::order).equal(entity)
                            )
                        )
                    })
                )
            }
        }

        // then
        assertThat(existFoundOrders.map { it.id }).isEqualTo(listOf(order2.id, order3.id))
    }
}
