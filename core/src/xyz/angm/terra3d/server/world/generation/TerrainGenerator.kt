package xyz.angm.terra3d.server.world.generation

import xyz.angm.terra3d.common.*
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.world.Chunk
import xyz.angm.terra3d.server.world.World
import kotlin.random.Random

/** Generates terrain/chunks for a [World].
 * @param world The world to generate for */
class TerrainGenerator(val world: World) {

    private val noiseGenerator = NoiseGenerator(world.seed.convertToLong())
    private val tmpIV = IntVector3()

    /** Generate a line of chunks.
     * @param position Position of the chunks; Y axis ignored. */
    fun generateChunks(position: IntVector3): Array<Chunk> {
        position.norm(CHUNK_SIZE).y = 0
        val biomeMap = noiseGenerator.generateChunkBiomeMap(position.x, position.z)
        val heightMap = noiseGenerator.generateChunkHeightMap(position.x, position.z, biomeMap)

        val chunks = Array(WORLD_HEIGHT_IN_CHUNKS) {
            val chunk = Chunk(chunkPosition = IntVector3(position.x, it * CHUNK_SIZE, position.z))
            generateChunk(chunk, heightMap, biomeMap)
            world.addChunk(chunk)
            chunk
        }

        Structure.update(world)
        log.debug { "[WORLDGEN] Generated chunks at $position." }
        return chunks
    }

    /** Generates missing chunks in a line of chunks.
     * @param alreadyCreated Chunks already created in this line */
    fun generateMissing(alreadyCreated: Array<Chunk>, position: IntVector3): Array<Chunk> {
        position.norm(CHUNK_SIZE).y = 0

        val biomeMap = noiseGenerator.generateChunkBiomeMap(position.x, position.z)
        val heightMap = noiseGenerator.generateChunkHeightMap(position.x, position.z, biomeMap)

        return Array(WORLD_HEIGHT_IN_CHUNKS) { chunkIndex ->
            alreadyCreated.firstOrNull { it.position.y == chunkIndex * CHUNK_SIZE } ?: {
                val chunk = Chunk(chunkPosition = IntVector3(position.x, chunkIndex * CHUNK_SIZE, position.z))
                generateChunk(chunk, heightMap, biomeMap)
                world.addChunk(chunk)
                chunk
            }()
        }
    }

    private fun generateChunk(chunk: Chunk, heightMap: Array<IntArray>, biomeMap: Array<Array<Biome>>) {
        val random = Random((chunk.position.x * chunk.position.y + chunk.position.z).toLong() + world.seed.convertToLong() + heightMap[0][0])

        for (x in 0 until CHUNK_SIZE) {
            for (z in 0 until CHUNK_SIZE) {
                generateTerrain(chunk, heightMap[x][z], biomeMap[x][z], x, z)

                generateStructures(
                    chunk, tmpIV.set(chunk.position.x + x, heightMap[x][z], chunk.position.z + z),
                    heightMap[x][z], biomeMap[x][z], random
                )
            }
        }
    }

    private fun generateTerrain(chunk: Chunk, height: Int, biome: Biome, x: Int, z: Int) {
        for (y in 0 until CHUNK_SIZE) {
            val diff = height - (chunk.position.y + y)

            if (diff < 0) continue // block should be air; aka null

            val type = when (diff) {
                in 0 until biome.surfaceLayers -> biome.surfaceBlock
                in biome.surfaceLayers until biome.belowSurfaceLayers -> biome.belowSurfaceBlock
                else -> "stone"
            }

            chunk.setBlock(tmpIV.set(x, y, z), Item.Properties.fromIdentifier(type).type)
        }
    }

    private fun generateStructures(chunk: Chunk, position: IntVector3, height: Int, biome: Biome, random: Random) {
        if (isSurfaceChunk(chunk.position.y, height)) {
            var chance = 1.0
            for (structure in biome.structuresGenerating) {
                chance -= structure.value
                if (random.nextDouble(1.0) > chance) {
                    Structure.generate(structure.key, position)
                    break
                }
            }
        }
    }

    private fun isSurfaceChunk(chunkY: Int, surfaceHeight: Int) = (surfaceHeight - chunkY) < CHUNK_SIZE && (surfaceHeight - chunkY) >= 0
}