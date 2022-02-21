package org.jukeboxmc.world.palette.integer;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jukeboxmc.nbt.NBTInputStream;
import org.jukeboxmc.nbt.NBTOutputStream;
import org.jukeboxmc.nbt.NbtMap;
import org.jukeboxmc.nbt.NbtUtils;
import org.jukeboxmc.utils.BinaryStream;
import org.jukeboxmc.world.palette.bitarray.BitArray;
import org.jukeboxmc.world.palette.bitarray.BitArrayVersion;

import java.io.IOException;

@Getter
@EqualsAndHashCode
public class IntPalette {

    public static final int SIZE = 4096;

    private final IntList palette;
    private BitArray bitArray;

    public IntPalette( int first ) {
        this( first, BitArrayVersion.V2 );
    }

    public IntPalette( int first, BitArrayVersion version ) {
        this.bitArray = version.createArray( SIZE );
        this.palette = new IntArrayList( 16 );
        this.palette.add( first );
    }

    private static int getPaletteHeader( BitArrayVersion version, boolean runtime ) {
        return ( version.bits << 1 ) | ( runtime ? 1 : 0 );
    }

    private static BitArrayVersion getVersionFromHeader( short header ) {
        return BitArrayVersion.get( header >> 1, true );
    }

    private static boolean hasCopyLastFlag( short header ) {
        return ( header >> 1 ) == 0x7F;
    }

    private static int getCopyLastFlagHeader() {
        return ( 0x7F << 1 ) | 1;
    }

    public static boolean isPersistent( short header ) {
        return ( header & 1 ) == 0;
    }

    public int get( int index ) {
        return this.palette.getInt( this.bitArray.get( index ) );
    }

    public void set( int index, int value ) {
        this.bitArray.set( index, this.paletteIndexFor( value ) );
    }

    public void writeToNetwork( BinaryStream buffer, IntRuntimeDataSerializer serializer ) {
        buffer.writeByte( IntPalette.getPaletteHeader( this.bitArray.getVersion(), true ) );

        for ( int word : this.bitArray.getWords() ) {
            buffer.writeLInt( word );
        }

        this.bitArray.writeSizeToNetwork( buffer, this.palette.size() );
        for ( int value : this.palette ) {
            buffer.writeSignedVarInt( serializer.serialize( value ) );
        }
    }

    public void writeToStoragePersistent( BinaryStream buffer, IntPersistentDataSerializer serializer ) {
        buffer.writeByte( IntPalette.getPaletteHeader( this.bitArray.getVersion(), false ) );

        for ( int word : this.bitArray.getWords() ) {
            buffer.writeLInt( word );
        }

        buffer.writeLInt( this.palette.size() );

        try ( final ByteBufOutputStream bufOutputStream = new ByteBufOutputStream( buffer.getBuffer() );
              final NBTOutputStream outputStream = NbtUtils.createWriterLE( bufOutputStream ) ) {
            for ( int value : this.palette ) {
                outputStream.writeTag( serializer.serialize( value ) );
            }
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    public void writeToStorageRuntime( BinaryStream buffer, IntRuntimeDataSerializer serializer, IntPalette last ) {
        if ( last != null && last.palette.equals( this.palette ) ) {
            buffer.writeByte( IntPalette.getCopyLastFlagHeader() );
            return;
        }

        if ( this.isEmpty() ) {
            buffer.writeByte( IntPalette.getPaletteHeader( BitArrayVersion.V0, true ) );
            buffer.writeLInt( serializer.serialize( this.palette.getInt( 0 ) ) );
            return;
        }

        buffer.writeByte( IntPalette.getPaletteHeader( this.bitArray.getVersion(), true ) );

        for ( int word : this.bitArray.getWords() ) {
            buffer.writeLInt( word );
        }

        buffer.writeLInt( this.palette.size() );
        for ( int value : this.palette ) {
            buffer.writeLInt( serializer.serialize( value ) );
        }
    }

    public void readFromStoragePersistent( BinaryStream buffer, IntPersistentDataDeserializer deserializer ) {
        final short header = buffer.readUnsignedByte();

        final BitArrayVersion version = IntPalette.getVersionFromHeader( header );
        final int wordCount = version.getWordsForSize( SIZE );
        final int[] words = new int[wordCount];
        for ( int i = 0; i < wordCount; i++ ) {
            words[i] = buffer.readLInt();
        }

        this.bitArray = version.createArray( SIZE, words );
        this.palette.clear();

        final int paletteSize = buffer.readLInt();
        try ( final ByteBufInputStream bufInputStream = new ByteBufInputStream( buffer.getBuffer() );
              final NBTInputStream inputStream = NbtUtils.createReaderLE( bufInputStream ) ) {
            for ( int i = 0; i < paletteSize; i++ ) {
                this.palette.add( deserializer.deserialize( (NbtMap) inputStream.readTag() ) );
            }
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    public void readFromStorageRuntime( BinaryStream buffer, IntRuntimeDataDeserializer deserializer, IntPalette last ) {
        final short header = buffer.readUnsignedByte();

        if ( IntPalette.hasCopyLastFlag( header ) ) {
            if ( last == null ) {
                throw new IllegalArgumentException( "Palette has copy last flag but no last entry!" );
            }

            last.copyTo( this );
            return;
        }

        final BitArrayVersion version = IntPalette.getVersionFromHeader( header );
        if ( version == BitArrayVersion.V0 ) {
            this.bitArray = version.createArray( SIZE, null );
            this.palette.clear();
            this.palette.add( deserializer.deserialize( buffer.readLInt() ) );

            this.onResize( BitArrayVersion.V2 );
            return;
        }

        final int wordCount = version.getWordsForSize( SIZE );
        final int[] words = new int[wordCount];
        for ( int i = 0; i < wordCount; i++ ) {
            words[i] = buffer.readLInt();
        }

        this.bitArray = version.createArray( SIZE, words );
        this.palette.clear();

        final int paletteSize = buffer.readLInt();
        for ( int i = 0; i < paletteSize; i++ ) {
            this.palette.add( deserializer.deserialize( buffer.readLInt() ) );
        }
    }

    private void onResize( BitArrayVersion version ) {
        final BitArray newBitArray = version.createArray( SIZE );
        for ( int i = 0; i < SIZE; i++ )
            newBitArray.set( i, this.bitArray.get( i ) );

        this.bitArray = newBitArray;
    }

    public int paletteIndexFor( int value ) {
        int index = this.palette.indexOf( value );
        if ( index != -1 ) return index;

        index = this.palette.size();
        this.palette.add( value );

        final BitArrayVersion version = this.bitArray.getVersion();
        if ( index > version.maxEntryValue ) {
            final BitArrayVersion next = version.next;
            if ( next != null ) this.onResize( next );
        }

        return index;
    }

    public boolean isEmpty() {
        if ( this.palette.size() == 1 ) return true;

        for ( int word : this.bitArray.getWords() )
            if ( Integer.toUnsignedLong( word ) != 0L )
                return false;

        return true;
    }

    public void copyTo( IntPalette palette ) {
        palette.bitArray = this.bitArray.copy();
        palette.palette.clear();
        palette.palette.addAll( this.palette );
    }

}
