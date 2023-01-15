package com.linecorp.kotlinjdsl.test.reactive.querydsl.expression

import com.linecorp.kotlinjdsl.listQuery
import com.linecorp.kotlinjdsl.querydsl.expression.*
import com.linecorp.kotlinjdsl.singleQuery
import com.linecorp.kotlinjdsl.test.entity.order.Order
import com.linecorp.kotlinjdsl.test.entity.order.OrderItem
import com.linecorp.kotlinjdsl.test.entity.test.TestTable
import com.linecorp.kotlinjdsl.test.reactive.CriteriaQueryDslIntegrationTest
import com.linecorp.kotlinjdsl.test.reactive.blockingDetect
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

abstract class AbstractCriteriaQueryDslExpressionIntegrationTest<S> : CriteriaQueryDslIntegrationTest<S> {
    private val orderItem1 = orderItem { productName = "test1"; productImage = null; price = 10; claimed = true }
    private val orderItem2 = orderItem { productName = "test1"; productImage = null; price = 20; claimed = false }
    private val orderItem3 = orderItem { productName = "test2"; productImage = null; price = 30; claimed = false }
    private val orderItem4 = orderItem { productName = "test3"; productImage = "image"; price = 50; claimed = false }

    private val order1 = order {
        purchaserId = 1000
        groups = hashSetOf(orderGroup { items = hashSetOf(orderItem1, orderItem2) })
    }
    private val order2 = order {
        purchaserId = 1000
        groups = hashSetOf(orderGroup { items = hashSetOf(orderItem3) })
    }
    private val order3 = order {
        purchaserId = 2000
        groups = hashSetOf(orderGroup { items = hashSetOf(orderItem4) })
    }

    private val orders = listOf(order1, order2, order3)

    @BeforeEach
    fun setUp(): Unit = blockingDetect {
        persistAll(order1, order2, order3)
    }

    @Test
    fun entity(): Unit = blockingDetect {
        // when
        val orders = withFactory { queryFactory ->
            queryFactory.listQuery<Order> {
                select(entity(Order::class))
                from(entity(Order::class))
            }
        }

        // then
        assertThat(orders.map { it.id }).containsOnly(order1.id, order2.id, order3.id)
    }

    @Test
    fun entityAlias(): Unit = blockingDetect {
        // when
        val orders = withFactory { queryFactory ->
            queryFactory.listQuery {
                val entity = entity(Order::class, "orderAlias")
                select(entity)
                from(entity)
            }
        }

        // then
        assertThat(orders.map { it.id }).containsOnly(order1.id, order2.id, order3.id)
    }

    @Test
    fun literal(): Unit = blockingDetect {
        // when
        val literals = withFactory { queryFactory ->
            queryFactory.listQuery {
                select(literal(10))
                from(entity(Order::class))
            }
        }

        // then
        assertThat(literals).containsOnly(10, 10, 10, 10)
    }

    @Test
    fun nullLiteral(): Unit = blockingDetect {
        // when
        val literals = withFactory { queryFactory ->
            queryFactory.listQuery {
                select(nullLiteral(Int::class.java))
                from(entity(Order::class))
            }
        }

        // then
        assertThat(literals).containsOnly(null, null, null, null)
    }

    @Test
    fun column(): Unit = blockingDetect {
        // when
        val literals = withFactory { queryFactory ->
            queryFactory.listQuery {
                select(column(Order::id))
                from(entity(Order::class))
            }
        }

        // then
        assertThat(literals).containsOnly(order1.id, order2.id, order3.id)
    }

    @Test
    fun max(): Unit = blockingDetect {
        // when
        val max = withFactory { queryFactory ->
            queryFactory.singleQuery {
                select(max(OrderItem::price))
                from(entity(OrderItem::class))
            }
        }

        // then
        assertThat(max).isEqualByComparingTo(50.toString())
    }

    @Test
    fun min(): Unit = blockingDetect {
        // when
        val min = withFactory { queryFactory ->
            queryFactory.singleQuery {
                select(min(OrderItem::price))
                from(entity(OrderItem::class))
            }
        }

        // then
        assertThat(min).isEqualByComparingTo(10.toString())
    }

    @Test
    fun avg(): Unit = blockingDetect {
        // when
        val avg = withFactory { queryFactory ->
            queryFactory.singleQuery {
                select(avg(OrderItem::price))
                from(entity(OrderItem::class))
            }
        }

        // then
        assertThat(avg).isEqualTo(27.5)
    }

    @Test
    fun sum(): Unit = blockingDetect {
        // when
        val sum = withFactory { queryFactory ->
            queryFactory.singleQuery {
                select(sum(OrderItem::price))
                from(entity(OrderItem::class))
            }
        }

        // then
        assertThat(sum).isEqualByComparingTo(110.toString())
    }

    @Test
    fun sumWhen(): Unit = blockingDetect {
        // when
        data class DataDTO(
            val id: Long,
            val amount: Long
        )

        persist(TestTable(id = 1, role = "A", occurAmount = 7))
        persist(TestTable(id = 2, role = "A", occurAmount = 5))
        persist(TestTable(id = 1, role = "B", occurAmount = 5))
        persist(TestTable(id = 2, role = "B", occurAmount = 6))
        persist(TestTable(id = 3, role = "C", occurAmount = 6))

        val sum = withFactory { queryFactory ->
            queryFactory.listQuery<DataDTO> {
                selectMulti(
                    col(TestTable::id),
                    sum(
                        case(
                            `when`(col(TestTable::role).`in`("A", "B")).then(col(TestTable::occurAmount)),
                            `else` = literal(0L)
                        )
                    ),
                )
                from(entity(TestTable::class))
                groupBy(
                    col(TestTable::id),
                )
            }
        }

        // then
        assertThat(sum.first { it.id == 1L }.amount).isEqualTo(12L)
        assertThat(sum.first { it.id == 2L }.amount).isEqualTo(11L)
        assertThat(sum.first { it.id == 3L }.amount).isEqualTo(0L)
    }

    @Test
    fun count(): Unit = blockingDetect {
        // when
        val count = withFactory { queryFactory ->
            queryFactory.singleQuery {
                select(count(OrderItem::id))
                from(entity(OrderItem::class))
            }
        }
        // then
        assertThat(count).isEqualTo(4)
    }

    @Test
    fun countExpression(): Unit = blockingDetect {
        // when
        val count = withFactory { queryFactory ->
            queryFactory.singleQuery {
                select(
                    count(literal(1))
                )
                from(entity(Order::class))
            }
        }

        // then
        assertThat(count).isEqualTo(3L)
    }

    @Test
    fun countDistinct(): Unit = blockingDetect {
        // when
        val count = withFactory { queryFactory ->
            queryFactory.singleQuery {
                select(countDistinct(OrderItem::productName))
                from(entity(OrderItem::class))
            }
        }

        // then
        assertThat(count).isEqualTo(3)
    }

    @Test
    fun greatest(): Unit = blockingDetect {
        // when
        val greatest = withFactory { queryFactory ->
            queryFactory.singleQuery {
                select(greatest(OrderItem::productName))
                from(entity(OrderItem::class))
            }
        }

        // then
        assertThat(greatest).isEqualTo("test3")
    }

    @Test
    fun least(): Unit = blockingDetect {
        // when
        val least = withFactory { queryFactory ->
            queryFactory.singleQuery {
                select(least(OrderItem::productName))
                from(entity(OrderItem::class))
            }
        }

        // then
        assertThat(least).isEqualTo("test1")
    }

    @Test
    fun caseWhen(): Unit = blockingDetect {
        // when
        val values = withFactory { queryFactory ->
            queryFactory.listQuery<Int?> {
                select(
                    case(
                        `when`(col(OrderItem::productName).equal("test1")).then(literal(1)),
                        `when`(col(OrderItem::productName).equal("test2")).then(literal(2)),
                        `else` = nullLiteral()
                    )
                )
                from(entity(OrderItem::class))
                orderBy(col(OrderItem::productName).asc())
            }
        }

        // then
        assertThat(values).isEqualTo(listOf(1, 1, 2, null))
    }
}
