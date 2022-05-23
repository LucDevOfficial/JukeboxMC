package org.jukeboxmc.world.generator.object;

import org.jukeboxmc.block.BlockType;
import org.jukeboxmc.world.chunk.Chunk;

import java.util.Random;

/**
 * @author LucGamesYT
 * @version 1.0
 */
public class BirchTree extends Tree {

    public BirchTree() {
        this.leafBlock = BlockType.BIRCH_LEAVES;
        this.blockLog = BlockType.BIRCH_LOG;
    }

    @Override
    public void grow( Chunk chunk, int x, int y, int z, Random random ) {
        this.treeHeight = random.nextInt( 3 ) + 4;
        if ( this.canPlaceObject( chunk, x, y, z, random ) ) {
            this.placeObject( chunk, x, y, z, random );
        }
    }
}
