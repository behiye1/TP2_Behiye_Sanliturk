package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javafx.util.Pair;
import server.models.Course;
import server.models.RegistrationForm;

public class ClientSimple {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 1337;
    public final static String REGISTER_COMMAND = "INSCRIRE";
    public final static String LOAD_COMMAND = "CHARGER";

    public static Pair<ObjectInputStream, ObjectOutputStream> connect() throws IOException {
        Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);

        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        return new Pair<>(in, out);
    }

    public static void main(String[] args) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("*** Bienvenue au portail d'inscription de cours de l'UDEM ***");
        try {
            while (true) {
                // Fonctionnalité F1
                System.out.println("Veuillez choisir la session pour laquelle vous voulez consulter la liste des cours:");
                System.out.println("1. Automne");
                System.out.println("2. Hiver");
                System.out.println("3. Ete");
                System.out.print("> Choix: ");
                String option = reader.readLine();
                String session = "";
                switch (option) {
                    case "1":
                        session = "Automne";
                        break;
                    case "2":
                        session = "Hiver";
                        break;
                    case "3":
                        session = "Ete";
                        break;
                    default:
                        System.out.println("Choix n'est pas 1, 2, ou 3");
                }
                if (session.equals("")) continue;
                System.out.println("Les cours offerts pendant la session d'" + session + " sont:");
                Pair<ObjectInputStream, ObjectOutputStream> connection = connect();
                connection.getValue().writeObject(LOAD_COMMAND + " " + session);
                ArrayList<Course> courses = (ArrayList<Course>) connection.getKey().readObject();
                for (Course course : courses) {
                    System.out.printf("* %s\t%s\n", course.getCode(), course.getName());
                }
                while (true) {
                    System.out.println("Que voulez-vous faire? ");
                    System.out.println("1. Consulter les cours offerts pour une autre session");
                    System.out.println("2. Inscription à un cours");
                    System.out.print("> Choix: ");
                    option = reader.readLine();
                    if (!option.equals("1") && !option.equals("2")) {
                        System.out.println("Choix n'est pas 1 ou 2");
                    } else {
                        break;
                    }
                }
                if (option.equals("2")) {
                    // Fonctionnalité F2 : Faire une demande d'inscription à un cours
                    System.out.print("Veuillez saisir votre prénom: ");
                    String prenom = reader.readLine();
                    System.out.print("Veuillez saisir votre nom: ");
                    String nom = reader.readLine();
                    System.out.print("Veuillez saisir votre email: ");
                    String email = reader.readLine();
                    System.out.print("Veuillez saisir votre matricule: ");
                    String matricule = reader.readLine();
                    System.out.print("Veuillez saisir le code du cours: ");
                    String code = reader.readLine();
                    System.out.print("Veuillez saisir la session: ");
                    session = reader.readLine();

                    // Valide le cours et la session
                    connection = connect();
                    connection.getValue().writeObject("CHARGER " + session);
                    courses = (ArrayList<Course>) connection.getKey().readObject();
                    boolean found = false;
                    for (Course course : courses) {
                        found |= course.getCode().equals(code);
                    }
                    if (found) {
                        RegistrationForm registrationForm = new RegistrationForm(prenom, nom, email, matricule, new Course("", code, session));
                        // Reconnect parce que le serveur déconnecte après chaque requete
                        connection = connect();
                        connection.getValue().writeObject("INSCRIRE");
                        connection.getValue().writeObject(registrationForm);

                        String response = (String) connection.getKey().readObject();
                        System.out.println(response);
                    } else {
                        System.out.println("Cours ou session invalide");
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Il y a eu une erreur inattendu.");
        }
    }
}
