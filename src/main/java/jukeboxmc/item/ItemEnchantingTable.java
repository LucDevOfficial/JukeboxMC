package jukeboxmc.item;

import org.jukeboxmc.block.BlockEnchantingTable;

/**
 * @author LucGamesYT
 * @version 1.0
 */
public class ItemEnchantingTable extends Item {

    public ItemEnchantingTable() {
        super ( "minecraft:enchanting_table" );
    }

    @Override
    public BlockEnchantingTable getBlock() {
        return new BlockEnchantingTable();
    }
}