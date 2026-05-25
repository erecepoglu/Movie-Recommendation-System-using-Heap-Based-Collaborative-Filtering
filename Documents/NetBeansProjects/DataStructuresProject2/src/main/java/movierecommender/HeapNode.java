package movierecommender;

public class HeapNode {
    public String userId;
    public double similarity;
    public int[] ratings;

    public HeapNode(String userId, double similarity, int[] ratings) {
        this.userId = userId;
        this.similarity = similarity;
        this.ratings = ratings;
    }
}
