package codeanalyser;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.BorderLayout;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class CmdGraphe extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					CmdGraphe frame = new CmdGraphe("");
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public CmdGraphe(String data) {
		setTitle("Graphe d'appel");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 597, 429);
		contentPane = new JPanel();
		contentPane.setBackground(new Color(14, 41, 84));
		contentPane.setLayout(new BorderLayout(0, 0));
		
		final JTextArea cmdDisplayPanel = new JTextArea(9, 30);
		cmdDisplayPanel.setLineWrap(true);
		cmdDisplayPanel.setWrapStyleWord(true);
		cmdDisplayPanel.setFont(new Font("Monospaced", Font.ITALIC, 16));
		cmdDisplayPanel.setForeground(new Color(255, 255, 255));
		cmdDisplayPanel.setBackground(new Color(14, 41, 84));
		
		JScrollPane ScrollableCmd = new JScrollPane(
				cmdDisplayPanel,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
		);
		contentPane.add(ScrollableCmd);
		
		cmdDisplayPanel.setText(data);
		
		JButton btnNewButton = new JButton("Close the cmd");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		btnNewButton.setBackground(new Color(255, 128, 128));
		btnNewButton.setFont(new Font("Microsoft Sans Serif", Font.BOLD, 19));
		ScrollableCmd.setColumnHeaderView(btnNewButton);

		setContentPane(contentPane);
	}

}
