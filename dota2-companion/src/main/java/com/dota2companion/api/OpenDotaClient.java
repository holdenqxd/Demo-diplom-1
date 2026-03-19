package com.dota2companion.api;

import com.dota2companion.model.Match;
import com.dota2companion.model.Player;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class OpenDotaClient {

    private static final String BASE_URL = "https://api.opendota.com/api";
    private static final long STEAM64_BASE = 76561197960265728L;

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    public CompletableFuture<Long> resolveAccountId(String query) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Пустой запрос."));
        }

        boolean hasLetters = trimmed.chars().anyMatch(Character::isLetter);
        if (!hasLetters) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)").matcher(trimmed);
            if (matcher.find()) {
                String digits = matcher.group(1);
            try {
                    long value = Long.parseLong(digits);
                    if (value >= STEAM64_BASE) {
                        return CompletableFuture.completedFuture(value - STEAM64_BASE);
                    }
                    return CompletableFuture.completedFuture(value);
            } catch (NumberFormatException e) {
                    return CompletableFuture.failedFuture(
                            new IllegalArgumentException("Слишком большое число для Steam ID/account_id.")
                    );
                }
            }
        }

        CompletableFuture<Long> future = new CompletableFuture<>();

        HttpUrl url = HttpUrl.parse(BASE_URL + "/search")
                .newBuilder()
                .addQueryParameter("q", trimmed)
                .build();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        future.completeExceptionally(new IOException("HTTP " + response.code()));
                        return;
                    }
                    String body = response.body().string();
                    JsonArray array = gson.fromJson(body, JsonArray.class);
                    if (array == null || array.size() == 0) {
                        future.completeExceptionally(new IOException("Игрок не найден по запросу."));
                        return;
                    }
                    JsonObject first = array.get(0).getAsJsonObject();
                    long accountId = first.get("account_id").getAsLong();
                    future.complete(accountId);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
        });

        return future;
    }

    public CompletableFuture<Player> fetchPlayer(long accountId) {
        CompletableFuture<Player> future = new CompletableFuture<>();

        HttpUrl url = HttpUrl.parse(BASE_URL + "/players/" + accountId);
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        future.completeExceptionally(new IOException("HTTP " + response.code()));
                        return;
                    }
                    String body = response.body().string();
                    JsonObject json = gson.fromJson(body, JsonObject.class);
                    if (json == null) {
                        future.completeExceptionally(new IOException("Пустой ответ от OpenDota."));
                        return;
                    }

                    Player player = new Player();
                    player.setAccountId(accountId);
                    player.setMmrEstimate(-1);

                    JsonObject profile = json.getAsJsonObject("profile");
                    if (profile != null && profile.has("personaname")) {
                        player.setPersonaName(profile.get("personaname").getAsString());
                    }

                    JsonObject mmrEstimate = json.getAsJsonObject("mmr_estimate");
                    if (mmrEstimate != null && mmrEstimate.has("estimate")) {
                        player.setMmrEstimate((int) Math.round(mmrEstimate.get("estimate").getAsDouble()));
                    }

                    if (json.has("rank_tier") && !json.get("rank_tier").isJsonNull()) {
                        player.setRankTier(json.get("rank_tier").getAsInt());
                    }

                    future.complete(player);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
        });

        return future;
    }

    public CompletableFuture<List<Match>> fetchRecentMatches(long accountId, int limit) {
        CompletableFuture<List<Match>> future = new CompletableFuture<>();

        HttpUrl url = HttpUrl.parse(BASE_URL + "/players/" + accountId + "/recentMatches")
                .newBuilder()
                .addQueryParameter("limit", String.valueOf(limit))
                .build();

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        future.completeExceptionally(new IOException("HTTP " + response.code()));
                        return;
                    }
                    String body = response.body().string();
                    JsonArray array = gson.fromJson(body, JsonArray.class);
                    if (array == null) {
                        future.completeExceptionally(new IOException("Пустой ответ от OpenDota."));
                        return;
                    }

                    List<Match> matches = new ArrayList<>();

                    for (int i = 0; i < array.size(); i++) {
                        JsonObject m = array.get(i).getAsJsonObject();
                        Match match = new Match();
                        match.setMatchId(m.get("match_id").getAsLong());
                        match.setDurationSeconds(m.get("duration").getAsInt());
                        match.setGpm(m.get("gold_per_min").getAsInt());
                        match.setXpm(m.get("xp_per_min").getAsInt());
                        match.setKills(m.get("kills").getAsInt());
                        match.setDeaths(m.get("deaths").getAsInt());
                        match.setAssists(m.get("assists").getAsInt());

                        if (m.has("start_time")) {
                            match.setStartTime(Instant.ofEpochSecond(m.get("start_time").getAsLong()));
                        }

                        boolean radiantWin = m.get("radiant_win").getAsBoolean();
                        int playerSlot = m.get("player_slot").getAsInt();
                        boolean isRadiant = playerSlot < 128;
                        match.setRadiantWin(radiantWin);
                        match.setPlayerWin(radiantWin == isRadiant);

                        if (m.has("hero_id")) {
                            int heroId = m.get("hero_id").getAsInt();
                            match.setHeroId(heroId);
                            match.setHeroName("Hero " + heroId);
                        }

                        matches.add(match);
                    }

                    future.complete(matches);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
        });

        return future;
    }

    public CompletableFuture<Map<Integer, String>> fetchHeroes() {
        CompletableFuture<Map<Integer, String>> future = new CompletableFuture<>();

        HttpUrl url = HttpUrl.parse(BASE_URL + "/heroes");
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        future.completeExceptionally(new IOException("HTTP " + response.code()));
                        return;
                    }
                    String body = response.body().string();
                    JsonArray array = gson.fromJson(body, JsonArray.class);
                    if (array == null) {
                        future.completeExceptionally(new IOException("Пустой ответ от OpenDota."));
                        return;
                    }
                    Map<Integer, String> result = new HashMap<>();
                    for (int i = 0; i < array.size(); i++) {
                        JsonObject hero = array.get(i).getAsJsonObject();
                        int id = hero.get("id").getAsInt();
                        String name = hero.has("localized_name")
                                ? hero.get("localized_name").getAsString()
                                : hero.get("name").getAsString();
                        result.put(id, name);
                    }
                    future.complete(result);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
        });

        return future;
    }
}

