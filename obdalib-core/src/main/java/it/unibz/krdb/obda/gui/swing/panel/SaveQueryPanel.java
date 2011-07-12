/***
 * Copyright (c) 2008, Mariano Rodriguez-Muro.
 * All rights reserved.
 *
 * The OBDA-API is licensed under the terms of the Lesser General Public
 * License v.3 (see OBDAAPI_LICENSE.txt for details). The components of this
 * work include:
 *
 * a) The OBDA-API developed by the author and licensed under the LGPL; and,
 * b) third-party components licensed under terms that may be different from
 *   those of the LGPL.  Information about such licenses can be found in the
 *   file named OBDAAPI_3DPARTY-LICENSES.txt.
 */

package it.unibz.krdb.obda.gui.swing.panel;

import it.unibz.krdb.obda.queryanswering.QueryController;
import it.unibz.krdb.obda.queryanswering.QueryControllerGroup;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 *
 * @author mariano
 */

public class SaveQueryPanel extends javax.swing.JPanel {

	/**
	 *
	 */
	private static final long	serialVersionUID	= -7101725310389493765L;
	private String NEWGROUP = "New group...";
	private String NOGROUP = "No group...";



	private JDialog parent = null;
	private String query = null;
	private String currentGroup = null;
	private String currentId = null;
	private QueryController queryController = null;

	/**
	 *
	 * @param query
	 * @param parent
	 * @param queryController
	 */
	public SaveQueryPanel(String query, JDialog parent,
			QueryController queryController) {
		this.queryController = queryController;
		currentGroup = null;
		currentId = null;
		initComponents();
		initCombo();
		this.parent = parent;
		this.query = query;

	}

	/**
	 * Creates new form SaveQueryPanel and use the parameters currentGroup and
	 * currentId to initialize the JDialog
	 */
	public SaveQueryPanel(String query, JDialog parent,
			QueryController queryController, String currentGroup,
			String currentId) {

		this.queryController = queryController;
		this.currentGroup = currentGroup;
		this.currentId = currentId;
		initComponents();
		initCombo();
		this.parent = parent;
		this.query = query;

		// GUIUtils.setAntializaing(this, true);
	}

	/**
	 *
	 * @param query
	 * @param parent
	 * @param queryController
	 * @param currentId
	 */
	public SaveQueryPanel(String query, JDialog parent,
			QueryController queryController, String currentId) {

		this.queryController = queryController;
		this.currentGroup = null;
		this.currentId = currentId;
		initComponents();
		initCombo();
		this.parent = parent;
		this.query = query;

		// GUIUtils.setAntializaing(this, true);
	}

	private void initCombo() {
		cmbQueryGroup.removeAllItems();
		cmbQueryGroup.insertItemAt(this.NOGROUP, cmbQueryGroup.getItemCount());
		QueryController qcontroller = queryController;
		Vector<QueryControllerGroup> groups = qcontroller.getGroups();

		for (QueryControllerGroup queryGroup : groups) {
			cmbQueryGroup.insertItemAt(queryGroup.getID(), cmbQueryGroup
					.getItemCount());
		}
		cmbQueryGroup.insertItemAt(this.NEWGROUP, cmbQueryGroup.getItemCount());
		cmbQueryGroup.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				String name = (String) e.getItem();
				if (name.equals(NEWGROUP)) {
					txtGroupName.setEnabled(true);
				} else
					txtGroupName.setEnabled(false);

			}

		});
		if (currentGroup == null) {
			cmbQueryGroup.setSelectedItem(this.NOGROUP);
			txtQueryID.setText(currentId);
		} else if (currentGroup != null) {
			cmbQueryGroup.setSelectedItem(this.currentGroup);
			txtQueryID.setText(currentId);
		}

	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed"
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        lblSaveQuery = new JLabel();
        pnlSaveForm = new JPanel();
        lblDescription = new JLabel();
        lblID = new JLabel();
        lblGroup = new JLabel();
        lblGroupName = new JLabel();
        txtQueryID = new JTextField();
        cmbQueryGroup = new JComboBox();
        txtGroupName = new JTextField();
        pnlCommandButton = new JPanel();
        pnlRightButtonGroup = new JPanel();
        cmdCancel = new JButton();
        cmdCreateNew = new JButton();
        pnlLeftButtonGroup = new JPanel();
        cmdOverwrite = new JButton();

        setMinimumSize(new Dimension(320, 185));
        setPreferredSize(new Dimension(320, 185));
        setLayout(new BorderLayout());

        lblSaveQuery.setBackground(new Color(153, 0, 0));
        lblSaveQuery.setFont(new Font("Arial", 1, 11));
        lblSaveQuery.setForeground(new Color(255, 255, 255));
        lblSaveQuery.setHorizontalAlignment(SwingConstants.LEFT);
        lblSaveQuery.setText("  SAVE QUERY ");
        lblSaveQuery.setAlignmentX(10.0F);
        lblSaveQuery.setFocusable(false);
        lblSaveQuery.setHorizontalTextPosition(SwingConstants.LEFT);
        lblSaveQuery.setIconTextGap(10);
        lblSaveQuery.setMinimumSize(new Dimension(25, 25));
        lblSaveQuery.setOpaque(true);
        lblSaveQuery.setPreferredSize(new Dimension(121, 20));
        add(lblSaveQuery, BorderLayout.NORTH);

        pnlSaveForm.setMinimumSize(new Dimension(340, 140));
        pnlSaveForm.setPreferredSize(new Dimension(340, 140));
        pnlSaveForm.setLayout(new GridBagLayout());

        lblDescription.setFont(new Font("Arial", 0, 12));
        lblDescription.setText("<html>Insert an ID and group  for this query.</html>");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10, 0, 10, 0);
        pnlSaveForm.add(lblDescription, gridBagConstraints);

        lblID.setFont(new Font("Arial", 0, 11));
        lblID.setText("ID:");
        lblID.setPreferredSize(new Dimension(100, 20));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.insets = new Insets(0, 0, 4, 0);
        pnlSaveForm.add(lblID, gridBagConstraints);

        lblGroup.setFont(new Font("Arial", 0, 11));
        lblGroup.setText("Group");
        lblGroup.setPreferredSize(new Dimension(100, 20));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.insets = new Insets(0, 0, 4, 0);
        pnlSaveForm.add(lblGroup, gridBagConstraints);

        lblGroupName.setFont(new Font("Arial", 0, 11));
        lblGroupName.setText("Group Name:");
        lblGroupName.setPreferredSize(new Dimension(100, 20));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.insets = new Insets(0, 0, 4, 0);
        pnlSaveForm.add(lblGroupName, gridBagConstraints);

        txtQueryID.setMinimumSize(new Dimension(150, 22));
        txtQueryID.setPreferredSize(new Dimension(170, 22));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(0, 0, 4, 0);
        pnlSaveForm.add(txtQueryID, gridBagConstraints);

        cmbQueryGroup.setModel(new DefaultComboBoxModel(new String[] { "No group..." }));
        cmbQueryGroup.setPreferredSize(new Dimension(140, 27));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        pnlSaveForm.add(cmbQueryGroup, gridBagConstraints);

        txtGroupName.setEnabled(false);
        txtGroupName.setMinimumSize(new Dimension(150, 22));
        txtGroupName.setPreferredSize(new Dimension(170, 22));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new Insets(0, 0, 4, 0);
        pnlSaveForm.add(txtGroupName, gridBagConstraints);

        add(pnlSaveForm, BorderLayout.CENTER);

        pnlCommandButton.setMinimumSize(new Dimension(340, 35));
        pnlCommandButton.setPreferredSize(new Dimension(340, 35));
        pnlCommandButton.setLayout(new BorderLayout());

        pnlRightButtonGroup.setMinimumSize(new Dimension(220, 40));
        pnlRightButtonGroup.setPreferredSize(new Dimension(220, 40));
        pnlRightButtonGroup.setLayout(new FlowLayout(FlowLayout.RIGHT));

        cmdCancel.setText("Cancel");
        cmdCancel.setBorder(BorderFactory.createEtchedBorder());
        cmdCancel.setMaximumSize(new Dimension(65, 23));
        cmdCancel.setMinimumSize(new Dimension(65, 23));
        cmdCancel.setOpaque(true);
        cmdCancel.setPreferredSize(new Dimension(65, 23));
        cmdCancel.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent evt) {
            cmdCancelActionPerformed(evt);
          }
      });

        pnlRightButtonGroup.add(cmdCancel);

        cmdCreateNew.setBorder(BorderFactory.createEtchedBorder());
        cmdCreateNew.setText("Create New");
        cmdCreateNew.setMaximumSize(new Dimension(95, 23));
        cmdCreateNew.setMinimumSize(new Dimension(95, 23));
        cmdCreateNew.setPreferredSize(new Dimension(95, 23));
        cmdCreateNew.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                cmdCreateNewActionPerformed(evt);
            }
        });
        pnlRightButtonGroup.add(cmdCreateNew);

        pnlCommandButton.add(pnlRightButtonGroup, BorderLayout.EAST);

        pnlLeftButtonGroup.setMinimumSize(new Dimension(114, 40));
        pnlLeftButtonGroup.setPreferredSize(new Dimension(114, 40));
        pnlLeftButtonGroup.setLayout(new FlowLayout(FlowLayout.LEFT));

        cmdOverwrite.setText("Overwrite");
        cmdOverwrite.setBorder(BorderFactory.createEtchedBorder());
        cmdOverwrite.setMaximumSize(new Dimension(85, 23));
        cmdOverwrite.setMinimumSize(new Dimension(85, 23));
        cmdOverwrite.setPreferredSize(new Dimension(85, 23));
        cmdOverwrite.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                cmdOverwriteActionPerformed(evt);
            }
        });
        pnlLeftButtonGroup.add(cmdOverwrite);

        pnlCommandButton.add(pnlLeftButtonGroup, BorderLayout.WEST);

        add(pnlCommandButton, BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    private void cmdOverwriteActionPerformed(ActionEvent evt) {//GEN-FIRST:event_cmdCreateNewActionPerformed
      String id = txtQueryID.getText();
      String group = (String)cmbQueryGroup.getSelectedItem();

      if (group.equals(NOGROUP) || group.equals(NEWGROUP)) { // for query without group
        queryController.addQuery(this.query, id);
      }
      else {
        queryController.addQuery(this.query, id, group);
      }

      parent.dispose();
    }//GEN-LAST:event_cmdCreateNewActionPerformed

	private void cmdCreateNewActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_buttonAcceptActionPerformed
		String id = txtQueryID.getText();
		if (id.isEmpty()) {
		  JOptionPane.showMessageDialog(this, "The query ID can't be blank!", "Error", JOptionPane.ERROR_MESSAGE);
		  return;
		}

		String group = (String)cmbQueryGroup.getSelectedItem();

		boolean newgroup = false;
		if (group.equals(NOGROUP)) {
			group = null;
		}
		else if (group.equals(NEWGROUP)) {
			newgroup = true;
			group = txtGroupName.getText();
		}

		int index = queryController.getElementPosition(id);

		if (index != -1) {
			JOptionPane.showMessageDialog(null, "Query/Group with the same ID already exists.\nIDs must be unique. Please modify and try again.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (group == null) {
		  queryController.addQuery(this.query, id);
		}

		if (newgroup) {
		  queryController.createGroup(group);
			if (id != null) {
			  queryController.addQuery(this.query, id, group);
			}
		}
		else {
		  queryController.addQuery(this.query, id, group);
		}

		parent.dispose();
	}// GEN-LAST:event_buttonAcceptActionPerformed

	private void cmdCancelActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_buttonCancelActionPerformed
		parent.dispose();
	}// GEN-LAST:event_buttonCancelActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JComboBox cmbQueryGroup;
    private JButton cmdCancel;
    private JButton cmdCreateNew;
    private JButton cmdOverwrite;
    private JLabel lblDescription;
    private JLabel lblGroup;
    private JLabel lblGroupName;
    private JLabel lblID;
    private JLabel lblSaveQuery;
    private JPanel pnlCommandButton;
    private JPanel pnlLeftButtonGroup;
    private JPanel pnlRightButtonGroup;
    private JPanel pnlSaveForm;
    private JTextField txtGroupName;
    private JTextField txtQueryID;
    // End of variables declaration//GEN-END:variables

}
