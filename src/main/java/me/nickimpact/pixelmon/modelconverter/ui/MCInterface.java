package me.nickimpact.pixelmon.modelconverter.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import me.nickimpact.pixelmon.modelconverter.ModelConverter;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Paths;
import java.util.function.Consumer;

public class MCInterface extends JFrame {

    private JPanel main;
    private JRadioButton generations;
    private JRadioButton reforged;
    private JButton inFileSelector;
    private JTextField inputLoc;
    private JRadioButton decode;
    private JRadioButton encode;
    private JTextField outLoc;
    private JButton outFileSelector;
    private JButton beginOperationButton;
    private JProgressBar progressBar;
    private JTextField status;
    private JLabel total;
    private JLabel successful;
    private JLabel processed;

    private ButtonGroup mode = new ButtonGroup();
    private ButtonGroup state = new ButtonGroup();

    public MCInterface() {
        super("Pixelmon Model Converter");

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setContentPane(this.main);
        this.setMinimumSize(new Dimension(420, 420));
        this.pack();

        this.applyFileChooserAction(this.inFileSelector, result -> this.inputLoc.setText(result.getAbsolutePath()));
        this.applyFileChooserAction(this.outFileSelector, result -> this.outLoc.setText(result.getAbsolutePath()));

        this.mode.add(this.reforged);
        this.mode.add(this.generations);

        this.state.add(this.decode);
        this.state.add(this.encode);

        this.beginOperationButton.addActionListener(action -> {
            ModelConverter.run(
                    this.mode.isSelected(this.reforged.getModel()),
                    this.state.isSelected(this.decode.getModel()),
                    Paths.get(this.inputLoc.getText()).toFile(),
                    Paths.get(this.outLoc.getText()).toFile(),
                    this.status,
                    this.total,
                    this.processed,
                    this.successful,
                    this.progressBar
            );
        });
    }

    public void open() {
        this.setVisible(true);
    }

    private void applyFileChooserAction(JButton button, Consumer<File> consumer) {
        button.addActionListener(action -> {
            new FileSelector(consumer.andThen(x -> {
                try {
                    if (this.isNonEmpty(this.inputLoc.getText()) && this.isNonEmpty(this.outLoc.getText())) {
                        this.beginOperationButton.setEnabled(true);
                    }
                } catch (NullPointerException ignored) {
                }
            }));
        });
    }

    private boolean isNonEmpty(String in) {
        return !in.isEmpty();
    }

    //---------------------------------------------------------------------------------------
    //
    // IntelliJ Generated Code below, leave untouched
    //
    //---------------------------------------------------------------------------------------

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        main = new JPanel();
        main.setLayout(new GridLayoutManager(7, 3, new Insets(20, 20, 20, 20), -1, -1));
        main.setMinimumSize(new Dimension(420, 420));
        inputLoc = new JTextField();
        inputLoc.setEditable(false);
        inputLoc.setInheritsPopupMenu(false);
        main.add(inputLoc, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        generations = new JRadioButton();
        generations.setText("Generations");
        main.add(generations, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        reforged = new JRadioButton();
        reforged.setSelected(true);
        reforged.setText("Reforged");
        main.add(reforged, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_NORTHEAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(6, 3, new Insets(20, 0, 0, 0), -1, -1));
        main.add(panel1, new GridConstraints(6, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        panel1.add(progressBar, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(4, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Operation Progress");
        panel1.add(label1, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        total = new JLabel();
        total.setText("Total: 0");
        panel1.add(total, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        successful = new JLabel();
        successful.setText("Successful: 0");
        panel1.add(successful, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        processed = new JLabel();
        processed.setText("Processed: 0");
        panel1.add(processed, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Status");
        panel1.add(label2, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_SOUTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        status = new JTextField();
        status.setEditable(false);
        panel1.add(status, new GridConstraints(5, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        decode = new JRadioButton();
        decode.setLabel("Decode");
        decode.setSelected(true);
        decode.setText("Decode");
        main.add(decode, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        encode = new JRadioButton();
        encode.setLabel("Encode");
        encode.setText("Encode");
        main.add(encode, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        inFileSelector = new JButton();
        inFileSelector.setText("Select Input Location");
        main.add(inFileSelector, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(60, -1), null, null, 0, false));
        outLoc = new JTextField();
        outLoc.setEditable(false);
        main.add(outLoc, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        outFileSelector = new JButton();
        outFileSelector.setText("Select Output Location");
        main.add(outFileSelector, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        main.add(spacer2, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 25), null, new Dimension(-1, 100), 0, false));
        beginOperationButton = new JButton();
        beginOperationButton.setEnabled(false);
        beginOperationButton.setText("Begin Operation");
        main.add(beginOperationButton, new GridConstraints(5, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, -1), null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return main;
    }

}
