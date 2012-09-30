/*
 * MpakObject.java
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

import java.util.Date;

public class MpakObject {
	public Long seek = null;
	public Integer compressed_size = null;
	public Integer uncompressed_size = null;
	public Date timestamp = null;
	public Integer index_file_sum = null;
	public MpakObject(Long seek,Integer compressed_size,Integer uncompressed_size,Integer unixtimestamp,Integer index_file_sum) {
		this.seek=seek;
		this.compressed_size=compressed_size;
		this.uncompressed_size=uncompressed_size;
		this.timestamp = new Date((unixtimestamp*1000)); // sec -> msec
		this.index_file_sum = index_file_sum;
	}
	public String toString() {
		return "{seek:"+seek+",compressed:"+compressed_size+",uncompressed_size:"+uncompressed_size+"}";
	}
}
