/*
 * Copyright 2011 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jayway.jsonpath;


import com.jayway.jsonpath.internal.PathToken;
import com.jayway.jsonpath.internal.PathTokenizer;
import com.jayway.jsonpath.internal.filter.Filter;
import com.jayway.jsonpath.spi.JsonProviderFactory;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * <p/>
 * JsonPath is to JSON what XPATH is to XML, a simple way to extract parts of a given document. JsonPath is
 * available in many programming languages such as Javascript, Python and PHP.
 * <p/>
 * JsonPath allows you to compile a json path string to use it many times or to compile and apply in one
 * single on demand operation.
 * <p/>
 * Given the Json document:
 * <p/>
 * <code>
 * String json =
 * "{
 * "store":
 * {
 * "book":
 * [
 * {
 * "category": "reference",
 * "author": "Nigel Rees",
 * "title": "Sayings of the Century",
 * "price": 8.95
 * },
 * {
 * "category": "fiction",
 * "author": "Evelyn Waugh",
 * "title": "Sword of Honour",
 * "price": 12.99
 * }
 * ],
 * "bicycle":
 * {
 * "color": "red",
 * "price": 19.95
 * }
 * }
 * }";
 * </code>
 * <p/>
 * A JsonPath can be compiled and used as shown:
 * <p/>
 * <code>
 * JsonPath path = JsonPath.compile("$.store.book[1]");
 * <br/>
 * List&lt;Object&gt; books = path.read(json);
 * </code>
 * </p>
 * Or:
 * <p/>
 * <code>
 * List&lt;Object&gt; authors = JsonPath.read(json, "$.store.book[*].author")
 * </code>
 * <p/>
 * If the json path returns a single value (is definite):
 * </p>
 * <code>
 * String author = JsonPath.read(json, "$.store.book[1].author")
 * </code>
 *
 * @author Kalle Stenflo
 */
public class JsonPath {

    private static Pattern DEFINITE_PATH_PATTERN = Pattern.compile(".*(\\.\\.|\\*|\\[[\\\\/]|\\?|,|:\\s?\\]|\\[\\s?:|>|\\(|<|=|\\+).*");

    private PathTokenizer tokenizer;

    private JsonPath(String jsonPath) {
        if (jsonPath == null ||
                jsonPath.trim().isEmpty() ||
                jsonPath.matches("[^\\?\\+\\=\\-\\*\\/\\!]\\(")) {

            throw new InvalidPathException("Invalid path");
        }
        this.tokenizer = new PathTokenizer(jsonPath);
    }

    public String getPath() {
        return this.tokenizer.getPath();
    }

    /**
     * Checks if a path points to a single item or if it potentially returns multiple items
     * <p/>
     * a path is considered <strong>not</strong> definite if it contains a scan fragment ".."
     * or an array position fragment that is not based on a single index
     * <p/>
     * <p/>
     * definite path examples are:
     * <p/>
     * $store.book
     * $store.book[1].title
     * <p/>
     * not definite path examples are:
     * <p/>
     * $..book
     * $.store.book[1,2]
     * $.store.book[?(@.category = 'fiction')]
     *
     * @return true if path is definite (points to single item)
     */
    public boolean isPathDefinite() {
        String preparedPath = getPath().replaceAll("\"[^\"\\\\\\n\r]*\"", "");

        return !DEFINITE_PATH_PATTERN.matcher(preparedPath).matches();
    }

    /**
     * Applies this JsonPath to the provided json document.
     * Note that the document must either a {@link List} or a {@link Map}
     *
     * @param json a container Object ({@link List} or {@link Map})
     * @param <T>  expected return type
     * @return list of objects matched by the given path
     */
    @SuppressWarnings({"unchecked"})
    public <T> T read(Object json) {
        if (!(json instanceof Map) && !(json instanceof List)) {
            throw new IllegalArgumentException("Invalid container object");
        }

        Object result = json;

        boolean inArrayContext = false;

        for (PathToken pathToken : tokenizer) {
            Filter filter = pathToken.getFilter();
            result = filter.filter(result, JsonProviderFactory.getInstance(), inArrayContext);

            if (!inArrayContext) {
                inArrayContext = filter.isArrayFilter();
            }
        }
        return (T) result;
    }

    /**
     * Applies this JsonPath to the provided json string
     *
     * @param json a json string
     * @param <T>  expected return type
     * @return list of objects matched by the given path
     */
    @SuppressWarnings({"unchecked"})
    public <T> T read(String json) {
        return (T) read(JsonProviderFactory.getInstance().parse(json));
    }

    /**
     * Applies this JsonPath to the provided json URL
     *
     * @param jsonURL url to read from
     * @param <T>     expected return type
     * @return list of objects matched by the given path
     * @throws IOException
     */
    @SuppressWarnings({"unchecked"})
    public <T> T read(URL jsonURL) throws IOException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(jsonURL.openStream()));
            return (T) read(JsonProviderFactory.getInstance().parse(in));
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Applies this JsonPath to the provided json file
     *
     * @param jsonFile file to read from
     * @param <T>      expected return type
     * @return list of objects matched by the given path
     * @throws IOException
     */
    @SuppressWarnings({"unchecked"})
    public <T> T read(File jsonFile) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(jsonFile);
            return (T) read(JsonProviderFactory.getInstance().parse(fis));
        } finally {
            IOUtils.closeQuietly(fis);
        }
    }

    /**
     * Applies this JsonPath to the provided json input stream
     *
     * @param jsonInputStream input stream to read from
     * @param <T>             expected return type
     * @return list of objects matched by the given path
     * @throws IOException
     */
    @SuppressWarnings({"unchecked"})
    public <T> T read(InputStream jsonInputStream) throws IOException {
        try {
            return (T) read(JsonProviderFactory.getInstance().parse(jsonInputStream));
        } finally {
            IOUtils.closeQuietly(jsonInputStream);
        }
    }

    // --------------------------------------------------------
    //
    // Static factory methods
    //
    // --------------------------------------------------------

    /**
     * Compiles a JsonPath
     *
     * @param jsonPath to compile
     * @return compiled JsonPath
     */
    public static JsonPath compile(String jsonPath) {
        return new JsonPath(jsonPath);
    }


    // --------------------------------------------------------
    //
    // Static utility functions
    //
    // --------------------------------------------------------

    /**
     * Creates a new JsonPath and applies it to the provided Json string
     *
     * @param json     a json string
     * @param jsonPath the json path
     * @param <T>      expected return type
     * @return list of objects matched by the given path
     */
    @SuppressWarnings({"unchecked"})
    public static <T> T read(String json, String jsonPath) {
        return (T) compile(jsonPath).read(json);
    }

    /**
     * Creates a new JsonPath and applies it to the provided Json object
     *
     * @param json     a json object
     * @param jsonPath the json path
     * @param <T>      expected return type
     * @return list of objects matched by the given path
     */
    @SuppressWarnings({"unchecked"})
    public static <T> T read(Object json, String jsonPath) {
        return (T) compile(jsonPath).read(json);
    }

    /**
     * Creates a new JsonPath and applies it to the provided Json object
     *
     * @param jsonURL  url pointing to json doc
     * @param jsonPath the json path
     * @param <T>      expected return type
     * @return list of objects matched by the given path
     */
    @SuppressWarnings({"unchecked"})
    public static <T> T read(URL jsonURL, String jsonPath) throws IOException {
        return (T) compile(jsonPath).read(jsonURL);
    }

    /**
     * Creates a new JsonPath and applies it to the provided Json object
     *
     * @param jsonFile  json file
     * @param jsonPath the json path
     * @param <T>      expected return type
     * @return list of objects matched by the given path
     */
    @SuppressWarnings({"unchecked"})
    public static <T> T read(File jsonFile, String jsonPath) throws IOException {
        return (T) compile(jsonPath).read(jsonFile);
    }

    /**
     * Creates a new JsonPath and applies it to the provided Json object
     *
     * @param jsonInputStream  json input stream
     * @param jsonPath the json path
     * @param <T>      expected return type
     * @return list of objects matched by the given path
     */
    @SuppressWarnings({"unchecked"})
    public static <T> T read(InputStream jsonInputStream, String jsonPath) throws IOException {
        return (T) compile(jsonPath).read(jsonInputStream);
    }

}
