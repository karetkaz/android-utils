# android-utils
Utility classes for Android

using AsyncRequest class:
```java
// ...
final URL url = new URL("http://host:8080/data.xml");
AsyncRequest<DataEntity> request = new AsyncRequest<DataEntity>(AsyncRequest.Method.GET, url) {

	@Override
	protected DataEntity readResponse(InputStream in) throws Exception {
		return new XmlParser().readValue(in, DataEntity.class);
	}

	@Override
	public void onError(Exception error) {
		Log.e(TAG, "error: " + this.getResponseCode(), error);
	}

	@Override
	public void onResult(DataEntity result) {
		Log.d(TAG, "response: " + this.getResponseCode() + ": " + this.getResponseMessage());
	}
};

// allow server to compress the response
request.acceptGzipEncoding();

// execute the request
request.execute();

// ...
```
