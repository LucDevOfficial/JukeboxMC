package org.jukeboxmc.block;

/**
 * @author LucGamesYT
 * @version 1.0
 */
public class BlockDoublePlant extends Block {

    public BlockDoublePlant() {
        super( "minecraft:double_plant" );
    }

    public BlockDoublePlant setPlantType( PlantType plantType ) {
        return this.setStates( "double_plant_type", plantType.name().toLowerCase() );
    }

    public PlantType getPlantType() {
        return this.states.containsKey( "double_plant_type" ) ? PlantType.valueOf( this.states.getString( "double_plant_type" ).toUpperCase() ) : PlantType.SUNFLOWER;
    }

    public BlockDoublePlant setUpperBlock( boolean value ) {
        return this.setStates( "upper_block_bit", value ? (byte) 1 : (byte) 0 );
    }

    public boolean isUpperBlock( boolean value ) {
        return this.states.containsKey( "upper_block_bit" ) && this.states.getByte( "upper_block_bit" ) == 1;
    }

    public enum PlantType {
        FERN,
        GRASS,
        PAEONIA,
        ROSE,
        SUNFLOWER,
        SYRINGA
    }
}