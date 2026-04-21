package ui;

import application.AppFacade;
import domain.LocalRepo;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ui.controller.BranchController;
import ui.controller.HistoryController;
import ui.controller.IssueController;
import ui.controller.RepoController;
import ui.controller.StatusController;
import ui.controller.DifferenceController;

/**
 * JavaFX application entry point.
 * Composes the UI by creating the shared AppFacade and repository state,
 * wiring all controllers, and building the tab-based main window.
 */
public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        AppFacade facade = new AppFacade();
        ObjectProperty<LocalRepo> repoProperty = new SimpleObjectProperty<>();

        RepoController repoController = new RepoController(facade, repoProperty);
        StatusController statusController = new StatusController(facade, repoProperty);
        HistoryController historyController = new HistoryController(facade, repoProperty);
        DifferenceController differenceController = new DifferenceController(facade, repoProperty);
        BranchController branchController = new BranchController(facade, repoProperty);
        IssueController issueController = new IssueController(facade, repoProperty);

        TabPane tabPane = new TabPane(
                createTab("Repository", repoController.getView()),
                createTab("Status", statusController.getView()),
                createTab("History", historyController.getView()),
                createTab("Difference", differenceController.getView()),
                createTab("Branches", branchController.getView()),
                createTab("Issues", issueController.getView())
        );

        Label repoLabel = new Label();
        repoLabel.textProperty().bind(Bindings.createStringBinding(
                () -> repoProperty.get() == null
                        ? "Selected repo: (none)"
                        : "Selected repo: " + repoProperty.get().root(),
                repoProperty
        ));

        VBox root = new VBox(10, repoLabel, tabPane);
        root.setPadding(new Insets(12));

        stage.setTitle("Local Git GUI");

        // Load the dark mode CSS
        Scene scene = new Scene(root, 1024, 768);
        String css = getClass().getResource("style.css").toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("Git Manager");
        stage.setScene(scene);
        stage.show();
    }

    private Tab createTab(String title, javafx.scene.Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
