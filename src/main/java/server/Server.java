/**
 * Tous les imports nécessaires pour réaliser notre code
 */
package server;

import javafx.util.Pair;
import server.models.Course;
import server.models.RegistrationForm;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * La classe Server implémente un serveur qui prend en compte deux commandes:
 * Le serveur attend et écoute le client lorsqu'il se connecte
 * Le client peut passer la commande "INSCRIRE" ou "CHARGER"
 * Une fois la commande entrée, la méthode handle() est appelée et chaque commande ci dessus est gérée par une méthode associée
 */

public class Server {

    /**
     * "INSCRIRE" est une commande qui sert à inscrire un étudiant nouveau
     */
    public final static String REGISTER_COMMAND = "INSCRIRE";

    /**
     * "CHARGER" permet de charger les cours de la session
     */
    public final static String LOAD_COMMAND = "CHARGER";
    /**
     * Le ServerSocket écoute s'il y a des connexions entrantes
     */
    private final ServerSocket server;
    /**
     * La connexion en cours est représentée par Socket
     */
    private Socket client;
    /**
     * ObjectInputStream est le flux d'inputs permettant de traiter les objets à revoyer au client
     */
    private ObjectInputStream objectInputStream;
    /**
     * ObjectOutputStream est le flux d'outputs permettant de traiter les objets à revoyer au client
     */
    private ObjectOutputStream objectOutputStream;
    /**
     * Array contenat tous les évènements enregistrés
     */
    private final ArrayList<EventHandler> handlers;

    /**
     * La  lasse Server a une nouvelle instance créée qui écoute les connexions au port spécifié
     *
     * @param port ets le port où le serveur écoute les nouvelles connexions
     * @throws IOException se déclenche lorsqu'il y a un problème au niveau de la génération du nouveau socket
     */
    public Server(int port) throws IOException {
        this.server = new ServerSocket(port, 1);
        this.handlers = new ArrayList<EventHandler>();
        this.addEventHandler(this::handleEvents);
    }

    /**
     * addEventHandler ajoute les nouveaux évènements et les enregistre
     *
     * @param h est le gestionnaire des évènements
     */
    public void addEventHandler(EventHandler h) {
        this.handlers.add(h);
    }

    /**
     * la méthode alertHandlers alerte les gestionnaire d'events
     *
     * @param cmd la commande donnée par le client
     * @param arg est l'argument de cetet commande
     */
    private void alertHandlers(String cmd, String arg) {
        for (EventHandler h : this.handlers) {
            h.handle(cmd, arg);
        }
    }

    /**
     * La méthode run constitue la boucle principale du serveur
     * Elle accepte d'abord les connexion du client et écoute les donées passées
     * Elle appelle par la suite les méthodes necessaires
     */
    public void run() {
        while (true) {
            try {
                client = server.accept();
                System.out.println("Connecté au client: " + client);
                objectInputStream = new ObjectInputStream(client.getInputStream());
                objectOutputStream = new ObjectOutputStream(client.getOutputStream());
                listen();
                disconnect();
                System.out.println("Client déconnecté!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * La méthode listen() sert à gérer les commandes données par le client en y associant chaque méthode correspondante
     *
     * @throws IOException            de déclenche s'il y a une erreur de lecture de l'input
     * @throws ClassNotFoundException si la classe entrée est introuvable
     */
    public void listen() throws IOException, ClassNotFoundException {
        String line;
        if ((line = this.objectInputStream.readObject().toString()) != null) {
            Pair<String, String> parts = processCommandLine(line);
            String cmd = parts.getKey();
            String arg = parts.getValue();
            this.alertHandlers(cmd, arg);
        }
    }

    /**
     * la méthode Pair traite de façon séparée la commande et les arguments qui lui ont été associés
     *
     * @param line ligne de la commande à séparer
     * @return une Pair avec la commande + les arguments sous le format d'une String
     */
    public Pair<String, String> processCommandLine(String line) {
        String[] parts = line.split(" ");
        String cmd = parts[0];
        String args = String.join(" ", Arrays.asList(parts).subList(1, parts.length));
        return new Pair<>(cmd, args);
    }

    /**
     * La méthode disconnect ferme les flux d'entrée et sortie du socket
     * Elle met fin à la connexion entre le client et son serveur
     *
     * @throws IOException s'il y a un problème au niveau des fermetures
     */
    public void disconnect() throws IOException {
        objectOutputStream.close();
        objectInputStream.close();
        client.close();
    }

    /**
     * La méthode handleEvents gère les évènements
     *
     * @param cmd la commande de l'évènement
     * @param arg les arguments de la commande de l'évènemenement
     */
    public void handleEvents(String cmd, String arg) {
        if (cmd.equals(REGISTER_COMMAND)) {
            handleRegistration();
        } else if (cmd.equals(LOAD_COMMAND)) {
            handleLoadCourses(arg);
        }
    }

    /**
     * Lire un fichier texte contenant des informations sur les cours et les transformer en liste d'objets 'Course'.
     * La méthode filtre les cours par la session spécifiée en argument.
     * Ensuite, elle renvoie la liste des cours pour une session au client en utilisant l'objet 'objectOutputStream'.
     * La méthode gère les exceptions si une erreur se produit lors de la lecture du fichier ou de l'écriture de l'objet dans le flux.
     *
     * @param arg la session pour laquelle on veut récupérer la liste des cours
     */
    public void handleLoadCourses(String arg) {
        try {
            // Créer une liste pour stocker les cours qui correspondent à la session demandée
            ArrayList<Course> courses = new ArrayList<Course>();
            // Trouve l'emplacement du fichier jar (ou du fichier java)
            File jarFile = new File(Server.class.getProtectionDomain().getCodeSource().getLocation().getPath());
            String jarPath = jarFile.getParentFile().getCanonicalPath();
            BufferedReader reader = new BufferedReader(new FileReader(jarPath + File.separator + "data" + File.separator + "cours.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                // Séparer chaque ligne en utilisant une tabulation comme séparateur
                String[] parts = line.split("\t");
                // Vérifier si la ligne contient les informations du cours et si la session correspond à celle demandée
                if (parts.length == 3 && parts[2].trim().equals(arg)) {
                    String code = parts[0].trim();
                    String name = parts[1].trim();
                    // Créer un objet Course avec les informations lues
                    Course course = new Course(name, code, arg);
                    // Ajouter le cours à la liste
                    courses.add(course);
                }
            }
            reader.close();

            // Envoyer la liste des cours au client en utilisant le flux de sortie de l'objet
            objectOutputStream.writeObject(courses);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Récupérer l'objet 'RegistrationForm' envoyé par le client en utilisant 'objectInputStream', l'enregistrer dans un fichier texte
     * et renvoyer un message de confirmation au client.
     * La méthode gére les exceptions si une erreur se produit lors de la lecture de l'objet, l'écriture dans un fichier ou dans le flux de sortie.
     */
    public void handleRegistration() {
        try {
            // Lire l'objet RegistrationForm envoyé par le client en utilisant le flux d'entrée de l'objet
            RegistrationForm registrationForm = (RegistrationForm) objectInputStream.readObject();
            // Trouve l'emplacement du fichier jar (ou du fichier java)
            File jarFile = new File(Server.class.getProtectionDomain().getCodeSource().getLocation().getPath());
            String jarPath = jarFile.getParentFile().getCanonicalPath();
            // Enregistrer les informations de l'inscription dans le fichier inscription.txt
            BufferedWriter writer = new BufferedWriter(new FileWriter(jarPath + File.separator + "data" + File.separator + "inscription.txt", true));
            String line = registrationForm.getCourse().getSession() + "\t" + registrationForm.getCourse().getCode() + "\t" + registrationForm.getMatricule() + "\t" + registrationForm.getPrenom() + "\t" + registrationForm.getNom() + "\t" + registrationForm.getEmail() + "\n";
            writer.write(line);
            writer.close();
            // Envoyer un message de confirmation au client en utilisant le flux de sortie de l'objet
            objectOutputStream.writeObject(String.format("Félicitations! Inscription réussie de %s au cours %s", registrationForm.getPrenom(), registrationForm.getCourse().getCode()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}