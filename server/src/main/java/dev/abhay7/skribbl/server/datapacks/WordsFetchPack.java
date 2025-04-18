package dev.abhay7.skribbl.server.datapacks;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WordsFetchPack extends DataPackage {
    
    private ArrayList<String> wordList;

    // If client sends WordsFetchPack, it will always be requesting the server
    public WordsFetchPack() {
        super(true);
    }

    // Server responds with ArrayList of words
    public WordsFetchPack(ArrayList<String> wordList) {
        super(false);
        this.wordList = wordList;
    }

    public ArrayList<String> getWordList() { return this.wordList; }

    public static WordsFetchPack fromJSON(String json) throws JSONException {
        JSONObject jo = new JSONObject(json);
        boolean isServerRequest = jo.getBoolean("isServerRequest");
    
        if (isServerRequest) return new WordsFetchPack();

        JSONArray wordListJsonArray = jo.getJSONArray("wordList");
        ArrayList<String> wordList = new ArrayList<>();

        for (int i = 0; i < wordListJsonArray.length(); i++) {
            wordList.add(wordListJsonArray.getString(i));
        }

        return new WordsFetchPack(wordList);
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jo = new JSONObject();
        jo.put("isServerRequest", this.isServerRequest());

        if(this.isServerRequest()) return jo;

        JSONArray jsonLobbiesArray = new JSONArray();
        for (String word: wordList) {
            jsonLobbiesArray.put(word);
        }
        jo.put("wordList", jsonLobbiesArray);

        return jo;
    }
}
