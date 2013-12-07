package main;

import id3v2.Id3Generator;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.swing.UIManager;

import proxy.ProxyListener;
import proxy.ProxyServer;
import proxy.ProxyServerThread;
import ui.ConsoleFrame;
import xspf.Parser;
import xspf.Track;

public class Malcolm implements ProxyListener {

	private ByteArrayOutputStream xmlBuffer; // note: piped streams produce dead locks here
	private HashMap<String, Track> trackDatabase; // location -> track meta info
	
	private ProxyServer proxy;
	
	private int port;
	private String dir;
	private PrintStream dump;
	private PrintStream proxyDump;
	java.util.HashSet<String> excludedFiles = new HashSet<String>();
	
	private long startTime;	
	
	public Malcolm(PrintStream dump, PrintStream proxyDump, String dir, int port, java.util.HashSet<String> excludedFiles) {
		this.port = port;
		this.dir = dir;
		this.dump = dump;
		this.proxyDump = proxyDump;
		this.excludedFiles = excludedFiles;
		
		startTime = System.currentTimeMillis();
		
		dump("using output directory: " + dir);
		dump("using server port: " + port);
		dump("ignoring " + excludedFiles.size() + " files");
		
		xmlBuffer = new ByteArrayOutputStream();
		trackDatabase = new HashMap<String, Track>();
	}	
	
	public void start() {
		try {			
			//proxy = new ProxyServer(port, null);
			proxy = new ProxyServer(port, proxyDump);
			proxy.setListener(this);
			proxy.start();
			dump("Proxy started at port " + port);
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	/****************************************************************************************
	 * Event Handler
	 ****************************************************************************************/	
	
	@Override
	public void request(ProxyServerThread sender, String request, HashMap<String, String> header) {
		// parse request
		String[] req = request.split(" ");
		String url = req[1];
		
		if (url.contains("http://ws.audioscrobbler.com/radio/xspf.php")) { // client requested the playlist
			dump("client requested playlist");
			sender.setName("xspf"); // mark this thread as playlist-receiving thread
			sender.setResponseFork(xmlBuffer); // redirect body to XML buffer
		} else if (trackDatabase.containsKey(url)) { // requested location is known in track database
			Track track = trackDatabase.get(url);
			dump("client requested song from playlist: " + track.getTitle());
			if (!new File(dir, track.getFullFilename()).exists()) { // track already exists
				if (!excludedFiles.contains(track.getFilename().toLowerCase())) { // track is excluded
					sender.setName("mp3" + "\n" + url); // mark this thread as mp3-receiving thread (and remember URL)
				} else {					
					dump("excluded: " + track.getFullFilename());
				}
			} else {
				dump("already downloaded: " + track.getFullFilename());
			}
		}
	}
	
	@Override
	public void response(ProxyServerThread sender, String response, HashMap<String, String> header) {
		String threadName = sender.getName();
		
		// parse response
		String[] res = response.split(" ");
		int responseCode = Integer.parseInt(res[1]);
		int contentLength = header.containsKey("content-length") ? Integer.parseInt(header.get("content-length")) : -1;
		
		if (threadName.startsWith("mp3")) { // thread was marked as mp3-receiving thread
			String url = threadName.split("\n")[1]; // retrieve requested URL
			Track track = trackDatabase.get(url); // meta data to URL
			
			switch (responseCode) {
				case 302: // Redirect
					// add new location to database
					track.setLocation( header.get("location") );
					trackDatabase.remove(url); // remove URL from database
					trackDatabase.put(track.getLocation(), track); // add with updated location
					dump("redirect: " + url + " -> " + header.get("location"));
					break;
				case 200: // OK					
					dump("saving " + track.getTitle() + " to: " + track.getTempFilename());
					track.setNumberOfBytes(contentLength);
					try {
						FileOutputStream fileOutputStream = new FileOutputStream(new File(dir,track.getTempFilename()));
						sender.setResponseFork(fileOutputStream); // redirect body to file
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
					break;
				default:
					break;
			}
		}
	}	
	
	@Override
	public void closed(ProxyServerThread sender) {	
		String threadName = sender.getName();
		
		if (threadName.equals("xspf")) { // playlist transmitted			
			try {
				// parse playlist
				List<Track> tracks = Parser.parse(new ByteArrayInputStream(xmlBuffer.toByteArray()));
				// store results in database
				for (Track track : tracks) {
					trackDatabase.put(track.getLocation(), track);
				}
				// clear XML buffer (ready for new playlist)
				xmlBuffer.reset();
				dump("received playlist with " + tracks.size() + " entries");
				for (Track track : tracks) {
					dump("\t" + track.getCreator() + " - " + track.getTitle());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (threadName.startsWith("mp3")) { // mp3 transmitted
			String url = threadName.split("\n")[1]; // retrieve requested URL
			Track track = trackDatabase.get(url); // meta data to URL
			try {
				if (sender.getResponseFork() != null) {
					// close file
					sender.getResponseFork().close();
					
					// create artist folder if necessary
					File artistFolder = new File(dir, track.getDirectory());
					if (!artistFolder.exists()) {
						artistFolder.mkdir();
					}
					
					// rename file
					File tempFile = new File(dir, track.getTempFilename());
					File finalFile = new File(dir, track.getFullFilename());
					File file = tempFile;
					
//					tempFile.renameTo(finalFile);
//					file = finalFile;					
//					// download cover
//					StringBuffer mime = new StringBuffer();
//					dump("downloading cover of " + track.getAlbum() + ": " + track.getImage());
//					byte[] image = downloadImage(track.getImage(), mime);
//					// remove possibly existing id3 tags from downloaded file					
//					RandomAccessFile mp3 = new RandomAccessFile(file, "rw");
//					byte[] buffer = new byte[(int) mp3.length()];
//					mp3.readFully(buffer);
//					buffer = removeId3(buffer);
//					// generate new id3v2 tag
//					Id3Generator id3 = new Id3Generator();
//					id3.writeLeadPerformer(track.getCreator());
//					id3.writeTitle(track.getTitle());
//					id3.writeAlbum(track.getAlbum());
//					if (image != null) {
//						id3.writeCover(image, mime.toString());
//					} else {
//						dump("could not downlaod: " + track.getImage());
//					}
//					// write new tag and raw mp3 data back to file
//					mp3.seek(0);
//					mp3.setLength(id3.getLength() + buffer.length);
//					mp3.write(id3.toByteArray());
//					mp3.write(buffer);
//					mp3.close();					
					
					if ( track.getNumberOfBytes() < 0 || file.length() == track.getNumberOfBytes() ) { // download finished
						tempFile.renameTo(finalFile);
						file = finalFile;
						dump("finished: " + file.getName());
						
						// download cover
						StringBuffer mime = new StringBuffer();
						dump("downloading cover of " + track.getAlbum() + ": " + track.getImage());
						byte[] image = downloadImage(track.getImage(), mime);						
						
						// remove possibly existing id3 tags from downloaded file					
						RandomAccessFile mp3 = new RandomAccessFile(file, "rw");
						byte[] buffer = new byte[(int) mp3.length()];
						mp3.readFully(buffer);
						buffer = removeId3(buffer);
						
						// generate new id3v2 tag
						Id3Generator id3 = new Id3Generator();
						id3.writeLeadPerformer(track.getCreator());
						id3.writeTitle(track.getTitle());
						id3.writeAlbum(track.getAlbum());
						if (image != null) {
							id3.writeCover(image, mime.toString());
						} else {
							dump("could not downlaod: " + track.getImage());
						}
						
						// write new tag and raw mp3 data back to file
						mp3.seek(0);
						mp3.setLength(id3.getLength() + buffer.length);
						mp3.write(id3.toByteArray());
						mp3.write(buffer);
						mp3.close();
						
						dump("updated id3: " + file.getName());
					} else {
						dump("aborted: " + file.getName());
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			trackDatabase.remove(url); // remove URL from database
		}
	}
	
	/****************************************************************************************
	 * Misc
	 ****************************************************************************************/	
	
	public static byte[] downloadImage(String uri, StringBuffer mime) {		
		byte[] data = null;
		
		try {
			URL url = new URL(uri);
			URLConnection connection = url.openConnection();
			String contentType = connection.getContentType();
			int length = connection.getContentLength();
			InputStream in = url.openStream();
			int bytesRead = 0;
			data = new byte[length];
			
			if (contentType.contains("image/jpeg")) {
				mime.append("image/jpeg");
			} else if(contentType.contains("image/png")) {
				mime.append("image/png");
			} else {
				System.err.println("unknown image: " + uri + " " + contentType);
				return null;				
			}
			
			while (bytesRead < length) {
				bytesRead += in.read(data, bytesRead, length - bytesRead);
			}			
		} catch (Exception e) {
			return null;
		} 
		
		return data;
	}
	
	public static byte[] removeId3(byte[] buffer) {
		byte[] tagfree = null;
		int copyStar = 0;
		int copyLength = buffer.length;
		
//		http://www.id3.org:
//		An ID3v2 tag can be detected with the following pattern:
//		$49 44 33 yy yy xx zz zz zz zz 
//		Where yy is less than $FF, xx is the 'flags' byte and zz is less than $80.		
		
		// check for id2v2 tag (dynamic size; at the beginning)
		if ((buffer[0] == 0x49 && buffer[1] == 0x44 && buffer[2] == 0x33) && 
			(buffer[3] < 0xff && buffer[4] < 0xff) && 
			(buffer[6] < 0x80 && buffer[7] < 0x80 && buffer[8] < 0x80 && buffer[9] < 0x80))
		{ // ID3 tag in file
			// header size + tag size
			int size = 10 + Id3Generator.unsynchsafe( buffer[6]<<24 | buffer[7]<<16 | buffer[8]<<8 | buffer[9] );

			// reset copy markers
			copyStar += (size); // header size + tag size
			copyLength -= (size);
		}

//		http://www.id3.org:
//		The easiest way to find a ID3v1/1.1 tag is to look for the word "TAG" 128 bytes from the end of a file.

		// check for id3v1 tag (static size; at the end)
		int l = buffer.length;
		if (buffer[l-128] == 0x54 && buffer[l-127] == 0x41 && buffer[l-126] == 0x47) {
			System.out.println("id1");
			// reset copy marker
			copyLength -= 128;
		}
		
		// extract tag free mp3 data
		tagfree = new byte[copyLength];
		System.arraycopy(buffer, copyStar, tagfree, 0, copyLength);
		
		return tagfree;
	}
	
	private void dump(String msg) {
		if (dump != null) {
			dump.println((System.currentTimeMillis() - startTime) + " : " + msg);
		}
	}

	/****************************************************************************************
	 * Entry Point
	 ****************************************************************************************/

	public static void main(String[] args) throws Exception {
		// default values
		int port = 8080;
		String dir = ".";
		boolean gui = true;
		
		// parse arguments
		int i;
		for (i = 0; i < args.length; i++) {
			if (args[i].equals("/nogui")) {
				gui = false;
			}
			
			if (args[i].equals("/dir")) {
				try {
					dir = ( args[++i] );
				} catch (Exception e) {}
			}
			
			if (args[i].equals("/port")) {
				try {
					port = Integer.parseInt( args[++i] );
				} catch (Exception e) {}
			}			
		}
		
		// build set of excluded files from index
		java.util.HashSet<String> excludedFiles = new HashSet<String>();
		File index = new File(dir + "/index.txt");
		if (index.exists()) {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(index)));
			String line;
			while ( (line = br.readLine()) != null) {
				excludedFiles.add(line.toLowerCase());
			}
		}		
		
		// start server
		if (gui) {
			// run in GUI mode
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			
			ConsoleFrame console = new ConsoleFrame();
			console.setTitle("Malcolm - Last.fm Proxy Server");
			
			System.setErr(console.getPrintStream());
			console.setVisible(true);
			
			new Malcolm(console.getPrintStream(), null, dir, port, excludedFiles).start();			
		} else {
			// run in command line mode
			new Malcolm(System.out, null, dir, port, excludedFiles).start();
		}
		
		//new Malcolm(System.err, System.out, dir, port).start();
	}

}
