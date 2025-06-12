package kmz.utils.entity;

import kmz.utils.XmlParser;

import javax.xml.bind.annotation.XmlValue;

public class Bookstore {
	public Book[] book;

	public static class Book {
		public Title title;
		public String author;
		public int year;
		public double price;
		public String category;
		public String text;
	}

	public static class Title {
		public String lang;
		@XmlValue
		@XmlParser.Name(name = XmlParser.VALUE)
		public String text;
	}
}
