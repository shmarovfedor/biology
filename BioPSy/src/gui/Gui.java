package gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultCaret;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import model.AdvancedOptionsModel;
import model.ODEModel;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.AssignmentRule;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.RateRule;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Rule;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.Species;

import parser.OutputParser;
import parser.SMT2SettingsParser;
import parser.TraceParser;
import util.*;
import util.Box;
import util.Utility.Tuple;

public class Gui implements ActionListener {

    private TimeSeriesPanel timeSeriesPanel;

    private JFrame gui;

    private JTextArea logTextArea, sbmlTextArea;

    private BoxTable boxTable;

    private Box domain;

	private JTextField sbml, series;

	private JButton browseSBML, browseSeries, generateSMT2, run, advancedOptionsButton, stopButton, okButton;

	private JScrollPane paramsScroll, speciesScroll, outputScroll, sbmlScroll, logScroll, graphOutputScroll;

    private PlotPanel plotPanel2D;

	private JLabel sbmlLabel, seriesLabel;

	private JFileChooser fc;

    private JTabbedPane tabbedPane;

	private JPanel paramsPanel, speciesPanel;

    private JProgressBar progressBar;

    private BackgroundWorker bgWorker;

    private Thread outputListener;

    boolean isStopped = false;

	public Gui(){

		fc = new JFileChooser();

		// Create the frame
		gui = new JFrame("BioPSy");
		gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gui.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                int pid = AdvancedOptionsModel.getParsynPID();
                if((pid != -1) && (!isStopped)) {
                    killParSyn(pid);
                }
            }
        });

		sbml = new JTextField(30);
		sbmlLabel = new JLabel("SBML File:");
		browseSBML = new JButton("Browse");
		browseSBML.addActionListener(this);

		series = new JTextField(30);
		seriesLabel = new JLabel("Time Series File:");
		browseSeries = new JButton("Browse");
		browseSeries.addActionListener(this);

		//generateSMT2 = new JButton("Generate SMT2");
		//generateSMT2.addActionListener(this);

        // Run button
		run = new JButton("Run");
		run.addActionListener(this);
        gui.getRootPane().setDefaultButton(run);
        run.requestFocus();

        // Advanced Options button
        advancedOptionsButton = new JButton("Advanced Options");
        advancedOptionsButton.addActionListener(this);

        okButton = new JButton("OK");
        okButton.addActionListener(this);
        okButton.setVisible(false);

        // Stop button
        stopButton = new JButton("Stop");
        stopButton.addActionListener(this);
        stopButton.setEnabled(false);
        stopButton.setVisible(false);

		// Create panels for the inputs and buttons
		JPanel topPanel = new JPanel(new GridLayout(2, 3));
		paramsPanel = new JPanel();
		speciesPanel = new JPanel();
		JPanel bottomPanel = new JPanel(new GridLayout(1, 2));
		JPanel buttonsPanel = new JPanel();
		JPanel middlePanel = new JPanel(new BorderLayout());
		JPanel mainPanel = new JPanel(new BorderLayout());

        // Create text area for program output
        logTextArea = new JTextArea();
        logTextArea.setText("Application: started " + new Date() + "\n");
        logTextArea.setEditable(false);
        // Updating the text area
        DefaultCaret caret = (DefaultCaret) logTextArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        // Create text area for SBML file
        sbmlTextArea = new JTextArea();
        sbmlTextArea.setEditable(false);

        // Create text area for time series
        //timeSeriesTextArea = new JTextArea();
        //timeSeriesTextArea.setEditable(false);
        timeSeriesPanel = new TimeSeriesPanel();

        paramsScroll = new JScrollPane();
        paramsScroll.setMinimumSize(new Dimension(600, 600));
        paramsScroll.setPreferredSize(new Dimension(600, 600));

        speciesScroll = new JScrollPane();
        speciesScroll.setMinimumSize(new Dimension(600, 600));
        speciesScroll.setPreferredSize(new Dimension(600, 600));

        logScroll = new JScrollPane();
        logScroll.setMinimumSize(new Dimension(600, 600));
        logScroll.setPreferredSize(new Dimension(600, 600));

        outputScroll = new JScrollPane();
        outputScroll.setMinimumSize(new Dimension(600, 600));
        outputScroll.setPreferredSize(new Dimension(600, 600));

        sbmlScroll = new JScrollPane();
        sbmlScroll.setMinimumSize(new Dimension(600, 600));
        sbmlScroll.setPreferredSize(new Dimension(600, 600));

        graphOutputScroll = new JScrollPane();
        graphOutputScroll.setMinimumSize(new Dimension(600, 600));
        graphOutputScroll.setPreferredSize(new Dimension(600, 600));

        logScroll.setViewportView(logTextArea);
		paramsScroll.setViewportView(paramsPanel);
		speciesScroll.setViewportView(speciesPanel);
        sbmlScroll.setViewportView(sbmlTextArea);
        boxTable = new BoxTable();
        outputScroll.setViewportView(boxTable);

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("SBML", sbmlScroll);
        tabbedPane.addTab("Time series", timeSeriesPanel);
        tabbedPane.addTab("Parameters", paramsScroll);
		tabbedPane.addTab("Variables", speciesScroll);
        tabbedPane.addTab("Log", logScroll);
        tabbedPane.addTab("Output", outputScroll);
        tabbedPane.addTab("Plot(2D only)", graphOutputScroll);
        tabbedPane.setEnabledAt(6, false);


        topPanel.add(sbmlLabel);
        topPanel.add(sbml);
        topPanel.add(browseSBML);
        topPanel.add(seriesLabel);
        topPanel.add(series);
        topPanel.add(browseSeries);

        progressBar = new JProgressBar();
        progressBar.setMaximum(100);
        progressBar.setMinimum(0);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);

		buttonsPanel.add(run);
        buttonsPanel.add(advancedOptionsButton);
        buttonsPanel.add(progressBar);
        buttonsPanel.add(stopButton);
        buttonsPanel.add(okButton);

        middlePanel.add(tabbedPane, BorderLayout.CENTER);
        middlePanel.add(bottomPanel, BorderLayout.SOUTH);

        mainPanel.add(topPanel, BorderLayout.NORTH);
		mainPanel.add(middlePanel, BorderLayout.CENTER);
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        // Add the main panel to the frame
		gui.setContentPane(mainPanel);
		gui.pack();

		// Get the screen dimensions
		Dimension screenSize;
		try {
			Toolkit tk = Toolkit.getDefaultToolkit();
			screenSize = tk.getScreenSize();
		} catch (AWTError awe) {
			screenSize = new Dimension(640, 480);
		}

		// Get/set the frame dimensions
		Dimension frameSize = gui.getSize();
		if (frameSize.height > screenSize.height) {
			frameSize.height = screenSize.height;
			gui.setSize(frameSize);
		}
		if (frameSize.width > screenSize.width) {
			frameSize.width = screenSize.width;
			gui.setSize(frameSize);
		}

		// Position the frame
		int x = screenSize.width / 2 - frameSize.width / 2;
		int y = screenSize.height / 2 - frameSize.height / 2;
		gui.setLocation(x, y);

		// Display the frame
		gui.setVisible(true);

	}

	@Override
	public void actionPerformed(ActionEvent e) {
        //System.out.println("Action event: " + e.getActionCommand() + " " + e.getSource());
		if (e.getSource() == browseSBML) {
            FileFilter xmlFilter = new FileNameExtensionFilter("XML File","xml");
            fc.resetChoosableFileFilters();
            fc.setFileFilter(xmlFilter);
			int returnVal = fc.showOpenDialog(gui);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
				sbml.setText(fc.getSelectedFile().getAbsolutePath());
                try {
					SBMLDocument document = SBMLReader.read(new File(sbml
							.getText()));
					List<String> assignments = new ArrayList<String>();
					for (Rule rule : document.getModel().getListOfRules()) {
						if (rule.isAssignment()) {
							AssignmentRule aRule = ((AssignmentRule) rule);
							assignments.add(aRule.getVariable());
						}
					}
					Map<String,String> parameters = new HashMap<String,String>();
					List<String> vars = new ArrayList<String>();
					for (Parameter param : document.getModel()
							.getListOfParameters()) {
						if (!assignments.contains(param.getId())) {
							parameters.put(param.getId(), String.valueOf(param.getValue()));
						}
					}
					for (Species s : document.getModel().getListOfSpecies()) {
						if (!assignments.contains(s.getId())) {
							vars.add(s.getId());
						}
					}
					for (Rule r : document.getModel().getListOfRules()) {
						if (r.isRate()) {
							String var = ((RateRule) r).getVariable();
							if (parameters.containsKey(var)) {
								parameters.remove(var);
							}
							if (!vars.contains(var)
									&& !assignments.contains(var)) {
								vars.add(var);
							}
						}
					}
					for (Reaction reaction : document.getModel()
							.getListOfReactions()) {
						for (LocalParameter parameter : reaction
								.getKineticLaw().getListOfLocalParameters()) {
							String newName = reaction.getId() + "_"
									+ parameter.getId();
							parameters.put(newName, String.valueOf(parameter.getValue()));
						}
					}
					List<String> params = new ArrayList<String>(parameters.keySet());
					Collections.sort(params);
					Collections.sort(vars);
					paramsPanel.removeAll();
					paramsPanel.setLayout(new GridLayout(params.size() + 1,
							4));
					paramsPanel.add(new JLabel("Synthesize"));
					paramsPanel.add(new JLabel("Name"));
					paramsPanel.add(new JLabel("Lower Bound"));
					paramsPanel.add(new JLabel("Upper Bound"));
                    for (String p : params) {
                        paramsPanel.add(new JCheckBox());
						paramsPanel.add(new JLabel(p));
						paramsPanel.add(new JTextField(parameters.get(p)));
						paramsPanel.add(new JTextField(parameters.get(p)));
					}
					paramsPanel.revalidate();
					speciesPanel.removeAll();
					speciesPanel.setLayout(new GridLayout(vars.size() + 1, 4));
					speciesPanel.add(new JLabel("Name"));
					speciesPanel.add(new JLabel("Lower Bound"));
					speciesPanel.add(new JLabel("Upper Bound"));
                    speciesPanel.add(new JLabel("Noise"));
					for (String s : vars) {
						speciesPanel.add(new JLabel(s));
						speciesPanel.add(new JTextField("0"));
						speciesPanel.add(new JTextField("1"));
                        speciesPanel.add(new JTextField("0.1"));
					}
					speciesPanel.revalidate();

                    // Output sbml to text area
                    tabbedPane.setSelectedIndex(0);
                    sbmlTextArea.read(new FileReader(sbml.getText()), null);
                    logTextArea.append("Application: loaded SBML model " + fc.getSelectedFile().getAbsolutePath() + " " + new Date() + "\n");
				} catch (XMLStreamException e1) {
					// TODO Auto-generated catch block
                    JOptionPane.showMessageDialog(gui,
                            "Error occurred while parsing the model file.",
                                "SBML parser",
                                    JOptionPane.ERROR_MESSAGE);
					e1.printStackTrace();
				} catch (IOException e1) {
                    JOptionPane.showMessageDialog(gui,
                            "Error occurred while parsing the model file.",
                                "SBML parser",
                                    JOptionPane.ERROR_MESSAGE);
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
            }
		} else if (e.getSource() == browseSeries) {
            FileFilter csvFilter = new FileNameExtensionFilter("CSV File","csv");
            fc.resetChoosableFileFilters();
            fc.setFileFilter(csvFilter);
            int returnVal = fc.showOpenDialog(gui);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				series.setText(fc.getSelectedFile().getAbsolutePath());
                // Output time series to text area
                try {
                    Trace trace = TraceParser.parseCopasiOutput(new File(series.getText().trim()));
                    timeSeriesPanel.updateTimeSeriesTable(trace);
                    if (trace == null) {
                        JOptionPane.showMessageDialog(gui,
                                "Error occurred while parsing time series data.",
                                "Time series parser",
                                JOptionPane.ERROR_MESSAGE);
                    } else {
                        for (int i = 4; i < speciesPanel.getComponentCount(); i += 4) {
                            double varMin = trace.getMinForVar(((JLabel) speciesPanel.getComponent(i)).getText());
                            double varMax = trace.getMaxForVar(((JLabel) speciesPanel.getComponent(i)).getText());
                            double interval = varMax - varMin;
                            if (interval != 0) {
                                ((JTextField) speciesPanel.getComponent(i + 1)).setText(Double.toString(varMin - 0.5 * interval));
                                ((JTextField) speciesPanel.getComponent(i + 2)).setText(Double.toString(varMax + 0.5 * interval));
                            } else {
                                ((JTextField) speciesPanel.getComponent(i + 1)).setText(Double.toString(0.5 * varMin));
                                ((JTextField) speciesPanel.getComponent(i + 2)).setText(Double.toString(1.5 * varMax));
                            }
                        }
                        tabbedPane.setSelectedIndex(1);
                        //timeSeriesTextArea.read(new FileReader(series.getText()), null);
                        logTextArea.append("Application: loaded time series " + fc.getSelectedFile().getAbsolutePath() + " " + new Date() + "\n");
                    }
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(gui,
                            "Error occurred while parsing time series data.",
                            "Time series parser",
                            JOptionPane.ERROR_MESSAGE);
                    e1.printStackTrace();
                }
            }
        }/* else if (e.getSource() == generateSMT2) {
			try {
				List<String> params = new ArrayList<String>();
				for (int i = 4; i < paramsPanel.getComponentCount(); i += 4) {
					if (((JCheckBox) paramsPanel.getComponent(i)).isSelected()) {
						params.add(((JLabel) paramsPanel.getComponent(i + 1))
								.getText());
					}
				}
				System.out.println(Utility.writeSMT2ToString(new ModelSettings(
						sbml.getText().trim(), series.getText().trim(), params,
						Double.parseDouble(noise.getText().trim()))));
			} catch (NumberFormatException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (XMLStreamException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}*/ else if (e.getSource() == advancedOptionsButton) {
            new AdvancedOptionsDialog(gui, "Advanced Options");
        } else if (e.getSource() == okButton) {
            isStopped = false;
            run.setVisible(true);
            advancedOptionsButton.setVisible(true);
            progressBar.setVisible(false);
            okButton.setVisible(false);
            browseSBML.setEnabled(true);
            browseSeries.setEnabled(true);
        } else if (e.getSource() == stopButton) {
            // Kill ParSyn process and children
            killParSyn(AdvancedOptionsModel.getParsynPID());
            isStopped = true;
            stopButton.setEnabled(false);
        } else if (e.getSource() == run) {
			try {
				Map<String, Tuple<Double, Double>> variables = new HashMap<String, Tuple<Double, Double>>();
				Map<String, ASTNode> odes = new HashMap<String, ASTNode>();
				List<String> params = new ArrayList<String>();
                domain = new Box(Box.BoxType.DOMAIN);
				for (int i = 4; i < paramsPanel.getComponentCount(); i += 4) {
					if (((JCheckBox) paramsPanel.getComponent(i)).isSelected()) {

                        String paramName = ((JLabel) paramsPanel.getComponent(i + 1)).getText();
                        double paramLeft = Double.parseDouble(((JTextField) paramsPanel.getComponent(i + 2)).getText().trim());
                        double paramRight = Double.parseDouble(((JTextField) paramsPanel.getComponent(i + 3)).getText().trim());

                        domain.addInterval(new Interval(paramLeft, paramRight, paramName));

                        variables.put(paramName, new Tuple<Double, Double>(paramLeft, paramRight));
						params.add(((JLabel) paramsPanel.getComponent(i + 1))
								.getText());
					}
				}
				ODEModel model = new ODEModel(SBMLReader.read(new File(sbml
						.getText().trim())), params);
                List<Double> noise = new ArrayList<Double>();
				for (int i = 4; i < speciesPanel.getComponentCount(); i += 4) {
					variables.put(
                            ((JLabel) speciesPanel.getComponent(i)).getText(),
                            new Tuple<Double, Double>(Double
                                    .parseDouble(((JTextField) speciesPanel
                                            .getComponent(i + 1)).getText()
                                            .trim()), Double
                                    .parseDouble(((JTextField) speciesPanel
                                            .getComponent(i + 2)).getText()
                                            .trim())));
					odes.put(
							((JLabel) speciesPanel.getComponent(i)).getText(),
							model.getODE(((JLabel) speciesPanel.getComponent(i))
									.getText()));
                    noise.add(Double.parseDouble(((JTextField) speciesPanel
                                .getComponent(i + 3)).getText().trim()));
				}
				SMT2SettingsParser.writeSettingsToFile(
						"model.xml",
						new SMT2Settings(variables, "t", odes, TraceParser
								.parseCopasiOutput(new File(series.getText()
										.trim())), noise));

                // creating a table
                boxTable.setDomain(domain);

                if(domain.getIntervals().size() == 2) {
                    tabbedPane.setEnabledAt(6, true);
                    plotPanel2D = new PlotPanel(domain);
                    graphOutputScroll.setViewportView(plotPanel2D);
                } else {
                    tabbedPane.setEnabledAt(6, false);
                }

                // Creating a background worker
                bgWorker = new BackgroundWorker(logTextArea);

                // Adding a listener checking the status of background worker
                bgWorker.addPropertyChangeListener(
                        new PropertyChangeListener() {
                            public  void propertyChange(PropertyChangeEvent evt) {
                                if (evt.getNewValue() == SwingWorker.StateValue.STARTED) {
                                    tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 2);
                                    run.setEnabled(false);
                                    advancedOptionsButton.setEnabled(false);
                                    //stopButton.setEnabled(true);
                                    browseSBML.setEnabled(false);
                                    browseSeries.setEnabled(false);
                                    // Setting visibility
                                    run.setVisible(false);
                                    advancedOptionsButton.setVisible(false);
                                    progressBar.setVisible(true);
                                    stopButton.setVisible(true);
                                    logTextArea.append("Execution: started " + new Date() + "\n");
                                    logTextArea.append("Execution: " + AdvancedOptionsModel.getString() + "\n");
                                    stopButton.setEnabled(true);
                                } else if (evt.getNewValue() == SwingWorker.StateValue.DONE) {
                                    run.setEnabled(true);
                                    advancedOptionsButton.setEnabled(true);
                                    stopButton.setEnabled(false);
                                    progressBar.setValue(100);
                                    //browseSBML.setEnabled(true);
                                    //browseSeries.setEnabled(true);
                                    // Setting visibility
                                    //run.setVisible(true);
                                    //advancedOptionsButton.setVisible(true);
                                    okButton.setVisible(true);
                                    stopButton.setVisible(false);
                                    //progressBar.setVisible(false);
                                    if (isStopped) {
                                        logTextArea.append("Execution: terminated by the user " + new Date() + "\n");
                                    } else {
                                        logTextArea.append("Execution: terminated " + new Date() + "\n");
                                    }
                                }
                            }
                        });

                // Executing background worker
                bgWorker.execute();

                outputListener = new Thread() {

                    @Override
                    public void run() {
                        while(!bgWorker.isDone()) {
                            try {
                                Thread.sleep(1000);
                                OutputParser.parse("model.xml.output");
                                boxTable.updateRows(OutputParser.getBoxes());
                                if(domain.getIntervals().size() == 2) {
                                    plotPanel2D.updateBoxes(OutputParser.getBoxes());
                                }
                                progressBar.setValue((int) (OutputParser.getProgress() * 100));
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                        }
                    }

                };
                outputListener.start();

    		} catch (NumberFormatException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (XMLStreamException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (ParserConfigurationException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (TransformerFactoryConfigurationError e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (TransformerException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (Exception e1) {
                e1.printStackTrace();
            }
        }
	}

    public void killParSyn(int pid) {

        try {
        /*
            String termCode = "#!/bin/bash\n" +
                "function get_children {\n" +
                "\tclist=`pgrep -P $1`\n" +
                "\tplist+=($1)\n" +
                "\tif [ -n \"$clist\" ]\n" +
                "\tthen\n" +
                "\t\tfor p in $clist\n" +
                "\t\tdo\n" +
                "\t\t\tget_children $p\t\t\t\t\n" +
                "\t\tdone\t\n" +
                "\tfi\t\n" +
                "}\n" +
                "\n" +
                "get_children $1\n" +
                "\n" +
                "for ((i=${#plist[@]}-1;i>=0;i--));\n" +
                "do\n" +
                "\tkill -9 ${plist[i]}\n" +
                "done";
            */

            String termCode = "#!/bin/bash\n" +
                    "function get_children {\n" +
                    "clist=`pgrep -P $1`\n" +
                    "plist=\"$plist $1\"\n" +
                    "if [ -n \"$clist\" ]\n" +
                    "then\n" +
                    "    for c in $clist\n" +
                    "    do\n" +
                    "        get_children $c\n" +
                    "    done\n" +
                    "fi\n" +
                    "}\n" +
                    "\n" +
                    "fclist=`pgrep -P $1`\n" +
                    "plist=\"$plist $fclist\"\n" +
                    "kill -9 $1\n" +
                    "for fc in $fclist\n" +
                    "do\n" +
                    "    get_children $fc\n" +
                    "done\n" +
                    "\n" +
                    "for p in $plist\n" +
                    "do\n" +
                    "    kill -9 $p\n" +
                    "done\n";

            String termFilename = "terminate.sh";

            PrintWriter termWriter = null;
            termWriter = new PrintWriter(termFilename, "UTF-8");
            termWriter.print(termCode);
            termWriter.close();

            Runtime exec = Runtime.getRuntime();
            String killCall = "/bin/bash " + termFilename + " " + pid;
            Process kill = exec.exec(killCall);
            Thread.sleep(1000);
            (new File (termFilename)).delete();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
