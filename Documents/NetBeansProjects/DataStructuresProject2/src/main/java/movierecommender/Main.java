package movierecommender;

public class Main {
    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                GUI mainInterface = new GUI();
                mainInterface.setLocationRelativeTo(null); 
                mainInterface.setVisible(true);
            }
        });
    }
}
