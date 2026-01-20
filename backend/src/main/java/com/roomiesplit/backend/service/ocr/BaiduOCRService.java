package com.roomiesplit.backend.service.ocr;

import okhttp3.*;
import org.json.JSONObject;
import org.json.JSONArray;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
public class BaiduOCRService {

    private static final String API_KEY = "HetMudFI0uVWzWDdDANcN5wC";
    private static final String SECRET_KEY = "YBEYk8Y3l2tp9Ge3j0farftr9ZNl2rIw"; // 实际项目中应放在配置中

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient().newBuilder()
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private String cachedAccessToken = null;
    private long tokenExpireTime = 0;

    /**
     * 识别购物小票
     * 
     * @param filePath 本地文件路径
     * @return 解析后的结构化数据
     */
    public java.util.Map<String, Object> recognizeReceipt(Path filePath) {
        try {
            byte[] fileContent = Files.readAllBytes(filePath);
            String imageBase64 = Base64.getEncoder().encodeToString(fileContent);
            String imageParam = URLEncoder.encode(imageBase64, "UTF-8");

            String accessToken = getAccessToken();

            MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
            // 使用image参数传递Base64
            RequestBody body = RequestBody.create(mediaType,
                    "image=" + imageParam + "&probability=false&location=false");

            Request request = new Request.Builder()
                    .url("https://aip.baidubce.com/rest/2.0/ocr/v1/shopping_receipt?access_token=" + accessToken)
                    .method("POST", body)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .addHeader("Accept", "application/json")
                    .build();

            Response response = HTTP_CLIENT.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String resultJson = response.body().string();
                // Debug: Write to file code removed
                // System.out.println("Baidu OCR Success: " + resultJson); // Keep logging or
                // remove? User didn't say remove logging, just file writing.
                // Keeping console log is useful.
                System.out.println("Baidu OCR Success: " + resultJson);

                return parseBaiduResponse(resultJson);
            } else {
                String errorBody = response.body() != null ? response.body().string() : "null";
                System.err.println("Baidu OCR Failed: Code=" + response.code() + ", Body=" + errorBody);
                java.util.Map<String, Object> error = new java.util.HashMap<>();
                error.put("error", "API Error: " + response.code() + " " + errorBody);
                return error;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Baidu OCR Exception: " + e.getMessage());
            java.util.Map<String, Object> error = new java.util.HashMap<>();
            error.put("error", "Exception: " + e.getMessage());
            return error;
        }
    }

    private String getAccessToken() throws IOException {
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return cachedAccessToken;
        }

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "grant_type=client_credentials&client_id=" + API_KEY
                + "&client_secret=" + SECRET_KEY);
        Request request = new Request.Builder()
                .url("https://aip.baidubce.com/oauth/2.0/token")
                .method("POST", body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        Response response = HTTP_CLIENT.newCall(request).execute();
        String respStr = response.body().string();
        JSONObject jsonObject = new JSONObject(respStr);

        cachedAccessToken = jsonObject.getString("access_token");
        // token通常有效期为30天，这里保守设为1天或更短
        int expiresIn = jsonObject.optInt("expires_in", 2592000);
        tokenExpireTime = System.currentTimeMillis() + (expiresIn * 1000L);

        return cachedAccessToken;
    }

    private java.util.Map<String, Object> parseBaiduResponse(String jsonStr) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        JSONObject root = new JSONObject(jsonStr);

        result.put("raw_response", jsonStr);

        if (root.has("words_result")) {
            Object wordsObj = root.get("words_result");
            if (wordsObj instanceof JSONObject) {
                // Not array, single object structure (Unlikely for "shopping_receipt" but
                // handling just in case)
                JSONObject words = (JSONObject) wordsObj;
                parseSingleReceiptObject(words, result, jsonStr);
            } else if (wordsObj instanceof org.json.JSONArray) {
                org.json.JSONArray wordsArray = (org.json.JSONArray) wordsObj;
                if (wordsArray.length() > 0) {
                    JSONObject firstItem = wordsArray.getJSONObject(0);
                    parseSingleReceiptObject(firstItem, result, jsonStr);
                } else {
                    result.put("description", "未找到识别结果字段");
                }
            }
        } else {
            result.put("description", "未找到识别结果字段");
        }

        return result;
    }

    private void parseSingleReceiptObject(JSONObject data, java.util.Map<String, Object> result, String rawJson) {
        // Extraction helper
        java.util.function.Function<String, String> extract = (key) -> {
            if (data.has(key)) {
                Object val = data.get(key);
                if (val instanceof org.json.JSONArray) {
                    org.json.JSONArray arr = (org.json.JSONArray) val;
                    if (arr.length() > 0) {
                        return arr.getJSONObject(0).optString("word");
                    }
                }
            }
            return null;
        };

        // 1. Basic Info Extraction
        String shopName = extract.apply("shop_name");
        String dateStr = extract.apply("consumption_date");
        if (dateStr == null || dateStr.isEmpty())
            dateStr = extract.apply("date");
        if (dateStr != null && !dateStr.isEmpty()) {
            result.put("date", dateStr);
        }

        // 2. Category Inference (Prioritize this to help description)
        // Pass rawJson for full-text fallback
        String category = inferCategory(shopName, data, rawJson);
        result.put("category", category);

        // 3. Smart Description Logic
        if (shopName != null && !shopName.isEmpty()) {
            result.put("description", shopName);
        } else {
            // Generate smart default based on category
            String smartDesc = "小票消费";
            switch (category) {
                case "餐饮":
                    smartDesc = "餐饮美食";
                    break;
                case "购物":
                    smartDesc = "超市购物";
                    break;
                case "交通":
                    smartDesc = "交通出行";
                    break;
                case "娱乐":
                    smartDesc = "休闲娱乐";
                    break;
            }
            result.put("description", smartDesc);
        }

        // 4. Smart Amount Calculation (Level 1-4)
        Double amount = calculateSmartAmount(data, extract);
        if (amount != null) {
            // Round to 2 decimal places
            amount = Math.round(amount * 100.0) / 100.0;
            result.put("amount", amount);
        }
    }

    /**
     * Multilevel Fallback Strategy for Amount
     */
    private Double calculateSmartAmount(JSONObject data, java.util.function.Function<String, String> extract) {
        // Level 1: API Total Amount
        Double amount = parseDoubleSafe(extract.apply("total_amount"));
        if (amount != null)
            return amount;

        // Level 2: API Paid Amount
        amount = parseDoubleSafe(extract.apply("paid_amount"));
        if (amount != null)
            return amount;

        // Level 3 & 4: Calculate from Table
        if (data.has("table")) {
            Object tableObj = data.get("table");
            if (tableObj instanceof org.json.JSONArray) {
                org.json.JSONArray table = (org.json.JSONArray) tableObj;
                double calculatedTotal = 0.0;
                boolean hasValidRow = false;

                for (int i = 0; i < table.length(); i++) {
                    JSONObject row = table.getJSONObject(i);

                    // Helper to extract word from cell
                    java.util.function.Function<String, String> extractCell = (k) -> {
                        if (row.has(k)) {
                            JSONObject cell = row.optJSONObject(k);
                            if (cell != null)
                                return cell.optString("word");
                        }
                        return null;
                    };

                    // Level 3: Sum Subtotals
                    Double rowSum = parseDoubleSafe(extractCell.apply("subtotal_amount"));

                    // Level 4: Calculate row from Qty * Price
                    if (rowSum == null) {
                        Double qty = parseDoubleSafe(extractCell.apply("quantity"));
                        Double price = parseDoubleSafe(extractCell.apply("unit_price"));
                        if (qty != null && price != null) {
                            rowSum = qty * price;
                        }
                    }

                    if (rowSum != null) {
                        calculatedTotal += rowSum;
                        hasValidRow = true;
                    }
                }

                if (hasValidRow) {
                    return calculatedTotal;
                }
            }
        }
        return null; // Failed to find any amount
    }

    /**
     * Enhanced Category Inference
     */
    private String inferCategory(String shopName, JSONObject data, String rawJson) {
        if (shopName == null)
            shopName = "";

        // 1. Check Shop Name Keywords
        String cat = checkKeywords(shopName);
        if (!"其他".equals(cat))
            return cat;

        // 2. Item-based Inference (if table exists)
        if (data.has("table")) {
            Object tableObj = data.get("table");
            if (tableObj instanceof org.json.JSONArray) {
                org.json.JSONArray table = (org.json.JSONArray) tableObj;
                int diningCount = 0;
                int shoppingCount = 0;

                int limit = Math.min(table.length(), 5);
                for (int i = 0; i < limit; i++) {
                    JSONObject row = table.getJSONObject(i);
                    JSONObject prodObj = row.optJSONObject("product");
                    if (prodObj != null) {
                        String itemName = prodObj.optString("word");
                        if (itemName != null && !itemName.isEmpty()) {
                            String itemCat = checkKeywords(itemName);
                            if ("餐饮".equals(itemCat))
                                diningCount++;
                            if ("购物".equals(itemCat))
                                shoppingCount++;
                        }
                    }
                }

                if (diningCount > 0 && diningCount >= shoppingCount)
                    return "餐饮";
                if (shoppingCount > 0)
                    return "购物";
            }
        }

        // 3. Full-Text Voting (New Fallback)
        // If we are here, neither shop name nor items gave a conclusive result.
        // We scan the entire text for keywords.
        return inferCategoryFromFullText(rawJson);
    }

    private String inferCategoryFromFullText(String fullText) {
        if (fullText == null || fullText.isEmpty())
            return "其他";

        java.util.Map<String, Integer> scores = new java.util.HashMap<>();
        scores.put("餐饮", 0);
        scores.put("购物", 0);
        scores.put("交通", 0);
        scores.put("娱乐", 0);

        for (java.util.Map.Entry<String, String[]> entry : CATEGORY_KEYWORDS.entrySet()) {
            String category = entry.getKey();
            String[] keywords = entry.getValue();
            for (String k : keywords) {
                // Count occurrences (simple non-overlapping count)
                int count = 0;
                int idx = 0;
                while ((idx = fullText.indexOf(k, idx)) != -1) {
                    count++;
                    idx += k.length();
                }
                scores.put(category, scores.get(category) + count);
            }
        }

        // Find max score
        String bestCat = "其他";
        int maxScore = 0;
        for (java.util.Map.Entry<String, Integer> entry : scores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                bestCat = entry.getKey();
            }
        }

        // Threshold: at least 1 keyword match to override "其他"
        if (maxScore > 0) {
            return bestCat;
        }
        return "其他";
    }

    private static final java.util.Map<String, String[]> CATEGORY_KEYWORDS = new java.util.HashMap<>();
    static {
        CATEGORY_KEYWORDS.put("餐饮", new String[] { "饮", "餐", "饭", "食", "面", "粉", "茶", "吃", "饿",
                "酒", "串", "火锅", "海底捞", "麦当劳", "肯德基", "汉堡",
                "必胜客", "星巴克", "喜茶", "奈雪", "瑞幸", "咖啡", "烧烤",
                "料理", "寿司", "烤肉", "麻辣烫", "米线", "厨房", "菜", "味" });

        CATEGORY_KEYWORDS.put("购物", new String[] { "超", "市", "购", "买", "店", "便利", "百货", "商场", "广场",
                "7-11", "全家", "罗森", "物美", "家乐福", "沃尔玛", "永辉",
                "屈臣氏", "药房", "优衣库", "ZARA", "H&M", "市场", "生鲜", "果", "肉", "奶" });

        CATEGORY_KEYWORDS.put("交通", new String[] { "车", "行", "路", "油", "铁", "机", "票", "滴滴", "打车",
                "出租", "交通", "航空", "停车", "加油", "石化", "石油", "ETC", "过路费" });

        CATEGORY_KEYWORDS.put("娱乐", new String[] { "玩", "乐", "影", "KTV", "歌", "网咖", "剧本杀", "游",
                "迪士尼", "台球", "足浴", "按摩", "洗浴", "公园", "门票" });
    }

    private String checkKeywords(String text) {
        if (text == null || text.isEmpty())
            return "其他";

        for (java.util.Map.Entry<String, String[]> entry : CATEGORY_KEYWORDS.entrySet()) {
            if (matchesAny(text, entry.getValue())) {
                return entry.getKey();
            }
        }
        return "其他";
    }

    private boolean matchesAny(String text, String... keywords) {
        for (String k : keywords) {
            if (text.contains(k))
                return true;
        }
        return false;
    }

    private Double parseDoubleSafe(String str) {
        if (str == null || str.isEmpty())
            return null;
        try {
            str = str.replaceAll("[^0-9.]", "");
            if (str.isEmpty())
                return null;
            return Double.parseDouble(str);
        } catch (Exception e) {
            return null;
        }
    }
}
