package tcpdataclient;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.File;

public class Controller {
    private boolean compiled = false;

    @FXML
    private Button button_AlegeProiect;
    @FXML
    private TextField textField_ProjectPath;
    @FXML
    private Button button_Compile;
    @FXML
    private TextArea textarea_Output;
    @FXML
    private MenuItem menuItem_Executabil;
    @FXML
    private MenuItem menuItem_Executabil_Si_Artefacte;
    @FXML
    private Label label_alegeProiect;
    @FXML
    private MenuButton menuButton_Descarca;
    @FXML
    private Label label_timp;

    private TCPDataClient tdc = new TCPDataClient(this);

    @FXML
    void setTextLabel_Time(String text) {
        if (label_timp != null)
            label_timp.setText(text);
    }

    @FXML
    void setButton_AlegeProiect_Callback() {
        compiled = false;
        textField_ProjectPath.setText(tdc.chooseFolder());
        label_alegeProiect.setText("Compilează proiectul ales");
    }

    @FXML
    void setButton_Compile_Callback() {
        compiled = false;
        String folderPath = tdc.folder;
        File project = new File(folderPath);
        if (project.exists()) {
            if (project.isDirectory()) {
                textarea_Output.setEditable(false);
                label_alegeProiect.setText("Proiectul se compilează ...");
                textarea_Output.setText(tdc.compileProject());
                if (isCompiled()) {
                    compiled = true;
                    label_alegeProiect.setText("Proiectul a fost compilat cu succes");
                }
                else {
                    label_alegeProiect.setText("Proiectul are erori de compilare");
                }
            }
        }
        else {
            showErrorWindow(Alert.AlertType.WARNING, "Cale invalidă către directorul proiectului",
                    "Cale invalidă către directorul proiectului care urmează a fi compilat. Alegeți un director valid.");
        }
    }

    private void showErrorWindow(Alert.AlertType type, String headerTest, String contentText) {
        Alert alert = new Alert(type);
        if (headerTest.isEmpty())
            alert.setHeaderText(headerTest);
        if (contentText.isEmpty())
            alert.setContentText(contentText);
        alert.showAndWait().ifPresent(rs -> {
            if (rs == ButtonType.OK) {
                System.out.println("Pressed OK.");
            }
        });
    }

    private boolean isCompiled() {
        if (!textarea_Output.isEditable()) {
            String output = textarea_Output.getText();
            return !output.toLowerCase().contains("error");
        }
        return false;
    }

    @FXML
    void setMenuItem_Executabil_Callback() {
        if (compiled) {
            label_alegeProiect.setText("Se descarcă executabilul ...");
            textarea_Output.setText(tdc.downloadExe(true));
            label_alegeProiect.setText("Executabilul s-a descărcat");
        }
        else {
            showErrorWindow(Alert.AlertType.WARNING, "Proiectul nu este compilat", "");
        }
    }

    @FXML
    void setMenuItem_Executabil_Si_Artefacte_Callback() {
        if (compiled) {
            label_alegeProiect.setText("Se descarcă executabilul și fișierele obiect ...");
            String output = tdc.downloadExe(false) + "\n";
            output += tdc.downloadExe(true);
            textarea_Output.setText(output);
            label_alegeProiect.setText("Executabilul și fișierele obiect s-au descărcat");
        }
        else {
            showErrorWindow(Alert.AlertType.WARNING, "Proiectul nu este compilat", "");
        }
    }
}
