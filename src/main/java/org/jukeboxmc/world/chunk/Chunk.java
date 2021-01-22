package org.jukeboxmc.world.chunk;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.PooledByteBufAllocator;
import lombok.ToString;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.WriteBatch;
import org.jukeboxmc.block.Block;
import org.jukeboxmc.block.BlockPalette;
import org.jukeboxmc.blockentity.BlockEntity;
import org.jukeboxmc.entity.Entity;
import org.jukeboxmc.nbt.NBTOutputStream;
import org.jukeboxmc.nbt.NbtUtils;
import org.jukeboxmc.utils.BinaryStream;
import org.jukeboxmc.utils.Palette;
import org.jukeboxmc.utils.Utils;
import org.jukeboxmc.world.Biome;
import org.jukeboxmc.world.World;
import org.jukeboxmc.world.leveldb.LevelDBChunk;

import javax.rmi.CORBA.Util;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * @author LucGamesYT
 * @version 1.0
 */

@ToString ( exclude = { "subChunks" } )
public class Chunk extends LevelDBChunk {

    public static final int CHUNK_LAYERS = 2;

    public SubChunk[] subChunks;

    private World world;
    private int chunkX;
    private int chunkZ;
    public byte chunkVersion = 21;

    private List<Entity> entitys = new CopyOnWriteArrayList<>();

    public Chunk( World world, int chunkX, int chunkZ ) {
        super( world, chunkX, chunkZ );
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.subChunks = new SubChunk[16];
    }

    public void setBlock( int x, int y, int z, int layer, Block block ) {
        int subY = y >> 4;
        this.getCheckAndCreateSubChunks( subY );
        this.subChunks[subY].setBlock( x & 15, y & 15, z & 15, layer, block );
    }

    public Block getBlock( int x, int y, int z, int layer ) {
        int subY = y >> 4;
        this.getCheckAndCreateSubChunks( subY );
        return this.subChunks[subY].getBlock( x & 15, y & 15, z & 15, layer );
    }

    public void setBlockEntity( int x, int y, int z, BlockEntity blockEntity ) {
        int subY = y >> 4;
        this.getCheckAndCreateSubChunks( subY );
        this.subChunks[subY].setBlockEntity( x & 15, y & 15, z & 15, blockEntity );
    }

    public BlockEntity getBlockEntity( int x, int y, int z ) {
        int subY = y >> 4;
        this.getCheckAndCreateSubChunks( subY );
        return this.subChunks[subY].getBlockEntity( x & 15, y & 15, z & 15 );
    }

    public void removeBlockEntity( int x, int y, int z ) {
        int subY = y >> 4;
        this.getCheckAndCreateSubChunks( subY );
        this.subChunks[subY].removeBlockEntity( x & 15, y & 15, z & 15 );
    }

    public Biome getBiome( int x, int z ) {
        return Biome.findById( this.biomes[(x << 4) | z] );
    }

    public void getCheckAndCreateSubChunks( int subY ) {
        for ( int y = 0; y <= subY; y++ ) {
            if ( this.subChunks[y] == null ) {
                this.subChunks[y] = new SubChunk( y );
            }
        }
    }

    public void iterateEntities( Consumer<Entity> consumer ) {
        for ( Entity player : this.entitys ) {
            consumer.accept( player );
        }
    }

    public void writeTo( BinaryStream binaryStream ) {
        for ( SubChunk subChunk : this.subChunks )
            if ( subChunk != null ) {
                subChunk.writeTo( binaryStream );
            }
        binaryStream.writeBytes( this.biomes );
        binaryStream.writeUnsignedVarInt( 0 ); //Extradata
    }

    public void save( DB db ) {
        WriteBatch writeBatch = db.createWriteBatch();

        for ( int subY = 0; subY < this.subChunks.length; subY++ ) {
            if ( this.subChunks[subY] == null ) {
                continue;
            }
            this.saveChunkSlice( subY, writeBatch );
        }

        //ELSE
        byte[] versionKey = Utils.getKey( this.chunkX, this.chunkZ, (byte) 0x2c );
        BinaryStream versionBuffer = new BinaryStream();
        versionBuffer.writeByte( this.chunkVersion );
        writeBatch.put( versionKey, versionBuffer.getArray() );

        byte[] finalizedKey = Utils.getKey( this.chunkX, this.chunkZ, (byte) 0x36 );
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.directBuffer( 1 );
        byteBuf.writeByte( this.populated ? 2 : 0 ).writeByte( 0 ).writeByte( 0 ).writeByte( 0 );
        writeBatch.put( finalizedKey, new BinaryStream( byteBuf ).getArray() );

        byte[] biomeKey = Utils.getKey( this.chunkX, this.chunkZ, (byte) 0x2d );
        BinaryStream biomeBuffer = new BinaryStream();

        for ( short height : this.height ) {
            biomeBuffer.writeLShort( height );
        }
        biomeBuffer.writeBytes( this.biomes );
        writeBatch.put( biomeKey, biomeBuffer.getArray() );

        db.write( writeBatch );
        try {
            writeBatch.close();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    private void saveChunkSlice( int subY, WriteBatch writeBatch ) {
        BinaryStream buffer = new BinaryStream();
        SubChunk subChunk = this.subChunks[subY];

        buffer.writeByte( (byte) 8 );
        buffer.writeByte( (byte) Chunk.CHUNK_LAYERS );

        for ( int layer = 0; layer < Chunk.CHUNK_LAYERS; layer++ ) {
            Integer[] layerBlocks = subChunk.blocks[layer];
            int[] blockIds = new int[4096];

            Map<Integer, Integer> indexList = new LinkedHashMap<>();
            List<Integer> runtimeIds = new ArrayList<>();

            Integer foundIndex = 0;
            int nextIndex = 0;
            int lastRuntimeId = -1;

            for ( short blockIndex = 0; blockIndex < layerBlocks.length; blockIndex++ ) {
                int runtimeId = layerBlocks[blockIndex];
                if ( runtimeId != lastRuntimeId ) {
                    foundIndex = indexList.get( runtimeId );
                    if ( foundIndex == null ) {
                        runtimeIds.add( runtimeId );
                        indexList.put( runtimeId, nextIndex );
                        foundIndex = nextIndex;
                        nextIndex++;
                    }
                    lastRuntimeId = runtimeId;
                }
                blockIds[blockIndex] = foundIndex;
            }

            float numberOfBits = Utils.log2( indexList.size() ) + 1;
            int amountOfBlocks = (int) Math.floor( 32 / numberOfBits );
            Palette palette = new Palette( buffer, amountOfBlocks, false );

            byte paletteWord = (byte) ( (byte) ( palette.getPaletteVersion().getVersionId() << 1 ) | 1 );
            buffer.writeByte( paletteWord );
            palette.addIndexIDs( blockIds );
            palette.finish();

            buffer.writeLInt( indexList.size() );

            for ( int runtimeId : runtimeIds ) {
                try {
                    NBTOutputStream networkWriter = NbtUtils.createWriterLE( new ByteBufOutputStream( buffer.getBuffer() ) );
                    networkWriter.writeTag( BlockPalette.getBlockNBT( runtimeId ) );
                } catch ( IOException e ) {
                    e.printStackTrace();
                }
            }
        }
        byte[] subChunkKey = Utils.getSubChunkKey( this.chunkX, this.chunkZ, (byte) 0x2f, (byte) subY );
        writeBatch.put( subChunkKey, buffer.getArray() );
    }

    public int getAvailableSubChunks() {
        return Arrays.stream( this.subChunks ).mapToInt( o -> o == null ? 0 : 1 ).sum();
    }

    public int getChunkX() {
        return this.chunkX;
    }

    public int getChunkZ() {
        return this.chunkZ;
    }

    public SubChunk[] getSubChunks() {
        return this.subChunks;
    }

    public void addEntity( Entity entity ) {
        if ( !this.entitys.contains( entity ) ) {
            this.entitys.add( entity );
        }
    }

    public void removeEntity( Entity entity ) {
        this.entitys.remove( entity );
    }

    public Collection<Entity> getEntitys() {
        return this.entitys;
    }
}
