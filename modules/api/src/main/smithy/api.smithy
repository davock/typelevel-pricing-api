$version: "2"

namespace com.example.api

use alloy#simpleRestJson

/// A minimal REST service, codegen'd into circe codecs + http4s bindings by smithy4s.
@simpleRestJson
service WidgetService {
    version: "1.0.0"
    operations: [GetWidget, CreateWidget, HealthCheck]
}

@readonly
@http(method: "GET", uri: "/widgets/{id}", code: 200)
operation GetWidget {
    input: GetWidgetInput
    output: Widget
    errors: [NotFoundError]
}

structure GetWidgetInput {
    @required
    @httpLabel
    id: String
}

@http(method: "POST", uri: "/widgets", code: 201)
operation CreateWidget {
    input: CreateWidgetInput
    output: Widget
}

structure CreateWidgetInput {
    @required
    name: String
    @required
    quantity: Integer
}

structure Widget {
    @required
    id: String
    @required
    name: String
    @required
    quantity: Integer
    @required
    createdAt: String
}

@error("client")
@httpError(404)
structure NotFoundError {
    @required
    message: String
}

@readonly
@http(method: "GET", uri: "/health", code: 200)
operation HealthCheck {
    output: HealthStatus
}

structure HealthStatus {
    @required
    status: String
}
