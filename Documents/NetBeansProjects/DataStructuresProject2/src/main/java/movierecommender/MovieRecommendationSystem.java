package movierecommender;

import java.io.*;
import java.util.*;

public class MovieRecommendationSystem {

    private ArrayList<UserRating> users = new ArrayList<>();
    private HashMap<Integer, String> movieNames = new HashMap<>();
    private HashMap<String, int[]> targetUsers = new HashMap<>();
    private HashMap<Integer, Integer> movieIdToColIndex = new HashMap<>();
    private HashMap<Integer, Integer> colIndexToMovieId = new HashMap<>(); 
    private int movieCount = 0;

    public MovieRecommendationSystem(String mainDataPath, String moviesPath, String targetUserPath) throws IOException {
        loadMainData(mainDataPath);
        loadMovies(moviesPath);
        loadTargetUsers(targetUserPath);
    }

    private void loadMainData(String path) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(path));
        String header = br.readLine();
        if (header == null) {
            br.close();
            throw new IOException("main_data.csv is empty");
        }

        String[] headerParts = header.split(",");
        movieCount = headerParts.length - 1;

        for (int i = 1; i < headerParts.length; i++) {
            String cleanHeader = headerParts[i].replace("\"", "").trim();
            int headerMovieId = parseIntSafe(cleanHeader);

            int actualMovieId = (headerMovieId != 0) ? headerMovieId : i;
            movieIdToColIndex.put(actualMovieId, i - 1);
            colIndexToMovieId.put(i - 1, actualMovieId); 
        }

        String line;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split(",", -1);
            if (parts.length == 0) {
                continue;
            }

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
        if (header == null) {
            br.close();
            return;
        }

        String line;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split(",", -1);
            if (parts.length == 0) {
                continue;
            }

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
        ArrayList<String> names = new ArrayList<>();

        for (int col = 0; col < movieCount; col++) {
            boolean hasRating = false;

            for (UserRating user : users) {
                if (user.getRatings()[col] > 0) {
                    hasRating = true;
                    break;
                }
            }

            if (hasRating) {
                Integer movieId = colIndexToMovieId.get(col);
                String movieName = movieNames.get(movieId);

                if (movieName != null) {
                    names.add(movieName);
                }
            }
        }

        Collections.shuffle(names);

        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < count && i < names.size(); i++) {
            result.add(names.get(i));
        }

        return result;
    }

    public int getMovieIdByName(String name) {
        for (Integer id : movieNames.keySet()) {
            if (movieNames.get(id).equals(name)) {
                return id;
            }
        }
        return -1;
    }

    public ArrayList<String> recommendForTargetUser(String targetUserId, int xUsers, int kMovies) {
        int[] targetVector = targetUsers.get(targetUserId);
        if (targetVector == null) {
            return new ArrayList<>();
        }
        return recommendFromVector(targetVector, xUsers, kMovies);
    }

    public ArrayList<String> recommendFromManualRatings(HashMap<Integer, Integer> selectedRatings, int xUsers, int kMovies) {
        int[] userVector = new int[movieCount];

        for (Integer movieId : selectedRatings.keySet()) {
            Integer index = movieIdToColIndex.get(movieId);
            if (index == null) {
                index = movieId - 1;
            }
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
        HashSet<Integer> alreadyRecommended = new HashSet<>();

        int neededTotal = xUsers * kMovies;

        while (recommendations.size() < neededTotal && !heap.isEmpty()) {
            HeapNode similarUser = heap.extractMax();

            ArrayList<Integer> movies = getTopUniqueMoviesFromUser(
                    similarUser.ratings,
                    kMovies,
                    alreadyRecommended
            );

            for (Integer movieId : movies) {
                alreadyRecommended.add(movieId);

                String movieName = movieNames.get(movieId);
                if (movieName != null) {
                    recommendations.add(movieName + " | similar user: "
                            + similarUser.userId
                            + " | similarity: "
                            + String.format("%.4f", similarUser.similarity));
                }

                if (recommendations.size() == neededTotal) {
                    break;
                }
            }
        }

        return recommendations;
    }

    private ArrayList<Integer> getTopUniqueMoviesFromUser(int[] ratings, int k, HashSet<Integer> alreadyRecommended) {
        ArrayList<Integer> movieIds = new ArrayList<>();

        for (int i = 0; i < ratings.length; i++) {
            Integer movieId = colIndexToMovieId.get(i);

            if (movieId == null) {
                movieId = i + 1;
            }

            if (ratings[i] > 0
                    && movieNames.containsKey(movieId)
                    && !alreadyRecommended.contains(movieId)) {
                movieIds.add(movieId);
            }
        }

        movieIds.sort((m1, m2) -> {
            int index1 = movieIdToColIndex.get(m1);
            int index2 = movieIdToColIndex.get(m2);

            return Integer.compare(ratings[index2], ratings[index1]);
        });

        ArrayList<Integer> result = new ArrayList<>();

        for (int i = 0; i < k && i < movieIds.size(); i++) {
            result.add(movieIds.get(i));
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

        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private int parseIntSafe(String text) {
        try {
            if (text == null || text.trim().isEmpty()) {
                return 0;
            }
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private String[] splitCsvLine(String line) {
        ArrayList<String> values = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                insideQuotes = !insideQuotes;
            } else if (c == ',' && !insideQuotes) {
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