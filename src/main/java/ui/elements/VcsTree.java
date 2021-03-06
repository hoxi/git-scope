package ui.elements;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ui.ChangeNodeDecorator;
import com.intellij.openapi.vcs.changes.ui.ChangesBrowser;
import com.intellij.openapi.vcs.changes.ui.SimpleChangesBrowser;
import com.intellij.openapi.vcs.changes.ui.TreeModelBuilder;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import ui.MySimpleChangesBrowser;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.Collection;
import java.util.List;

public class VcsTree extends JPanel implements Element {

    private final Project project;

    public VcsTree(Project project) {

        this.project = project;

        this.createElement();
        this.addListener();

    }

    public void createElement() {
        this.setLayout(new BorderLayout());

        this.setLoading();

    }

    public void addListener() {

    }

    public void setLoading() {
        this.setComponent(centeredPanel(loadingIcon()));
    }

    public void update(Collection<Change> changes) {

        // Build the Diff-Tool-Window
        // SimpleChangesBrowser changesBrowser = new SimpleChangesBrowser(
        SimpleChangesBrowser changesBrowser = new MySimpleChangesBrowser(
                project,
                changes
        );

        setComponent(changesBrowser);

    }

    public void setComponent(Component component) {

        for (Component c : this.getComponents()) {
            this.remove(c);
        }

        this.add(component);

    }

    public JPanel centeredPanel(Component component){
        JPanel masterPane = new JPanel(new GridBagLayout());

        JPanel centerPane = new JPanel();
        centerPane.setLayout(new BoxLayout(centerPane, BoxLayout.Y_AXIS));

        masterPane.add(component);

        return masterPane;
    }

    private Component loadingIcon() {

        ClassLoader cldr = this.getClass().getClassLoader();
        java.net.URL imageURL = cldr.getResource("loading.png");
        ImageIcon imageIcon = new ImageIcon(imageURL);
        JLabel iconLabel = new JLabel();
        iconLabel.setIcon(imageIcon);
        imageIcon.setImageObserver(iconLabel);

        return iconLabel;

    }

}
