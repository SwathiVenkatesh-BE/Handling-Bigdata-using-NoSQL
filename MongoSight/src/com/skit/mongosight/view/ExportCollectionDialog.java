/*
 * @Author
 * Ashok Kumar K
 * AKLC
 * celestialcluster@gmail.com | 9742024066  
 */
package com.skit.mongosight.view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import com.mongodb.DBCursor;
import com.mongodb.MongoClient;
import com.skit.mongosight.domain.Collection;
import com.skit.mongosight.domain.Connections;
import com.skit.mongosight.domain.Database;
import com.skit.mongosight.domain.Host;
import com.skit.mongosight.utils.MongoUtils;
import com.skit.mongosight.utils.UIUtils;

@SuppressWarnings("serial")
public class ExportCollectionDialog extends JDialog implements ActionListener, ItemListener {

	private static final int DIALOG_WIDTH = 640;
	
	private static final int DIALOG_HEIGHT = 480;
	
	private JLabel srcHostLabel;
	
	private JComboBox<Host> srcHost;
	
	private JLabel srcDatabaseLabel;
	
	private JComboBox<Database> srcDatabase;
	
	private JLabel srcCollectionLabel;
	
	private JComboBox<Collection> srcCollection;
	
	private JLabel tgtHostLabel;
	
	private JComboBox<Host> tgtHost;
	
	private JLabel tgtDatabaseLabel;
	
	private JComboBox<Database> tgtDatabase;
	
	private JLabel tgtCollectionLabel;
	
	private JComboBox<Collection> tgtCollection;
	
	private JCheckBox tgtCreateNewCollection;
	
	private JTextField tgtNewCollection;
	
	private JButton cancel;
	
	private JButton ok;

    private ExecutorService es = Executors.newSingleThreadExecutor();

	public ExportCollectionDialog(JFrame parent, Connections connections, Collection collection) {
		super(parent);
		setTitle("Export Collection");
		
		setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
		
		int x = parent.getX() + parent.getWidth() / 2 - DIALOG_WIDTH / 2;
		int y = parent.getY() + parent.getHeight() / 2 - DIALOG_HEIGHT / 2;
		setLocation(x, y);
		
		setModal(true);
	
		setLayout(new BorderLayout());
		
		add(createContentPanel(), BorderLayout.CENTER);
		add(createButtonsPanel(), BorderLayout.SOUTH);
		
		// add hosts
		for (Host host : connections.getHosts()) {
			srcHost.addItem(host);
			tgtHost.addItem(host);
		}
		
		// set selected host, database, and collection
		srcHost.setSelectedItem(collection.getDatabase().getHost());
		srcDatabase.setSelectedItem(collection.getDatabase());
		srcCollection.setSelectedItem(collection);
		
		setVisible(true);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if ("cancel".equals(e.getActionCommand())) {
			dispose();
		}
		if ("ok".equals(e.getActionCommand())) {
			if (srcCollection.getSelectedItem() == null) {
				UIUtils.error(this, "Please select the source collection");
				return;
			}
			if (tgtCollection.getSelectedItem() == null) {
				UIUtils.error(this, "Please select the target collection");
				return;
			}
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					ok.setEnabled(false);
					int result = handleExportCollection((Collection) srcCollection.getSelectedItem(), (Collection) tgtCollection.getSelectedItem());
					ok.setEnabled(true);
					if (result != -1) {
						UIUtils.info(ExportCollectionDialog.this, result + " document(s) exported");
						dispose();
					}
				}
			};
			es.submit(runnable);
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == srcHost) {
			Host host = (Host) e.getItem();
			srcDatabase.removeAllItems();
			srcCollection.removeAllItems();
			for (Database database : host.getDatabases()) {
				srcDatabase.addItem(database);
			}
		}
		if (e.getSource() == srcDatabase) {
			Database database = (Database) e.getItem();
			srcCollection.removeAllItems();
			for (Collection collection : database.getCollections()) {
				srcCollection.addItem(collection);
			}
		}
		if (e.getSource() == tgtHost) {
			Host host = (Host) e.getItem();
			tgtDatabase.removeAllItems();
			tgtCollection.removeAllItems();
			for (Database database : host.getDatabases()) {
				tgtDatabase.addItem(database);
			}
		}
		if (e.getSource() == tgtDatabase) {
			Database database = (Database) e.getItem();
			tgtCollection.removeAllItems();
			for (Collection collection : database.getCollections()) {
				tgtCollection.addItem(collection);
			}
		}
	}
	
	// *********
	// private
	// *********
	
	private JPanel createContentPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(1, 2));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		JPanel source = new JPanel();
		source.setBorder(BorderFactory.createTitledBorder("Source"));
		srcHostLabel = new JLabel("Host:", UIUtils.icon("resources/small/host.png"), SwingConstants.LEFT);
		source.add(srcHostLabel);
		srcHost = new JComboBox<Host>();
		srcHost.addItemListener(this);
		source.add(srcHost);
		srcDatabaseLabel = new JLabel("Database:", UIUtils.icon("resources/small/database.png"), SwingConstants.LEFT);
		source.add(srcDatabaseLabel);
		srcDatabase = new JComboBox<Database>();
		srcDatabase.addItemListener(this);
		source.add(srcDatabase);
		srcCollectionLabel = new JLabel("Collection:", UIUtils.icon("resources/small/collection.png"), SwingConstants.LEFT);
		source.add(srcCollectionLabel);
		srcCollection = new JComboBox<Collection>();
		srcCollection.addItemListener(this);
		source.add(srcCollection);
		SpringLayout slSource = new SpringLayout();
		slSource.putConstraint(SpringLayout.WEST, srcHostLabel, 10, SpringLayout.WEST, source);
		slSource.putConstraint(SpringLayout.NORTH, srcHostLabel, 10, SpringLayout.NORTH, source);
		slSource.putConstraint(SpringLayout.WEST, srcHost, 10, SpringLayout.WEST, source);
		slSource.putConstraint(SpringLayout.NORTH, srcHost, 10, SpringLayout.SOUTH, srcHostLabel);
		slSource.putConstraint(SpringLayout.WEST, srcDatabaseLabel, 10, SpringLayout.WEST, source);
		slSource.putConstraint(SpringLayout.NORTH, srcDatabaseLabel, 10, SpringLayout.SOUTH, srcHost);
		slSource.putConstraint(SpringLayout.WEST, srcDatabase, 10, SpringLayout.WEST, source);
		slSource.putConstraint(SpringLayout.NORTH, srcDatabase, 10, SpringLayout.SOUTH, srcDatabaseLabel);
		slSource.putConstraint(SpringLayout.WEST, srcCollectionLabel, 10, SpringLayout.WEST, source);
		slSource.putConstraint(SpringLayout.NORTH, srcCollectionLabel, 10, SpringLayout.SOUTH, srcDatabase);
		slSource.putConstraint(SpringLayout.WEST, srcCollection, 10, SpringLayout.WEST, source);
		slSource.putConstraint(SpringLayout.NORTH, srcCollection, 10, SpringLayout.SOUTH, srcCollectionLabel);
		source.setLayout(slSource);

		JPanel target = new JPanel();
		target.setBorder(BorderFactory.createTitledBorder("Target"));
		tgtHostLabel = new JLabel("Host:", UIUtils.icon("resources/small/host.png"), SwingConstants.LEFT);
		target.add(tgtHostLabel);
		tgtHost = new JComboBox<Host>();
		tgtHost.addItemListener(this);
		target.add(tgtHost);
		tgtDatabaseLabel = new JLabel("Database:", UIUtils.icon("resources/small/database.png"), SwingConstants.LEFT);
		target.add(tgtDatabaseLabel);
		tgtDatabase = new JComboBox<Database>();
		tgtDatabase.addItemListener(this);
		target.add(tgtDatabase);
		tgtCollectionLabel = new JLabel("Collection:", UIUtils.icon("resources/small/collection.png"), SwingConstants.LEFT);
		target.add(tgtCollectionLabel);
		tgtCollection = new JComboBox<Collection>();
		tgtCollection.addItemListener(this);
		target.add(tgtCollection);
		tgtCreateNewCollection = new JCheckBox("Create new collection");
		target.add(tgtCreateNewCollection);
		tgtNewCollection = new JTextField("Name", 16);
		tgtNewCollection.setEnabled(false);
		target.add(tgtNewCollection);
		SpringLayout slTarget = new SpringLayout();
		slTarget.putConstraint(SpringLayout.WEST, tgtHostLabel, 10, SpringLayout.WEST, target);
		slTarget.putConstraint(SpringLayout.NORTH, tgtHostLabel, 10, SpringLayout.NORTH, target);
		slTarget.putConstraint(SpringLayout.WEST, tgtHost, 10, SpringLayout.WEST, target);
		slTarget.putConstraint(SpringLayout.NORTH, tgtHost, 10, SpringLayout.SOUTH, tgtHostLabel);
		slTarget.putConstraint(SpringLayout.WEST, tgtDatabaseLabel, 10, SpringLayout.WEST, target);
		slTarget.putConstraint(SpringLayout.NORTH, tgtDatabaseLabel, 10, SpringLayout.SOUTH, tgtHost);
		slTarget.putConstraint(SpringLayout.WEST, tgtDatabase, 10, SpringLayout.WEST, target);
		slTarget.putConstraint(SpringLayout.NORTH, tgtDatabase, 10, SpringLayout.SOUTH, tgtDatabaseLabel);
		slTarget.putConstraint(SpringLayout.WEST, tgtCollectionLabel, 10, SpringLayout.WEST, target);
		slTarget.putConstraint(SpringLayout.NORTH, tgtCollectionLabel, 10, SpringLayout.SOUTH, tgtDatabase);
		slTarget.putConstraint(SpringLayout.WEST, tgtCollection, 10, SpringLayout.WEST, target);
		slTarget.putConstraint(SpringLayout.NORTH, tgtCollection, 10, SpringLayout.SOUTH, tgtCollectionLabel);
		slTarget.putConstraint(SpringLayout.WEST, tgtCreateNewCollection, 10, SpringLayout.WEST, target);
		slTarget.putConstraint(SpringLayout.NORTH, tgtCreateNewCollection, 10, SpringLayout.SOUTH, tgtCollection);
		slTarget.putConstraint(SpringLayout.WEST, tgtNewCollection, 10, SpringLayout.WEST, target);
		slTarget.putConstraint(SpringLayout.NORTH, tgtNewCollection, 10, SpringLayout.SOUTH, tgtCreateNewCollection);
		
		target.setLayout(slTarget);
		
		panel.add(source);
		panel.add(target);
		
		tgtCreateNewCollection.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (tgtCreateNewCollection.isSelected()) {
					tgtNewCollection.setText("");
					tgtNewCollection.setEnabled(true);
					tgtCollection.setEnabled(false);
				} else {
					tgtNewCollection.setText("Name");
					tgtNewCollection.setEnabled(false);
					tgtCollection.setEnabled(true);
				}
			}
		});
		
		return (panel);
	}
	
	private JPanel createButtonsPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		cancel = new JButton("Cancel");
		cancel.setActionCommand("cancel");
		cancel.addActionListener(this);
		panel.add(cancel);
		ok = new JButton("Ok");
		ok.setActionCommand("ok");
		ok.addActionListener(this);
		panel.add(ok);
		return (panel);
	}
	
	private int handleExportCollection(Collection source, Collection target) {
		MongoClient sourceClient = null;
		MongoClient targetClient = null;
		try {
			sourceClient = MongoUtils.getMongoClient(source.getDatabase().getHost());
			targetClient = MongoUtils.getMongoClient(target.getDatabase().getHost());
			String targetCollectionName = target.getName();
			if (tgtCreateNewCollection.isSelected()) {
				targetCollectionName = tgtNewCollection.getText();
				if (targetCollectionName == null || targetCollectionName.isEmpty()) {
					UIUtils.error(this, "Please enter the new collection name");
					return (-1);
				}
 			}
			DBCursor cursor = sourceClient.getDB(source.getDatabase().getName()).getCollection(source.getName()).find();
			int count = 0;
			while (cursor.hasNext()) {
				targetClient.getDB(target.getDatabase().getName()).getCollection(targetCollectionName).insert(cursor.next());
				count++;
			}
			return (count);
		} catch (Exception e) {
			new ErrorDialog(this, e);
			return (-1);
		} finally {
			if (sourceClient != null) {
				sourceClient.close();
			}
			if (targetClient != null) {
				targetClient.close();
			}
		}
	}
	
}
