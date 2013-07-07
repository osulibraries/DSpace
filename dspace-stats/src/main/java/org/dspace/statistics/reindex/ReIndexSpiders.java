package org.dspace.statistics.reindex;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.statistics.ElasticSearchLogger;
import org.dspace.statistics.util.SpiderDetector;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestController;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to use the org.dspace.statistics.reindex plugin as rewrite/refeed plugin - directly from
 * java.
 *
 * @author Peter Karich
 */
public class ReIndexSpiders {

    private final static String charset = "UTF-8";

    private static Logger log = Logger.getLogger(ReIndexSpiders.class);

    public static void main(String[] args) {
        //TODO consider reindexing to new type?
        String indexName   = ConfigurationManager.getConfigurationStringWithFallBack("elastic-search-statistics", "indexName", "dspaceindex");
        String indexType   = ConfigurationManager.getConfigurationStringWithFallBack("elastic-search-statistics", "indexType", "stats");

        String searchIndexName = indexName;
        String searchType = indexType;
        String newIndexName = indexName;
        String newType = indexType;

        String filter = "{\n" +
                "   \"query\": {\n" +
                "       \"bool\" : {\n" +
                "           \"must\" : { \n" +
                "                    \"match_all\" : { }\n" +
                "           }, \n" +
                "           \"must_not\" : [\n" +
                "                  {\n" +
                "                    \"term\" : {\n" +
                "                       \"isBot\" : true\n" +
                "                    }\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"term\" : {\n" +
                "                       \"isBotUA\" : true\n" +
                "                    }\n" +
                "                  },\n" +
                "                  {\n" +
                "                    \"term\" : {\n" +
                "                       \"roboChecked\" : false\n" +
                "                    }\n" +
                "                   },\n" +
                "                   {\n" +
                "                      \"term\" : {\n" +
                "                            \"roboChecked\" : true\n" +
                "                      }\n" +
                "                   }\n" +
                "           ]\n" +
                "       },\n" +
                "      \"constant_score\" : {\n" +
                "        \"filter\" : {\n" +
                "          \"and\" : [\n" +
                "              {\n" +
                "                \"exists\" : { \"field\" : \"userAgent\" }\n" +
                "              }\n" +
                "            ]\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "}";


        boolean withVersion = false;
        final int hitsPerPage = 500;
        float waitInSeconds = 0.1f;
        // increase if you have lots of things to update
        int keepTimeInMinutes = 90;



        log.info("ElasticSearch Reindexing is querying: " + searchIndexName);

        Client client = ElasticSearchLogger.getInstance().getClient();
        if(client == null) {
            log.info("ElasticSearchReindex could not get client, aborting...");
            return;
        }

        Settings emptySettings = ImmutableSettings.settingsBuilder().build();
        RestController contrl = new RestController(emptySettings);
        ReIndexAction action = new ReIndexAction(emptySettings, client, contrl) {
            protected MySearchHits callback(MySearchHits hits) {
                SimpleList res = new SimpleList(hitsPerPage, hits.totalHits());
                Iterable<MySearchHit> hitsIterator = hits.getHits();

                for (MySearchHit h : hitsIterator) {
                    try {
                        String str = new String(h.source(), charset);
                        RewriteSearchHit newHit = new RewriteSearchHit(h.id(), h.version(), str);

                        //Detect if this hit was from a robot, based on available IP / DNS / UserAgent
                        String userAgent = newHit.get("userAgent");

                        if(SpiderDetector.isSpiderByUserAgent(userAgent)) {
                            newHit.put("isBotUA", true);
                        } else {
                            newHit.put("isBotUA", false);
                        }


                        newHit.put("roboChecked", true);

                        res.add(newHit);
                    } catch (Exception e) {
                        if(e.getMessage().contains("Duplicate key")) {
                            log.warn("Invalid hit, based on UA, skipping?");
                        } else {
                            throw new RuntimeException(e);
                        }
                    }
                }
                return res;
            }
        };
        // first query, further scroll-queries in org.dspace.statistics.reindex!
        SearchRequestBuilder srb = action.createScrollSearch(searchIndexName, searchType, filter,
                hitsPerPage, withVersion, keepTimeInMinutes);
        SearchResponse sr = srb.execute().actionGet();
        MySearchResponse rsp = new MySearchResponseES(client, sr, keepTimeInMinutes);

        // now feed and call callback
        action.reindex(rsp, newIndexName, newType, withVersion, waitInSeconds);

        client.close();
    }

    public static class SimpleList implements MySearchHits {

        long totalHits;
        List<MySearchHit> hits;

        public SimpleList(int size, long total) {
            hits = new ArrayList<MySearchHit>(size);
            totalHits = total;
        }

        public void add(MySearchHit hit) {
            hits.add(hit);
        }

        @Override public Iterable<MySearchHit> getHits() {
            return hits;
        }

        @Override
        public long totalHits() {
            return totalHits;
        }
    }

    public static class RewriteSearchHit implements MySearchHit {

        String id;
        long version;
        JSONObject json;

        public RewriteSearchHit(String id, long version, String jsonStr) {
            this.id = id;
            this.version = version;
            try {
                json = new JSONObject(jsonStr);
            } catch (JSONException ex) {
                //java.lang.RuntimeException: org.json.JSONException: Duplicate key "userAgent"
                log.error(jsonStr);
                throw new RuntimeException(ex);
            }
        }

        public String get(String key) {
            try {
                if (!json.has(key))
                    return "";
                String val = json.getString(key);
                if (val == null)
                    return "";
                return val;
            } catch (JSONException ex) {
                throw new RuntimeException(ex);
            }
        }

        public JSONObject put(String key, Object obj) {
            try {
                return json.put(key, obj);
            } catch (JSONException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override public String id() {
            return id;
        }

        @Override public long version() {
            return version;
        }

        @Override public byte[] source() {
            try {
                return json.toString().getBytes(charset);
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
