package com.pl3.helper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

public class QuoteBySymbol {
    String url;
    String symbol;

    public QuoteBySymbol(String symbol) {
        this.symbol = symbol;
        this.url = "https://cloud.iexapis.com/stable/stock/" + symbol + "/batch?types=quote&token=pk_f535647735c344969d3ba45244040413";
    }

    private String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public JSONObject readJsonFromUrl() throws IOException, JSONException {
        URL currentUrl = new URL(this.url);
        HttpURLConnection http = (HttpURLConnection)currentUrl.openConnection();
        int statusCode = http.getResponseCode();

        if (statusCode != 200) {
            return null;
        }

        InputStream is = currentUrl.openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONObject json = new JSONObject(jsonText);
            is.close();
            return json;
        }
        finally {
            is.close();
        }
    }
}
