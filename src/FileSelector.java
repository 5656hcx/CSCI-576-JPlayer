import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public abstract class FileSelector implements ActionListener {

    private final String title;
    private final Component parent;

    public FileSelector(String title, Component parent) {
        this.title = title;
        this.parent = parent;
    }

    public abstract void onFileSelected(File file);

    @Override
    public final void actionPerformed(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle(title);
        if (JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(parent)) {
            onFileSelected(fileChooser.getSelectedFile());
        }
    }

}
