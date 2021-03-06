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
package com.jayway.jsonpath.spi.impl;

import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.spi.JsonProvider;
import com.jayway.jsonpath.spi.Mode;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONAware;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;

/**
 * @author Kalle Stenflo
 */
public class JsonSmartProvider extends AbstractJsonProvider {

    private Mode mode;

    private JSONParser parser;

    public JsonSmartProvider() {
        this(Mode.SLACK);
    }

    public JsonSmartProvider(Mode mode) {
        this.mode = mode;
        this.parser = new JSONParser(mode.intValue());
    }

    public Map<String, Object> createMap() {
        return new JSONObject();
    }

    public List<Object> createList() {
        return new JSONArray();
    }

    public Object parse(String json) {
        try {
            return parser.parse(json);
        } catch (ParseException e) {
            throw new InvalidJsonException(e);
        }
    }

    @Override
    public Object parse(Reader jsonReader) throws InvalidJsonException {
        try {
            return parser.parse(jsonReader);
        } catch (ParseException e) {
            throw new InvalidJsonException(e);
        }
    }

    @Override
    public Object parse(InputStream jsonStream) throws InvalidJsonException {
        try {
            return parser.parse(new InputStreamReader(jsonStream));
        } catch (ParseException e) {
            throw new InvalidJsonException(e);
        }
    }

    @Override
    public String toJson(Object obj) {
        if(!(obj instanceof JSONAware)){
            throw new InvalidJsonException();
        }
        JSONAware aware = (JSONAware)obj;

        return aware.toJSONString();
    }

    public Mode getMode() {
        return mode;
    }
}
