/**
 * 
 */
package de.braintags.vertx.auth;

import io.vertx.core.json.JsonObject;

/**
 * @author mremme
 *
 */
public class JsonUtil {

	static public Object getPath(JsonObject base, String... path) {
		JsonObject current=base;
		// descend, but skip the very last element
		for(int i=0;i < path.length-1;++i) {
			current=(JsonObject) current.getJsonObject( path[i]);
		}
		return current.getValue(path[path.length-1]);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getPath(Class<T> clazz, JsonObject obj, String[] path) {
		Object o = getPath(obj,path);
		if(clazz.isInstance(o)) {
			return (T) o;
		}
		return null;
	}

}
