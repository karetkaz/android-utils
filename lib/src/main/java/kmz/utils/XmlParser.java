package kmz.utils;

import android.util.Log;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.Reader;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

// todo: add a dictionary / map parser/mapper
// todo: maybe split this file, extract the inner parser classes
// todo: maybe allow annotation on class declarations, not only on fields

/**
 * Simple Gson like Xml deserializer.
 * @see XmlParserTest for more information on usage
 */
public class XmlParser {
	protected static final String TAG = "XmlParser";

	/**
	 * default constant for {@link Name#name()} to parse values like cdata or text
	 */
	public static final String VALUE = "";

	/**
	 * Parser annotation to be used on fields
	 * <p>
	 * {@link Name#name()} specifies the tag or attribute name
	 * <p>
	 * {@link Name#wrapper()} can be used in case the value or list of values are wrapped inside an extra tag
	 * <p>
	 * {@link Name#mapper()} can be used in case the value needs to be mapped with a custom parser
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Name {

		String name();

		String wrapper() default "";

		Class<? extends Mapper> mapper() default Mapper.class;
	}

	/**
	 * Base Mapper interface to convert string values to different types like numbers, dates, enums, etc.
	 * @param <T> the result type of the conversion
	 */
	public interface Mapper<T> {
		T valueOf(String value) throws Exception;
	}


	protected final XmlPullParser parser;

	public XmlParser() throws XmlPullParserException {
		parser = Xml.newPullParser();
		parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
	}

	public <T> T readValue(Reader input, Class<T> type) throws Exception {
		parser.setInput(input);
		consume(XmlPullParser.START_DOCUMENT, 0, null);
		require(XmlPullParser.START_TAG, 1, null);
		String rootTag = parser.getName();
		TypeInfo root = new TypeInfo(type, rootTag, null);
		Object result;

		if (root.collection != null) {
			// todo: both parsing should be similarly handled
			consume(XmlPullParser.START_TAG, 1, rootTag);
			require(XmlPullParser.START_TAG, 2, null); // skip over any empty text to next tag
			ArrayParser arrParser = new ArrayParser(root.type, parser.getName(), null);
			ArrayList<?> values = arrParser.parse(this, arrParser.values);
			result = root.mergeArray(null, values);
			consume(XmlPullParser.END_TAG, 1, rootTag);
		} else {
			result = root.mapper.parse(this, root);
		}

		consume(XmlPullParser.END_DOCUMENT, 0, null);
		return type.cast(result);
	}

	protected String readText(int depth) throws XmlPullParserException, IOException {
		require(XmlPullParser.TEXT, depth, null);
		String text = parser.getText();
		parser.next();
		return text;
	}

	protected boolean peek(int type, int depth) throws XmlPullParserException, IOException {
		if (type != XmlPullParser.TEXT && parser.getEventType() == XmlPullParser.TEXT) {
			// skip blank text (tabs spaces and new lines) between tags
			if (parser.getText().isBlank()) {
				parser.next();
			}
		}
		if (depth >= 0 && depth != parser.getDepth()) {
			return false;
		}
		return type == parser.getEventType();
	}

	protected void require(int type, int depth, String name) throws XmlPullParserException, IOException {
		if (!peek(type, -1)) {
			throw new XmlPullParserException("expected token: " + XmlPullParser.TYPES[type] + " at: " + parser.getPositionDescription());
		}
		if (depth >= 0 && depth != parser.getDepth()) {
			throw new XmlPullParserException("unexpected depth: " + depth + " != " + parser.getDepth() + " at: " + parser.getPositionDescription());
		}
		if (name != null && !name.equals(parser.getName())) {
			throw new XmlPullParserException("expected name: " + name + " at: " + parser.getPositionDescription());
		}
		parser.require(type, null, name);
	}

	protected void consume(int type, int depth, String name) throws XmlPullParserException, IOException {
		require(type, depth, name);
		parser.next();
	}


	/**
	 * Internal mapper for parsing value types, like text, number, ...
	 */
	private static class ValueParser<T> implements Mapper<T> {

		@Override
		public T valueOf(String value) throws Exception {
			throw new XmlPullParserException("Use a Custom mapper to convert values");
		}

		protected T parse(XmlParser parser, TypeInfo type) throws Exception {
			int depth = parser.parser.getDepth();
			if (type.name == null || type.name.isEmpty()) {
				return valueOf(parser.readText(depth));
			}

			parser.consume(XmlPullParser.START_TAG, depth, type.name);
			if (parser.peek(XmlPullParser.END_TAG, depth)) {
				parser.consume(XmlPullParser.END_TAG, depth, type.name);
				// no text for constructs like: `<tag/>`
				return valueOf(null);
			}

			T result = valueOf(parser.readText(depth));
			parser.consume(XmlPullParser.END_TAG, depth, type.name);
			return result;
		}

		protected void skip(XmlParser parser) throws XmlPullParserException, IOException {
			if (parser.peek(XmlPullParser.TEXT, -1)) {
				parser.parser.next();
				return;
			}

			int depth = parser.parser.getDepth();
			parser.require(XmlPullParser.START_TAG, depth, null);
			while (!parser.peek(XmlPullParser.END_TAG, depth)) {
				parser.parser.next();
			}
			parser.consume(XmlPullParser.END_TAG, depth, null);
		}
	}


	/**
	 * Internal mapper for custom types, delegates the conversion to the mapper.
	 * It can be used for duration, datetime or any other custom format conversion.
	 */
	private static class CustomParser extends ValueParser<Object> {
		private final Mapper<?> mapper;

		public CustomParser(Mapper<?> mapper) {
			this.mapper = mapper;
		}

		public CustomParser(Name name) throws Exception {
			this(name.mapper().getDeclaredConstructor().newInstance());
		}

		@Override
		public Object valueOf(String value) throws Exception {
			return mapper.valueOf(value);
		}
	}


	/**
	 * Internal mapper for enumerated types, returns null in case there are no matches
	 */
	private static class EnumParser extends ValueParser<Enum<?>> {
		private final Enum<?>[] values;

		public EnumParser(Class<?> type) {
			this.values = (Enum<?>[]) type.getEnumConstants();
		}

		@Override
		public Enum<?> valueOf(String name) {
			for (Enum<?> value : values) {
				if (name.equals(value.name())) {
					return value;
				}
			}
			return null;
		}
	}


	/**
	 * Internal mapper for parsing arrays and collections
	 */
	private static class ArrayParser extends ValueParser<ArrayList<Object>> {
		private final TypeInfo values;

		public ArrayParser(Class<?> type, String name, ValueParser<?> mapper) throws XmlPullParserException {
			this.values = new TypeInfo(type, name, mapper);
		}

		@Override
		protected ArrayList<Object> parse(XmlParser parser, TypeInfo type) throws Exception {
			ArrayList<Object> result = new ArrayList<>();
			int depth = parser.parser.getDepth();
			int wrapped = depth;

			if (!type.wrapper.isEmpty()) {
				parser.consume(XmlPullParser.START_TAG, depth, type.wrapper);
				wrapped = depth + 1;
			}

			while (!parser.peek(XmlPullParser.END_TAG, -1)) {
				if (wrapped == depth && !type.name.equals(parser.parser.getName())) {
					// stop parsing if list tag names change, probably a different list
					break;
				}
				parser.require(XmlPullParser.START_TAG, wrapped, type.name);
				Object value = values.mapper.parse(parser, values);
				if (value == null) {
					throw new XmlPullParserException("Invalid array element");
				}
				result.add(value);
			}

			if (wrapped != depth) {
				parser.consume(XmlPullParser.END_TAG, depth, type.wrapper);
			}

			return result;
		}
	}


	/**
	 * Internal mapper for objects: recursive parsing of the subtree
	 */
	private static class ObjectParser extends ValueParser<Object> {
		private final Constructor<?> constructor;
		private final HashMap<String, TypeInfo> fields = new HashMap<>();

		public ObjectParser(Class<?> type) {
			try {
				this.constructor = type.getDeclaredConstructor();
				this.constructor.setAccessible(true);
			} catch (Exception e) {
				throw new RuntimeException("No constructor for type: " + type, e);
			}
		}

		public void cacheFields(Class<?> type) throws XmlPullParserException {
			for (Class<?> base = type; base != null; base = base.getSuperclass()) {
				if (base == Object.class) {
					break;
				}
				for (Field field : base.getDeclaredFields()) {
					if (Modifier.isStatic(field.getModifiers())) {
						continue;
					}
					if (Modifier.isTransient(field.getModifiers())) {
						continue;
					}
					if (field.isSynthetic()) {
						continue;
					}
					try {
						field.setAccessible(true);
						TypeInfo info = new TypeInfo(field);
						if (!info.wrapper.isEmpty()) {
							fields.put(info.wrapper, info);
						} else {
							fields.put(info.name, info);
						}
					} catch (XmlPullParserException e) {
						throw e;
					} catch (Exception e) {
						throw new XmlPullParserException("Field `" + field.getName() + "` is not accessible in: " + type.getCanonicalName(), null, e);
					}
				}
			}
		}

		@Override
		protected Object parse(XmlParser parser, TypeInfo type) throws Exception {
			Object result = this.constructor.newInstance();
			int depth = parser.parser.getDepth();
			int wrapped = depth;

			// deserialize attributes first.
			parser.require(XmlPullParser.START_TAG, wrapped, type.name);
			for (int i = 0; i < parser.parser.getAttributeCount(); i += 1) {
				TypeInfo field = this.fieldOf(parser.parser.getAttributeName(i));
				if (field == null) {
					continue;
				}
				field.setValue(result, field.mapper.valueOf(parser.parser.getAttributeValue(i)));
			}

			if (!type.wrapper.isEmpty()) {
				parser.consume(XmlPullParser.START_TAG, depth, type.wrapper);
				wrapped = depth + 1;
			}

			parser.consume(XmlPullParser.START_TAG, wrapped, type.name);
			while (!parser.peek(XmlPullParser.END_TAG, depth)) {

				TypeInfo field = fieldOf(parser.parser.getName());
				if (field == null) {
					skip(parser);
					continue;
				}

				Object value = field.mapper.parse(parser, field);
				if (value == null) {
					continue;
				}

				field.setValue(result, value);
			}

			parser.consume(XmlPullParser.END_TAG, wrapped, type.name);

			if (wrapped != depth) {
				parser.consume(XmlPullParser.END_TAG, depth, type.wrapper);
			}

			return result;
		}

		private TypeInfo fieldOf(String value) {
			return fields.get(value == null ? VALUE : value);
		}
	}


	/**
	 * Internal reflection type for a field containing the tag name in the XML document, constructor, parser, etc.
	 */
	private static class TypeInfo {
		private static final TypeCache typeCache = new TypeCache();
		private static final Object[] EMPTY_OBJECT_ARRAY = {};

		protected final String wrapper;
		protected final String name;

		private final Class<?> collection;
		private final Class<?> type;
		private final Field field;

		protected final ValueParser<?> mapper;

		public TypeInfo(Field field) throws Exception {
			this.field = field;
			this.type = getComponentType(field);

			// collection or an object
			if (this.type != field.getType()) {
				this.collection = field.getType();
			} else {
				this.collection = null;
			}

			Name name = field.getAnnotation(Name.class);
			if (name == null) {
				this.wrapper = "";
				this.name = field.getName();
				if (this.collection != null) {
					this.mapper = new ArrayParser(this.type, this.name, null);
				} else {
					this.mapper = typeCache.getMapper(this.type);
				}
				return;
			}

			this.wrapper = name.wrapper();
			this.name = name.name();

			ValueParser<?> mapper;
			if (name.mapper() == Mapper.class) {
				// no mapper specified, do not use custom mapper
				mapper = typeCache.getMapper(this.type);
			} else {
				mapper = new CustomParser(name);
			}

			if (this.collection != null) {
				this.mapper = new ArrayParser(this.type, this.name, mapper);
			} else {
				this.mapper = mapper;
			}
		}

		public TypeInfo(Class<?> type, String name, ValueParser<?> customMapper) throws XmlPullParserException {
			this.wrapper = "";
			this.name = name;
			this.field = null;
			if (type.isArray() || Collection.class.isAssignableFrom(type)) {
				this.collection = type;
				this.type = type.getComponentType();
				this.mapper = new ArrayParser(this.type, name, customMapper);
			}
			else if (customMapper != null) {
				this.collection = null;
				this.type = type;
				this.mapper = customMapper;
			}
			else {
				this.collection = null;
				this.type = type;
				this.mapper = typeCache.getMapper(type);
			}
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		public Object mergeArray(Object oldValue, ArrayList<?> values) {
			if (!collection.isArray()) {
				if (oldValue != null && this.wrapper.isEmpty()) {
					values.addAll(0, (Collection) oldValue);
				}
				return values;
			}

			Object[] oldValues = EMPTY_OBJECT_ARRAY;
			if (oldValue != null && this.wrapper.isEmpty()) {
				oldValues = (Object[]) oldValue;
			}
			Object result = Array.newInstance(type, oldValues.length + values.size());
			for (int i = 0; i < oldValues.length; i++) {
				Array.set(result, i, oldValues[i]);
			}
			for (int i = 0; i < values.size(); i++) {
				Array.set(result, i + oldValues.length, values.get(i));
			}
			return result;
		}

		public void setValue(Object instance, Object value) throws IllegalAccessException {
			if (this.collection != null) {
				Object oldValue = this.field.get(instance);
				value = mergeArray(oldValue, (ArrayList<?>) value);
			}

			/* DEBUG
			if (oldValue != null && !this.type.isPrimitive()) {
				Log.w("TAG", "updating an existing value: " + this + ", value: " + oldValue);
			}// */
			this.field.set(instance, value);
		}

		private static Class<?> getComponentType(Field field) {
			Class<?> type = field.getType();
			if (type.isArray()) {
				return type.getComponentType();
			}

			if (Collection.class.isAssignableFrom(type)) {
				ParameterizedType generic = (ParameterizedType) field.getGenericType();
				return (Class<?>) generic.getActualTypeArguments()[0];
			}

			return type;
		}

		@Override
		public String toString() {
			return "FieldInfo { '" +
					name + "': " + type.getSimpleName() +
					(collection == null ? "" : "[]") +
					" }";
		}
	}


	/**
	 * A small type-cache to speed up the parsing.
	 */
	private static class TypeCache {

		private final HashMap<Type, ValueParser<?>> cache = new HashMap<>();

		public TypeCache() {
			cache.put(Boolean.class, boolMapper);
			cache.put(boolean.class, boolMapper);
			cache.put(Byte.class, byteMapper);
			cache.put(byte.class, byteMapper);
			cache.put(Short.class, shortMapper);
			cache.put(short.class, shortMapper);
			cache.put(Integer.class, integerMapper);
			cache.put(int.class, integerMapper);
			cache.put(Long.class, longMapper);
			cache.put(long.class, longMapper);
			cache.put(Float.class, floatMapper);
			cache.put(float.class, floatMapper);
			cache.put(Double.class, doubleMapper);
			cache.put(double.class, doubleMapper);
			cache.put(Character.class, charMapper);
			cache.put(char.class, charMapper);
			cache.put(Object.class, textMapper);
			cache.put(String.class, textMapper);
			cache.put(Void.class, nullMapper);
		}

		public ValueParser<?> getMapper(Class<?> type) throws XmlPullParserException {
			if (type.isArray() || Collection.class.isAssignableFrom(type)) {
				throw new RuntimeException("Arrays not supported here yet");
			}

			synchronized (this) {
				ValueParser<?> cached = cache.get(type);
				if (cached != null) {
					return cached;
				}

				if (Enum.class.isAssignableFrom(type)) {
					ValueParser<?> mapper = new EnumParser(type);
					cache.put(type, mapper);
					return mapper;
				}

				ObjectParser mapper = new ObjectParser(type);
				cache.put(type, mapper);
				mapper.cacheFields(type);
				return mapper;
			}
		}

		private static char parseChar(String value) {
			if (value.length() != 1) {
				Log.w(TAG, String.format("failed to convert text to char: %s", value));
			}
			if (value.isEmpty()) {
				return 0;
			}
			return value.charAt(0);
		}

		private static final ValueParser<Boolean> boolMapper = new ValueParser<>() {
			@Override
			public Boolean valueOf(String value) {
				return Boolean.parseBoolean(value);
			}
		};
		private static final ValueParser<Byte> byteMapper = new ValueParser<>() {
			@Override
			public Byte valueOf(String value) {
				return Byte.parseByte(value);
			}
		};
		private static final ValueParser<Short> shortMapper = new ValueParser<>() {
			@Override
			public Short valueOf(String value) {
				return Short.parseShort(value);
			}
		};
		private static final ValueParser<Integer> integerMapper = new ValueParser<>() {
			@Override
			public Integer valueOf(String value) {
				return Integer.parseInt(value);
			}
		};
		private static final ValueParser<Long> longMapper = new ValueParser<>() {
			@Override
			public Long valueOf(String value) {
				return Long.parseLong(value);
			}
		};
		private static final ValueParser<Float> floatMapper = new ValueParser<>() {
			@Override
			public Float valueOf(String value) {
				return Float.parseFloat(value);
			}
		};
		private static final ValueParser<Double> doubleMapper = new ValueParser<>() {
			@Override
			public Double valueOf(String value) {
				return Double.parseDouble(value);
			}
		};
		private static final ValueParser<Character> charMapper = new ValueParser<>() {
			@Override
			public Character valueOf(String value) {
				return parseChar(value);
			}
		};
		private static final ValueParser<String> textMapper = new ValueParser<>() {
			@Override
			public String valueOf(String value) {
				return value;
			}
		};
		private static final ValueParser<Object> nullMapper = new ValueParser<>() {
			@Override
			public Object valueOf(String value) {
				return null;
			}
		};
	}
}
