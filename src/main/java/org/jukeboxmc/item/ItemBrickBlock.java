package org.jukeboxmc.item;

import org.jukeboxmc.block.BlockBrick;

/**
 * @author LucGamesYT
 * @version 1.0
 */
public class ItemBrickBlock extends Item {

    public ItemBrickBlock() {
        super ( "minecraft:brick_block" );
    }

    @Override
    public BlockBrick getBlock() {
        return new BlockBrick();
    }
}
