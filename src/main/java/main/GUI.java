package main;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import input.InputObject;

public class GUI implements NativeKeyListener {
	private static final String TOPBAR_FORMAT = "<html>%s<br><h1 style=\"text-align:center\"><b>%s</b></h1></html>";
	
	private List<EventListener> listeners = new ArrayList<EventListener>();
	private boolean recording;
	private boolean playing;
	
	private JLabel labelRecording;
	private JLabel labelPlayback;
	private JLabel labelOptions;
	private JCheckBoxMenuItem checkBoxMenuItem;
	
	private Recorder parent;
	
    public GUI(Recorder parent) {
    	this.parent = parent;
        SwingUtilities.invokeLater(() -> {
            createAndShowGUI();
        });
    }
    
    public void setRecording(boolean enabled) {
    	recording = enabled;
    	
    	if (enabled) {
    		labelRecording.setForeground(Color.RED);
    	} else {
    		labelRecording.setForeground(Color.WHITE);
    	}

    	for (EventListener listener : listeners) {
            listener.onEventTriggered(1, recording);
        }
    }
    
    public void setPlaying(boolean enabled) {
    	playing = enabled;
    	
    	if (enabled) {
    		labelPlayback.setForeground(Color.RED);
    	} else {
    		labelPlayback.setForeground(Color.WHITE);
    	}
    	
    	for (EventListener listener : listeners) {
            listener.onEventTriggered(2, playing);
        }
    }
    
    public boolean isRecording() {
    	return recording;
    }
    
    public void addListener(EventListener listener) {
    	listeners.add(listener);
    }

    private void createAndShowGUI() {
        // Create the JFrame (window) without borders
        JFrame frame = new JFrame("Top Bar");
        frame.setUndecorated(true); // Removes window borders
        frame.setAlwaysOnTop(true); // Keep it always on top of other windows
        frame.setResizable(false); // Not resizable

        // Create the top bar panel with horizontal layout
        JPanel topBarPanel = new JPanel();
        topBarPanel.setLayout(new BoxLayout(topBarPanel, BoxLayout.X_AXIS)); // Horizontal BoxLayout
        topBarPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // Add some padding
        topBarPanel.setBackground(new Color(0, 0, 0, 255)); // Semi-transparent black background

        // Create labels (or buttons) for "A", "B", "C", "D"
        labelRecording = new JLabel(String.format(TOPBAR_FORMAT, "Record", "F1"));
        labelPlayback = new JLabel(String.format(TOPBAR_FORMAT, "Play/Stop", "F2"));
        labelOptions = new JLabel("Options"); // Change label to reflect that it's clickable
        
        // Customize label appearance
        labelRecording.setForeground(Color.WHITE);
        labelPlayback.setForeground(Color.WHITE);
        labelOptions.setForeground(Color.WHITE);

        Font labelFont = new Font("Arial", Font.PLAIN, 18);
        labelRecording.setFont(labelFont);
        labelPlayback.setFont(labelFont);
        labelOptions.setFont(labelFont);

        // Add elements to the panel with spacing
        topBarPanel.add(labelRecording);
        topBarPanel.add(Box.createRigidArea(new Dimension(10, 0))); // Add spacing
        topBarPanel.add(new JSeparator(SwingConstants.VERTICAL));
        topBarPanel.add(Box.createRigidArea(new Dimension(10, 0))); // Add spacing
        topBarPanel.add(labelPlayback);
        topBarPanel.add(Box.createRigidArea(new Dimension(10, 0))); // Add spacing
        topBarPanel.add(new JSeparator(SwingConstants.VERTICAL));
        topBarPanel.add(Box.createRigidArea(new Dimension(10, 0))); // Add spacing
        topBarPanel.add(labelOptions);
        
        // Add the import/export buttons to the top bar
        JButton btnExport = new JButton("Export");
        JButton btnImport = new JButton("Import");

        topBarPanel.add(Box.createRigidArea(new Dimension(10, 0))); // Add spacing
        topBarPanel.add(btnExport);
        topBarPanel.add(Box.createRigidArea(new Dimension(10, 0))); // Add spacing
        topBarPanel.add(btnImport);
        
        topBarPanel.add(Box.createRigidArea(new Dimension(10, 0))); // Add spacing
        topBarPanel.add(btnExport);
        topBarPanel.add(Box.createRigidArea(new Dimension(10, 0))); // Add spacing
        topBarPanel.add(btnImport);

        // Add the top bar to the frame
        frame.getContentPane().add(topBarPanel);
        frame.pack(); // Size the window to fit the content

        // Set the frame at the top-center of the screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int frameWidth = frame.getWidth();
        int xPos = (screenSize.width / 2) - (frameWidth / 2); // Center horizontally
        frame.setLocation(xPos, 0); // Stick to the top of the screen
        
        // Add action listeners for Import/Export
        btnExport.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportClubs();
            }
        });

        btnImport.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                importClubs();
            }
        });

        addOptionsMenu();
        
        // Make the frame visible
        frame.setVisible(true);
    }
    
    private void addOptionsMenu() {
        // Create the popup menu
        JPopupMenu optionsMenu = new JPopupMenu();

        // Create a checkbox menu item
        checkBoxMenuItem = new JCheckBoxMenuItem("Continous Playback");
        checkBoxMenuItem.setSelected(false); // Default state
        checkBoxMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Handle checkbox state change
                boolean isSelected = checkBoxMenuItem.isSelected();
                parent.setContinousPlayback(isSelected);
                // You can notify listeners or handle the state change here
            }
        });

        // Add the checkbox item to the popup menu
        optionsMenu.add(checkBoxMenuItem);

        // Add a mouse listener to the "Options" label to show the popup menu when clicked
        labelOptions.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Show the popup menu at the location of the click
                optionsMenu.show(labelOptions, e.getX(), e.getY());
            }
        });
    }
    
    // Method to export the list of clubs
    private void exportClubs() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showSaveDialog(null);

        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (FileOutputStream fos = new FileOutputStream(file);
                ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                oos.writeObject(parent.loggedRecording);
                JOptionPane.showMessageDialog(null, "Export Successful!");
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Export Failed!");
            }
        }
    }

    // Method to import the list of clubs
    private void importClubs() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showOpenDialog(null);

        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (FileInputStream fis = new FileInputStream(file);
                ObjectInputStream ois = new ObjectInputStream(fis)) {
                parent.loggedRecording = (ArrayList<InputObject>) ois.readObject();
                JOptionPane.showMessageDialog(null, "Import Successful!");
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Import Failed!");
            }
        }
    }
    
    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
    	if (e.getKeyCode() == Recorder.RECORD_HOTKEY) {
    		setRecording(!recording);
    	} else if (e.getKeyCode() == Recorder.PLAYBACK_HOTKEY) {
    		setPlaying(!playing);
    	}
    }
}