/*
 * MpakFile.java
 * 
 * Copyright (c) 2012, Marko Harjula. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.  
*/

package lan.sahara.mpkloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class MpakFile {
	private Map<String,ByteArrayOutputStream> fileCache = Collections.synchronizedMap( new HashMap<String,ByteArrayOutputStream>() );
	private FileInputStream fileInputStream = null; 
	private FileChannel fileChannel=null;
	public String name = null;
	private Map<String,MpakObject> fileContent = new HashMap<String,MpakObject>();
	public MpakFile() {}
	
	/**
	 * @param file as path to mpak File
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws DataFormatException
	 */
	public MpakFile(File file) throws FileNotFoundException,IOException, DataFormatException {
		open(file);
	}
	
	/**
	 * <pre>
	 * {@code
	 * try {
	 *     MpakFile treemap = new MpakFile(new File(srcPath+"/trees/treemap.mpk"));
	 *     ByteArrayOutputStream treemapcsv = treeMap.readFile("treemap.csv");
	 * } catch(...
	 * }
	 * </pre> 
	 * @param file as path to mpak File
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws DataFormatException
	 */
	@SuppressWarnings("unused")
	public void open(File file) throws FileNotFoundException,IOException, DataFormatException {
		fileInputStream = new FileInputStream(file);
		fileChannel = fileInputStream.getChannel();
		ByteBuffer head = ByteBuffer.allocate(4);
		fileChannel.read(head);
		if ( !  new String(head.array()).equals("MPAK")) {
			fileInputStream.close();
			throw new DataFormatException("Header not match");
		}
		// TODO: identify 5->20 bytes
		fileChannel.position(21); // "head" done
		// stage0 // package name
		ByteArrayOutputStream stage0 = unCompress(80,null);
		name = stage0.toString().toLowerCase();
		// stage1 // file index list
		ByteArrayOutputStream stage1 = unCompress(null,null);
		ByteBuffer indexes = ByteBuffer.wrap( stage1.toByteArray() ).order(ByteOrder.LITTLE_ENDIAN);
		long loc_pointer = fileChannel.position();
		while ( indexes.hasRemaining() ) {
			byte[] byteArrayFilename = new byte[256];
			indexes.get(byteArrayFilename);
			String filename = new String(byteArrayFilename).split("\0")[0].toLowerCase();
			int unixtimestamp = indexes.getInt();
			int attrs = indexes.getInt();
			int uncompressed_seek = indexes.getInt();
			int uncompressed_size = indexes.getInt();
			long compressed_seek = (long)indexes.getInt() + loc_pointer;
			int compressed_size = indexes.getInt();
			int file_sum = indexes.getInt(); // TODO: identify this value
			fileContent.put(new String(filename), new MpakObject(compressed_seek,compressed_size,uncompressed_size,unixtimestamp,file_sum));
		}
	}
	
	/**
	 * @param filename inside mpak file
	 * @return file content in ByteArrayOutputStream
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws DataFormatException
	 */
	public ByteArrayOutputStream readFile(String filename) throws FileNotFoundException,IOException,DataFormatException {
		if ( ! fileContent.containsKey(filename) )
			throw new FileNotFoundException("Compressed file "+filename+" not found!");
		MpakObject c = fileContent.get(filename);
		if ( fileCache.containsKey(filename) ) // read from cache
			return fileCache.get(filename);
		fileChannel.position( c.seek ); // read from file
		ByteArrayOutputStream bb = unCompress(c.compressed_size,c.uncompressed_size);
		synchronized (fileCache) {	// thread safe write
			fileCache.put(filename,bb); // build simple caching if load same file multiple times
		}
		return bb;
	}

	/**
	 * used for junit testing
	 * @return all data as entry set Set&lt;Map.Entry&lt;"filename",ByteArrayOutputStream&gt;&gt;
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws DataFormatException
	 */
	public Set<Map.Entry<String,ByteArrayOutputStream>> entrySet() throws FileNotFoundException, IOException, DataFormatException {
		HashMap<String,ByteArrayOutputStream> ret = new HashMap<String,ByteArrayOutputStream>();
		for ( Entry<String, MpakObject> cs : fileContent.entrySet() ) {
			ret.put(cs.getKey(), readFile(cs.getKey()));
		}
		return ret.entrySet();
	}
	
	/**
	 * @param compressed_size or use null if not known
	 * @param uncompressed_size or use null if not known
	 * @return uncompressed ByteArrayOutputStream
	 * @throws IOException
	 * @throws DataFormatException
	 */
	private ByteArrayOutputStream unCompress(Integer compressed_size,Integer uncompressed_size) throws IOException, DataFormatException {
		byte[] uncompressed_data=null;
		byte[] input_data=null;
		ByteArrayOutputStream ret = new ByteArrayOutputStream();
		Inflater decompresser = new Inflater(false);
		long first_seek = fileChannel.position();
		Boolean uncompressing = true;
		while ( uncompressing ) {
			if ( decompresser.needsInput() ) {
				input_data = new byte[(compressed_size!=null)?compressed_size.intValue():1024];
				fileChannel.read(ByteBuffer.wrap(input_data));
				decompresser.setInput(input_data, 0, input_data.length);
			}
			uncompressed_data = new byte[(uncompressed_size!=null)?uncompressed_size.intValue():(input_data.length*4)];
			decompresser.inflate(uncompressed_data);
			int op=(int)(decompresser.getBytesWritten()-(long)ret.size());
			if (op > 0 ) 
				ret.write(uncompressed_data,0,op);

			if ( decompresser.finished() ) 
				uncompressing = false;
		}
		fileChannel.position( (first_seek + decompresser.getBytesRead()) ); // move file pointer to start of next stream
		decompresser.end();
		return ret;
	}
	
	/**
	 * @param name of compressed file
	 * @return if can be found as boolean
	 */
	public boolean contains(String name) {
		return fileContent.containsKey(name);
	}
	
	/**
	 * cleanup everything
	 * @throws IOException
	 */
	public void close() throws IOException {
		fileChannel.close();
		fileInputStream.close();
		fileContent.clear();
		synchronized (fileCache) {	// thread safe
			fileCache.clear();
		}
		name = null;
	}
}