package codeanalyser;

import java.awt.EventQueue;
import java.awt.FlowLayout;
import javax.swing.JFrame;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Map;
import java.awt.Font;
import javax.swing.JPanel;
import javax.swing.JButton;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;

import net.miginfocom.swing.MigLayout;

import javax.swing.JFileChooser;

public class CodeAnalyserGUI {

	private JFrame frmCodeanaliser;
	private String filePath;
	private File folder;
	private Map<String, Integer> topMethodsClass;
	private Map<String, Integer> topAttributesClass;
	private Map<String, Integer> topLinesMethods;
	private Map<String, Integer> topAttributesMethodsClasses;
	private String cmdString;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					CodeAnalyserGUI window = new CodeAnalyserGUI();
					window.frmCodeanaliser.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public CodeAnalyserGUI() {
		initialize();
	}
	
	private void initialize() {
		frmCodeanaliser = new JFrame();
		frmCodeanaliser.setTitle("Code Analyser");
		frmCodeanaliser.getContentPane().setBackground(new Color(255, 255, 255));
		frmCodeanaliser.getContentPane().setLayout(new BorderLayout(0, 0));
		frmCodeanaliser.setMinimumSize(new Dimension(500, 500));
		
		JPanel panelHeader = new JPanel();
		panelHeader.setBorder(new LineBorder(new Color(31, 110, 140), 14));
		panelHeader.setBackground(new Color(31, 110, 140));
		frmCodeanaliser.getContentPane().add(panelHeader, BorderLayout.NORTH);
		
		JPanel panelFooter = new JPanel();
		panelFooter.setBackground(new Color(31, 110, 140));
		frmCodeanaliser.getContentPane().add(panelFooter, BorderLayout.SOUTH);
		panelFooter.setLayout(new BorderLayout(0, 0));
		
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
		panelFooter.add(ScrollableCmd);
		
		JPanel panel = new JPanel();
		panel.setBackground(new Color(31, 110, 140));
		panelFooter.add(panel, BorderLayout.NORTH);
		
		JPanel panel_1 = new JPanel();
		panel_1.setBackground(new Color(31, 110, 140));
		panelFooter.add(panel_1, BorderLayout.SOUTH);
		
		JPanel panel_2 = new JPanel();
		panel_2.setBackground(new Color(31, 110, 140));
		panelFooter.add(panel_2, BorderLayout.WEST);
		
		JPanel panel_3 = new JPanel();
		panel_3.setBackground(new Color(31, 110, 140));
		panelFooter.add(panel_3, BorderLayout.EAST);
		panelHeader.setLayout(new MigLayout("", "[182px]", "[29px]"));
		
		JLabel header = new JLabel("Code Analyser");
		header.setForeground(new Color(255, 255, 255));
		header.setFont(new Font("Lucida Sans Typewriter", Font.BOLD, 28));
		panelHeader.add(header, "cell 0 0,alignx center,aligny top");
		
		JPanel centerPanel = new JPanel();
		centerPanel.setBackground(new Color(14, 41, 84));
		frmCodeanaliser.getContentPane().add(centerPanel, BorderLayout.CENTER);
		centerPanel.setLayout(new BorderLayout(0, 0));
		
		JPanel panel_4 = new JPanel(new FlowLayout(FlowLayout.CENTER));
		panel_4.setBackground(new Color(14, 41, 84));
        panel_4.setBorder(new EmptyBorder(10, 10, 10, 10));
		centerPanel.add(panel_4, BorderLayout.NORTH);
		
		JLabel chooseProjectLabel = new JLabel("Choose project path : ");
		chooseProjectLabel.setFont(new Font("Tahoma", Font.BOLD, 16));
		chooseProjectLabel.setForeground(new Color(255, 255, 255));
		panel_4.add(chooseProjectLabel);

		
		final JLabel choosedFilePathDisplay = new JLabel("Chosen File Path");
		choosedFilePathDisplay.setForeground(new Color(255, 255, 255));
		choosedFilePathDisplay.setFont(new Font("Tahoma", Font.BOLD, 13));
		choosedFilePathDisplay.setVisible(false);
		panel_4.add(choosedFilePathDisplay);
		
		final JButton chooseProjectBtn = new JButton("Choose Directory");
		chooseProjectBtn.setForeground(new Color(255, 255, 255));
		chooseProjectBtn.setBackground(new Color(132, 167, 161));
		panel_4.add(chooseProjectBtn);
		
		final JButton discardChoosedProject = new JButton("Discard");
		discardChoosedProject.setBackground(new Color(255, 128, 128));
		discardChoosedProject.setForeground(new Color(255, 255, 255));
		discardChoosedProject.setVisible(false);
		panel_4.add(discardChoosedProject);
		
		final JButton analyseBtn = new JButton("Analyse");
		analyseBtn.setBackground(new Color(0, 255, 0));
		analyseBtn.setForeground(new Color(255, 255, 255));
		analyseBtn.setToolTipText("Start process");
		analyseBtn.setVisible(false);
		panel_4.add(analyseBtn);
		
		//Choosing the filePath
		chooseProjectBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                int returnValue = fileChooser.showOpenDialog(null);

                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    filePath = selectedFile.getAbsolutePath();
                    JOptionPane.showMessageDialog(frmCodeanaliser, "Selected File : " + filePath);
                    choosedFilePathDisplay.setText("...." + filePath.substring(Math.max(0, filePath.length() - 20)));
                    choosedFilePathDisplay.setVisible(true);
                    chooseProjectBtn.setVisible(false);
                    discardChoosedProject.setVisible(true);
            		analyseBtn.setEnabled(true);
            		analyseBtn.setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(frmCodeanaliser, "No file selected.");
                }
            }
        });
		
		JPanel panel_6 = new JPanel();
		panel_6.setBackground(new Color(14, 41, 84));
		centerPanel.add(panel_6, BorderLayout.CENTER);
		panel_6.setLayout(new BorderLayout(0, 0));
		
		JPanel separatorPanel = new JPanel();
		separatorPanel.setBackground(new Color(14, 41, 84));
		panel_6.add(separatorPanel, BorderLayout.NORTH);
		separatorPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		JLabel separatorLabel = new JLabel("- • • • • • -");
		separatorLabel.setForeground(new Color(255, 255, 255));
		separatorLabel.setFont(new Font("Tahoma", Font.BOLD, 15));
		separatorPanel.add(separatorLabel);
		
		final JPanel mainContentPanel = new JPanel();
		panel_6.add(mainContentPanel, BorderLayout.CENTER);
		mainContentPanel.setLayout(new BorderLayout(0, 0));
		
		final JPanel subMainContentPanel = new JPanel();
		subMainContentPanel.setBackground(new Color(14, 41, 84));
		subMainContentPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		final JPanel subMainContentPanelWaiting = new JPanel();
		mainContentPanel.add(subMainContentPanelWaiting, BorderLayout.CENTER);
		subMainContentPanelWaiting.setBackground(new Color(14, 41, 84));
		subMainContentPanelWaiting.setVisible(true);
		subMainContentPanelWaiting.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		final JLabel waitingText = new JLabel("Click on analyse to see your project informations");
		waitingText.setForeground(new Color(255, 255, 255));
		waitingText.setFont(new Font("Tahoma", Font.BOLD, 25));
		subMainContentPanelWaiting.add(waitingText);
		
		//Class count panel
		
		JPanel classCountPanel = new JPanel();
		classCountPanel.setBackground(new Color(31, 110, 140));
		classCountPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		subMainContentPanel.add(classCountPanel);
		
		final JLabel classCountNumber = new JLabel("?");
		classCountNumber.setForeground(new Color(255, 190, 111));
		classCountNumber.setFont(new Font("Tahoma", Font.BOLD, 25));
		classCountPanel.add(classCountNumber);
		
		JLabel classCountLabel = new JLabel("Classe(s)");
		classCountLabel.setForeground(new Color(255, 255, 255));
		classCountLabel.setFont(new Font("Tahoma", Font.BOLD, 25));
		classCountPanel.add(classCountLabel);
		
		//Line count panel
		
		JPanel totalCountLinesPanel = new JPanel();
		totalCountLinesPanel.setBackground(new Color(31, 110, 140));
		totalCountLinesPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		subMainContentPanel.add(totalCountLinesPanel);
		
		final JLabel totalLinesNumber = new JLabel("?");
		totalLinesNumber.setForeground(new Color(255, 190, 111));
		totalLinesNumber.setFont(new Font("Tahoma", Font.BOLD, 25));
		totalCountLinesPanel.add(totalLinesNumber);
		
		JLabel lineCountLabel = new JLabel("Line(s) of code");
		lineCountLabel.setForeground(new Color(255, 255, 255));
		lineCountLabel.setFont(new Font("Tahoma", Font.BOLD, 25));
		totalCountLinesPanel.add(lineCountLabel);
		
		//Method count panel
		
		JPanel totalMethodPanel = new JPanel();
		totalMethodPanel.setBackground(new Color(31, 110, 140));
		totalMethodPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		subMainContentPanel.add(totalMethodPanel);
		
		final JLabel methodCountNumber = new JLabel("?");
		methodCountNumber.setForeground(new Color(255, 190, 111));
		methodCountNumber.setFont(new Font("Tahoma", Font.BOLD, 25));
		totalMethodPanel.add(methodCountNumber);
		
		JLabel methodCountLabel = new JLabel("Method(s)");
		methodCountLabel.setForeground(new Color(255, 255, 255));
		methodCountLabel.setFont(new Font("Tahoma", Font.BOLD, 25));
		totalMethodPanel.add(methodCountLabel);
		
		//Packages count panel
		
		JPanel packageCountPanel = new JPanel();
		packageCountPanel.setBackground(new Color(31, 110, 140));
		packageCountPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		subMainContentPanel.add(packageCountPanel);
		
		final JLabel packageCountNumber = new JLabel("?");
		packageCountNumber.setForeground(new Color(255, 190, 111));
		packageCountNumber.setFont(new Font("Tahoma", Font.BOLD, 25));
		packageCountPanel.add(packageCountNumber);
		
		JLabel packageCountLabel = new JLabel("Package(s)");
		packageCountLabel.setForeground(new Color(255, 255, 255));
		packageCountLabel.setFont(new Font("Tahoma", Font.BOLD, 25));
		packageCountPanel.add(packageCountLabel);
		
		//Average lines count per method panel
		
		JPanel avgLinesMethodPanel = new JPanel();
		avgLinesMethodPanel.setBackground(new Color(31, 110, 140));
		avgLinesMethodPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		subMainContentPanel.add(avgLinesMethodPanel);
		
		final JLabel avgLinesMethodNumber = new JLabel("?");
		avgLinesMethodNumber.setForeground(new Color(255, 190, 111));
		avgLinesMethodNumber.setFont(new Font("Tahoma", Font.BOLD, 25));
		avgLinesMethodPanel.add(avgLinesMethodNumber);
		
		JLabel avgLinesMethodLabel = new JLabel("Avg Line(s)/Methods");
		avgLinesMethodLabel.setForeground(new Color(255, 255, 255));
		avgLinesMethodLabel.setFont(new Font("Tahoma", Font.BOLD, 25));
		avgLinesMethodPanel.add(avgLinesMethodLabel);
		
		//Average method count per class panel
		
		JPanel avgMethodClassPanel = new JPanel();
		avgMethodClassPanel.setBackground(new Color(31, 110, 140));
		avgMethodClassPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		subMainContentPanel.add(avgMethodClassPanel);
		
		final JLabel avgMethodClassNumber = new JLabel("?");
		avgMethodClassNumber.setForeground(new Color(255, 190, 111));
		avgMethodClassNumber.setFont(new Font("Tahoma", Font.BOLD, 25));
		avgMethodClassPanel.add(avgMethodClassNumber);
		
		JLabel avgMethodClassLabel = new JLabel("Avg Methods(s)/Classes");
		avgMethodClassLabel.setForeground(new Color(255, 255, 255));
		avgMethodClassLabel.setFont(new Font("Tahoma", Font.BOLD, 25));
		avgMethodClassPanel.add(avgMethodClassLabel);
		
		//Average attribute count per class panel
		
		JPanel avgAttClassPanel = new JPanel();
		avgAttClassPanel.setBackground(new Color(31, 110, 140));
		avgAttClassPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		subMainContentPanel.add(avgAttClassPanel);
		
		final JLabel avgAttClassNumber = new JLabel("?");
		avgAttClassNumber.setForeground(new Color(255, 190, 111));
		avgAttClassNumber.setFont(new Font("Tahoma", Font.BOLD, 25));
		avgAttClassPanel.add(avgAttClassNumber);
		
		JLabel avgAttClassLabel = new JLabel("Avg Attribute(s)/Classes");
		avgAttClassLabel.setForeground(new Color(255, 255, 255));
		avgAttClassLabel.setFont(new Font("Tahoma", Font.BOLD, 25));
		avgAttClassPanel.add(avgAttClassLabel);
		
		//Average attribute count per class panel
		
		JPanel maxaParameterAppPanel = new JPanel();
		maxaParameterAppPanel.setBackground(new Color(31, 110, 140));
		maxaParameterAppPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		subMainContentPanel.add(maxaParameterAppPanel);
		
		final JLabel maxaParameterAppNumber = new JLabel("?");
		maxaParameterAppNumber.setForeground(new Color(255, 190, 111));
		maxaParameterAppNumber.setFont(new Font("Tahoma", Font.BOLD, 25));
		maxaParameterAppPanel.add(maxaParameterAppNumber);
		
		JLabel maxaParameterAppLabel = new JLabel("Max App Parameter(s) ");
		maxaParameterAppLabel.setForeground(new Color(255, 255, 255));
		maxaParameterAppLabel.setFont(new Font("Tahoma", Font.BOLD, 25));
		maxaParameterAppPanel.add(maxaParameterAppLabel);		
		
		
		//ChartPanel
		
		final JPanel subMainContentChartPanel = new JPanel();
		subMainContentChartPanel.setBackground(new Color(14, 41, 84));
		subMainContentChartPanel.setVisible(true);
		subMainContentChartPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
		
		//Panel for charts
		
		JPanel GraphPanel = new JPanel();
		GraphPanel.setBackground(new Color(14, 41, 84));
		subMainContentChartPanel.add(GraphPanel);
		
		//Normal Chart
		
		final DefaultPieDataset datasetPieClassPerMethods = new DefaultPieDataset();

        JFreeChart chartPieMethodPerClass = ChartFactory.createPieChart(
                "10% Classes with largest number of methods",  // Titre du diagramme
                datasetPieClassPerMethods,  // Ensemble de données
                true,     // Afficher la légende
                true,     // Activer l'outil d'infobulle
                false);   // Ne pas générer d'URL

        PiePlot plot = (PiePlot) chartPieMethodPerClass.getPlot();
        plot.setSectionOutlinesVisible(true);

        ChartPanel chartPanelPieClassPerMethod = new ChartPanel(chartPieMethodPerClass);
        chartPanelPieClassPerMethod.setPreferredSize(new Dimension(300, 200));
        
        //Normal Chart
		
        final DefaultPieDataset datasetPieAttributesPerClass = new DefaultPieDataset();

        JFreeChart chartPieAttributesPerClass = ChartFactory.createPieChart(
                "10% Classes with largest number of attributes",  // Titre du diagramme
                datasetPieAttributesPerClass,  // Ensemble de données
                true,     // Afficher la légende
                true,     // Activer l'outil d'infobulle
                false);   // Ne pas générer d'URL

        PiePlot plotAttributes = (PiePlot) chartPieAttributesPerClass.getPlot();
        plot.setSectionOutlinesVisible(true);

        ChartPanel chartPanelPieAttributesPerClass = new ChartPanel(chartPieAttributesPerClass);
        chartPanelPieAttributesPerClass.setPreferredSize(new Dimension(300, 200));
        
        //Pie Chart
        
        final DefaultPieDataset datasetPieMostLineMethods = new DefaultPieDataset();

        JFreeChart chartPieMostLineMethods = ChartFactory.createPieChart(
                "10% Largest Methods",  // Titre du diagramme
                datasetPieMostLineMethods,  // Ensemble de données
                true,     // Afficher la légende
                true,     // Activer l'outil d'infobulle
                false);   // Ne pas générer d'URL

        PiePlot plotMostLineMethods = (PiePlot) chartPieMostLineMethods.getPlot();
        plot.setSectionOutlinesVisible(true);

        ChartPanel chartPanelPieMostLineMethod = new ChartPanel(chartPieMostLineMethods);
        chartPanelPieMostLineMethod.setPreferredSize(new Dimension(300, 200));
        
        //Pie Chart
        
        final DefaultPieDataset datasetPieAttributesMethodsClass = new DefaultPieDataset();

        JFreeChart chartPieAttributesMethodsClass = ChartFactory.createPieChart(
                "Class that are in 10% Most Methods and Attributes",  // Titre du diagramme
                datasetPieMostLineMethods,  // Ensemble de données
                true,     // Afficher la légende
                true,     // Activer l'outil d'infobulle
                false);   // Ne pas générer d'URL

        PiePlot plotAttributesMethodsClass = (PiePlot) chartPieAttributesMethodsClass.getPlot();
        plot.setSectionOutlinesVisible(true);

        ChartPanel chartPanelPieAttributesMethodsClass = new ChartPanel(chartPieAttributesMethodsClass);
        chartPanelPieAttributesMethodsClass.setPreferredSize(new Dimension(300, 200));
        
        //Adding chartsPanel to the main GraphPanel
        GraphPanel.add(chartPanelPieClassPerMethod);
        GraphPanel.add(chartPanelPieAttributesPerClass);
        GraphPanel.add(chartPanelPieMostLineMethod);
        GraphPanel.add(chartPanelPieAttributesMethodsClass);
	    
		frmCodeanaliser.setBackground(new Color(255, 255, 255));
		frmCodeanaliser.setBounds(100, 100, 1346, 791);
		
		//Anlayse button action
		analyseBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				cmdDisplayPanel.setText("Code analysis startup");
				Parser parser;
				CodeAnalyser analyse = new CodeAnalyser();
				//Creating the folder with the source Path
                folder = new File(filePath + "/src");
                parser = new Parser();
				try {
	                parser.setProjectSourcePath(filePath + "/src");
					CodeAnalyser.runAllStats(folder);
				} catch (Exception e2) {
					JOptionPane.showMessageDialog(frmCodeanaliser, "Error ! Please verify your selected folder");
				}
				
//				totalLinesNumber.setText(Integer.toString(analyse.getProjectLinesOfCode()));
//				methodCountNumber.setText(Integer.toString(analyse.getProjectMethodsNumber()));
//				packageCountNumber.setText(Integer.toString(analyse.getProjectPackagesNumber()));
//				classCountNumber.setText(Integer.toString(analyse.getProjectClassesNumber()));
//				avgLinesMethodNumber.setText(new DecimalFormat("##.##").format(analyse.getAverageLinesPerMethod()));
//				avgMethodClassNumber.setText(new DecimalFormat("##.##").format(analyse.getAveragMethodPerClass()));
//				avgAttClassNumber.setText(new DecimalFormat("##.##").format(analyse.getAverageAttPerClass()));
//				maxaParameterAppNumber.setText(new DecimalFormat("##.##").format(analyse.getTotalParametersPerMethod()));
				cmdString = analyse.getGraphCmd();
				cmdDisplayPanel.setText(analyse.getCmd());
				subMainContentPanelWaiting.setVisible(false);
//				topMethodsClass = analyse.getMethodsCountForTopClasses();
//				topAttributesClass = analyse.getAttributesCountForTopClasses();
//				topLinesMethods = analyse.getLinesPerMethods();
//				topAttributesMethodsClasses = analyse.getMethodAttributeClasses();
				
				//add infos to datasets
				if(topMethodsClass != null) {
					for (Map.Entry<String, Integer> entry : topMethodsClass.entrySet()) {
						datasetPieClassPerMethods.setValue(entry.getKey() + ":" + entry.getValue(), entry.getValue());
					}
				}
				
				if(topAttributesClass != null) {
					for (Map.Entry<String, Integer> entry : topAttributesClass.entrySet()) {
						datasetPieAttributesPerClass.setValue(entry.getKey() + ":" + entry.getValue(), entry.getValue());
					}
				}
				
				if(topLinesMethods != null) {
					for (Map.Entry<String, Integer> entry : topLinesMethods.entrySet()) {
						datasetPieMostLineMethods.setValue(entry.getKey() + ":" + entry.getValue(), entry.getValue());
					}
				}
				
				if(topAttributesMethodsClasses != null) {
					for (Map.Entry<String, Integer> entry : topAttributesMethodsClasses.entrySet()) {
						datasetPieAttributesMethodsClass.setValue(entry.getKey() + ":" + entry.getValue(), entry.getValue());
					}
				}
				
				mainContentPanel.add(subMainContentPanel, BorderLayout.CENTER);
				mainContentPanel.add(subMainContentChartPanel, BorderLayout.SOUTH);
			}
		});
		
		
		//Choosing another project path
		discardChoosedProject.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				choosedFilePathDisplay.setText("");
				filePath = null;
				discardChoosedProject.setVisible(false);
				chooseProjectBtn.setVisible(true);
				analyseBtn.setEnabled(false);
				analyseBtn.setVisible(false);
				cmdDisplayPanel.setText("");
				subMainContentPanelWaiting.setVisible(true);
				datasetPieClassPerMethods.clear();
				datasetPieAttributesPerClass.clear();
				datasetPieMostLineMethods.clear();
				cmdString = "";
				datasetPieAttributesMethodsClass.clear();
				mainContentPanel.remove(subMainContentPanel);
				mainContentPanel.remove(subMainContentChartPanel);
			}
		});
		
		//Debug
		//mainContentPanel.add(subMainContentPanel, BorderLayout.CENTER);
		//mainContentPanel.add(subMainContentChartPanel, BorderLayout.SOUTH);

		JButton graphBtn = new JButton("See graph");
		graphBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(cmdString != null) {
					launchGraphCmd(cmdString);
				} else {
                    JOptionPane.showMessageDialog(frmCodeanaliser, "Error ! No CMD or graph");
				}
			}
		});
		graphBtn.setBackground(new Color(255, 128, 0));
		graphBtn.setForeground(new Color(255, 255, 255));
		graphBtn.setFont(new Font("Segoe UI Semibold", Font.BOLD, 28));
		subMainContentPanel.add(graphBtn);

	    
		frmCodeanaliser.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	private void launchGraphCmd(String data) {
        CmdGraphe secondFrame = new CmdGraphe(data);
        secondFrame.setVisible(true);
    }
}
