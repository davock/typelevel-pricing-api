package com.example.domain

import java.time.Instant
import java.util.UUID

// Internal domain model — the shape your business logic works with.
final case class Widget(
    id: UUID,
    name: String,
    quantity: Int,
    createdAt: Instant
)

// Wire-level DTO — the shape exposed over the API (matches the Smithy model
// in modules/api/src/main/smithy/api.smithy). Kept separate from the domain
// model so internal refactors don't leak into the public contract.
final case class WidgetDto(
    id: String,
    name: String,
    quantity: Int,
    createdAt: String
)
