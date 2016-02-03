import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import android.os.AsyncTask;
import android.util.Log;
import com.squareup.okhttp.OkHttpClient;
import org.apache.http.protocol.HTTP;

// TODO: JavaDoc
public abstract class AsyncRequest<Entity> extends AsyncTask<Object, Void, Entity> {
	private static final String TAG = AsyncRequest.class.getSimpleName();

	private static final int CONNECT_TIMEOUT = 15000;
	private static final int READ_TIMEOUT = 20000;

	private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
	private static final String CONTENT_ENCODING_GZIP = "gzip";

	public enum State {
		INIT, // before connect
		SEND, // sending request body
		READ, // retrieving response body
		DONE  // connection closed
	}

	public enum Method {
		OPTIONS(false),
		GET(false),
		HEAD(false),
		POST(true),
		PUT(true),
		DELETE(true),
		TRACE(false),
		PATCH(true);

		Method(boolean hasPayload) {
			this.hasPayload = hasPayload;
		}
		private final boolean hasPayload;

		//GET, POST, PUT, PATCH, DELETE, COPY, HEAD, OPTIONS, LINK, UNLINK, PURGE, LOCK, UNLOCK, PROPFIND, VIEW
	}

	public AsyncRequest(final Method method, URL url) throws IOException {
		super();
		this.url = url;
		this.method = method;
		//this.connection = (HttpURLConnection) this.url.openConnection();

		// using okHttp connection.disconnect closes the connection.
		OkHttpClient client = new OkHttpClient();
		this.connection = client.open(this.url);
	}

	protected void sendRequest(OutputStream out) throws Exception {}

	protected abstract Entity readResponse(InputStream in) throws Exception;

	protected abstract void onError(Exception error);

	protected abstract void onResult(Entity result);

	// request
	public URL getUrl() {
		return url;
	}
	public Method getMethod() {
		return method;
	}
	public boolean isAcceptGzip() {
		return acceptGzip;
	}

	public AsyncRequest acceptGzipEncoding() {
		this.assertState(State.INIT);
		this.acceptGzip = true;
		return this;
	}
	public AsyncRequest setRequestHeader(String key, String value) {
		this.connection.setRequestProperty(key, value);
		return this;
	}
	public String getRequestHeader(String key) {
		return this.connection.getRequestProperty(key);
	}
	// TODO: useHttpCache

	// cancel
	public void setCancelable() {
		this.assertState(State.INIT);
		this.cancelable = true;
	}

	// response
	public int getResponseCode() {
		this.assertState(State.DONE);
		return this.responseCode;
	}
	public String getResponseMessage() {
		this.assertState(State.DONE);
		return this.responseMessage;
	}
	public String getResponseHeader(String key) {
		this.assertState(State.DONE);
		return this.connection.getHeaderField(key);
	}

	// implementation
	@Override
	protected Entity doInBackground(Object... param) {
		InputStream in = null;
		OutputStream out = null;
		try {
			this.state = State.SEND;
			if (this.isCancelled()) {
				throw new InterruptedIOException();
			}
			// open connection
			this.connection.setConnectTimeout(CONNECT_TIMEOUT);
			this.connection.setReadTimeout(READ_TIMEOUT);
			this.connection.setRequestMethod(this.method.name());
			// set or override headers
			if (this.acceptGzip) {
				this.connection.setRequestProperty(HEADER_ACCEPT_ENCODING, CONTENT_ENCODING_GZIP);
			}
			if (this.debugStream != null) {
				this.debugStream.onConnect(connection);
			}
			this.connection.connect();

			// send payload
			if (this.method.hasPayload) {
				out = this.connection.getOutputStream();
				if (this.cancelable) {
					out = new CancelableOutputStream(out);
				}
				if (this.debugStream != null) {
					out = this.debugStream.startSend(out);
				}
				this.sendRequest(out);
			}

			// retrieve result
			this.state = State.READ;
			if (this.isCancelled()) {
				throw new InterruptedIOException();
			}
			this.responseCode = this.connection.getResponseCode();
			this.responseMessage = this.connection.getResponseMessage();

			in = this.connection.getInputStream();
			// use the cancelable stream
			if (this.cancelable) {
				in = new CancelableInputStream(in);
			}
			if (CONTENT_ENCODING_GZIP.equals(this.connection.getContentEncoding())) {
				in = new GZIPInputStream(in);
			}
			if (this.debugStream != null) {
				in = this.debugStream.startRead(in);
			}

			// return entity
			return this.readResponse(in);
		}
		catch (Exception e) {
			Log.e(TAG, "Error in request: " + this.url, e);
			this.error = e;
		}
		finally {
			if (in != null) {
				try {
					in.close();
				}
				catch (IOException ignore) {
				}
			}
			if (out != null) {
				try {
					out.close();
				}
				catch (IOException ignore) {
				}
			}
			try {
				this.connection.disconnect();
			}
			catch (Exception ignore) {
			}
			if (this.error == null) {
				if (this.debugStream != null) {
					this.debugStream.onDisconnect();
				}
			}
			this.state = State.DONE;
		}
		return null;
	}

	@Override
	protected void onPostExecute(Entity result) {
		if (this.error != null) {
			this.onError(this.error);
		}
		else {
			this.onResult(result);
		}
	}

	private void assertState(State state) {
		if (this.state != state) {
			throw new IllegalStateException();
		}
	}

	// request
	private final URL url;
	private final Method method;
	private boolean acceptGzip = false;

	// response
	private int responseCode = 0;
	private String responseMessage = null;
	private Exception error = null;

	private volatile State state = State.INIT;
	private volatile boolean cancelable = false;

	private final HttpURLConnection connection;
	private final DebugStream debugStream = new DebugStream();

	/// cancel reading response when {@cancel} is true;
	private class CancelableInputStream extends InputStream {
		private final InputStream stream;

		private CancelableInputStream(InputStream stream) {
			this.stream = stream;
		}

		@Override
		public int read() throws IOException {
			if (AsyncRequest.this.isCancelled()) {
				throw new InterruptedIOException();
			}
			return stream.read();
		}
		@Override
		public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
			if (AsyncRequest.this.isCancelled()) {
				throw new InterruptedIOException();
			}
			return stream.read(buffer, byteOffset, byteCount);
		}

	}

	/// cancel sending request when {@cancel} is true;
	private class CancelableOutputStream extends OutputStream {
		private final OutputStream stream;

		private CancelableOutputStream(OutputStream stream) {
			this.stream = stream;
		}

		@Override
		public void write(int oneByte) throws IOException {
			if (AsyncRequest.this.isCancelled()) {
				throw new InterruptedIOException();
			}
			stream.write(oneByte);
		}
		@Override
		public void write(byte[] buffer, int offset, int count) throws IOException {
			if (AsyncRequest.this.isCancelled()) {
				throw new InterruptedIOException();
			}
			stream.write(buffer, offset, count);
		}
	}

	// utility class to check communication
	private static final class DebugStream {

		// configuration
		boolean logResponseHeaders = !true;
		boolean logRequestHeaders = !true;
		boolean logResponseBody = !true;
		boolean logRequestBody = !true;
		boolean logStatistics = true;
		boolean logStackTrace = !true;
		int maxLogLength = 2048;

		long tsStart = 0;
		long tsSend = 0;
		long tsRead = 0;
		long tsEnd = 0;

		String url;
		String method;
		long requestSize = 0;
		long responseSize = 0;
		Exception stacktrace = null;
		HttpURLConnection connection = null;

		private final OutputStream text = new ByteArrayOutputStream() {

			private boolean incomplete = false;

			@Override
			public synchronized void write(int oneByte) {
				if (this.size() > maxLogLength) {
					incomplete = true;
					return;
				}
				super.write(oneByte);
			}

			@Override
			public synchronized void write(byte[] buffer, int offset, int len) {
				if (this.size() > maxLogLength) {
					incomplete = true;
					return;
				}
				super.write(buffer, offset, len);
			}

			@Override
			public synchronized String toString() {
				if (incomplete) {
					return super.toString() + "...";
				}
				return super.toString();
			}
		};

		private static final String BINARY_DATA = "<binary data>";
		private static final String MISSING_HEADERS = "<no headers>";
		private static final String TAG = "AsyncRequest.DbgStream";

		public DebugStream() throws IOException {
			if (this.logStackTrace) {
				this.stacktrace = new Exception();
			}
		}

		void onConnect(HttpURLConnection connection) {
			this.tsStart = System.currentTimeMillis();

			this.connection = connection;
			this.url = connection.getURL().toString();
			this.method = connection.getRequestMethod();

			PrintStream ps = new PrintStream(text);
			Map<String, List<String>> headers = connection.getRequestProperties();
			if (logRequestHeaders) {
				ps.append("\nrequest.headers: {");
				headers(ps, headers);
				ps.append("}");
			}
			if (this.logRequestBody) {
				ps.append("\nrequest.body: ");
				if (!isTextContent(headers)) {
					this.logRequestBody = false;
					ps.append(BINARY_DATA);
				}
			}
		}

		OutputStream startSend(OutputStream stream) {
			this.tsSend = System.currentTimeMillis();

			if (stream != null) {
				final OutputStream finalStream = stream;
				stream = new OutputStream() {
					@Override
					public void write(int oneByte) throws IOException {
						finalStream.write(oneByte);
						if (logRequestBody) {
							DebugStream.this.text.write(oneByte);
						}
						DebugStream.this.requestSize += 1;
					}
					@Override
					public void write(byte[] buffer, int offset, int count) throws IOException {
						finalStream.write(buffer, offset, count);
						if (DebugStream.this.logRequestBody) {
							DebugStream.this.text.write(buffer, offset, count);
						}
						DebugStream.this.requestSize += count;
					}
				};
			}
			return stream;
		}

		InputStream startRead(InputStream stream) {
			this.tsRead = System.currentTimeMillis();

			PrintStream ps = new PrintStream(text);
			Map<String, List<String>> headers = this.connection.getHeaderFields();
			if (this.logResponseHeaders) {
				ps.append("\nresponse.headers: {");
				headers(ps, headers);
				ps.append("}");
			}
			if (this.logResponseBody) {
				ps.append("\nresponse.body: ");
				if (!isTextContent(headers)) {
					this.logResponseBody = false;
					ps.append(BINARY_DATA);
				}
			}
			final InputStream finalStream = stream;
			return new InputStream() {
				@Override
				public int read() throws IOException {
					int result = finalStream.read();
					if (result != -1) {
						if (DebugStream.this.logResponseBody) {
							DebugStream.this.text.write(result);
						}
						DebugStream.this.responseSize += 1;
					}
					return result;
				}

				@Override
				public int read(byte[] buffer, int offset, int length) throws IOException {
					int result = finalStream.read(buffer, offset, length);
					if (result != -1) {
						if (DebugStream.this.logResponseBody) {
							DebugStream.this.text.write(buffer, offset, result);
						}
						DebugStream.this.responseSize += result;
					}
					return result;
				}
			};
		}

		void onDisconnect() {
			this.tsEnd = System.currentTimeMillis();

			StringBuilder log = new StringBuilder();

			// log method and url
			log.append(this.method).append(": ").append(android.net.Uri.decode(this.url));

			if (this.logStatistics) {
				double time = (this.tsEnd - this.tsStart) / 1000.;
				log.append("\nconnection.time: ").append(time).append(" sec");
				if (requestSize > 0) {
					long requestTime = this.tsRead - this.tsSend;
					log.append(", request: ").append(formatSize(requestSize))
						.append(" in ").append(requestTime / 1000.).append(" sec")
						.append(" (").append(formatSpeed(requestSize, requestTime)).append(')');
				}
				long responseTime = this.tsEnd - this.tsRead;
				log.append(", response: ").append(formatSize(this.responseSize))
					.append(" in ").append(responseTime / 1000.).append(" sec")
					.append(" (").append(formatSpeed(this.responseSize, responseTime)).append(')');
			}
			Log.d(TAG, log.append(text.toString()).toString(), this.stacktrace);
		}

		private void headers(PrintStream stream, Map<String, List<String>> headers, String quote, String headerSeparator, String valueSeparator) {
			if (headers == null) {
				stream.append(MISSING_HEADERS);
				return;
			}

			boolean firstKey = true;
			for (String key : headers.keySet()) {
				if (!firstKey) {
					stream.append(headerSeparator);
				}
				else {
					firstKey = false;
				}
				stream.append(quote).append(key).append(quote).append(valueSeparator);
				List<String> values = headers.get(key);
				boolean firstValue = true;
				stream.append(quote);
				for (String value : values) {
					if (!firstValue) {
						stream.append(";");
					}
					else {
						firstValue = false;
					}
					stream.append(value);
				}
				stream.append(quote);
			}
		}

		private void headers(PrintStream stream, Map<String, List<String>> headers) {
			headers(stream, headers, "'", ", ", ": ");
		}

		boolean isTextContent(Map<String, List<String>> headers) {
			List<String> header = headers.get(HTTP.CONTENT_TYPE);
			if (header == null) {
				header = Collections.emptyList();
			}
			return isTextContent(header.size() > 0 ? header.get(0) : null);
		}

		boolean isTextContent(String contentType) {
			if (contentType == null || contentType.equals("")) {
				// used by tests
				return true;
			}
			else if (contentType.contains("text")) {
				return true;
			}
			else if (contentType.contains("json")) {
				return true;
			}
			else if (contentType.contains("csv")) {
				return true;
			}
			return false;
		}

		String formatSize(long sizeBytes) {
			if (sizeBytes > (1 << 30)) {
				return String.format("%.2f GB", (double) sizeBytes / (1 << 30));
			}
			if (sizeBytes > (1 << 20)) {
				return String.format("%.2f MB", (double) sizeBytes / (1 << 20));
			}
			if (sizeBytes > (1 << 10)) {
				return String.format("%.2f KB", (double) sizeBytes / (1 << 10));
			}
			return String.format("%d Bytes", sizeBytes);
		}

		String formatSpeed(long sizeBytes, long timeMillis) {
			if (timeMillis > 0) {
				sizeBytes = sizeBytes * 1000 / timeMillis;
			}
			if (sizeBytes > (1 << 30)) {
				return String.format("%.2f GB/s", (double) sizeBytes / (1 << 30));
			}
			if (sizeBytes > (1 << 20)) {
				return String.format("%.2f MB/s", (double) sizeBytes / (1 << 20));
			}
			if (sizeBytes > (1 << 10)) {
				return String.format("%.2f KB/s", (double) sizeBytes / (1 << 10));
			}
			return String.format("%d Bytes/s", sizeBytes);
		}
	}
}
