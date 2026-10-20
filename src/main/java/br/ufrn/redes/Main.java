package br.ufrn.redes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

public class Main
{
    private static final String RESET   = "\u001B[0m";
    private static final String BOLD    = "\u001B[1m";
    private static final String CYAN    = "\u001B[36m";
    private static final String GREEN   = "\u001B[32m";
    private static final String YELLOW  = "\u001B[33m";
    private static final String RED     = "\u001B[31m";
    private static final String MAGENTA = "\u001B[35m";

    public record Region(String code, String name, String currencySymbol) { }

    public record GameReport(int rank, int appId, String name, String formatedPrice, 
            List<String> genres, String reviewSummary, int totalPositive, int totalReviews, double approvalPercentage) { }

    public record ReviewData(String summary, int positive, int total, double percentage) { }

    private static final HttpClient client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    public static void main(String[] args)
    {
        Scanner scanner = new Scanner(System.in);

        Map<Integer, Region> regions = Map.of(
                1, new Region("br", "Brasil", "R$"),
                2, new Region("us", "Estados Unidos", "US$"),
                3, new Region("jp", "Japão", "¥"),
                4, new Region("de", "Alemanha (União Europeia)", "€"),
                5, new Region("gb", "Reino Unido", "£")
                );

        while (true)
        {
            System.out.println("\n" + BOLD + CYAN + "========================================================" + RESET);
            System.out.println(BOLD + "   TOP VENDAS STEAM & ANÁLISE DE VENDAS (REDES - UFRN)" + RESET);
            System.out.println("\n" + BOLD + CYAN + "========================================================" + RESET);
            System.out.println("Escolha a região para coletar os dados: ");
            System.out.println("[1] Brasil");
            System.out.println("[2] Estados Unidos");
            System.out.println("[3] Japão");
            System.out.println("[4] Alemanha / Europa");
            System.out.println("[5] Reino Unido");
            System.out.println("[0] Sair");
            System.out.print("\nOpção > ");

            String input = scanner.nextLine().trim();

            if (input.equals("0"))
            {
                System.out.println("Encerrando...");
                break;
            }

            int choice;
            try{
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e)
            {
                System.out.println(RED + BOLD + "Opção inválida! Tente novamente!" + RESET);
                continue;
            }

            Region selectedRegion = regions.get(choice);
            if (selectedRegion == null)
            { 
                System.out.println(RED + BOLD + "Opção inválida! Tente novamente!" + RESET);
                continue;
            }

            fetchAndDisplayReport(selectedRegion);
            System.out.println("\nAperte Enter para continuar!");
            scanner.nextLine();
            System.out.print("\033[H\033[2J");
            System.out.flush();
        }

        scanner.close();
    }

    private static void fetchAndDisplayReport(Region region)
    {
        long startTime = System.currentTimeMillis();
        System.out.printf("\n%s[REDE]%s Iniciando coleta sequencial para %s%s (%s)%s\n", 
                CYAN, RESET, BOLD, region.name(), region.code().toUpperCase(), RESET);

        try
        {
            String featuredUrl = String.format("https://store.steampowered.com/api/featuredcategories?cc=%s&l=brazilian", region.code());
            System.out.print("=> GET " + featuredUrl + " ... ");
            String featuredJson = sendGetRequest(featuredUrl);
            System.out.println(GREEN + "200 OK" + RESET);

            JsonObject root = JsonParser.parseString(featuredJson).getAsJsonObject();
            JsonArray items = root.getAsJsonObject("top_sellers").getAsJsonArray("items");

            List<GameReport> reports = new ArrayList<>();
            Map<String, Integer> genreFreq = new HashMap<>();
            Set<Integer> seenAppIds = new HashSet<>();

            int limit = Math.min(items.size(), 15);
            int rankCount = 1;

            for (int i = 0; i < limit && reports.size() < 10; ++i)
            {
                JsonObject item = items.get(i).getAsJsonObject();
                int appId = item.get("id").getAsInt();

                if (!seenAppIds.add(appId))
                {
                    //continue;
                }

                String name = item.get("name").getAsString();
                int finalPriceCents = item.get("final_price").getAsInt();
                String formattedPrice = formatPrice(finalPriceCents, region);

                System.out.printf(" => [%02d/10] Coletando metadados e reviews do AppID %-8d (%s)...\r",
                        rankCount, appId, truncate(name, 25));

                List<String> genres = fetchGenres(appId, region.code());
                for (String g : genres)
                {
                    genreFreq.put(g, genreFreq.getOrDefault(g, 0) + 1);
                }

                ReviewData reviewData = fetchReviews(appId);

                reports.add(new GameReport(
                            rankCount++,
                            appId,
                            name,
                            formattedPrice,
                            genres,
                            reviewData.summary(),
                            reviewData.positive(),
                            reviewData.total(),
                            reviewData.percentage()
                            ));

                long elapsedMs = System.currentTimeMillis() - startTime;
                System.out.println("\n" + GREEN + "Coleta concluída em " + elapsedMs + " ms (Reuso de conexão HTTP/2)." + RESET);
            } 
            printResultsTable(region, reports, genreFreq);
        }
        catch(Exception e)
        {
            System.out.println("\n" + RED + "Erro durante a comunicação com a API da Steam: " + e.getMessage() + RESET);
        }


    }

    private static List<String> fetchGenres(int appId, String countryCode)
    {
        List<String> genres = new ArrayList<>();
        String url = String.format("https://store.steampowered.com/api/appdetails?appids=%d&cc=%s&l=brazilian", appId, countryCode);

        try {
            String json = sendGetRequest(url);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject appNode = root.getAsJsonObject(String.valueOf(appId));

            if (appNode != null && appNode.get("success").getAsBoolean())
            {
                JsonObject data = appNode.getAsJsonObject("data");
                if (data.has("genres"))
                {
                    JsonArray genresArray = data.getAsJsonArray("genres");
                    for (JsonElement g : genresArray)
                    {
                        genres.add(g.getAsJsonObject().get("description").getAsString());
                    }
                }
            }
        } 
        catch (Exception ignored)
        {

        }

        return genres.isEmpty() ? List.of("N/A") : genres;
    }

    private static ReviewData fetchReviews(int appId)
    {
        String url = String.format("https://store.steampowered.com/appreviews/%d?json=1&language=all&purchase_type=all", appId);

        try
        {
            String json = sendGetRequest(url);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (root.has("query_summary"))
            {
                JsonObject summary = root.getAsJsonObject("query_summary");
                String desc = summary.has("review_score_desc") ? summary.get("review_score_desc").getAsString() : "Sem análises";
                int pos = summary.has("total_positive") ? summary.get("total_positive").getAsInt() : 0;
                int total = summary.has("total_reviews") ? summary.get("total_reviews").getAsInt() : 0;
                double pct = total > 0 ? (pos * 100.0) / total : 0.0;
                return new ReviewData(translateReviewDesc(desc), pos, total, pct);
            }
        } catch (Exception ignored) { }

        return new ReviewData("N/A", 0, 0, 0.0);
    }

    public static String sendGetRequest(String urlString) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(urlString))
            .header("User-Agent", "UFRN-Redes-SteamAnalyzer/1.0")
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200)
        {
            throw new IOException("HTTP Status Code inesperado: " + response.statusCode());
        }

        return response.body();
    }

    private static void printResultsTable(Region region, List<GameReport> reports, Map<String, Integer> genreFrequency) {
        System.out.println("\n" + BOLD + "=== TOP 10 MAIS VENDIDOS - " + region.name().toUpperCase() + " ===" + RESET);
        String line = "+----+------------------------------+--------------+--------------------------------+---------------------------+";
        System.out.println(line);
        System.out.printf("| %-2s | %-28s | %-12s | %-30s | %-25s |\n",
                "#", "JOGO", "PREÇO", "GÊNEROS", "ANÁLISES (APROVAÇÃO)");
        System.out.println(line);

        for (GameReport r : reports) {
            String genresStr = truncate(String.join(", ", r.genres()), 30);
            String reviewStr = r.totalReviews() > 0
                    ? String.format("%5.1f%% (%s)", r.approvalPercentage(), truncate(r.reviewSummary(), 15))
                    : "Sem análises";

            String color = r.approvalPercentage() >= 80 ? GREEN : (r.approvalPercentage() >= 50 ? YELLOW : RED);
            if (r.totalReviews() == 0) color = RESET;

            System.out.printf("| %02d | %-28s | %-12s | %-30s | %s%-25s%s |\n",
                    r.rank(),
                    truncate(r.name(), 28),
                    r.formatedPrice(),
                    genresStr,
                    color, reviewStr, RESET);
        }
        System.out.println(line);

        System.out.println("\n" + BOLD + MAGENTA + "=== DISTRIBUIÇÃO DE GÊNEROS NO TOP 10 (" + region.name().toUpperCase() + ") ===" + RESET);
        genreFrequency.entrySet().stream()
                .filter(e -> !e.getKey().equals("N/A"))
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEach(entry -> {
                    int count = entry.getValue();
                    String bar = "█".repeat(count * 2);
                    System.out.printf("  %-22s : %s%-20s%s (%d de 10 jogos)\n",
                            entry.getKey(), CYAN, bar, RESET, count);
                });
    }

    private static String formatPrice(int priceInCents, Region region) {
        if (priceInCents == 0) return "Gratuito";
        if (region.code().equals("jp")) {
            return String.format("%s %d", region.currencySymbol(), priceInCents / 100);
        }
        return String.format("%s %.2f", region.currencySymbol(), priceInCents / 100.0);
    }

    private static String translateReviewDesc(String desc) {
        return switch (desc) {
            case "Overwhelmingly Positive" -> "Extrem. Positivas";
            case "Very Positive" -> "Muito Positivas";
            case "Positive" -> "Positivas";
            case "Mostly Positive" -> "Ligeir. Positivas";
            case "Mixed" -> "Neutras";
            case "Mostly Negative" -> "Ligeir. Negativas";
            case "Negative", "Very Negative", "Overwhelmingly Negative" -> "Negativas";
            default -> desc;
        };
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
    }
}
