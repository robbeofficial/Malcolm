package xspf;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Parser {
	public static List<Track> parse(InputStream is) throws ParserConfigurationException, SAXException, IOException {

		List<Track> tracks = new LinkedList<Track>();

		DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document document = documentBuilder.parse(is);

		NodeList trackNodes = document.getElementsByTagName("track");

		for (int i = 0; i < trackNodes.getLength(); i++) {
			Track track = new Track();
			
			Element trackElement = (Element) trackNodes.item(i);
			
			// try to read title
			try {
				track.title = trackElement.getElementsByTagName("title").item(0).getFirstChild().getTextContent();
			} catch (Exception e) {}
			
			// try to read 
			try {
				track.creator = trackElement.getElementsByTagName("creator").item(0).getFirstChild().getTextContent();
			} catch (Exception e) {}	
			
			// try to read location
			try {
				track.location = trackElement.getElementsByTagName("location").item(0).getFirstChild().getTextContent();
			} catch (Exception e) {}
			
			// try to read album
			try {
				track.album = trackElement.getElementsByTagName("album").item(0).getFirstChild().getTextContent();
			} catch (Exception e) {}			
			
			// try to read duration
			try {
				track.duration = Integer.parseInt(trackElement.getElementsByTagName("duration").item(0).getFirstChild().getTextContent());
			} catch (Exception e) {}
			
			// try to read image
			try {
				track.image = trackElement.getElementsByTagName("image").item(0).getFirstChild().getTextContent();
			} catch (Exception e) {}			
			
			tracks.add(track);
		}

		return tracks;
	}
}
