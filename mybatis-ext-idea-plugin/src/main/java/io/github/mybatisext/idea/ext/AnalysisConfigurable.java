package io.github.mybatisext.idea.ext;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;

public final class AnalysisConfigurable implements Configurable {

    private final Project project;
    private JCheckBox filterable;
    private JTextArea scalarTypes;

    public AnalysisConfigurable(Project project) {
        this.project = project;
    }

    @Override
    public String getDisplayName() {
        return "MyBatis Ext";
    }

    @Override
    public JComponent createComponent() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        filterable = new JCheckBox("Enable defaultFilterable (match your runtime ExtContext)");
        panel.add(filterable, BorderLayout.NORTH);
        scalarTypes = new JTextArea(12, 60);
        JPanel types = new JPanel(new BorderLayout());
        types.add(new JLabel("Java types with configured TypeHandlers (one qualified name per line):"), BorderLayout.NORTH);
        types.add(new JScrollPane(scalarTypes));
        panel.add(types);
        reset();
        return panel;
    }

    private AnalysisSettings.State state() {
        return project.getService(AnalysisSettings.class).getState();
    }

    private List<String> types() {
        return scalarTypes.getText().lines().map(String::trim).filter(s -> !s.isEmpty()).distinct().sorted().toList();
    }

    @Override
    public boolean isModified() {
        return state().defaultFilterable != filterable.isSelected() || !new TreeSet<>(state().scalarTypes).equals(new TreeSet<>(types()));
    }

    @Override
    public void apply() {
        state().defaultFilterable = filterable.isSelected();
        state().scalarTypes = new ArrayList<>(types());
        DaemonCodeAnalyzer.getInstance(project).restart();
    }

    @Override
    public void reset() {
        filterable.setSelected(state().defaultFilterable);
        scalarTypes.setText(String.join("\n", state().scalarTypes));
    }

    @Override
    public void disposeUIResources() {
        filterable = null;
        scalarTypes = null;
    }
}
