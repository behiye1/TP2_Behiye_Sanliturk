package client.clientfx;

import server.models.Course;
import server.models.RegistrationForm;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Classe ClientFX permettant de gérer une interface utilisateur pour l'inscription aux cours.
 */
public class ClientFX extends Application {
    /**
     * Adresse du serveur
     */
    private static final String SERVER_ADDRESS = "localhost";

    /**
     * Port du serveur
     */
    private static final int SERVER_PORT = 1337;

    /**
     * Commande d'inscription
     */
    public final static String REGISTER_COMMAND = "INSCRIRE";

    /**
     * Commande de chargement
     */
    public final static String LOAD_COMMAND = "CHARGER";

    /**
     * Arrière-plan pour l'interface utilisateur
     */
    public static final Background BACKGROUND = new Background(new BackgroundFill(Color.rgb(226, 219, 197), null, null));

    public static void main(String[] args) {
        ClientFX.launch(args);
    }

    /**
     * Méthode permettant de se connecter au serveur et d'établir la communication.
     *
     * @return Une paire d'ObjectInputStream et ObjectOutputStream pour la communication avec le serveur.
     * @throws IOException Si une erreur de communication se produit.
     */

    public static Pair<ObjectInputStream, ObjectOutputStream> connect() throws IOException {
        Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);

        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        return new Pair<>(in, out);
    }

    /**
     * Méthode permettant de charger les cours disponibles pour une session spécifique.
     *
     * @param data     La liste des cours à afficher.
     * @param choixBox Le ComboBox contenant la session choisie.
     */
    public static void charger(ObservableList<Course> data, ComboBox<String> choixBox) {
        String session = choixBox.getValue();
        try {
            Pair<ObjectInputStream, ObjectOutputStream> connection = connect();
            connection.getValue().writeObject(LOAD_COMMAND + " " + session);
            ArrayList<Course> courses = (ArrayList<Course>) connection.getKey().readObject();
            data.clear();
            data.addAll(courses);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Échec avec la connection au serveur");
        }
    }

    /**
     * Méthode de validation du formulaire d'inscription.
     *
     * @return Vrai si le formulaire est valide, sinon faux.
     */
    public static boolean validateForm(TextField prenomField, TextField nomField, TextField emailField, TextField matriculeField, TableView<Course> table) {
        String prenom = prenomField.getText();
        String nom = nomField.getText();
        String email = emailField.getText();
        String matricule = matriculeField.getText();

        if (prenom.isEmpty() || nom.isEmpty() || email.isEmpty() || matricule.isEmpty()) {
            return false;
        }

        if (!email.matches("^[\\w\\-\\.]+@([\\w-]+\\.)+[\\w-]{2,}$")) {
            return false;
        }

        if (matricule.length() != 8 || !matricule.matches("\\d{8}")) {
            return false;
        }

        if (table.getSelectionModel().getSelectedItem() == null) {
            return false;
        }

        return true;
    }

    /**
     * Méthode pour afficher une erreur en cas de formulaire invalide.
     */
    private static void showError() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Attention!");
        alert.setHeaderText("Attention!");
        alert.setContentText("Fait en sort de sélectionner un cours et d'avoir remplis tout les informations demander correctement.");
        alert.showAndWait();
    }

    /**
     * Méthode pour créer et afficher une fenêtre de succès avec un message personnalisé.
     *
     * @param message Le message à afficher dans la fenêtre de succès.
     */
    private static void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Success!");
        alert.setHeaderText("Success!");
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Méthode pour soumettre le formulaire d'inscription et envoyer les données au serveur.
     *
     * @param prenomField    Champ de texte pour le prénom.
     * @param nomField       Champ de texte pour le nom.
     * @param emailField     Champ de texte pour l'adresse e-mail.
     * @param matriculeField Champ de texte pour le matricule.
     * @param table          Vue de table contenant la liste des cours.
     */
    private static void submitForm(TextField prenomField, TextField nomField, TextField emailField, TextField matriculeField, TableView<Course> table) {
        if (!validateForm(prenomField, nomField, emailField, matriculeField, table)) {
            showError();
            return;
        }

        String prenom = prenomField.getText();
        String nom = nomField.getText();
        String email = emailField.getText();
        String matricule = matriculeField.getText();
        Course course = table.getSelectionModel().getSelectedItem();

        RegistrationForm form = new RegistrationForm(prenom, nom, email, matricule, course);

        try {
            Pair<ObjectInputStream, ObjectOutputStream> connection = connect();
            connection.getValue().writeObject(REGISTER_COMMAND);
            connection.getValue().writeObject(form);
            showSuccess((String) connection.getKey().readObject());

            prenomField.clear();
            nomField.clear();
            emailField.clear();
            matriculeField.clear();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Échec avec la connection au serveur");
        }
    }

    /**
     * Méthode pour démarrer l'interface utilisateur et configurer les éléments de l'interface.
     *
     * @param stage Le stage principal de l'application.
     */
    @Override
    public void start(Stage stage) {
        HBox root = new HBox();
        Scene scene = new Scene(root, 530, 400);

        // LEFT SIDE
        Text titre_left = new Text("Formulaire d'inscription");
        titre_left.setTextAlignment(TextAlignment.CENTER);

        // TABLE
        TableColumn<Course, String> codeColumn = new TableColumn<>("Code");
        TableColumn<Course, String> nomColumn = new TableColumn<>("Cours");

        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        nomColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableView<Course> tableView = new TableView<>();
        tableView.getColumns().addAll(codeColumn, nomColumn);

        ObservableList<Course> data = FXCollections.observableArrayList();

        Label emptyLabel = new Label("No content in table");
        emptyLabel.setStyle("-fx-font-size: 16px");
        tableView.setPlaceholder(emptyLabel);

        tableView.setItems(data);

        tableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // DROPDOWN
        ComboBox<String> choixSession = new ComboBox<>();
        choixSession.getItems().addAll("Automne", "Hiver", "Ete");
        choixSession.setValue("Automne");

        Button chargerButton = new Button("Charger");
        chargerButton.setOnAction(event -> {
            charger(data, choixSession);
        });

        HBox bottom = new HBox(choixSession, chargerButton);
        bottom.setSpacing(10);
        bottom.setAlignment(Pos.CENTER);

        VBox left = new VBox(titre_left, tableView, new Separator(), bottom);
        left.setBackground(BACKGROUND);
        left.setAlignment(Pos.TOP_CENTER);
        left.setPrefWidth(scene.getWidth() / 2);
        left.setSpacing(10);
        left.setPadding(new Insets(10));


        // RIGHT SIDE
        Text titre_right = new Text("Liste des cours");
        titre_right.setTextAlignment(TextAlignment.CENTER);

        TextField prenomField = new TextField();
        Label prenomLabel = new Label("Prénom");
        prenomLabel.setAlignment(Pos.CENTER_LEFT);
        prenomLabel.setMinWidth(50);
        HBox prenom = new HBox(prenomLabel, prenomField);
        prenom.setAlignment(Pos.CENTER_LEFT);
        prenom.setSpacing(10);

        TextField nomField = new TextField();
        Label nomLabel = new Label("Nom");
        nomLabel.setAlignment(Pos.CENTER_LEFT);
        nomLabel.setMinWidth(50);
        HBox nom = new HBox(nomLabel, nomField);
        nom.setAlignment(Pos.CENTER_LEFT);
        nom.setSpacing(10);

        TextField emailField = new TextField();
        Label emailLabel = new Label("Email");
        emailLabel.setAlignment(Pos.CENTER_LEFT);
        emailLabel.setMinWidth(50);
        HBox email = new HBox(emailLabel, emailField);
        email.setAlignment(Pos.CENTER_LEFT);
        email.setSpacing(10);

        TextField matriculeField = new TextField();
        Label matriculeLabel = new Label("Matricule");
        matriculeLabel.setAlignment(Pos.CENTER_LEFT);
        matriculeLabel.setMinWidth(50);
        HBox matricule = new HBox(matriculeLabel, matriculeField);
        matricule.setAlignment(Pos.CENTER_LEFT);
        matricule.setSpacing(10);

        Button envoyer = new Button("Envoyer");
        envoyer.setOnAction(event -> {
            submitForm(prenomField, nomField, emailField, matriculeField, tableView);
        });

        VBox formBox = new VBox(prenom, nom, email, matricule, envoyer);
        formBox.setAlignment(Pos.TOP_CENTER);
        formBox.setSpacing(10);
        formBox.setPadding(new Insets(10));

        VBox right = new VBox(titre_right, formBox);
        right.setAlignment(Pos.TOP_CENTER);
        right.setBackground(BACKGROUND);
        right.setPrefWidth(scene.getWidth() / 2);
        right.setPadding(new Insets(10));


        // ADD TO ROOT
        root.getChildren().add(left);
        root.getChildren().add(new Separator());
        root.getChildren().add(right);

        stage.setTitle("Inscription UdeM");
        stage.setScene(scene);
        stage.show();

    }


}
