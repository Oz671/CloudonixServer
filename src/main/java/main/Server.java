package main;

import java.util.TreeMap;

import io.vertx.core.*;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.*;

/*
 * For running the server run this cmd command in the project directory:
 * mvn org.codehaus.mojo:exec-maven-plugin:exec -Dexec.executable=java -Dexec.args="-cp %classpath io.vertx.core.Launcher run main.Server"
 */
public class Server extends AbstractVerticle {
	private Router router;
	/*
	 * Used TreeMap to make the search to be O(logN) where N is the number of
	 * words in the collection
	 */
	private TreeMap<Integer, String> valueToWord = new TreeMap<Integer, String>();
	private TreeMap<String, String> lexicalToWord = new TreeMap<String, String>();

	private void updateCollections(String word) {
		if (lexicalToWord.containsKey(word))
			return;

		valueToWord.put(calculateValue(word), word);
		lexicalToWord.put(word, word);
	}

	private Integer calculateValue(String word) {
		int sum = 0;
		for (int i = 0; i < word.length(); i++) {
			sum += (word.charAt(i) - 'a' + 1);
		}
		return sum;
	}

	private String wrap(String str) {
		return str == null ? "null" : "\"" + str + "\"";
	}

	private String createResponse(String value, String lexical) {
		return "{\"value\": " + wrap(value) + ", \"lexical\": " + wrap(lexical)
				+ "}";
	}

	private String emptyReponse() {
		return createResponse(null, null);
	}

	@Override
	public void start(Future<Void> fut) throws Exception {
		router = Router.router(vertx);
		router.post("/analyze").handler(
				routingContext -> {
					HttpServerResponse response = routingContext.response();
					routingContext.request().bodyHandler(
							body -> {
								System.out.println(body);
								JsonObject json = new JsonObject(body);
								if (!json.containsKey("text")) {
									response.putHeader("content-type",
											"text/json").end(emptyReponse());
									return;
								}

								String word = json.getString("text")
										.toLowerCase();
								if (word.trim().contains(" ")) {
									response.putHeader("content-type",
											"text/json").end(emptyReponse());
									return;
								}

								if (lexicalToWord.isEmpty()) {
									response.putHeader("content-type",
											"text/json").end(emptyReponse());
								} else {
									String value = getValueOfWord(word);
									String lexical = getLexicalOfWord(word);
									response.putHeader("content-type",
											"text/json").end(
											createResponse(value, lexical));
								}

								// update the collection
								updateCollections(word);
							});
				});

		vertx //
		.createHttpServer() //
				.requestHandler(router::accept) //
				.listen(config().getInteger("http.port", 8080), result -> {
					if (result.succeeded()) {
						fut.complete();
					} else {
						fut.fail(result.cause());
					}
				});
	}

	private String getLexicalOfWord(String word) {
		String before = lexicalToWord.floorKey(word);
		String after = lexicalToWord.ceilingKey(word);

		if (before == null)
			return after;

		if (after == null)
			return before;

		int beforeDistance = calculateLevinsteinDistance(before, word);
		int afterDistance = calculateLevinsteinDistance(after, word);

		int min = Math.min(beforeDistance, afterDistance);

		return min == beforeDistance ? before : after;
	}

	private int calculateLevinsteinDistance(String a, String b) {
		a = a.toLowerCase();
		b = b.toLowerCase();

		int[] costs = new int[b.length() + 1];
		for (int j = 0; j < costs.length; j++)
			costs[j] = j;
		for (int i = 1; i <= a.length(); i++) {
			costs[0] = i;
			int nw = i - 1;
			for (int j = 1; j <= b.length(); j++) {
				int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
						a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
				nw = costs[j];
				costs[j] = cj;
			}
		}
		return costs[b.length()];

	}

	private String getValueOfWord(String word) {
		int value = calculateValue(word);
		Integer before = valueToWord.floorKey(value);
		Integer after = valueToWord.ceilingKey(value);
		if (before == null && after == null)
			return null;

		if (before == null && after != null)
			return valueToWord.get(after);

		if (before != null && after == null)
			return valueToWord.get(before);

		int closest = value - before < after - value ? before : after;
		return valueToWord.get(closest);
	}
}
