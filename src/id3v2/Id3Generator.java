package id3v2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

// http://www.id3.org/id3v2.3.0

public class Id3Generator extends ByteArrayOutputStream {
	
	public static enum Encoding {
		Latin_1,
		Unicode
	}
	
	private static final Charset ASCII = Charset.forName("US-ASCII");
	private static final Charset ISO_8859_1 = Charset.forName("ISO_8859_1");
	private static final Charset UTF_16 = Charset.forName("UTF-16");
	
	private boolean id3header;
	private Encoding encoding;
	
	public Id3Generator(Encoding encoding, boolean id3header) {
		this.id3header = id3header;
		this.encoding = encoding;
		if (id3header) {
			writeTagHeader(0x0300, 0, 0xffffffff);
		}
	}	
	
	public Id3Generator(Encoding encoding) {		
		this(encoding,true);
	}
	
	public Id3Generator() {		
		this(Encoding.Latin_1, true);
	}
	
	/********************************************************************************
	 * primitives
	 ********************************************************************************/
	
	private int writeInt32(int value) {
		write( value>>24 ); // MSB
		write( value>>16 ); 
		write( value>>8 );
		write( value>>0 ); // LSB
		return 4;
	}
	
	private int writeInt16(int value) {
		write( value>>8 ); // MSB
		write( value>>0 ); // LSB
		return 2;
	}
	
	private int writeInt8(int value) {
		write( value );
		return 1;
	}
	
	private int writeAscii(String value) {
		byte[] data = value.getBytes(ASCII);
		write(data, 0, data.length);
		return data.length;
	}	
	
	private int writeString(String value, Encoding encoding, boolean nullTerminated) {
		byte[] buffer;
		
		int before = count;
		
		switch (encoding) {
			case Latin_1:
				buffer = value.getBytes(ISO_8859_1);
				writeInt8(0x00);
				writeBinary(buffer);
				break;
			case Unicode:
				buffer = value.getBytes(UTF_16);
				writeInt8(0x01);
				writeBinary(buffer);
				break;
		}
		
		if (nullTerminated) {
			writeInt8(0);
		}
		
		return count - before;
	}
	
	private int writeBinary(byte[] data) {
		write(data, 0, data.length);
		return data.length;
	}
	
	// random access to buffer
	private int putInt32(int position, int value) {
		buf[position + 0] = (byte) (value >> 24); // MSB
		buf[position + 1] = (byte) (value >> 16);
		buf[position + 2] = (byte) (value >> 8);
		buf[position + 3] = (byte) (value >> 0); // LSB
		return 4;
	}
	
	/********************************************************************************
	 * id3 headers
	 ********************************************************************************/
	
	private int writeTagHeader(int version, int flags, int size) {

//		ID3v2/file identifier   "ID3" 
//		ID3v2 version           $03 00
//		ID3v2 flags             %abc00000
//		ID3v2 size              4 * %0xxxxxxx		
		
		writeAscii("ID3");
		writeInt16(version);
		writeInt8(flags);
		writeInt32(size);
		
		return 10;
	}
	
	private int writeFrameHeader(String FrameId, int size, int flags) {
		
//		Frame ID       $xx xx xx xx (four characters) 
//		Size           $xx xx xx xx
//		Flags          $xx xx
		
		writeAscii(FrameId); // four character code
		writeInt32(size);
		writeInt16(flags);
		
		return 10;
	}
	
	private int writeTextInformationFrame(String FrameId, int flags, String value, Encoding encoding) {
		
//		<Header for 'Text information frame', ID: "T000" - "TZZZ", excluding "TXXX" described in 4.2.2.> 
//		Text encoding    $xx
//		Information    <text string according to encoding>		
		
		int sizePtr = count + 4;
		int size = 0;
		
		writeFrameHeader(FrameId, 0xffffffff, flags);
		size += writeString(value, encoding, false);
		
		putInt32(sizePtr, size);
		
		return size + 10;
	}
	
	/********************************************************************************
	 * id3 frames
	 ********************************************************************************/
	
	public int writeLeadPerformer(String leadPerformer) {		
		return writeTextInformationFrame("TPE1", 0, leadPerformer, encoding);
	}
	
	public int writeTitle(String title) {
		return writeTextInformationFrame("TIT2", 0, title, encoding);
	}
	
	public int writeYear(String year) {
		return writeTextInformationFrame("TYER", 0, year, encoding);
	}
	
	public int writeAlbum(String album) {
		return writeTextInformationFrame("TALB", 0, album, encoding);
	}	
	
	public int writeCover(byte[] data, String mime) {
		
//		<Header for 'Attached picture', ID: "APIC"> 
//		Text encoding   $xx
//		MIME type       <text string> $00
//		Picture type    $xx
//		Description     <text string according to encoding> $00 (00)
//		Picture data    <binary data>
		
		int sizePtr = count + 4;
		int size = 0;
		
		writeFrameHeader("APIC", 0xffffffff, 0);
		size += writeString(mime, Encoding.Latin_1, true); // always use LATIN-1 for MIME type
		size += writeInt8(0x03);
		size += writeInt8(0);
		size += writeBinary(data);
		
		putInt32(sizePtr, size);
		
		return size + 10;
	}
	
	/********************************************************************************
	 * misc
	 ********************************************************************************/	
	
	public int getLength() {
		return count;
	}
	
	public Encoding getEncoding() {
		return encoding;
	}

	public void setEncoding(Encoding encoding) {
		this.encoding = encoding;
	}

	/********************************************************************************
	 * synchsafe en/decoding
	 ********************************************************************************/	
	
	public static int unsynchsafe(int in) {
		int out = 0;
		int mask = 0x7F000000;
	 
		for(int i=0; i<4; ++i) {
			out >>= 1;
			out |= in & mask;
			mask >>= 8;
		}
	 
		return out;
	}
	
	public static int synchsafe(int in) {
		int out = 0;
		int mask = 0x7F;
	 
		for(int i=0; i<4; ++i) {
			out |= in & mask;
			in <<= 1;
			mask <<= 8;
		}
	 
		return out;
	}

	@Override
	public synchronized byte[] toByteArray() {
		try {
			flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// update size in header
		if (id3header) {
			putInt32(6, synchsafe(count-10));
		}
		// return buffer
		return super.toByteArray();
	}

	@Override
	public synchronized String toString() {
		byte[] array = toByteArray();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < array.length; i++) {
			sb.append(i).append(":\t");
			sb.append( new String(new byte[]{ array[i] }, ISO_8859_1) ).append('\t');
			sb.append(Integer.toHexString(array[i])).append('\t');
			sb.append('\n');
		}
		return sb.toString();
	}

}
