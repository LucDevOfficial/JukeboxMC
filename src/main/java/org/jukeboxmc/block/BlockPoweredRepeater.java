package org.jukeboxmc.block;

import org.jukeboxmc.block.direction.Direction;
import org.jukeboxmc.item.ItemPoweredRepeater;

/**
 * @author LucGamesYT
 * @version 1.0
 */
public class BlockPoweredRepeater extends Block {

    public BlockPoweredRepeater() {
        super( "minecraft:powered_repeater" );
    }

    @Override
    public ItemPoweredRepeater toItem() {
        return new ItemPoweredRepeater();
    }

    @Override
    public BlockType getType() {
        return BlockType.POWERED_REPEATER;
    }

    public void setRepeaterDelay( int value ) { //0-3
        this.setState( "repeater_delay", value );
    }

    public int getRepeaterDelay() {
        return this.stateExists( "repeater_delay" ) ? this.getIntState( "repeater_delay" ) : 0;
    }

    public void setDirection( Direction direction ) {
        switch ( direction ) {
            case SOUTH:
                this.setState( "direction", 0 );
                break;
            case WEST:
                this.setState( "direction", 1 );
                break;
            case NORTH:
                this.setState( "direction", 2 );
                break;
            case EAST:
                this.setState( "direction", 3 );
                break;
        }
    }

    public Direction getDirection() {
        int value = this.stateExists( "direction" ) ? this.getIntState( "direction" ) : 0;
        switch ( value ) {
            case 0:
                return Direction.SOUTH;
            case 1:
                return Direction.WEST;
            case 2:
                return Direction.NORTH;
            default:
                return Direction.EAST;
        }
    }
}
