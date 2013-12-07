package xspf;

public class Track {
	
	public static final String unknownAlbum = "unknown album";
	public static final String unknownTitle = "unknown title";
	public static final String unknownArtist = "unknown artist";
	
	String location;
	String title;
	String album;
	String creator;
	String image;
	int duration;
	int numberOfBytes;
	 
	public Track() {
		location = null;
		title = unknownTitle;
		album = unknownAlbum;
		creator = unknownArtist;
		duration = -1;
		numberOfBytes = -1;
		image = null;
	}
	
	private String fileNameCapable(String value) {
		return value.replaceAll("[\\\\/:*?\"<>|]", "");
	}
	
	public String getFullFilename() {
		StringBuilder sb = new StringBuilder();
		sb.append(getDirectory()).append('/').append(getFilename());		
		return sb.toString();
	}
	
	public String getFilename() {
		StringBuilder sb = new StringBuilder();
		sb.append( fileNameCapable(creator) ).append(" - ").append( fileNameCapable(title) ).append(".mp3");		
		return sb.toString();
	}	
	
	public String getDirectory() {		
		String directory = fileNameCapable(creator);
		
		if (directory.toLowerCase().startsWith("the ")) {
			directory = directory.substring(4) + ", " + directory.substring(0,3);
		} else if (directory.toLowerCase().startsWith("a ")) {
			directory = directory.substring(2) + ", " + directory.substring(0,1);
		}
		
		return directory;
	}
	
	public String getTempFilename() {
		StringBuilder sb = new StringBuilder();
		sb.append( fileNameCapable(creator) ).append(" - ").append( fileNameCapable(title) ).append(".part");
		return sb.toString();
	}	

	public String getLocation() {
		return location;
	}

	public String getTitle() {
		return title;
	}

	public String getAlbum() {
		return album;
	}

	public String getCreator() {
		return creator;
	}

	public int getDuration() {
		return duration;
	}

	public int getNumberOfBytes() {
		return numberOfBytes;
	}

	public void setNumberOfBytes(int numberOfBytes) {
		this.numberOfBytes = numberOfBytes;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}
	
	
	
}
