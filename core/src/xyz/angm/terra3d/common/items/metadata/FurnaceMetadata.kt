package xyz.angm.terra3d.common.items.metadata

import xyz.angm.terra3d.common.items.Item

/** Metadata for a furnace.
 * @property progress Progress of the current smelt operation. Range 0-100.
 * @property burnTime Ticks the furnace will continue to be burning for.
 * @property fuel The fuel slot.
 * @property baking The slot that is being burnt.
 * @property result The result slot. */
class FurnaceMetadata : IMetadata {

    var progress = 0
    var burnTime = 0
    var fuel: Item? = null
    var baking: Item? = null
    var result: Item? = null

    override fun toString() = """

        Progress:       $progress
        Burn Time Left: $burnTime
        Fuel:           $fuel
        Baking:         $baking
        Result:         $result
    """.replace("null", "None").trimIndent()

    override fun equals(other: Any?) =
        other is FurnaceMetadata && other.fuel == fuel && other.baking == baking && other.result == result
}