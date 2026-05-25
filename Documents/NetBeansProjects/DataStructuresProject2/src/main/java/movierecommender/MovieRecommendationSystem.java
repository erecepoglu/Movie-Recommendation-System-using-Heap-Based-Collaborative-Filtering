package movierecommender;

import java.io.*;
import java.util.*;

public class MovieRecommendationSystem {
    private ArrayList<UserRating> users = new ArrayList<>();
    private HashMap<Integer, String> movieNames = new HashMap<>();
    private HashMap<String, int[]> targetUsers = new HashMap<>();
    private int movieCount = 0;

    public MovieRecommendationSystem(String mainDataPath, String moviesPath, String targetUserPath) throws IOException {
        loadMainData(mainDataPath);
        loadMovies(moviesPath);
        loadTargetUsers(targetUserPath);
    }

    private void loadMainData(String path) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(path));
        String header = br.readLine();
        if (header == null) throw new IOException("main_data.csv is empty");

        String[] headerParts = header.split(",");
        movieCount = headerParts.length - 1;

        String line;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split(",", -1);
            String userId = parts[0].trim();
            int[] ratings = new int[movieCount];

            for (int i = 1; i < parts.length && i <= movieCount; i++) {
                ratings[i - 1] = parseIntSafe(parts[i]);
            }
            users.add(new UserRating(userId, ratings));
        }
        br.close();
    }

    private void loadMovies(String path) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line = br.readLine(); // header

        while ((line = br.readLine()) != null) {
            String[] parts = splitCsvLine(line);
            if (parts.length >= 2) {
                int movieId = parseIntSafe(parts[0]);
                String movieName = parts[1].trim();
                movieNames.put(movieId, movieName);
            }
        }
        br.close();
    }

    private void loadTargetUsers(String path) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(path));
        String header = br.readLine();
        if (header == null) return;

        String line;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split(",", -1);
            String userId = parts[0].trim();
            int[] ratings = new int[movieCount];

            for (int i = 1; i < parts.length && i <= movieCount; i++) {
                ratings[i - 1] = parseIntSafe(parts[i]);
            }
            targetUsers.put(userId, ratings);
        }
        br.close();
    }

    public ArrayList<String> getTargetUserIds() {
        return new ArrayList<>(targetUsers.keySet());
    }

    public ArrayList<String> getRandomMovieNames(int count) {
        ArrayList<String> names = new ArrayList<>(movieNames.values());
        Collections.shuffle(names);
        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < count && i < names.size(); i++) result.add(names.get(i));
        return result;
    }

    public int getMovieIdByName(String name) {
        for (Integer id : movieNames.keySet()) {
            if (movieNames.get(id).equals(name)) return id;
        }
        return -1;
    }

    public ArrayList<String> recommendForTargetUser(String targetUserId, int xUsers, int kMovies) {
        int[] targetVector = targetUsers.get(targetUserId);
        if (targetVector == null) return new ArrayList<>();
        return recommendFromVector(targetVector, xUsers, kMovies);
    }

    public ArrayList<String> recommendFromManualRatings(HashMap<Integer, Integer> selectedRatings, int xUsers, int kMovies) {
        int[] userVector = new int[movieCount];

        for (Integer movieId : selectedRatings.keySet()) {
            int index = movieId - 1;
            if (index >= 0 && index < movieCount) {
                userVector[index] = selectedRatings.get(movieId);
            }
        }
        return recommendFromVector(userVector, xUsers, kMovies);
    }

    private ArrayList<String> recommendFromVector(int[] targetVector, int xUsers, int kMovies) {
        MaxHeap heap = new MaxHeap();

        for (UserRating user : users) {
            double sim = cosineSimilarity(targetVector, user.getRatings());
            heap.insert(new HeapNode(user.getUserId(), sim, user.getRatings()));
        }

        ArrayList<String> recommendations = new ArrayList<>();
        HashSet<Integer> alreadyAdded = new HashSet<>();

        for (int i = 0; i < xUsers && !heap.isEmpty(); i++) {
            HeapNode similarUser = heap.extractMax();
            ArrayList<Integer> topMovies = getTopKMovies(similarUser.ratings, kMovies, targetVector, alreadyAdded);

            for (Integer movieId : topMovies) {
                alreadyAdded.add(movieId);
                String movieName = movieNames.get(movieId);
                if (movieName != null) {
                    recommendations.add(movieName + "  | similar user: " + similarUser.userId + " | similarity: " + String.format("%.4f", similarUser.similarity));
                }
            }
        }
        return recommendations;
    }

    private ArrayList<Integer> getTopKMovies(int[] ratings, int k, int[] targetVector, HashSet<Integer> alreadyAdded) {
        ArrayList<Integer> result = new ArrayList<>();
        boolean[] used = new boolean[ratings.length];

        for (int count = 0; count < k; count++) {
            int bestIndex = -1;
            int bestRating = -1;

            for (int i = 0; i < ratings.length; i++) {
                int movieId = i + 1;

                // This avoids recommending movies the target user already rated/watched.
                // Remove this condition if your teacher wants all highest-rated movies.
                if (targetVector[i] != 0) continue;

                if (used[i]) continue;
                if (alreadyAdded.contains(movieId)) continue;

                if (ratings[i] > bestRating) {
                    bestRating = ratings[i];
                    bestIndex = i;
                }
            }

            if (bestIndex == -1 || bestRating <= 0) break;
            used[bestIndex] = true;
            result.add(bestIndex + 1);
        }
        return result;
    }

    private double cosineSimilarity(int[] a, int[] b) {
        double dot = 0;
        double normA = 0;
        double normB = 0;

        int length = Math.min(a.length, b.length);
        for (int i = 0; i < length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private int parseIntSafe(String text) {
        try {
            if (text == null || text.trim().isEmpty()) return 0;
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    // Handles movie titles that may contain commas inside quotes.
    private String[] splitCsvLine(String line) {
        ArrayList<String> values = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') insideQuotes = !insideQuotes;
            else if (c == ',' && !insideQuotes) {
                values.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        values.add(sb.toString());
        return values.toArray(new String[0]);
    }
}
