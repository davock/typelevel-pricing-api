$version: "2"

namespace typelevel.davock.pricing

use alloy#simpleRestJson

@simpleRestJson
service Pricing {
    version: "2026-09-18"
    resources: [Customer, Item, Order]
}

string CustomerId
string Sku
string OrderId
string CouponCode

enum OrderStatus {
    PRICED
    PENDING
    COMPLETED
}

resource Customer {
    identifiers: { customerId: CustomerId }
}

resource Item {
    identifiers: { sku: Sku }
}

resource Order {
    identifiers: { orderId: OrderId}
    collectionOperations: [PriceOrder]
}

structure OrderInputLineItem {
    @required
    sku: Sku

    @required
    quantity: Integer
}

structure OrderOutputLineItem {
    @required
    sku: Sku

    @required
    quantity: Integer

    @required
    unitPrice: Double

    @required
    lineTotal: Double
}

list OrderInputLineItems {
    member: OrderInputLineItem
}

list OrderOutputLineItems {
    member: OrderOutputLineItem
}


structure PriceOrderOutput {
    @required
    orderId: OrderId

    @required
    customerId: CustomerId

    @required
    status: OrderStatus

    @required
    items: OrderOutputLineItems

    @required
    subtotal: Double

    @required
    discountAmount: Double

    @required
    total: Double

    couponApplied: CouponCode

    @required
    createdAt: Timestamp
}

@error("client")
@httpError(422)
structure ValidationError {
    @required
    errors: ValidationErrorsList
}

list ValidationErrorsList {
    member: ValidationErrorDetails
}

structure ValidationErrorDetails {
    @required
    code: String

    @required
    field: String

    @required
    message: String
}

@http(method: "POST", uri: "/orders/price")
operation PriceOrder {
    input: PriceOrderInput
    output: PriceOrderOutput
    errors: [ValidationError]
}

@references([{ resource: Customer }]) // In Smithy 2.0, matches target id implicitly if names align
structure PriceOrderInput {
    @required
    customerId: CustomerId

    @required
    items: OrderInputLineItems

    couponCode: CouponCode
}
