package movierecommender;

public class UserRating {
    private String userId;
    private int[] ratings;

    public UserRating(String userId, int[] ratings) {
        this.userId = userId;
        this.ratings = ratings;
    }

    public String getUserId() { return userId; }
    public int[] getRatings() { return ratings; }
}
