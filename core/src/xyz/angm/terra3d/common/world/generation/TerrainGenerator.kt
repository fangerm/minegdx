package xyz.angm.terra3d.common.world.generation

import ktx.collections.*
import xyz.angm.terra3d.common.*
import xyz.angm.terra3d.common.items.Item
import xyz.angm.terra3d.common.world.Chunk
import xyz.angm.terra3d.common.world.WorldInterface
import kotlin.random.Random

/** Generates terrain/chunks for a world.
 * @param world The world to generate for */
class TerrainGenerator(val world: WorldInterface) {

    private val noiseGenerator = NoiseGenerator(world.seed.convertToLong())
    private val structures = Structures()
    private val tmpIVLocal = ThreadLocal.withInitial { IntVector3() }
    private val tmpIV get() = tmpIVLocal.get()

    /** Finalize generation. Call after finishing with generating something, usually something batched. */
    @Synchronized
    fun finalizeGen() = structures.update(world)

    /** Generate a line of chunks.
     * @param position Position of the chunks; Y axis ignored. */
    @Synchronized
    fun generateChunks(position: IntVector3): Array<Chunk> {
        position.chunk().y = 0
        val biomeMap = noiseGenerator.generateChunkBiomeMap(position.x, position.z)
        val heightMap = noiseGenerator.generateChunkHeightMap(position.x, position.z, biomeMap)

        val chunks = Array(WORLD_HEIGHT_IN_CHUNKS) {
            val chunk = Chunk(chunkPosition = IntVector3(position.x, it * CHUNK_SIZE, position.z))
            generateChunk(chunk, heightMap, biomeMap)
            world.addChunk(chunk)
            chunk
        }

        log.debug { "[WORLDGEN] Generated chunks at $position." }
        return chunks
    }

    /** Generates missing chunks in a line of chunks.
     * @param alreadyCreated Chunks already created in this line.
     * Optional, if null the generator will simply only add new chunks to the world. */
    @Synchronized
    fun generateMissing(alreadyCreated: GdxArray<Chunk>?, position: IntVector3) {
        val biomeMap = noiseGenerator.generateChunkBiomeMap(position.x, position.z)
        val heightMap = noiseGenerator.generateChunkHeightMap(position.x, position.z, biomeMap)

        for (chunkIndex in 0 until WORLD_HEIGHT_IN_CHUNKS) {
            position.y = chunkIndex * CHUNK_SIZE

            // Search in cache first in case that a chunk was changed
            // and the DB version is outdated
            val preexisting = world.getLoadedChunk(position) ?: alreadyCreated?.firstOrNull { it.position == position }
            val chunk = if (preexisting != null) preexisting
            else {
                val chunk = Chunk(chunkPosition = IntVector3(position.x, chunkIndex * CHUNK_SIZE, position.z))
                generateChunk(chunk, heightMap, biomeMap)
                world.addChunk(chunk)
                chunk
            }
            alreadyCreated?.add(chunk)
        }
    }

    @Synchronized
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
        if (height - chunk.position.y < 0) return // This chunk is 100% air

        for (y in 0 until CHUNK_SIZE) {
            val diff = height - (chunk.position.y + y)
            if (diff < 0) break // blocks beyond should be air; aka null

            val type = when {
                chunk.position.y == 0 && y == 0 -> "bedrock"
                diff in 0 until biome.surfaceLayers -> biome.surfaceBlock
                diff in biome.surfaceLayers until biome.belowSurfaceLayers -> biome.belowSurfaceBlock
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
                    structures.generate(structure.key, position)
                    break
                }
            }
        }
    }

    private fun isSurfaceChunk(chunkY: Int, surfaceHeight: Int) = (surfaceHeight - chunkY) < CHUNK_SIZE && (surfaceHeight - chunkY) >= 0
}