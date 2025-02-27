package local.to.popcornmovies.utils;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import local.to.popcornmovies.models.AnimeEpisode;
import local.to.popcornmovies.models.AnimeEpisodeResponseModel;
import local.to.popcornmovies.models.AnimeWishSearchResultModel;
import local.to.popcornmovies.models.AnimeSearchResultResponseModel;
import local.to.popcornmovies.models.AnimeSearchResultResponseModel.Edge;
import local.to.popcornmovies.models.AnimeStreamingModel;
import local.to.popcornmovies.models.AnimeStreamingSource;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AllAnimeLinkUtils {

    public static final String
            TAG = "test->AllAnLnUt", ANIME_API, SEARCH_URL, ANIME_REFERER, ANIME_URL, SHOW_ID, VARIABLES, QUERY,
            REFERER, SEARCH, ALLOW_ADULT, ALLOW_UNKNOWN,
            LIMIT, PAGE, COUNTRY_ORIGIN, TRANSLATION_TYPE, SUB, ALL, EPISODE_STRING;

    static {
        // https://api.allanime.day
        ANIME_API = HexDecoder.fromHex("68747470733a2f2f6170692e616c6c616e696d652e646179");
        // /api
        SEARCH_URL = ANIME_API + HexDecoder.fromHex("2f617069");
        // https://allmanga.to
        ANIME_REFERER = HexDecoder.fromHex("68747470733a2f2f616c6c6d616e67612e746f");
        // https://allanime.day
        ANIME_URL = HexDecoder.fromHex("68747470733a2f2f616c6c616e696d652e646179");
        // showId
        SHOW_ID = HexDecoder.fromHex("73686f774964");
        // variables
        VARIABLES = HexDecoder.fromHex("7661726961626c6573");
        // query
        QUERY = HexDecoder.fromHex("7175657279");
        // Referer
        REFERER = HexDecoder.fromHex("52656665726572");
        // search
        SEARCH = HexDecoder.fromHex("736561726368");
        // allowAdult
        ALLOW_ADULT = HexDecoder.fromHex("616c6c6f774164756c74");
        // allowUnknown
        ALLOW_UNKNOWN = HexDecoder.fromHex("616c6c6f77556e6b6e6f776e");
        // limit
        LIMIT = HexDecoder.fromHex("6c696d6974");
        // page
        PAGE = HexDecoder.fromHex("70616765");
        // countryOrigin
        COUNTRY_ORIGIN = HexDecoder.fromHex("636f756e7472794f726967696e");
        // translationType
        TRANSLATION_TYPE = HexDecoder.fromHex("7472616e736c6174696f6e54797065");
        // sub
        SUB = HexDecoder.fromHex("737562");
        // ALL
        ALL = HexDecoder.fromHex("414c4c");
        // episodeString
        EPISODE_STRING = HexDecoder.fromHex("657069736f6465537472696e67");
    }

    private OkHttpUtil httpUtil;
    private Gson gson;

    public AllAnimeLinkUtils(Context context) {
        this.httpUtil = OkHttpUtil.getInstance(context);
        this.gson = new Gson();
    }

    public ArrayList<AnimeEpisode> getAnimeEpisodes(String id) throws Exception {
        String query = "query ($showId: String!) {\n" +
                "            show(_id: $showId) {\n" +
                "                _id\n" +
                "                availableEpisodesDetail\n" +
                "            }\n" +
                "        }";
        JSONObject variables = new JSONObject().put(SHOW_ID, id);
        JSONObject payload = new JSONObject()
                .put(VARIABLES, variables)
                .put(QUERY, query);

        RequestBody requestBody = RequestBody.create(payload.toString(), MediaType.parse(OkHttpUtil.APPLICATION_JSON));
        Request.Builder requestBuilder = new Request.Builder()
                .url(SEARCH_URL)
                .header(REFERER, ANIME_REFERER)
                .post(requestBody);

        Response response = httpUtil.getHttpRequest(requestBuilder);
        if (response.isSuccessful()) {
            AnimeEpisodeResponseModel animeEpisodeResponseModel = gson.fromJson(response.body().string(), AnimeEpisodeResponseModel.class);

            List<String> episodes_sub = animeEpisodeResponseModel.data.show.availableEpisodesDetail.sub;
            List<String> episodes_dub = animeEpisodeResponseModel.data.show.availableEpisodesDetail.dub;

            Map<String, AnimeEpisode> episodeMap = new HashMap<>();

            for (String episode : episodes_sub) {
                episodeMap.put(episode, new AnimeEpisode(id, episode, false));
            }

            for (String episode : episodes_dub) {
                episodeMap.put(episode, new AnimeEpisode(id, episode, true));
            }
            ArrayList<AnimeEpisode> animeEpisodes = new ArrayList<>(episodeMap.values());
            Collections.sort(animeEpisodes,(a, b)->Float.valueOf(a.episode).compareTo(Float.valueOf(b.episode)));
            return animeEpisodes;
        }
        return null;
    }

    public ArrayList<AnimeStreamingSource> getAnimeVideoLink(String id, String episode, String subDub) throws Exception {
        String query = "query ($showId: String!, $translationType: VaildTranslationTypeEnumType!, $episodeString: String!) {\n" +
                "            episode(\n" +
                "                    showId: $showId\n" +
                "            translationType: $translationType\n" +
                "            episodeString: $episodeString\n" +
                "                    ) {\n" +
                "                episodeString\n" +
                "                        sourceUrls\n" +
                "            }\n" +
                "        }";

        JSONObject variables = new JSONObject()
                .put(SHOW_ID, id)
                .put(TRANSLATION_TYPE, subDub)
                .put(EPISODE_STRING, episode);

        JSONObject payload = new JSONObject()
                .put(VARIABLES, variables)
                .put(QUERY, query);

        RequestBody requestBody = RequestBody.create(payload.toString(), MediaType.parse(OkHttpUtil.APPLICATION_JSON));
        Request.Builder requestBuilder = new Request.Builder()
                .url(SEARCH_URL)
                .header(REFERER, ANIME_REFERER)
                .post(requestBody);

        Response response = httpUtil.getHttpRequest(requestBuilder);

        ArrayList<AnimeStreamingSource> streamingSources = new ArrayList<AnimeStreamingSource>();

        if (response.isSuccessful()) {
            JSONObject data = new JSONObject(response.body().string());
            JSONArray sources = data.getJSONObject("data").getJSONObject("episode").getJSONArray("sourceUrls");

            for (int i = 0; i < sources.length(); i++) {
                JSONObject source = sources.getJSONObject(i);

                if (source.getString("sourceName").equals("S-mp4")) {
                    String encryptedLink = source.getString("sourceUrl").substring(2);
                    String decrypted = decryptLink(encryptedLink);
                    if (!decrypted.contains("https://")) {
                        AnimeStreamingModel decryptedModel = getUrl(decrypted);
                        if(decryptedModel == null) continue;
                        String link = decryptedModel.links.get(0).link;
                        // just check if link contains mp4 or m3u8 or m3u
                        if (link.contains(".mp4") || link.contains(".m3u8") || link.contains(".m3u")) {
                            boolean ismp4;
                            if (link.contains(".mp4")) {
                                ismp4 = true;
                            } else {
                                ismp4 = false;
                            }
                            streamingSources.add(new AnimeStreamingSource("S-mp4", link, ismp4));
                        }else{
                            //for links like sharepoint.aspx ...
                            streamingSources.add(new AnimeStreamingSource("S-mp4",decryptedModel.links.get(0).link, decryptedModel.links.get(0).mp4));
                        }
                    } else {
                        streamingSources.add(new AnimeStreamingSource("S-mp4", decrypted, true));
                    }
                } else if (source.getString("sourceName").equals("Luf-mp4")) {
                    String encryptedLink = source.getString("sourceUrl").substring(2);
                    String decrypted = decryptLink(encryptedLink);
                    if (!decrypted.contains("https://")) {
                        AnimeStreamingModel decryptedModel = getUrl(decrypted);
                        if(decryptedModel == null) continue;
                        String link = decryptedModel.links.get(0).link;
                        if (link.contains(".mp4") || link.contains(".m3u8") || link.contains(".m3u")){
                            boolean ismp4;
                            if (link.contains(".mp4")) {
                                ismp4 = true;
                            } else {
                                ismp4 = false;
                            }
                            streamingSources.add(new AnimeStreamingSource("Luf-mp4", link, ismp4));
                        }else{
                            streamingSources.add(new AnimeStreamingSource("Luf-mp4",decryptedModel.links.get(0).link, decryptedModel.links.get(0).mp4));
                        
                        }
                    } else {
                        streamingSources.add(new AnimeStreamingSource("Luf-mp4", decrypted, true));
                    }
                } else if (source.getString("sourceName").equals("Yt-mp4")) {
                    String encryptedLink = source.getString("sourceUrl").substring(2);
                    String decrypted = decryptLink(encryptedLink);
                    if (!decrypted.contains("https://")) {
                        AnimeStreamingModel decryptedModel = getUrl(decrypted);
                        if(decryptedModel == null) continue;
                        String link = decryptedModel.links.get(0).link;
                        if (link.contains(".mp4") || link.contains(".m3u8") || link.contains(".m3u")){
                            boolean ismp4;
                            if (link.contains(".mp4")) {
                                ismp4 = true;
                            } else {
                                ismp4 = false;
                            }
                            streamingSources.add(new AnimeStreamingSource("Yt-mp4", link, ismp4));
                        }else{
                            streamingSources.add(new AnimeStreamingSource("Yt-mp4",decryptedModel.links.get(0).link, decryptedModel.links.get(0).mp4));
                        }
                        
                    } else {
                        streamingSources.add(new AnimeStreamingSource("Yt-mp4", decrypted, true));
                    }
                }
            }
        }

        return streamingSources;
    }

    private AnimeStreamingModel getUrl(String halfUrl) {
        try {
            // /clock? -> /clock.json?
            String finalUrl = ANIME_URL + halfUrl.replace(HexDecoder.fromHex("2f636c6f636b3f"), HexDecoder.fromHex("2f636c6f636b2e6a736f6e3f"));
            Response response = httpUtil.getHttpRequest(finalUrl);
            String responseStr = response.body().string();
            Log.i(TAG,responseStr);
            AnimeStreamingModel stream = gson.fromJson(responseStr, AnimeStreamingModel.class);
            // return stream.links.get(0).link;
            return stream;
        } catch (Exception e) {
            Log.e(TAG,"Error getting url",e);
            return null;
        }
    }

    public ArrayList<AnimeWishSearchResultModel> animeSearch(String query) throws Exception {
        String searchQuery =
                "query(\n"+
                    "$search: SearchInput,\n"+
                    "$limit: Int,\n"+
                    "$page: Int,\n"+
                    "$translationType: VaildTranslationTypeEnumType,\n"+
                    "$countryOrigin: VaildCountryOriginEnumType\n"+
                ") {\n"+
                    "shows(\n"+
                        "search: $search,\n"+
                        "limit: $limit,\n"+
                        "page: $page,\n"+
                        "translationType: $translationType,\n"+
                        "countryOrigin: $countryOrigin\n"+
                    ") {\n"+
                        "edges {\n"+
                            "_id\n"+
                            "name\n"+
                            "availableEpisodes\n"+
                            "__typename\n"+
                            "thumbnail\n"+
                        "}\n"+
                    "}\n"+
                "}";

        JSONObject variables = new JSONObject()
                .put(SEARCH, new JSONObject()
                        .put(ALLOW_ADULT,true)
                        .put(ALLOW_UNKNOWN,false)
                        .put(QUERY, query))
                .put(LIMIT, 40)
                .put(PAGE, 1)
                .put(TRANSLATION_TYPE, SUB)
                .put(COUNTRY_ORIGIN, ALL);

        JSONObject payload = new JSONObject()
                .put(VARIABLES, variables)
                .put(QUERY, searchQuery);

        RequestBody requestBody = RequestBody.create(payload.toString(), MediaType.parse(OkHttpUtil.APPLICATION_JSON));
        Request.Builder requestBuilder = new Request.Builder()
                .url(SEARCH_URL)
                .header(REFERER, ANIME_REFERER)
                .post(requestBody);

        Response response = httpUtil.getHttpRequest(requestBuilder);
        if (response.isSuccessful()) {
            AnimeSearchResultResponseModel animeSearchResultResponse = gson.fromJson(response.body().string(),
                    AnimeSearchResultResponseModel.class);

            List<Edge> results = animeSearchResultResponse.data.shows.edges;

            ArrayList<AnimeWishSearchResultModel> searchResults = new ArrayList<AnimeWishSearchResultModel>();
            for (Edge result : results) {
                searchResults.add(new AnimeWishSearchResultModel(result.id, result.thumbnail, result.name));
            }
            return searchResults;
        }
        return null;
    }

    private String decryptLink(String encLink) {
        try {
            byte[] bytes = hexStringToByteArray(encLink);
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) (bytes[i] ^ 56);
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

}
