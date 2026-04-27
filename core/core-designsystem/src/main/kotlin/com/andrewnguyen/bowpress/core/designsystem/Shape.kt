package com.andrewnguyen.bowpress.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Kenrokuen is uniformly flat — no rounded corners anywhere in the system.
// Material 3's `Shapes` takes `CornerBasedShape`, so we use a zero-radius
// rounded shape (equivalent to a rectangle). Callers draw 1dp hairlines via
// Modifier.border when they need to separate surfaces — do not add rounded
// corners to this shape set.
private val FlatShape = RoundedCornerShape(0.dp)

val BPShapes = Shapes(
    extraSmall = FlatShape,
    small = FlatShape,
    medium = FlatShape,
    large = FlatShape,
    extraLarge = FlatShape,
)
