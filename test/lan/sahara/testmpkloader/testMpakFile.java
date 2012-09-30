/*
 * testMpakFile.java
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

package lan.sahara.testmpkloader;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;

import lan.sahara.mpkloader.MpakFile;

import org.junit.Before;
import org.junit.Test;

public class testMpakFile {
	Boolean initOk = false;
	String srcPath = "C:/Program Files/EA/Dark Age of Camelot";

	@Before
	public void initialize() {
		// ask srcPath
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File(srcPath));
		chooser.setDialogTitle("Dark Age of Camelot base directory");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);
		if ( chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION ) {
			srcPath = chooser.getSelectedFile().getAbsolutePath().replace("\\", "/");
			initOk = true;
		}
	}

	@Test
	public void testLoadingAllZones() {
		Long size = 0l;
		int file_count = 0;
		long start = System.nanoTime();
		if (initOk == false) {
			fail("File choose failed!");
		} else {
			// load zone list
			MpakFile zones = null;
			try {
				zones = new MpakFile(new File(srcPath + "/zones/zones.mpk"));
				// uncompress zones.mpk and load data
				try {
					HashMap<String, HashMap<String, String>> p = parseIniData(zones.readFile("zones.dat"));
					MpakFile tester = null;
					for (Entry<String, HashMap<String, String>> a : p.entrySet()) {
						if (a.getKey().matches("zone(.*)")) {
							try {
								File file = new File(srcPath + "/zones/" + a.getKey());
								// load zone file
								if (file.isDirectory()) {
									tester = new MpakFile(new File(srcPath + "/zones/" + a.getKey() + "/dat" + a.getKey().replaceAll("\\D", "") + ".mpk"));
								}
								try {
									// try extract all with entrySet
									
									for( Entry<String, ByteArrayOutputStream> s : tester.entrySet() ){
										size += s.getValue().size();
										file_count++;
									}
									
								} catch (Exception e) {
									fail("Error in readFile, got "+e.getClass().getName()+" with " + a.getKey() + " : " + e.getMessage());
								}
							} catch (Exception e) {
								fail("Error in open, got "+e.getClass().getName()+" with " + a.getKey() + " : " + e.getMessage());
							}
						}
					}
					double seconds = ((double)( System.nanoTime() - start)) / 1000000000;
					System.out.println(file_count+" files loaded from zones, total size: "+readableFileSize(size) + ", time: "+seconds+", uncompressing speed: "+readableFileSize( (int)(size/seconds) )+"/sec");
				} catch (Exception e) {
					fail("Error in readFile, got "+e.getClass().getName()+" with zones.mpk : " + e.getMessage());
				}
			} catch (Exception e) {
				fail("Error in open, got "+e.getClass().getName()+" with zones.mpk : " + e.getMessage());
			}
		}
	}

	/**
	 * @param data ByteArrayOutputStream
	 * @return parsed ini file as HashMap<"group", HashMap<"key", "value">>
	 */
	private static HashMap<String, HashMap<String, String>> parseIniData(ByteArrayOutputStream data) {
		String[] sector = data.toString().split("\\n");
		Pattern exclude = Pattern.compile("^[;#]");
		Pattern d = Pattern.compile("^\\[([\\w|-]+)\\]$");
		Pattern lp = Pattern.compile("^(\\w+)=(.*+)$");
		HashMap<String, HashMap<String, String>> ret = new HashMap<String, HashMap<String, String>>();
		String group = null;
		if (sector.length > 0) {
			for (String l : sector) {
				l = l.trim();
				if (l.length() > 0 && !exclude.matcher(l).find()) { // drop empty lines and if excluded
					Matcher dm = d.matcher(l);
					if (dm.find()) { // matched group
						group = dm.group(1);
						if (!ret.containsKey(group))
							ret.put(group, new HashMap<String, String>());
					} else {
						Matcher lm = lp.matcher(l);
						if (lm.find()) {
							String val = lm.group(2).trim();
							if (val.length() == 0)
								val = null;
							ret.get(group).put(lm.group(1).trim(), val);
						} else {
							// System.err.println("Line not match:"+l);
						}
					}
				}
			}
		}
		return ret;
	}
	
	/**
	 * http://stackoverflow.com/questions/3263892/format-file-size-as-mb-gb-etc
	 * @param size size
	 * @return human readable size as String
	 */
	private static String readableFileSize(long size) {
	    if(size <= 0) return "0";
	    final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
	    int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
	    return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}
}
