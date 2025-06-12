package kmz.utils.mapper;

import kmz.utils.XmlParser;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateTimeMapper extends XmlAdapter<String, Date> implements XmlParser.Mapper<Date> {
	private static final String CUSTOM_DATETIME_FORMAT = "yyyy.MM.dd-HH:mm";

	ThreadLocal<SimpleDateFormat> dateTimeParserProvider = new ThreadLocal<>() {

		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat(CUSTOM_DATETIME_FORMAT, Locale.ROOT);
		}
	};

	@Override
	public Date valueOf(String value) throws Exception {
		return dateTimeParserProvider.get().parse(value);
	}

	@Override
	public Date unmarshal(String value) throws Exception {
		return dateTimeParserProvider.get().parse(value);
	}

	@Override
	public String marshal(Date v) {
		return dateTimeParserProvider.get().format(v);
	}
}
